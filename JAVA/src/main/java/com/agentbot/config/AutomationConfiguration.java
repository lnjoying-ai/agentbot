package com.agentbot.config;

import com.agentbot.core.agent.AgentRuntime;
import com.agentbot.core.automation.AutomationService;
import com.agentbot.core.bus.MessageBus;
import com.agentbot.core.cron.CronService;
import com.agentbot.core.heartbeat.HeartbeatService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class AutomationConfiguration {

  @Bean
  public AutomationService automationService(AgentRuntime runtime, MessageBus messageBus) {
    return new AutomationService(runtime, messageBus);
  }

  @Bean
  public CronService cronService(AgentbotProperties properties, AutomationService automationService) {
    CronService service = new CronService();
    AgentbotProperties.Cron cron = properties.getCron();
    if (cron.isEnabled() && cron.getDefaultPrompt() != null && !cron.getDefaultPrompt().isBlank()) {
      service.scheduleEverySeconds(
          cron.getDefaultIntervalSeconds(),
          cron.getDefaultPrompt(),
          "cron",
          () -> automationService.triggerCron("cron", cron.getDefaultPrompt())
      );
    }
    return service;
  }

  @Bean
  public HeartbeatService heartbeatService(AgentbotProperties properties, AutomationService automationService) {
    Path heartbeatFile = Path.of(properties.getWorkspaceDir()).resolve(properties.getHeartbeatFile());
    HeartbeatService service = new HeartbeatService(heartbeatFile, automationService::triggerHeartbeat);
    AgentbotProperties.Heartbeat heartbeat = properties.getHeartbeat();
    if (heartbeat.isEnabled()) {
      service.start(heartbeat.getIntervalSeconds());
    }
    return service;
  }
}

