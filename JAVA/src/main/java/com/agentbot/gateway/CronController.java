package com.agentbot.gateway;

import com.agentbot.core.automation.AutomationService;
import com.agentbot.core.cron.CronJob;
import com.agentbot.core.cron.CronService;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/api/cron")
public class CronController {
  private final CronService cronService;
  private final AutomationService automationService;

  public CronController(CronService cronService, AutomationService automationService) {
    this.cronService = cronService;
    this.automationService = automationService;
  }

  @PostMapping("/schedule")
  public Map<String, Object> schedule(@RequestBody CronScheduleRequest request) {
    String prompt = request.prompt() == null ? "" : request.prompt().trim();
    if (prompt.isEmpty()) {
      return Map.of("ok", false, "error", "prompt is required");
    }
    String sessionKey = request.sessionKey() == null ? "cron" : request.sessionKey();
    String name = request.name() == null || request.name().isBlank() ? "cron-job" : request.name();

    String scheduleType = "every";
    Long everySeconds = Math.max(5, request.intervalSeconds());
    String cronExpr = null;
    java.time.Instant runAt = null;

    if (request.cronExpr() != null && !request.cronExpr().isBlank()) {
      scheduleType = "cron";
      cronExpr = request.cronExpr();
      everySeconds = null;
    } else if (request.runAt() != null && !request.runAt().isBlank()) {
      scheduleType = "at";
      everySeconds = null;
      try {
        runAt = java.time.Instant.parse(request.runAt());
      } catch (Exception ex) {
        return Map.of("ok", false, "error", "runAt must be ISO-8601 format");
      }
    }

    CronJob job = cronService.addJob(
        name,
        scheduleType,
        everySeconds,
        cronExpr,
        runAt,
        prompt,
        sessionKey,
        request.deliver(),
        request.to(),
        request.channel(),
        () -> automationService.triggerCron(sessionKey, prompt)
    );
    return Map.of("ok", true, "id", job.getId());
  }

  @GetMapping("/jobs")
  public Collection<CronJob> jobs() {
    return cronService.listJobs();
  }

  public record CronScheduleRequest(
      long intervalSeconds,
      String prompt,
      String sessionKey,
      String name,
      String cronExpr,
      String runAt,
      boolean deliver,
      String to,
      String channel
  ) {}
}

