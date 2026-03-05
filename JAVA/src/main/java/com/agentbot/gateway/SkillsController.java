package com.agentbot.gateway;

import com.agentbot.core.agent.AgentConfig;
import com.agentbot.core.agent.AgentInstance;
import com.agentbot.core.agent.AgentRegistry;
import com.agentbot.core.skills.Skill;
import com.agentbot.core.skills.SkillLoader;
import com.agentbot.core.skills.SkillStoreService;
import com.agentbot.gateway.dto.SkillStatusItem;
import com.agentbot.gateway.dto.SkillStatusResponse;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/api/skills")
public class SkillsController {

    private static final Logger log = LoggerFactory.getLogger(SkillsController.class);
    private static final List<String> WINDOWS_EXT = List.of(".exe", ".cmd", ".bat");
    private final AgentRegistry agentRegistry;
    private final SkillLoader systemSkillLoader;
    private final SkillStoreService skillStoreService;

    public SkillsController(AgentRegistry agentRegistry, SkillLoader systemSkillLoader, SkillStoreService skillStoreService) {
        this.agentRegistry = agentRegistry;
        this.systemSkillLoader = systemSkillLoader;
        this.skillStoreService = skillStoreService;
    }


    /**
     * Aggregate skill status for a given agent. If agentId 为空/缺省，则读取 workspace/skills 下的系统技能返回。
     */
    @GetMapping("/status")
    public ResponseEntity<SkillStatusResponse> status(@RequestParam(value = "agentId", required = false) String agentId) {
        if (agentId == null || agentId.isBlank()) {
            List<SkillStatusItem> available = buildSkillStatusItems(systemSkillLoader.loadSkills(), Map.of(), "system");
            SkillStatusResponse resp = new SkillStatusResponse(
                    available,
                    Map.of(),
                    true,
                    null
            );
            return ResponseEntity.ok(resp);
        }

        AgentInstance instance = agentRegistry.getAgent(agentId);
        if (instance == null) {
            return ResponseEntity.badRequest().build();
        }

        AgentConfig config = instance.getConfig();
        Map<String, AgentConfig.SkillEntryConfig> entries = config.getCapabilities().getSkills().getEntries();
        if (entries == null) {
            entries = new HashMap<>();
        }

        List<SkillStatusItem> available = buildSkillStatusItems(instance.getLoadedSkills(), entries, "loaded");

        SkillStatusResponse resp = new SkillStatusResponse(
                available,
                entries,
                config.getCapabilities().getSkills().isInherited(),
                config.getCapabilities().getSkills().getCustomPath()
        );
        return ResponseEntity.ok(resp);
    }

