package com.agentbot.core.cron;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CronJobStore {
  private final ObjectMapper mapper;
  private final Path storePath;

  public CronJobStore(ObjectMapper mapper, Path storePath) {
    this.mapper = mapper;
    this.storePath = storePath;
  }

  public Path getStorePath() {
    return storePath;
  }

  public List<CronJob> load() {
    if (!Files.exists(storePath)) {
      return Collections.emptyList();
    }
    try {
      List<CronJobRecord> records = mapper.readValue(storePath.toFile(), new TypeReference<List<CronJobRecord>>() {});
      return records.stream().map(CronJobStore::toCronJob).collect(Collectors.toList());
    } catch (Exception ignored) {
      return Collections.emptyList();
    }
  }

  public void save(Iterable<CronJob> jobs) {
    try {
      Files.createDirectories(storePath.getParent());
      List<CronJobRecord> records = new java.util.ArrayList<>();
      for (CronJob job : jobs) {
        records.add(fromCronJob(job));
      }
      mapper.writerWithDefaultPrettyPrinter().writeValue(storePath.toFile(), records);
    } catch (Exception ignored) {
      // ignore
    }
  }

  private static CronJob toCronJob(CronJobRecord record) {
    Instant runAt = record.runAt == null || record.runAt.isBlank() ? null : Instant.parse(record.runAt);
    return new CronJob(
        record.id,
        record.name,
        record.scheduleType,
        record.everySeconds,
        record.cronExpr,
        runAt,
        record.prompt,
        record.sessionKey,
        record.deliver,
        record.to,
        record.channel,
        record.enabled
    );
  }

  private static CronJobRecord fromCronJob(CronJob job) {
    CronJobRecord record = new CronJobRecord();
    record.id = job.getId();
    record.name = job.getName();
    record.scheduleType = job.getScheduleType();
    record.everySeconds = job.getEverySeconds();
    record.cronExpr = job.getCronExpr();
    record.runAt = job.getRunAt() == null ? null : job.getRunAt().toString();
    record.prompt = job.getPrompt();
    record.sessionKey = job.getSessionKey();
    record.deliver = job.isDeliver();
    record.to = job.getTo();
    record.channel = job.getChannel();
    record.enabled = job.isEnabled();
    return record;
  }

  public static class CronJobRecord {
    public String id;
    public String name;
    public String scheduleType;
    public Long everySeconds;
    public String cronExpr;
    public String runAt;
    public String prompt;
    public String sessionKey;
    public boolean deliver;
    public String to;
    public String channel;
    public boolean enabled;
  }
}
