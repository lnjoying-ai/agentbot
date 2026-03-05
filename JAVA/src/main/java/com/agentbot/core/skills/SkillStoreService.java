package com.agentbot.core.skills;

import com.agentbot.core.util.ConfigPathResolver;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class SkillStoreService {
  private static final Logger log = LoggerFactory.getLogger(SkillStoreService.class);
  private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$", Pattern.DOTALL);
  private static final DateTimeFormatter VERSION_FALLBACK = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  private final ObjectMapper yamlMapper = new YAMLMapper();
  private final ObjectMapper jsonMapper = new ObjectMapper();
  private final Path workspaceDir;
  private final Path systemSkillsDir;
  private final Path workspaceSkillsDir;
  private final Path storeSkillsDir;
  private final Path ignoreFile;

  private final Object indexLock = new Object();
  private volatile Map<String, SkillIndex> localIndexById = Map.of();
  private volatile Map<String, SkillIndex> localIndexByName = Map.of();
  private volatile Map<String, SkillIndex> storeIndexById = Map.of();
  private volatile Set<String> ignoredIds = Set.of();

  public SkillStoreService() {
    this.workspaceDir = ConfigPathResolver.resolveUserDataDir().resolve("workspace").toAbsolutePath().normalize();

    this.systemSkillsDir = workspaceDir.resolve("system").resolve("skills");
    this.workspaceSkillsDir = workspaceDir.resolve("skills");
    this.storeSkillsDir = workspaceDir.resolve("store").resolve("skills");
    this.ignoreFile = storeSkillsDir.resolve(".ignore.json");
    ensureDirectories();
    refreshIndex();
  }

  public void refreshIndex() {
    synchronized (indexLock) {
      Map<String, SkillIndex> locals = new LinkedHashMap<>();
      Map<String, SkillIndex> localsByName = new HashMap<>();
      Map<String, SkillIndex> stores = new LinkedHashMap<>();
      Set<String> ignored = loadIgnored();

      scanSkills(systemSkillsDir, "system", "local", locals, localsByName);
      scanSkills(workspaceSkillsDir, "workspace", "local", locals, localsByName);
      scanStoreSkills(storeSkillsDir, stores, ignored);

      localIndexById = Map.copyOf(locals);
      localIndexByName = Map.copyOf(localsByName);
      storeIndexById = Map.copyOf(stores);
      ignoredIds = Set.copyOf(ignored);
    }
  }

  public List<SkillIndex> listStoreSkills() {
    return new ArrayList<>(storeIndexById.values());
  }

  public List<SkillIndex> listLocalSkills() {
    return new ArrayList<>(localIndexById.values());
  }

  public Optional<SkillIndex> findStoreSkill(String id) {
    if (id == null) return Optional.empty();
    return Optional.ofNullable(storeIndexById.get(id));
  }

  public Optional<SkillIndex> findLocalSkillByName(String name) {
    if (name == null) return Optional.empty();
    return Optional.ofNullable(localIndexByName.get(name));
  }

  public Optional<SkillIndex> findSkillById(String id) {
    if (id == null) return Optional.empty();
    SkillIndex local = localIndexById.get(id);
    if (local != null) return Optional.of(local);
    return Optional.ofNullable(storeIndexById.get(id));
  }

  public boolean hasSkillId(String id) {
    if (id == null || id.isBlank()) return false;
    return localIndexById.containsKey(id) || storeIndexById.containsKey(id);
  }

  public boolean hasSkillHash(String name, String hash) {
    if (name == null || name.isBlank() || hash == null || hash.isBlank()) return false;
    SkillIndex local = localIndexByName.get(name);
    if (local != null && hash.equals(local.hash)) return true;
    for (SkillIndex item : storeIndexById.values()) {
      if (name.equals(item.name) && hash.equals(item.hash)) return true;
    }
    return false;
  }

  public boolean isIgnored(String id) {
    if (id == null || id.isBlank()) return false;
    return ignoredIds.contains(id);
  }

  public boolean ignoreSkill(String id) {
    if (id == null || id.isBlank()) return false;
    synchronized (indexLock) {
      Set<String> updated = new java.util.HashSet<>(ignoredIds);
      updated.add(id);
      if (!saveIgnored(updated)) return false;
      ignoredIds = Set.copyOf(updated);
      storeIndexById = storeIndexById.entrySet().stream()
          .filter(entry -> !updated.contains(entry.getKey()))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      return true;
    }
  }

  public SkillDetail loadStoreSkillDetail(String id) {
    SkillIndex entry = storeIndexById.get(id);
    if (entry == null || entry.rootPath == null) return null;
    Path skillFile = entry.rootPath.resolve("SKILL.md");
    if (!Files.exists(skillFile)) return null;
    try {
      String content = Files.readString(skillFile);
      List<String> files = listFiles(entry.rootPath);
      return new SkillDetail(entry, content, files);
    } catch (IOException e) {
      log.warn("Failed to read store skill detail: {}", id, e);
      return null;
    }
  }

  public SkillDetail loadLocalSkillDetailByName(String name) {
    if (name == null || name.isBlank()) return null;
    SkillIndex entry = localIndexByName.get(name);
    if (entry == null || entry.rootPath == null) return null;
    Path skillFile = entry.rootPath.resolve("SKILL.md");
    if (!Files.exists(skillFile)) return null;
    try {
      String content = Files.readString(skillFile);
      List<String> files = listFiles(entry.rootPath);
      return new SkillDetail(entry, content, files);
    } catch (IOException e) {
      log.warn("Failed to read local skill detail: {}", name, e);
      return null;
    }
  }


  public boolean importToWorkspace(String id) {
    SkillIndex entry = storeIndexById.get(id);
    if (entry == null || entry.rootPath == null) return false;
    if (entry.name == null || entry.name.isBlank()) return false;
    if (localIndexByName.containsKey(entry.name)) return false;
    Path target = workspaceSkillsDir.resolve(entry.name);
    try {
      copyDirectory(entry.rootPath, target);
      refreshIndex();
      return true;
    } catch (IOException e) {
      log.warn("Failed to import skill: {}", id, e);
      return false;
    }
  }

  public SkillDataPayload buildSkillPayload(SkillIndex entry, String originNodeId) {
    if (entry == null || entry.rootPath == null) return null;
    try {
      PackResult pack = buildZipPackage(entry.rootPath);
      SkillDataPayload payload = new SkillDataPayload();
      payload.setPackId(entry.id + ":" + System.currentTimeMillis());
      SkillDataPayload.SkillMeta meta = new SkillDataPayload.SkillMeta();
      meta.setId(entry.id);
      meta.setName(entry.name);
      meta.setVersion(entry.version);
      meta.setHash(entry.hash);
      meta.setOrigin(originNodeId == null || originNodeId.isBlank() ? entry.origin : originNodeId);
      meta.setScope(entry.scope);
      meta.setUpdatedAt(entry.updatedAt);
      meta.setDescription(entry.description);
      payload.setSkill(meta);

      payload.setManifest(pack.manifest);
      payload.setPayloadBase64(Base64.getEncoder().encodeToString(pack.bytes));
      return payload;
    } catch (IOException e) {
      log.warn("Failed to build skill payload: {}", entry.id, e);
      return null;
    }
  }

  public boolean ingestSkillPayload(String payloadJson, String expectedMsgId) {
    if (payloadJson == null || payloadJson.isBlank()) return false;
    try {
      SkillDataPayload payload = jsonMapper.readValue(payloadJson, SkillDataPayload.class);
      if (payload == null || payload.getSkill() == null || payload.getManifest() == null) return false;
      if (payload.getSkill().getId() == null || payload.getSkill().getId().isBlank()) return false;
      if (expectedMsgId != null && !expectedMsgId.isBlank() && !expectedMsgId.equals(payload.getSkill().getId())) {
        return false;
      }
      if (payload.getPayloadBase64() == null || payload.getPayloadBase64().isBlank()) return false;

      byte[] packBytes = Base64.getDecoder().decode(payload.getPayloadBase64());
      if (!verifyHash(packBytes, payload.getManifest().getPackHash())) {
        return false;
      }
      if (payload.getManifest().getTotalSize() > 0 && packBytes.length > payload.getManifest().getTotalSize() * 2L) {
        return false;
      }

      List<FileEntry> files = unzipPackage(packBytes);
      if (files.isEmpty()) return false;
      if (!validateManifest(payload.getManifest(), files)) return false;

      String computedSkillHash = computeSkillHash(files);
      if (payload.getSkill().getHash() != null && !payload.getSkill().getHash().isBlank()) {
        if (!payload.getSkill().getHash().equals(computedSkillHash)) {
          return false;
        }
      }

      String origin = safeSegment(payload.getSkill().getOrigin());
      if (origin == null || origin.isBlank()) {
        origin = "unknown";
      }
      String name = safeSegment(payload.getSkill().getName());
      if (name == null || name.isBlank()) return false;
      String version = safeSegment(payload.getSkill().getVersion());
      if (version == null || version.isBlank()) {
        version = fallbackVersion(payload.getSkill().getUpdatedAt(), computedSkillHash);
      }
      Path target = storeSkillsDir.resolve(origin).resolve(name).resolve(version);
      if (!writeFiles(target, files)) return false;

      SkillMetadata meta = new SkillMetadata(name, payload.getSkill().getVersion(), payload.getSkill().getDescription());
      Map<String, Object> metaJson = new LinkedHashMap<>();
      metaJson.put("id", buildSkillId(name, version, computedSkillHash));
      metaJson.put("name", name);
      metaJson.put("version", version);
      metaJson.put("hash", computedSkillHash);
      metaJson.put("origin", origin);
      metaJson.put("scope", "store");
      metaJson.put("updatedAt", payload.getSkill().getUpdatedAt());
      metaJson.put("description", meta.description);
      metaJson.put("manifest", payload.getManifest());
      Files.writeString(target.resolve("metadata.json"), jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metaJson));

      refreshIndex();
      return true;
    } catch (Exception e) {
      log.warn("Failed to ingest skill payload", e);
      return false;
    }
  }

  private void scanSkills(Path baseDir, String scope, String origin, Map<String, SkillIndex> byId, Map<String, SkillIndex> byName) {
    if (baseDir == null || !Files.exists(baseDir)) return;
    try (Stream<Path> walk = Files.walk(baseDir)) {
      List<Path> skillFiles = walk
          .filter(p -> p.getFileName().toString().equalsIgnoreCase("SKILL.md"))
          .collect(Collectors.toList());
      for (Path file : skillFiles) {
        Path root = file.getParent();
        SkillMetadata meta = parseSkillFile(file);
        String name = meta.name != null && !meta.name.isBlank() ? meta.name : root.getFileName().toString();
        List<Path> files = listFilePaths(root);
        String hash = computeSkillHash(root, files);
        long updatedAt = lastModified(file);
        String version = resolveVersion(meta, updatedAt, hash);
        String id = buildSkillId(name, version, hash);
        long size = files.stream().mapToLong(this::fileSize).sum();
        SkillIndex entry = new SkillIndex(id, name, meta.description, version, hash, origin, scope, updatedAt, size, root, true);
        byId.put(id, entry);
        byName.putIfAbsent(name, entry);
      }

    } catch (IOException e) {
      log.warn("Failed to scan skills: {}", baseDir, e);
    }
  }

  private void scanStoreSkills(Path baseDir, Map<String, SkillIndex> byId, Set<String> ignored) {
    if (baseDir == null || !Files.exists(baseDir)) return;
    try (Stream<Path> walk = Files.walk(baseDir)) {
      List<Path> skillFiles = walk
          .filter(p -> p.getFileName().toString().equalsIgnoreCase("SKILL.md"))
          .collect(Collectors.toList());
      for (Path file : skillFiles) {
        Path root = file.getParent();
        SkillMetadata meta = parseSkillFile(file);
        String name = meta.name != null && !meta.name.isBlank() ? meta.name : root.getFileName().toString();
        List<Path> files = listFilePaths(root);
        String computedHash = computeSkillHash(root, files);
        long updatedAt = lastModified(file);
        String version = resolveVersion(meta, updatedAt, computedHash);
        String origin = "unknown";
        boolean checksumOk = true;


        Path metadataPath = root.resolve("metadata.json");
        if (Files.exists(metadataPath)) {
          try {
            Map<String, Object> stored = jsonMapper.readValue(Files.readString(metadataPath), Map.class);
            String storedHash = readString(stored.get("hash"));
            String storedVersion = readString(stored.get("version"));
            String storedOrigin = readString(stored.get("origin"));
            Long storedUpdated = readLong(stored.get("updatedAt"));
            if (storedHash != null && !storedHash.isBlank()) {
              checksumOk = storedHash.equals(computedHash);
            }
            if (storedVersion != null && !storedVersion.isBlank()) {
              version = storedVersion;
            }
            if (storedOrigin != null && !storedOrigin.isBlank()) {
              origin = storedOrigin;
            }
            if (storedUpdated != null && storedUpdated > 0) {
              updatedAt = storedUpdated;
            }
          } catch (Exception e) {
            checksumOk = false;
          }
        }

        String id = buildSkillId(name, version, computedHash);
        if (ignored.contains(id)) {
          continue;
        }
        long size = files.stream().mapToLong(this::fileSize).sum();
        SkillIndex entry = new SkillIndex(id, name, meta.description, version, computedHash, origin, "store", updatedAt, size, root, checksumOk);
        byId.put(id, entry);
      }
    } catch (IOException e) {
      log.warn("Failed to scan store skills: {}", baseDir, e);
    }
  }

  private SkillMetadata parseSkillFile(Path file) {
    if (file == null || !Files.exists(file)) return new SkillMetadata(null, null, null);
    try {
      String content = Files.readString(file);
      Matcher matcher = FRONT_MATTER_PATTERN.matcher(content);
      if (matcher.find()) {
        String yamlPart = matcher.group(1);
        Map<String, Object> metadata = yamlMapper.readValue(yamlPart, Map.class);
        String name = readString(metadata.get("name"));
        String description = readString(metadata.get("description"));
        String version = readString(metadata.get("version"));
        return new SkillMetadata(name, version, description);
      }
      return new SkillMetadata(null, null, null);
    } catch (Exception e) {
      log.warn("Failed to parse skill file: {}", file, e);
      return new SkillMetadata(null, null, null);
    }
  }

  private PackResult buildZipPackage(Path root) throws IOException {
    List<Path> files;
    try (Stream<Path> walk = Files.walk(root)) {
      files = walk.filter(Files::isRegularFile).sorted(Comparator.comparing(p -> root.relativize(p).toString())).toList();
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Map<String, SkillDataPayload.ManifestFile> manifestFiles = new TreeMap<>();
    long totalSize = 0L;
    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      for (Path file : files) {
        String rel = root.relativize(file).toString().replace('\\', '/');
        byte[] data = Files.readAllBytes(file);
        ZipEntry entry = new ZipEntry(rel);
        zip.putNextEntry(entry);
        zip.write(data);
        zip.closeEntry();
        String fileHash = sha256Hex(data);
        totalSize += data.length;
        manifestFiles.put(rel, new SkillDataPayload.ManifestFile(rel, data.length, fileHash));
      }
    }
    byte[] packBytes = out.toByteArray();
    String packHash = sha256Hex(packBytes);
    SkillDataPayload.Manifest manifest = new SkillDataPayload.Manifest();
    manifest.setTotalSize(totalSize);
    manifest.setPackHash(packHash);
    manifest.setCompression("zip");
    manifest.setFiles(new ArrayList<>(manifestFiles.values()));
    return new PackResult(packBytes, manifest);
  }

  private List<FileEntry> unzipPackage(byte[] packBytes) throws IOException {
    List<FileEntry> files = new ArrayList<>();
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(packBytes))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        if (entry.isDirectory()) continue;
        String name = entry.getName();
        if (name == null || name.isBlank()) continue;
        byte[] data = readAll(zip);
        files.add(new FileEntry(name, data));
      }
    }
    return files;
  }

  private boolean validateManifest(SkillDataPayload.Manifest manifest, List<FileEntry> files) {
    if (manifest == null || manifest.getFiles() == null) return false;
    Map<String, SkillDataPayload.ManifestFile> expected = new HashMap<>();
    for (SkillDataPayload.ManifestFile file : manifest.getFiles()) {
      expected.put(file.getPath(), file);
    }
    for (FileEntry file : files) {
      if (!expected.containsKey(file.path)) return false;
      SkillDataPayload.ManifestFile meta = expected.get(file.path);
      if (meta.getSize() > 0 && meta.getSize() != file.data.length) return false;
      if (!verifyHash(file.data, meta.getSha256())) return false;
    }
    return files.stream().anyMatch(f -> "SKILL.md".equalsIgnoreCase(Path.of(f.path).getFileName().toString()));
  }

  private boolean writeFiles(Path target, List<FileEntry> files) {
    try {
      Files.createDirectories(target);
      for (FileEntry file : files) {
        String safe = file.path.replace('\\', '/');
        if (safe.contains("..") || safe.startsWith("/")) {
          return false;
        }
        Path out = target.resolve(safe).normalize();
        if (!out.startsWith(target)) return false;
        Files.createDirectories(out.getParent());
        Files.write(out, file.data);
      }
      return true;
    } catch (IOException e) {
      log.warn("Failed to write skill files", e);
      return false;
    }
  }

  private String computeSkillHash(Path root, List<Path> files) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      List<Path> sorted = new ArrayList<>(files);
      sorted.sort(Comparator.comparing(p -> root.relativize(p).toString()));
      for (Path file : sorted) {
        String rel = root.relativize(file).toString().replace('\\', '/');
        digest.update(rel.getBytes(StandardCharsets.UTF_8));
        byte[] data = Files.readAllBytes(file);
        digest.update(data);
      }
      return bytesToHex(digest.digest());
    } catch (Exception e) {
      return "";
    }
  }


  private String computeSkillHash(List<FileEntry> files) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      List<FileEntry> sorted = new ArrayList<>(files);
      sorted.sort(Comparator.comparing(a -> a.path));
      for (FileEntry file : sorted) {
        digest.update(file.path.getBytes(StandardCharsets.UTF_8));
        digest.update(file.data);
      }
      return bytesToHex(digest.digest());
    } catch (Exception e) {
      return "";
    }
  }

  private String sha256Hex(byte[] data) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return bytesToHex(digest.digest(data));
    } catch (Exception e) {
      return "";
    }
  }

  private boolean verifyHash(byte[] data, String expected) {
    if (expected == null || expected.isBlank()) return false;
    String actual = sha256Hex(data);
    return expected.equalsIgnoreCase(actual);
  }

  private List<String> listFiles(Path root) {
    if (root == null || !Files.exists(root)) return List.of();
    try (Stream<Path> walk = Files.walk(root)) {
      return walk.filter(Files::isRegularFile)
          .map(p -> root.relativize(p).toString().replace('\\', '/'))
          .sorted()
          .collect(Collectors.toList());
    } catch (IOException e) {
      return List.of();
    }
  }

  private List<Path> listFilePaths(Path root) {
    if (root == null || !Files.exists(root)) return List.of();
    try (Stream<Path> walk = Files.walk(root)) {
      return walk.filter(Files::isRegularFile)
          .sorted(Comparator.comparing(p -> root.relativize(p).toString()))
          .collect(Collectors.toList());
    } catch (IOException e) {
      return List.of();
    }
  }


  private long lastModified(Path file) {
    try {
      FileTime time = Files.getLastModifiedTime(file);
      return time.toMillis();
    } catch (IOException e) {
      return System.currentTimeMillis();
    }
  }

  private long fileSize(Path file) {
    try {
      return Files.size(file);
    } catch (IOException e) {
      return 0L;
    }
  }

  private String resolveVersion(SkillMetadata meta, long updatedAt, String hash) {
    if (meta != null && meta.version != null && !meta.version.isBlank()) {
      return meta.version.trim();
    }
    String fallback = fallbackVersion(updatedAt, hash);
    return fallback;
  }

  private String fallbackVersion(long updatedAt, String hash) {
    if (updatedAt > 0) {
      LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedAt), ZoneOffset.UTC);
      return VERSION_FALLBACK.format(dt);
    }
    if (hash != null && hash.length() >= 12) return hash.substring(0, 12);
    return "unknown";
  }

  private String safeSegment(String raw) {
    if (raw == null) return null;
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) return null;
    return trimmed.replaceAll("[^a-zA-Z0-9._-]", "_");
  }

  private void copyDirectory(Path source, Path target) throws IOException {
    if (!Files.exists(source)) return;
    Files.walk(source).forEach(path -> {
      try {
        Path relative = source.relativize(path);
        Path dest = target.resolve(relative);
        if (Files.isDirectory(path)) {
          Files.createDirectories(dest);
        } else {
          Files.createDirectories(dest.getParent());
          Files.copy(path, dest);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private void ensureDirectories() {
    try {
      Files.createDirectories(workspaceDir);
      Files.createDirectories(systemSkillsDir);
      Files.createDirectories(workspaceSkillsDir);
      Files.createDirectories(storeSkillsDir);
    } catch (IOException e) {
      log.warn("Failed to ensure workspace directories", e);
    }
  }

  private Set<String> loadIgnored() {
    if (!Files.exists(ignoreFile)) return Set.of();
    try {
      List<String> list = jsonMapper.readValue(Files.readString(ignoreFile), List.class);
      return Set.copyOf(list);
    } catch (Exception e) {
      return Set.of();
    }
  }

  private boolean saveIgnored(Set<String> ids) {
    try {
      Files.createDirectories(ignoreFile.getParent());
      Files.writeString(ignoreFile, jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ids));
      return true;
    } catch (IOException e) {
      log.warn("Failed to save ignore list", e);
      return false;
    }
  }

  private byte[] readAll(InputStream input) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    input.transferTo(out);
    return out.toByteArray();
  }

  private String readString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private Long readLong(Object value) {
    if (value == null) return null;
    if (value instanceof Number num) return num.longValue();
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  public static String buildSkillId(String name, String version, String hash) {
    String safeName = name == null ? "" : name.trim();
    String safeVer = version == null ? "" : version.trim();
    String safeHash = hash == null ? "" : hash.trim();
    return safeName + ":" + safeVer + ":" + safeHash;
  }

  public static class SkillIndex {
    private final String id;
    private final String name;
    private final String description;
    private final String version;
    private final String hash;
    private final String origin;
    private final String scope;
    private final long updatedAt;
    private final long size;
    private final Path rootPath;
    private final boolean checksumOk;

    public SkillIndex(String id, String name, String description, String version, String hash, String origin, String scope, long updatedAt, long size, Path rootPath, boolean checksumOk) {
      this.id = id;
      this.name = name;
      this.description = description;
      this.version = version;
      this.hash = hash;
      this.origin = origin;
      this.scope = scope;
      this.updatedAt = updatedAt;
      this.size = size;
      this.rootPath = rootPath;
      this.checksumOk = checksumOk;
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public String getVersion() {
      return version;
    }

    public String getHash() {
      return hash;
    }

    public String getOrigin() {
      return origin;
    }

    public String getScope() {
      return scope;
    }

    public long getUpdatedAt() {
      return updatedAt;
    }

    public long getSize() {
      return size;
    }

    public Path getRootPath() {
      return rootPath;
    }

    public boolean isChecksumOk() {
      return checksumOk;
    }
  }

  public static class SkillDetail {
    private final SkillIndex skill;
    private final String content;
    private final List<String> files;

    public SkillDetail(SkillIndex skill, String content, List<String> files) {
      this.skill = skill;
      this.content = content;
      this.files = files;
    }

    public SkillIndex getSkill() {
      return skill;
    }

    public String getContent() {
      return content;
    }

    public List<String> getFiles() {
      return files;
    }
  }

  public static class SkillDataPayload {
    private String packId;
    private SkillMeta skill;
    private Manifest manifest;
    private String payloadBase64;

    public String getPackId() {
      return packId;
    }

    public void setPackId(String packId) {
      this.packId = packId;
    }

    public SkillMeta getSkill() {
      return skill;
    }

    public void setSkill(SkillMeta skill) {
      this.skill = skill;
    }

    public Manifest getManifest() {
      return manifest;
    }

    public void setManifest(Manifest manifest) {
      this.manifest = manifest;
    }

    public String getPayloadBase64() {
      return payloadBase64;
    }

    public void setPayloadBase64(String payloadBase64) {
      this.payloadBase64 = payloadBase64;
    }

    public static class SkillMeta {
      private String id;
      private String name;
      private String version;
      private String hash;
      private String origin;
      private String scope;
      private long updatedAt;
      private String description;

      public String getId() {
        return id;
      }

      public void setId(String id) {
        this.id = id;
      }

      public String getName() {
        return name;
      }

      public void setName(String name) {
        this.name = name;
      }

      public String getVersion() {
        return version;
      }

      public void setVersion(String version) {
        this.version = version;
      }

      public String getHash() {
        return hash;
      }

      public void setHash(String hash) {
        this.hash = hash;
      }

      public String getOrigin() {
        return origin;
      }

      public void setOrigin(String origin) {
        this.origin = origin;
      }

      public String getScope() {
        return scope;
      }

      public void setScope(String scope) {
        this.scope = scope;
      }

      public long getUpdatedAt() {
        return updatedAt;
      }

      public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
      }

      public String getDescription() {
        return description;
      }

      public void setDescription(String description) {
        this.description = description;
      }
    }

    public static class Manifest {
      private long totalSize;
      private String packHash;
      private String compression;
      private List<ManifestFile> files = new ArrayList<>();

      public long getTotalSize() {
        return totalSize;
      }

      public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
      }

      public String getPackHash() {
        return packHash;
      }

      public void setPackHash(String packHash) {
        this.packHash = packHash;
      }

      public String getCompression() {
        return compression;
      }

      public void setCompression(String compression) {
        this.compression = compression;
      }

      public List<ManifestFile> getFiles() {
        return files;
      }

      public void setFiles(List<ManifestFile> files) {
        this.files = files;
      }
    }

    public static class ManifestFile {
      private String path;
      private long size;
      private String sha256;

      public ManifestFile() {}

      public ManifestFile(String path, long size, String sha256) {
        this.path = path;
        this.size = size;
        this.sha256 = sha256;
      }

      public String getPath() {
        return path;
      }

      public void setPath(String path) {
        this.path = path;
      }

      public long getSize() {
        return size;
      }

      public void setSize(long size) {
        this.size = size;
      }

      public String getSha256() {
        return sha256;
      }

      public void setSha256(String sha256) {
        this.sha256 = sha256;
      }
    }
  }

  private static class PackResult {
    private final byte[] bytes;
    private final SkillDataPayload.Manifest manifest;

    private PackResult(byte[] bytes, SkillDataPayload.Manifest manifest) {
      this.bytes = bytes;
      this.manifest = manifest;
    }
  }

  private static class SkillMetadata {
    private final String name;
    private final String version;
    private final String description;

    private SkillMetadata(String name, String version, String description) {
      this.name = name;
      this.version = version;
      this.description = description;
    }
  }

  private static class FileEntry {
    private final String path;
    private final byte[] data;

    private FileEntry(String path, byte[] data) {
      this.path = path;
      this.data = data;
    }
  }
}
