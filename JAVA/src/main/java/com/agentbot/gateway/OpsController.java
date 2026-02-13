package com.agentbot.gateway;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.channel.ChannelManager;
import com.agentbot.core.ops.LogEntry;
import com.agentbot.core.ops.LogService;
import com.agentbot.core.workspace.WorkspaceInitializer;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ops")
public class OpsController {
  private final ChannelManager channelManager;
  private final AgentbotProperties properties;
  private final LogService logService;
  private final WorkspaceInitializer workspaceInitializer;

  public OpsController(ChannelManager channelManager,
                       AgentbotProperties properties,
                       LogService logService,
                       WorkspaceInitializer workspaceInitializer) {
    this.channelManager = channelManager;
    this.properties = properties;
    this.logService = logService;
    this.workspaceInitializer = workspaceInitializer;
  }

  @GetMapping("/status")
  public Map<String, Object> status() {
    List<String> channels = channelManager.status().keySet().stream().sorted().collect(Collectors.toList());
    long uptimeMillis = System.currentTimeMillis() - START_TIME;
    return Map.of(
        "status", "ok",
        "uptimeMillis", uptimeMillis,
        "toolCalls", DefaultAgentRuntime.getTotalToolCalls(),
        "workspace", properties.getWorkspaceDir(),


        "configFile", properties.getConfigFile(),
        "channels", channels,
        "heartbeat", Map.of("enabled", properties.getHeartbeat().isEnabled(), "intervalSeconds", properties.getHeartbeat().getIntervalSeconds()),
        "cron", Map.of("enabled", properties.getCron().isEnabled(), "defaultIntervalSeconds", properties.getCron().getDefaultIntervalSeconds()),
        "llm", Map.of("provider", properties.getLlm().getProvider(), "model", properties.getLlm().getModel())
    );
  }

  @GetMapping("/logs")
  public List<LogEntry> logs(@RequestParam(defaultValue = "200") int limit) {
    return logService.latest(limit);
  }

  @PostMapping("/init")
  public Map<String, Object> initWorkspace() {
    return Map.of(
        "ok", true,
        "workspace", Path.of(properties.getWorkspaceDir()).toString(),
        "files", workspaceInitializer.initialize()
    );
  }
}
