package com.agentbot.config;

import com.agentbot.core.config.ConfigStore;
import com.agentbot.core.events.SystemEventBus;
import com.agentbot.core.ops.LogEntry;
import com.agentbot.core.ops.LogService;
import com.agentbot.core.workspace.WorkspaceInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class OpsConfiguration {

  @Bean
  public LogService logService(AgentbotProperties properties) {
    return new LogService(properties.getOps().getLogBufferSize());
  }

  @Bean
  public ConfigStore configStore(ObjectMapper mapper, AgentbotProperties properties) {
    Path path = Path.of(properties.getWorkspaceDir()).resolve(properties.getConfigFile());
    return new ConfigStore(mapper, path);
  }

  @Bean
  public WorkspaceInitializer workspaceInitializer(AgentbotProperties properties) {
    return new WorkspaceInitializer(Path.of(properties.getWorkspaceDir()));
  }

  @Bean(destroyMethod = "unsubscribe")
  public SystemEventBus.Subscription logSubscription(SystemEventBus eventBus, LogService logService) {
    return eventBus.subscribe(event -> logService.append(new LogEntry(event.getType(), event.getPayload())));
  }
}

