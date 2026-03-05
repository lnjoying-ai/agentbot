package com.agentbot.core.cron;

import org.springframework.scheduling.support.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CronService {
  private static final Logger log = LoggerFactory.getLogger(CronService.class);
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final Map<String, Runnable> jobs = new ConcurrentHashMap<>();
  private final Map<String, CronJob> jobMetadata = new ConcurrentHashMap<>();
  private final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();
  private final CronJobStore jobStore;

  public CronService() {
    this(null);
  }

  public CronService(CronJobStore jobStore) {
    this.jobStore = jobStore;
  }

  public String scheduleEverySeconds(long seconds, String prompt, String sessionKey, Runnable task) {

    CronJob job = addJob("cron-" + seconds + "s", "every", seconds, null, null, prompt, sessionKey, false, null, null, task);
    return job.getId();
  }

  public CronJob addJob(String name,
                        String scheduleType,
                        Long everySeconds,
                        String cronExpr,
                        Instant runAt,
                        String prompt,
                        String sessionKey,
                        boolean deliver,
                        String to,
                        String channel,
                        Runnable task) {
    String id = UUID.randomUUID().toString();
    CronJob job = new CronJob(
        id,
        name,
        scheduleType,
        everySeconds,
        cronExpr,
        runAt,
        prompt,
        sessionKey,
        deliver,
        to,
        channel,
        true
    );
    return addJobWithId(job, task);
  }

  public CronJob addJobWithId(CronJob job, Runnable task) {
    String id = job.getId();
    ScheduledFuture<?> future = futures.remove(id);
    if (future != null) future.cancel(false);
    jobMetadata.put(id, job);
    jobs.put(id, task);
    scheduleJob(job, task);
    return job;
  }

  public CronJob enableJob(String id, boolean enabled) {
    CronJob job = jobMetadata.get(id);
    if (job == null) return null;
    job.setEnabled(enabled);
    ScheduledFuture<?> future = futures.remove(id);
    if (future != null) future.cancel(false);
    if (enabled) {
      Runnable task = jobs.get(id);
      if (task != null) {
        scheduleJob(job, task);
      }
    }
    return job;
  }

  public boolean removeJob(String id) {
    CronJob job = jobMetadata.remove(id);
    jobs.remove(id);
    ScheduledFuture<?> future = futures.remove(id);
    if (future != null) future.cancel(false);
    return job != null;
  }

  public boolean runJob(String id, boolean force) {
    CronJob job = jobMetadata.get(id);
    Runnable task = jobs.get(id);
    if (job == null || task == null) {
      log.debug("Cron run skipped: missing job or task, id={}", id);
      return false;
    }
    if (!job.isEnabled() && !force) {
      log.debug("Cron run skipped: disabled job, id={}, scheduleType={}", id, job.getScheduleType());
      return false;
    }
    try {
      task.run();
    } catch (RuntimeException ex) {
      log.warn("Cron job execution failed: id={}, scheduleType={}", id, job.getScheduleType(), ex);
      throw ex;
    } finally {
      if (isOneTime(job)) {
        disableJob(job);
      }
    }
    return true;
  }


  public Collection<CronJob> listJobs() {
    return jobMetadata.values();
  }

  public void stop() {
    scheduler.shutdownNow();
  }

  private void scheduleJob(CronJob job, Runnable task) {
    if (!job.isEnabled()) {
      log.debug("Cron schedule skipped: disabled job, id={}, scheduleType={}", job.getId(), job.getScheduleType());
      return;
    }
    switch (job.getScheduleType()) {
      case "every" -> {
        long seconds = Math.max(1, job.getEverySeconds() == null ? 1 : job.getEverySeconds());
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(task, seconds, seconds, TimeUnit.SECONDS);
        futures.put(job.getId(), future);
      }
      case "at" -> {
        if (job.getRunAt() == null) {
          log.warn("Cron schedule skipped: missing runAt, id={}", job.getId());
          return;
        }
        long delay = Math.max(0, job.getRunAt().toEpochMilli() - System.currentTimeMillis());
        ScheduledFuture<?> future = scheduler.schedule(() -> {
          try {
            if (job.isEnabled()) {
              task.run();
            }
          } catch (RuntimeException ex) {
            log.warn("Cron job execution failed: id={}, scheduleType={}", job.getId(), job.getScheduleType(), ex);
            throw ex;
          } finally {
            disableJob(job);
          }
        }, delay, TimeUnit.MILLISECONDS);
        futures.put(job.getId(), future);
      }

      case "cron" -> {
        if (job.getCronExpr() == null || job.getCronExpr().isBlank()) {
          log.warn("Cron schedule skipped: missing cron expr, id={}", job.getId());
          return;
        }
        try {
          scheduleCron(job, task);
        } catch (Exception ex) {
          log.warn("Cron schedule failed: invalid cron expr, id={}, expr={}", job.getId(), job.getCronExpr(), ex);
          job.setEnabled(false);
        }
      }

      default -> {
      }
    }
  }

  private void scheduleCron(CronJob job, Runnable task) {
    String expr = normalizeCronExpr(job.getCronExpr());
    CronExpression expression = CronExpression.parse(expr);

    ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
    ZonedDateTime next = expression.next(now);
    if (next == null) {
      log.warn("Cron schedule stopped: no next run, id={}, expr={}", job.getId(), job.getCronExpr());
      return;
    }
    long delay = Math.max(0, next.toInstant().toEpochMilli() - System.currentTimeMillis());
    ScheduledFuture<?> future = scheduler.schedule(() -> {
      try {
        if (job.isEnabled()) {
          task.run();
        }
      } catch (RuntimeException ex) {
        log.warn("Cron job execution failed: id={}, scheduleType={}", job.getId(), job.getScheduleType(), ex);
        throw ex;
      } finally {
        if (job.isEnabled()) {
          scheduleCron(job, task);
        }
      }
    }, delay, TimeUnit.MILLISECONDS);
    futures.put(job.getId(), future);
  }

  private String normalizeCronExpr(String cronExpr) {
    if (cronExpr == null) return null;
    String trimmed = cronExpr.trim();
    if (trimmed.isEmpty()) return trimmed;
    String[] parts = trimmed.split("\\s+");
    if (parts.length == 5) {
      return "0 " + trimmed;
    }
    return trimmed;
  }

  private boolean isOneTime(CronJob job) {
    return job != null && "at".equalsIgnoreCase(job.getScheduleType());
  }


  private void disableJob(CronJob job) {
    if (job == null) return;
    job.setEnabled(false);
    ScheduledFuture<?> future = futures.remove(job.getId());
    if (future != null) future.cancel(false);
    log.debug("Cron job disabled: id={}, scheduleType={}", job.getId(), job.getScheduleType());
    if (jobStore != null) {
      try {
        jobStore.save(listJobs());
      } catch (Exception ex) {
        log.warn("Cron job state persist failed: id={}", job.getId(), ex);
      }
    }
  }

}


