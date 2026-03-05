package com.agentbot.config;

import com.agentbot.core.agent.AgentRuntime;
import com.agentbot.core.automation.AutomationService;
import com.agentbot.core.bus.ExternalMessageBus;
import com.agentbot.core.cron.CronService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutomationConfiguration {

  @Bean
  public AutomationService automationService(
      AgentRuntime runtime,
      ExternalMessageBus messageBus,
      AgentbotProperties properties,
      com.agentbot.core.session.SessionService sessionService,
      com.agentbot.core.session.ChatUnreadService unreadService
  ) {
    return new AutomationService(runtime, messageBus, properties, sessionService, unreadService);
  }



  @Bean
  public CronService cronService(AgentbotProperties properties, AutomationService automationService, com.agentbot.core.cron.CronJobStore cronJobStore) {
    CronService service = new CronService(cronJobStore);

    AgentbotProperties.Cron cron = properties.getCron();
    if (cron.isEnabled() && cron.getDefaultPrompt() != null && !cron.getDefaultPrompt().isBlank()) {
      service.scheduleEverySeconds(
          cron.getDefaultIntervalSeconds(),
          cron.getDefaultPrompt(),
          "cron",
          () -> automationService.triggerCron("cron", cron.getDefaultPrompt())
      );
    }

    cronJobStore.load().forEach(job -> service.addJobWithId(
        job,
        () -> automationService.triggerCronWithDelivery(
            job.getSessionKey(),
            job.getPrompt(),
            new com.agentbot.core.automation.AutomationService.DeliveryOptions(
                job.isDeliver(),
                job.getChannel(),
                job.getTo()
            )
        )
    ));

    return service;
  }

  // Note: HeartbeatService is now configured in AgentConfiguration.java
  // using the new implementation with HeartbeatTaskExecutor
}

