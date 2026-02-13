package com.agentbot.cli;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.agent.AgentRuntime;
import com.agentbot.core.bus.events.InboundMessage;
import com.agentbot.core.bus.events.OutboundMessage;
import com.agentbot.core.channel.ChannelManager;
import com.agentbot.core.config.ConfigStore;
import com.agentbot.core.cron.CronJob;
import com.agentbot.core.cron.CronService;
import com.agentbot.core.memory.MemorySearch;
import com.agentbot.core.memory.MemoryService;
import com.agentbot.core.ops.LogEntry;
import com.agentbot.core.ops.LogService;
import com.agentbot.core.workspace.WorkspaceInitializer;
import com.agentbot.core.automation.AutomationService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
public class CliService {
  private final AgentbotProperties properties;
  private final ChannelManager channelManager;
  private final ConfigStore configStore;
  private final WorkspaceInitializer workspaceInitializer;
  private final AgentRuntime agentRuntime;
  private final CronService cronService;
  private final AutomationService automationService;
  private final MemorySearch memorySearch;
  private final LogService logService;

  public CliService(AgentbotProperties properties,
                    ChannelManager channelManager,
                    ConfigStore configStore,
                    WorkspaceInitializer workspaceInitializer,
                    AgentRuntime agentRuntime,
                    CronService cronService,
                    AutomationService automationService,
                    MemorySearch memorySearch,
                    LogService logService) {
    this.properties = properties;
    this.channelManager = channelManager;
    this.configStore = configStore;
    this.workspaceInitializer = workspaceInitializer;
    this.agentRuntime = agentRuntime;
    this.cronService = cronService;
    this.automationService = automationService;
    this.memorySearch = memorySearch;
    this.logService = logService;
  }


  public boolean execute(String command, ApplicationArguments args) {
    String[] parts = command == null ? new String[]{"status"} : command.split(":", 2);
    String root = parts.length == 0 || parts[0].isBlank() ? "status" : parts[0];
    String sub = parts.length > 1 ? parts[1] : "";

    return switch (root) {
      case "onboard", "init" -> {
        onboard();
        yield false;
      }
      case "gateway" -> {
        print("gateway", Map.of("status", "running", "port", 8080));
        yield true;
      }
      case "agent" -> {
        runAgent(args);
        yield false;
      }
      case "channels" -> {
        channels(sub, args);
        yield false;
      }
      case "cron" -> {
        cron(sub, args);
        yield false;
      }
      case "memory" -> {
        memory(sub, args);
        yield false;
      }
      case "heartbeat" -> {
        heartbeat(sub, args);
        yield false;
      }
      case "logs" -> {

        logs(args);
        yield false;
      }
      case "config" -> {
        print("config", Map.of("path", configStore.getConfigPath().toString(), "stored", configStore.load()));
        yield false;
      }
      case "status" -> {
        status();
        yield false;
      }
      case "workspace" -> {
        workspace(args);
        yield false;
      }
      case "version" -> {

        System.out.println("[agentbot] v0.1.0-SNAPSHOT");
        yield false;
      }
      default -> {
        status();
        yield false;
      }
    };
  }

  private void onboard() {
    Map<String, Object> templates = workspaceInitializer.initialize();
    Path configPath = resolveConfigPath();
    if (!Files.exists(configPath)) {
      try {
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, defaultConfigTemplate());
      } catch (Exception ignored) {
        // ignore
      }
    }
    System.out.println("---------------------------------------------------------");
    System.out.println("  __  __  ____  _      _______ ____   ____ _______ ");
    System.out.println(" |  \\/  |/ __ \\| |    |__   __|  _ \\ / __ \\__   __|");
    System.out.println(" | \\  / | |  | | |       | |  | |_) | |  | | | |   ");
    System.out.println(" | |\\/| | |  | | |       | |  |  _ <| |  | | | |   ");
    System.out.println(" | |  | | |__| | |____   | |  | |_) | |__| | | |   ");
    System.out.println(" |_|  |_|\\____/|______|  |_|  |____/ \\____/  |_|   ");
    System.out.println("---------------------------------------------------------");
    System.out.println("[OK] Workspace templates initialized: " + templates.keySet());
    System.out.println("[OK] Configuration file: " + configPath + " (Exists: " + Files.exists(configPath) + ")");
    System.out.println("\nNext steps:");