    /**
     * Install trigger executed server-side.
     */
    @PostMapping("/install")
    public ResponseEntity<Map<String, Object>> install(@RequestParam("agentId") String agentId,
                                                       @RequestParam("name") String name,
                                                       @RequestParam(value = "installId", required = false) String installId,
                                                       @RequestParam(value = "timeout", required = false, defaultValue = "300") long timeoutSeconds) {
        AgentInstance instance = agentRegistry.getAgent(agentId);
        if (instance == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "agent not found"));
        }

        Skill skill = findSkillByName(instance.getLoadedSkills(), name)
                .orElseGet(() -> findSkillByName(systemSkillLoader.loadSkills(), name).orElse(null));
        if (skill == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "skill not found"));
        }

        Map<String, Object> openclaw = extractOpenclawMeta(skill.getMetadata());
        List<SkillStatusItem.SkillInstallOption> options = parseInstallOptions(openclaw);
        if (options.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "no install options"));
        }

        SkillStatusItem.SkillInstallOption option = options.get(0);
        if (installId != null && !installId.isBlank()) {
            for (SkillStatusItem.SkillInstallOption candidate : options) {
                if (installId.equals(candidate.getId())) {
                    option = candidate;
                    break;
                }
            }
        }

        InstallResult result = executeInstall(option, timeoutSeconds);
        if (!result.ok) {
            return ResponseEntity.ok(Map.of(
                    "ok", false,
                    "message", result.message,
                    "skill", name,
                    "agentId", agentId,
                    "installId", option.getId()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", result.message,
                "skill", name,
                "agentId", agentId,
                "installId", option.getId()
        ));
    }

    @GetMapping("/detail")
    public ResponseEntity<SkillDetailResponse> detail(@RequestParam("name") String name,
                                                      @RequestParam(value = "agentId", required = false) String agentId) {
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        SkillDetailResponse response = buildDetailResponse(name, agentId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }


    private List<SkillStatusItem> buildSkillStatusItems(List<Skill> loaded,
                                                        Map<String, AgentConfig.SkillEntryConfig> entries,
                                                        String source) {
        List<SkillStatusItem> available = new ArrayList<>();
        if (loaded == null) {
            return available;
        }
        for (Skill s : loaded) {
            AgentConfig.SkillEntryConfig entry = entries.getOrDefault(s.getName(), new AgentConfig.SkillEntryConfig());
            SkillStatusItem item = new SkillStatusItem(s.getName(), s.getDescription(), source, false, s.getMetadata());

            Map<String, Object> openclaw = extractOpenclawMeta(s.getMetadata());
            SkillStatusItem.SkillRequirement requirements = buildRequirements(openclaw);
            String primaryEnv = readString(openclaw.get("primaryEnv"));
            List<SkillStatusItem.SkillConfigCheck> configChecks = buildConfigChecks(openclaw);
            SkillStatusItem.SkillMissing missing = computeMissing(requirements, entry, primaryEnv, configChecks);
            List<SkillStatusItem.SkillInstallOption> installOptions = parseInstallOptions(openclaw);

            boolean blockedByAllowlist = isBlockedByAllowlist(openclaw, s.getName(), source);
            boolean blocked = blockedByAllowlist || hasMissing(missing);
            boolean eligible = !blocked;
            boolean installable = !missing.getBins().isEmpty() && !installOptions.isEmpty();

            item.setRequirements(requirements);
            item.setPrimaryEnv(primaryEnv);
            item.setMissing(missing);
            item.setConfigChecks(configChecks);
            item.setInstall(installOptions);
            item.setBlocked(blocked);
            item.setEligible(eligible);
            item.setBlockedByAllowlist(blockedByAllowlist);
            item.setInstallable(installable);

            available.add(item);
        }
        return available;
    }

    private Map<String, Object> extractOpenclawMeta(Map<String, Object> metadata) {
        if (metadata == null) return Map.of();
        Object openclaw = metadata.get("openclaw");
        Map<String, Object> openclawMap = asMap(openclaw);
        if (!openclawMap.isEmpty()) {
            return openclawMap;
        }
        Object metaWrapper = metadata.get("metadata");
        Map<String, Object> wrapper = asMap(metaWrapper);
        if (!wrapper.isEmpty()) {
            Object nested = wrapper.get("openclaw");
            Map<String, Object> nestedMap = asMap(nested);
            if (!nestedMap.isEmpty()) {
                return nestedMap;
            }
        }
        return Map.of();
    }

    private SkillStatusItem.SkillRequirement buildRequirements(Map<String, Object> openclaw) {
        SkillStatusItem.SkillRequirement req = new SkillStatusItem.SkillRequirement();
        if (openclaw == null || openclaw.isEmpty()) return req;
        Object requiresObj = openclaw.get("requires");
        if (requiresObj instanceof Map<?, ?> requires) {
            req.setBins(readStringList(requires.get("bins")));
            req.setAnyBins(readStringList(requires.get("anyBins")));
            req.setEnv(readStringList(requires.get("env")));
            req.setConfig(readStringList(requires.get("config")));
            req.setOs(readStringList(requires.get("os")));
        }
        return req;
    }

    private List<SkillStatusItem.SkillConfigCheck> buildConfigChecks(Map<String, Object> openclaw) {
        List<SkillStatusItem.SkillConfigCheck> checks = new ArrayList<>();
        if (openclaw == null || openclaw.isEmpty()) return checks;

        List<Object> configCandidates = new ArrayList<>();
        Object requiresObj = openclaw.get("requires");
        if (requiresObj instanceof Map<?, ?> requires) {
            Object configObj = requires.get("config");
            configCandidates.addAll(asList(configObj));
        }
        Object configChecksObj = openclaw.get("configChecks");
        configCandidates.addAll(asList(configChecksObj));

        for (Object candidate : configCandidates) {
            if (candidate instanceof Map<?, ?> map) {
                String path = readString(map.get("path"));
                String value = readString(map.get("value"));
                boolean satisfied = checkConfig(path, value);
                checks.add(new SkillStatusItem.SkillConfigCheck(path, value, satisfied));
            } else if (candidate instanceof String str) {
                boolean satisfied = checkConfig(str, null);
                checks.add(new SkillStatusItem.SkillConfigCheck(str, null, satisfied));
            }
        }
        return checks;
    }

    private SkillStatusItem.SkillMissing computeMissing(SkillStatusItem.SkillRequirement requirements,
                                                       AgentConfig.SkillEntryConfig entry,
                                                       String primaryEnv,
                                                       List<SkillStatusItem.SkillConfigCheck> configChecks) {
        SkillStatusItem.SkillMissing missing = new SkillStatusItem.SkillMissing();
        if (requirements == null) {
            return missing;
        }

        for (String bin : requirements.getBins()) {
            if (!binaryExists(bin)) {
                missing.getBins().add(bin);
            }
        }

        List<String> anyBins = requirements.getAnyBins();
        if (!anyBins.isEmpty()) {
            boolean anyFound = false;
            for (String bin : anyBins) {
                if (binaryExists(bin)) {
                    anyFound = true;
                    break;
                }
            }
            if (!anyFound) {
                missing.getAnyBins().addAll(anyBins);
            }
        }

        List<String> envKeys = new ArrayList<>(requirements.getEnv());
        if (primaryEnv != null && !primaryEnv.isBlank() && !envKeys.contains(primaryEnv)) {
            envKeys.add(primaryEnv);
        }
        for (String envKey : envKeys) {
            if (!hasEnvValue(envKey, entry, primaryEnv)) {
                missing.getEnv().add(envKey);
            }
        }

        if (configChecks != null) {
            for (SkillStatusItem.SkillConfigCheck check : configChecks) {
                if (!check.isSatisfied()) {
                    String label = check.getPath();
                    if (check.getValue() != null && !check.getValue().isBlank()) {
                        label = check.getPath() + "=" + check.getValue();
                    }
                    missing.getConfig().add(label);
                }
            }
        }

        List<String> osRequired = requirements.getOs();
        if (!osRequired.isEmpty()) {
            String current = normalizeOs(System.getProperty("os.name"));
            boolean matches = osRequired.stream().anyMatch(req -> normalizeOs(req).equals(current));
            if (!matches) {
                missing.getOs().addAll(osRequired);
            }
        }

        return missing;
    }

    private boolean hasMissing(SkillStatusItem.SkillMissing missing) {
        if (missing == null) return false;
        return !(missing.getBins().isEmpty()
                && missing.getAnyBins().isEmpty()
                && missing.getEnv().isEmpty()
                && missing.getConfig().isEmpty()
                && missing.getOs().isEmpty());
    }

    private boolean isBlockedByAllowlist(Map<String, Object> openclaw, String skillName, String source) {
        if (openclaw == null || openclaw.isEmpty()) return false;
        List<String> allowlist = readStringList(openclaw.get("allowlist"));
        if (!allowlist.isEmpty() && !allowlist.contains(skillName)) {
            return true;
        }
        boolean allowBundled = readBoolean(openclaw.get("allowBundled"), true);
        boolean bundled = readBoolean(openclaw.get("bundled"), "system".equalsIgnoreCase(source));
        return !allowBundled && bundled;
    }

    private List<SkillStatusItem.SkillInstallOption> parseInstallOptions(Map<String, Object> openclaw) {
        if (openclaw == null || openclaw.isEmpty()) return Collections.emptyList();
        Object installObj = openclaw.get("install");
        if (!(installObj instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<SkillStatusItem.SkillInstallOption> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) continue;
            SkillStatusItem.SkillInstallOption option = new SkillStatusItem.SkillInstallOption();
            option.setId(readString(map.get("id")));
            option.setKind(readString(map.get("kind")));
            option.setLabel(readString(map.get("label")));
            option.setBins(readStringList(map.get("bins")));
            option.setPackageName(readString(map.get("package")));
            option.setFormula(readString(map.get("formula")));
            option.setUrl(readString(map.get("url")));
            option.setCommand(readString(map.get("command")));
            option.setTarget(readString(map.get("target")));
            result.add(option);
        }
        return result;
    }

    private Optional<Skill> findSkillByName(List<Skill> skills, String name) {
        if (skills == null) return Optional.empty();
        return skills.stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst();
    }

    private SkillDetailResponse buildDetailResponse(String name, String agentId) {
        SkillStoreService.SkillDetail detail = skillStoreService == null ? null : skillStoreService.loadLocalSkillDetailByName(name);
        String content = detail == null ? null : detail.getContent();
        String source = detail == null || detail.getSkill() == null ? null : detail.getSkill().getOrigin();
        String description = detail == null || detail.getSkill() == null ? null : detail.getSkill().getDescription();

        Skill resolved = resolveLoadedSkill(name, agentId);
        if (resolved != null) {
            if (description == null || description.isBlank()) {
                description = resolved.getDescription();
            }
            if (content == null || content.isBlank()) {
                content = resolved.getContent();
            }
        }

        if ((content == null || content.isBlank()) && detail == null && resolved == null) {
            return null;
        }

        SkillDetailInfo info = new SkillDetailInfo(name, description, source == null ? "unknown" : source);
        return new SkillDetailResponse(info, content == null ? "" : content);
    }

    private Skill resolveLoadedSkill(String name, String agentId) {
        if (name == null || name.isBlank()) return null;
        if (agentId == null || agentId.isBlank()) {
            return findSkillByName(systemSkillLoader.loadSkills(), name).orElse(null);
        }
        AgentInstance instance = agentRegistry.getAgent(agentId);
        if (instance == null) return null;
        return findSkillByName(instance.getLoadedSkills(), name).orElse(null);
    }

    public record SkillDetailInfo(String name, String description, String source) {}

    public record SkillDetailResponse(SkillDetailInfo skill, String content) {}


    private InstallResult executeInstall(SkillStatusItem.SkillInstallOption option, long timeoutSeconds) {
        if (option == null || option.getKind() == null) {
            return new InstallResult(false, "invalid install option");
        }
        String kind = option.getKind().toLowerCase();
        try {
            return switch (kind) {
                case "brew" -> execCommand(List.of("brew", "install", resolvePackage(option)), timeoutSeconds);
                case "apt" -> execCommand(List.of("apt-get", "install", "-y", resolvePackage(option)), timeoutSeconds);
                case "node" -> execCommand(List.of("npm", "i", "-g", resolvePackage(option)), timeoutSeconds);
                case "go" -> execCommand(List.of("go", "install", resolvePackage(option)), timeoutSeconds);
                case "uv" -> execCommand(List.of("uv", "tool", "install", resolvePackage(option)), timeoutSeconds);
                case "download" -> downloadFile(option, timeoutSeconds);
                default -> option.getCommand() != null
                        ? execCommand(splitCommand(option.getCommand()), timeoutSeconds)
                        : new InstallResult(false, "unsupported install kind: " + kind);
            };
        } catch (Exception e) {
            log.error("Install failed", e);
            return new InstallResult(false, e.getMessage());
        }
    }

    private List<String> splitCommand(String command) {
        if (command == null || command.isBlank()) return List.of();
        return List.of(command.trim().split("\\s+"));
    }

    private String resolvePackage(SkillStatusItem.SkillInstallOption option) {
        if (option.getFormula() != null && !option.getFormula().isBlank()) return option.getFormula();
        if (option.getPackageName() != null && !option.getPackageName().isBlank()) return option.getPackageName();
        if (option.getId() != null && !option.getId().isBlank()) return option.getId();
        return "";
    }

    private InstallResult downloadFile(SkillStatusItem.SkillInstallOption option, long timeoutSeconds) throws IOException, InterruptedException {
        if (option.getUrl() == null || option.getUrl().isBlank()) {
            return new InstallResult(false, "missing download url");
        }
        Path targetPath = resolveDownloadTarget(option);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(option.getUrl())).GET().build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 400) {
            return new InstallResult(false, "download failed: HTTP " + response.statusCode());
        }
        Files.createDirectories(targetPath.getParent());
        Files.write(targetPath, response.body());
        return new InstallResult(true, "downloaded to " + targetPath);
    }

    private Path resolveDownloadTarget(SkillStatusItem.SkillInstallOption option) {
        if (option.getTarget() != null && !option.getTarget().isBlank()) {
            return Paths.get(option.getTarget());
        }
        String fileName = "skill-download";
        if (option.getUrl() != null) {
            String url = option.getUrl();
            int idx = url.lastIndexOf('/');
            if (idx >= 0 && idx < url.length() - 1) {
                fileName = url.substring(idx + 1);
            }
        }
        return Paths.get(System.getProperty("java.io.tmpdir"), fileName);
    }

    private InstallResult execCommand(List<String> command, long timeoutSeconds) throws IOException, InterruptedException {
        if (command == null || command.isEmpty()) {
            return new InstallResult(false, "empty command");
        }
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        String output = readOutput(process.getInputStream());
        if (!finished) {
            process.destroyForcibly();
            return new InstallResult(false, "install timeout");
        }
        int exit = process.exitValue();
        if (exit != 0) {
            return new InstallResult(false, "install failed: " + output);
        }
        return new InstallResult(true, output.isBlank() ? "install ok" : output);
    }

    private String readOutput(InputStream inputStream) throws IOException {
        if (inputStream == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        inputStream.transferTo(out);
        return out.toString(StandardCharsets.UTF_8);
    }

    private boolean hasEnvValue(String envKey, AgentConfig.SkillEntryConfig entry, String primaryEnv) {
        if (envKey == null || envKey.isBlank()) return true;
        if (entry != null) {
            if (entry.getEnv() != null && entry.getEnv().containsKey(envKey)) {
                String val = entry.getEnv().get(envKey);
                if (val != null && !val.isBlank()) {
                    return true;
                }
            }
            if (primaryEnv != null && primaryEnv.equals(envKey)) {
                String apiKey = entry.getApiKey();
                if (apiKey != null && !apiKey.isBlank()) {
                    return true;
                }
            }
        }
        String systemVal = System.getenv(envKey);
        return systemVal != null && !systemVal.isBlank();
    }

    private boolean binaryExists(String bin) {
        if (bin == null || bin.isBlank()) return true;
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) return false;
        String[] parts = path.split(System.getProperty("path.separator"));
        for (String part : parts) {
            Path base = Paths.get(part);
            if (!Files.exists(base)) continue;
            if (isWindows()) {
                for (String ext : WINDOWS_EXT) {
                    Path candidate = base.resolve(bin + ext);
                    if (Files.exists(candidate)) return true;
                }
            }
            Path candidate = base.resolve(bin);
            if (Files.exists(candidate)) return true;
        }
        return false;
    }

    private boolean checkConfig(String path, String value) {
        if (path == null || path.isBlank()) {
            return true;
        }
        Path configPath = Paths.get(path);
        if (!Files.exists(configPath)) {
            return false;
        }
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            String content = Files.readString(configPath);
            return content.contains(value);
        } catch (IOException e) {
            log.warn("Failed to read config file: {}", path, e);
            return false;
        }
    }

    private String normalizeOs(String os) {
        if (os == null) return "";
        String lower = os.toLowerCase();
        if (lower.contains("win")) return "windows";
        if (lower.contains("mac") || lower.contains("darwin")) return "mac";
        if (lower.contains("nux") || lower.contains("nix")) return "linux";
        return lower;
    }

    private boolean isWindows() {
        return normalizeOs(System.getProperty("os.name")).equals("windows");
    }

    private List<String> readStringList(Object value) {
        if (value == null) return new ArrayList<>();
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item == null) continue;
                result.add(item.toString());
            }
            return result;
        }
        return new ArrayList<>(List.of(value.toString()));
    }

    private String readString(Object value) {
        return value == null ? null : value.toString();
    }

    private boolean readBoolean(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(value.toString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return new HashMap<>((Map<String, Object>) map);
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Object> asList(Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>((List<Object>) list);
        }
        return new ArrayList<>();
    }

    private static class InstallResult {
        private final boolean ok;
        private final String message;

        private InstallResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }
    }
}

