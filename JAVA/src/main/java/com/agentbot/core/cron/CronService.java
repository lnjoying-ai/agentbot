package com.agentbot.core.cron;

import org.springframework.scheduling.support.CronExpression;

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
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final Map<String, Runnable> jobs = new ConcurrentHashMap<>();
  private final Map<String, CronJob> jobMetadata = new ConcurrentHashMap<>();
  private final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

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
    if (job == null || task == null) return false;
    if (!job.isEnabled() && !force) return false;
    task.run();
    return true;
  }

  public Collection<CronJob> listJobs() {
    return jobMetadata.values();
  }

  public void stop() {
    scheduler.shutdownNow();
  }

  private void scheduleJob(CronJob job, Runnable task) {
    if (!job.isEnabled()) return;
    switch (job.getScheduleType()) {
      case "every" -> {
        long seconds = Math.max(1, job.getEverySeconds() == null ? 1 : job.getEverySeconds());
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(task, seconds, seconds, TimeUnit.SECONDS);
        futures.put(job.getId(), future);
      }
      case "at" -> {
        if (job.getRunAt() == null) return;
        long delay = Math.max(0, job.getRunAt().toEpochMilli() - System.currentTimeMillis());
        ScheduledFuture<?> future = scheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
        futures.put(job.getId(), future);
      }
      case "cron" -> {
        if (job.getCronExpr() == null || job.getCronExpr().isBlank()) return;
        try {
          scheduleCron(job, task);
        } catch (Exception ex) {
          job.setEnabled(false);
        }
      }

      default -> {
      }
    }
  }

  private void scheduleCron(CronJob job, Runnable task) {
    CronExpression expression = CronExpression.parse(job.getCronExpr());
    ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
    ZonedDateTime next = expression.next(now);
    if (next == null) return;
    long delay = Math.max(0, next.toInstant().toEpochMilli() - System.currentTimeMillis());
    ScheduledFuture<?> future = scheduler.schedule(() -> {
      try {
        if (job.isEnabled()) {
          task.run();
        }
      } finally {
        if (job.isEnabled()) {
          scheduleCron(job, task);
        }
      }
    }, delay, TimeUnit.MILLISECONDS);
    futures.put(job.getId(), future);
  }
}