    System.out.println("  1. Configure your LLM in " + configPath);
    System.out.println("  2. Run 'agent' to chat: java -jar ... --cli=agent -m \"Hello\"");
    System.out.println("  3. Start gateway: java -jar ... --cli=gateway");
    System.out.println("---------------------------------------------------------");
  }

  private void status() {
    Map<String, Object> channels = Map.of(
        "telegram", properties.getChannels().getTelegram().isEnabled(),
        "whatsapp", properties.getChannels().getWhatsapp().isEnabled(),
        "wechat", properties.getChannels().getWechat().isEnabled()
    );
    List<String> registered = new ArrayList<>(channelManager.status().keySet());
    Collections.sort(registered);
    print("status", Map.of(
        "workspace", properties.getWorkspaceDir(),
        "configFile", properties.getConfigFile(),
        "llm", Map.of("provider", properties.getLlm().getProvider(), "model", properties.getLlm().getModel()),
        "channels", channels,
        "registeredChannels", registered
    ));
  }

  private void channels(String sub, ApplicationArguments args) {
    String action = (sub == null || sub.isBlank()) ? getOption(args, "action", "status") : sub;
    switch (action) {
      case "status" -> {
        String tgToken = mask(properties.getChannels().getTelegram().getToken());
        String waBridge = properties.getChannels().getWhatsapp().getBridgeUrl();
        String wechatToken = mask(properties.getChannels().getWechat().getToken());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("telegram", Map.of("enabled", properties.getChannels().getTelegram().isEnabled(), "token", tgToken));
        data.put("whatsapp", Map.of("enabled", properties.getChannels().getWhatsapp().isEnabled(), "bridgeUrl", waBridge));
        data.put("wechat", Map.of("enabled", properties.getChannels().getWechat().isEnabled(), "token", wechatToken));
        print("channels", data);
      }
      case "login" -> print("channels", Map.of(
          "whatsapp", "Please start your bridge and scan QR in that console.",
          "bridgeUrl", properties.getChannels().getWhatsapp().getBridgeUrl()
      ));
      default -> print("channels", Map.of("error", "unknown action", "action", action));
    }
  }

  private void runAgent(ApplicationArguments args) {
    String message = getOption(args, "message", null);
    String session = getOption(args, "session", "cli:default");
    String chatId = session.contains(":") ? session.substring(session.indexOf(":") + 1) : session;
    if (message != null && !message.isBlank()) {
      InboundMessage inbound = new InboundMessage("cli", "cli", chatId, message);
      OutboundMessage reply = agentRuntime.handle(inbound);
      print("agent", reply == null ? Map.of("ok", false) : Map.of("ok", true, "content", reply.getContent()));
      return;
    }
    Scanner scanner = new Scanner(System.in);
    System.out.println("[agentbot] interactive mode (type 'exit' to quit)");
    while (true) {
      System.out.print("You: ");
      String input = scanner.nextLine();
      if (input == null || input.isBlank()) continue;
      if ("exit".equalsIgnoreCase(input.trim())) break;
      InboundMessage inbound = new InboundMessage("cli", "cli", chatId, input.trim());
      OutboundMessage reply = agentRuntime.handle(inbound);
      System.out.println("Agent: " + (reply == null ? "" : reply.getContent()));
    }
  }

  private void memory(String sub, ApplicationArguments args) {
    String action = (sub == null || sub.isBlank()) ? getOption(args, "action", "search") : sub;
    switch (action) {
      case "search" -> {
        String query = getOption(args, "q", "");
        int limit = Integer.parseInt(getOption(args, "limit", "10"));
        List<String> results = memorySearch.search(query, limit);
        print("memory:search", Map.of("query", query, "results", results));
      }
      case "list" -> {
        // Just show a snippet or summary if too many
        List<String> results = memorySearch.search("", 20); 
        print("memory:list", results);
      }
      default -> print("memory", Map.of("error", "unknown action", "action", action));
    }
  }

  private void heartbeat(String sub, ApplicationArguments args) {
    String action = (sub == null || sub.isBlank()) ? "status" : sub;
    if ("run".equalsIgnoreCase(action) || "trigger".equalsIgnoreCase(action)) {
      String content = getOption(args, "message", "Check my status");
      automationService.triggerHeartbeat(content);
      print("heartbeat", Map.of("ok", true, "action", "triggered", "message", content));
    } else {
      print("heartbeat", Map.of("enabled", properties.getHeartbeat().isEnabled(), "interval", properties.getHeartbeat().getIntervalSeconds()));
    }
  }


  private void logs(ApplicationArguments args) {
    int limit = Integer.parseInt(getOption(args, "limit", "20"));
    List<LogEntry> entries = logService.latest(limit);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    System.out.println("[agentbot] Recent logs (" + entries.size() + "):");
    for (LogEntry entry : entries) {
      LocalDateTime time = LocalDateTime.ofInstant(entry.getTimestamp(), ZoneId.systemDefault());
      System.out.printf("[%s] [%s] %s\n", time.format(formatter), entry.getType(), entry.getPayload());
    }
  }

  private void workspace(ApplicationArguments args) {
    Path path = Path.of(properties.getWorkspaceDir());
    if (!Files.exists(path)) {
      print("workspace", Map.of("error", "workspace directory does not exist", "path", path.toAbsolutePath().toString()));
      return;
    }
    try {
      List<String> files = Files.list(path)
          .map(p -> p.getFileName().toString())
          .sorted()
          .toList();
      print("workspace", Map.of("path", path.toAbsolutePath().toString(), "files", files));
    } catch (Exception e) {
      print("workspace", Map.of("error", "failed to list files", "message", e.getMessage()));
    }
  }

  private void cron(String sub, ApplicationArguments args) {

    String action = (sub == null || sub.isBlank()) ? getOption(args, "action", "list") : sub;
    switch (action) {
      case "list" -> print("cron", cronService.listJobs());
      case "add" -> cronAdd(args);
      case "remove" -> cronRemove(args);
      case "enable" -> cronEnable(args, false);
      case "disable" -> cronEnable(args, true);
      case "run" -> cronRun(args);
      default -> print("cron", Map.of("error", "unknown action", "action", action));
    }
  }

  private void cronAdd(ApplicationArguments args) {
    String name = getOption(args, "name", "cron-job");
    String message = getOption(args, "message", "");
    String session = getOption(args, "session", "cron");
    String every = getOption(args, "every", null);
    String cronExpr = getOption(args, "cron", null);
    String at = getOption(args, "at", null);
    boolean deliver = hasOption(args, "deliver");
    String to = getOption(args, "to", null);
    String channel = getOption(args, "channel", null);

    if (message.isBlank()) {
      print("cron", Map.of("ok", false, "error", "message is required"));
      return;
    }

    String scheduleType = "every";
    Long everySeconds = null;
    Instant runAt = null;

    if (every != null) {
      scheduleType = "every";
      try {
        everySeconds = Math.max(1, Long.parseLong(every));
      } catch (NumberFormatException ex) {
        print("cron", Map.of("ok", false, "error", "every must be number (seconds)"));
        return;
      }
    } else if (cronExpr != null) {
      scheduleType = "cron";
    } else if (at != null) {
      scheduleType = "at";
      try {
        runAt = Instant.parse(at);
      } catch (Exception ex) {
        print("cron", Map.of("ok", false, "error", "at must be ISO-8601 format"));
        return;
      }
    } else {
      print("cron", Map.of("ok", false, "error", "must specify --every or --cron or --at"));
      return;
    }

    CronJob job = cronService.addJob(
        name,
        scheduleType,
        everySeconds,
        cronExpr,
        runAt,
        message,
        session,
        deliver,
        to,
        channel,
        () -> automationService.triggerCron(session, message)
    );


    print("cron", Map.of("ok", true, "id", job.getId(), "name", job.getName(), "schedule", job.getScheduleType()));
  }

  private void cronRemove(ApplicationArguments args) {
    String jobId = getOption(args, "id", null);
    if (jobId == null || jobId.isBlank()) {
      print("cron", Map.of("ok", false, "error", "--id is required"));
      return;
    }
    boolean ok = cronService.removeJob(jobId);
    print("cron", Map.of("ok", ok, "id", jobId));
  }

  private void cronEnable(ApplicationArguments args, boolean disable) {
    String jobId = getOption(args, "id", null);
    if (jobId == null || jobId.isBlank()) {
      print("cron", Map.of("ok", false, "error", "--id is required"));
      return;
    }
    CronJob job = cronService.enableJob(jobId, !disable);
    print("cron", Map.of("ok", job != null, "id", jobId, "enabled", job != null && job.isEnabled()));
  }

  private void cronRun(ApplicationArguments args) {
    String jobId = getOption(args, "id", null);
    boolean force = hasOption(args, "force");
    if (jobId == null || jobId.isBlank()) {
      print("cron", Map.of("ok", false, "error", "--id is required"));
      return;
    }
    boolean ok = cronService.runJob(jobId, force);
    print("cron", Map.of("ok", ok, "id", jobId));
  }

  private String getOption(ApplicationArguments args, String name, String fallback) {
    List<String> values = args.getOptionValues(name);
    if (values == null || values.isEmpty()) return fallback;
    return values.get(0);
  }

  private boolean hasOption(ApplicationArguments args, String name) {
    return args.containsOption(name);
  }

  private Path resolveConfigPath() {
    String env = System.getenv("AGENTBOT_CONFIG");
    if (env != null && !env.isBlank()) {
      return Path.of(env);
    }
    return Path.of("config", "agentbot.yml");
  }

  private String defaultConfigTemplate() {
    return "agentbot:\n" +
        "  configFile: \"agentbot-config.json\"\n" +
        "  channels:\n" +
        "    telegram:\n" +
        "      enabled: false\n" +
        "      token: \"\"\n" +
        "      pollSeconds: 2\n" +
        "    whatsapp:\n" +
        "      enabled: false\n" +
        "      bridgeUrl: \"ws://127.0.0.1:3001\"\n" +
        "    wechat:\n" +
        "      enabled: false\n" +
        "      appId: \"\"\n" +
        "      appSecret: \"\"\n" +
        "      token: \"\"\n" +
        "      aesKey: \"\"\n" +
        "  heartbeat:\n" +
        "    enabled: false\n" +
        "    intervalSeconds: 60\n" +
        "  cron:\n" +
        "    enabled: false\n" +
        "    defaultIntervalSeconds: 3600\n" +
        "    defaultPrompt: \"\"\n" +
        "  ops:\n" +
        "    logBufferSize: 200\n" +
        "  llm:\n" +
        "    provider: \"openai\"\n" +
        "    apiKey: \"\"\n" +
        "    baseUrl: \"https://api.openai.com/v1\"\n" +
        "    model: \"gpt-4o-mini\"\n" +
        "    temperature: 0.7\n" +
        "    fallbackOrder: \"openai,openrouter,glm,kimi\"\n" +
        "    maxToolRounds: 20\n" +
        "    parallelTools: true\n" +
        "    toolParallelism: 4\n" +
        "    openrouter:\n" +
        "      apiKey: \"\"\n" +
        "      baseUrl: \"https://openrouter.ai/api/v1\"\n" +
        "      model: \"\"\n" +
        "    glm:\n" +
        "      apiKey: \"\"\n" +
        "      baseUrl: \"https://open.bigmodel.cn/api/paas/v4\"\n" +
        "      model: \"\"\n" +
        "    kimi:\n" +
        "      apiKey: \"\"\n" +
        "      baseUrl: \"https://api.moonshot.cn/v1\"\n" +
        "      model: \"\"\n" +
        "server:\n" +
        "  port: 8080\n";
  }

  private String mask(String value) {
    if (value == null || value.isBlank()) return "";
    if (value.length() <= 4) return "****";
    return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
  }

  public void printHelp() {
    System.out.println("---------------------------------------------------------");
    System.out.println("  __  __  ____  _      _______ ____   ____ _______ ");
    System.out.println(" |  \\/  |/ __ \\| |    |__   __|  _ \\ / __ \\__   __|");
    System.out.println(" | \\  / | |  | | |       | |  | |_) | |  | | | |   ");
    System.out.println(" | |\\/| | |  | | |       | |  |  _ <| |  | | | |   ");
    System.out.println(" | |  | | |__| | |____   | |  | |_) | |__| | | |   ");
    System.out.println(" |_|  |_|\\____/|______|  |_|  |____/ \\____/  |_|   ");
    System.out.println("---------------------------------------------------------");
    System.out.println("agentbot CLI usage:");
    System.out.println("  java -jar agentbot.jar --cli=<command>[:<subcommand>] [--option=value]");
    System.out.println();
    System.out.println("Commands:");
    System.out.println("  onboard | init      Initialize workspace and config");
    System.out.println("  status              Show current system status");
    System.out.println("  version             Show version info");
    System.out.println("  workspace           List files in workspace");
    System.out.println("  gateway             Start the agent gateway (REST/SSE/Channels)");
    System.out.println("  agent               Interact with the agent");
    System.out.println("    --message=\"...\"   Send a single message");
    System.out.println("    --session=\"...\"   Specify session (default: cli:default)");
    System.out.println("  channels            Manage communication channels");
    System.out.println("    :status           Show channel health");
    System.out.println("    :login            Start login process (e.g. WhatsApp QR)");
    System.out.println("  cron                Manage scheduled tasks");
    System.out.println("    :list             List all jobs");
    System.out.println("    :add              Add a new job");
    System.out.println("    :remove --id=...  Remove a job");
    System.out.println("    :enable --id=...  Enable a job");
    System.out.println("    :disable --id=... Disable a job");
    System.out.println("    :run --id=...     Run job immediately");
    System.out.println("  heartbeat           Manage heartbeat service");
    System.out.println("    :status           Show heartbeat status");
    System.out.println("    :run | :trigger   Trigger heartbeat now");
    System.out.println("  memory              Manage agent memory");

    System.out.println("    :search -q=\"...\"  Search memory");
    System.out.println("    :list             List recent memory");
    System.out.println("  logs                View system logs");
    System.out.println("    --limit=N         Number of logs to show (default: 20)");
    System.out.println("  config              Show current configuration");
    System.out.println();
    System.out.println("Common Options:");
    System.out.println("  --help | -h         Show this help message");
    System.out.println("---------------------------------------------------------");
  }



  private void print(String label, Object data) {
    System.out.println("[agentbot] " + label + " = " + data);
  }
}

