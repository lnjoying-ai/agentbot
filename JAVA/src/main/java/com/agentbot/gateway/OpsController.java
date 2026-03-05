package com.agentbot.gateway;

import com.agentbot.AgentbotApplication;
import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.agent.DefaultAgentRuntime;
import com.agentbot.core.channel.ChannelManager;
import com.agentbot.core.model.FallbackLlmProvider;
import com.agentbot.core.model.LLMProvider;
import com.agentbot.core.p2p.P2pMetrics;

import com.agentbot.core.ops.LogEntry;
import com.agentbot.core.ops.LogService;
import com.agentbot.core.workspace.WorkspaceInitializer;
import com.agentbot.core.util.ConfigPathResolver;

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
  private final LLMProvider llmProvider;

  public OpsController(ChannelManager channelManager,
                       AgentbotProperties properties,
                       LogService logService,
                       WorkspaceInitializer workspaceInitializer,
                       LLMProvider llmProvider) {
    this.channelManager = channelManager;
    this.properties = properties;
    this.logService = logService;
    this.workspaceInitializer = workspaceInitializer;
    this.llmProvider = llmProvider;
  }


  @GetMapping("/status")
  public Map<String, Object> status() {
    List<String> channels = channelManager.status().keySet().stream().sorted().collect(Collectors.toList());
    long uptimeMillis = System.currentTimeMillis() - AgentbotApplication.START_TIME;

    Map<String, Object> llm = new java.util.HashMap<>();
    llm.put("provider", properties.getLlm().getProvider());
    llm.put("model", properties.getLlm().getActiveModel());

    if (llmProvider instanceof FallbackLlmProvider) {
      llm.putAll(((FallbackLlmProvider) llmProvider).getStatus());
    }

    Map<String, Object> response = new java.util.HashMap<>();
    response.put("status", "ok");
    response.put("uptimeMillis", uptimeMillis);
    response.put("toolCalls", DefaultAgentRuntime.getTotalToolCalls());
    response.put("workspace", resolveWorkspaceDir().toString());

    response.put("channels", channels);

    response.put("heartbeat", Map.of("enabled", properties.getHeartbeat().isEnabled(), "intervalSeconds", properties.getHeartbeat().getIntervalSeconds()));
    response.put("cron", Map.of("enabled", properties.getCron().isEnabled(), "defaultIntervalSeconds", properties.getCron().getDefaultIntervalSeconds()));
    response.put("llm", llm);
    response.put("p2p", P2pMetrics.snapshot());
    return response;
  }



  @GetMapping("/logs")
  public List<LogEntry> logs(@RequestParam(value = "limit", defaultValue = "200") int limit) {
    return logService.latest(limit);
  }


  @PostMapping("/init")
  public Map<String, Object> initWorkspace() {
    return Map.of(
        "ok", true,
        "workspace", resolveWorkspaceDir().toString(),
        "files", workspaceInitializer.initialize()
    );
  }

  private Path resolveWorkspaceDir() {
    return ConfigPathResolver.resolveUserDataDir().resolve("workspace").toAbsolutePath().normalize();
  }

}
