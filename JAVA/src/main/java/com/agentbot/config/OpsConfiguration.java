package com.agentbot.config;

import com.agentbot.core.config.ConfigStore;
import com.agentbot.core.events.SystemEventBus;
import com.agentbot.core.ops.LogEntry;
import com.agentbot.core.ops.LogService;
import com.agentbot.core.workspace.WorkspaceInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
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
  public ConfigStore configStore(AgentbotProperties properties) {
    ObjectMapper yamlMapper = new YAMLMapper();
    Path path = com.agentbot.core.util.ConfigPathResolver.resolveConfigPath();
    return new ConfigStore(yamlMapper, path);
  }


  @Bean
  public com.agentbot.core.cron.CronJobStore cronJobStore(ConfigStore configStore) {
    ObjectMapper mapper = new YAMLMapper();
    Path configDir = configStore.getConfigPath().getParent();
    if (configDir == null) {
      configDir = Path.of("config");
    }
    Path storePath = configDir.resolve("cron-jobs.yml");
    return new com.agentbot.core.cron.CronJobStore(mapper, storePath);
  }


  @Bean
  public WorkspaceInitializer workspaceInitializer(AgentbotProperties properties) {
    Path workspaceDir = com.agentbot.core.util.ConfigPathResolver.resolveUserDataDir().resolve("workspace").toAbsolutePath().normalize();
    return new WorkspaceInitializer(workspaceDir);
  }


  @Bean(destroyMethod = "unsubscribe")
  public SystemEventBus.Subscription logSubscription(SystemEventBus eventBus, LogService logService) {
    return eventBus.subscribe(event -> logService.append(new LogEntry(event.getType(), event.getPayload())));
  }
}

