package com.agentbot.core.cron;

import java.time.Instant;

public class CronJob {
  private final String id;
  private final String name;
  private final String scheduleType;
  private final Long everySeconds;
  private final String cronExpr;
  private final Instant runAt;
  private final String prompt;
  private final String sessionKey;
  private final boolean deliver;
  private final String to;
  private final String channel;
  private boolean enabled;
  private final Instant createdAt;

  public CronJob(String id,
                 String name,
                 String scheduleType,
                 Long everySeconds,
                 String cronExpr,
                 Instant runAt,
                 String prompt,
                 String sessionKey,
                 boolean deliver,
                 String to,
                 String channel,
                 boolean enabled) {
    this.id = id;
    this.name = name;
    this.scheduleType = scheduleType;
    this.everySeconds = everySeconds;
    this.cronExpr = cronExpr;
    this.runAt = runAt;
    this.prompt = prompt;
    this.sessionKey = sessionKey;
    this.deliver = deliver;
    this.to = to;
    this.channel = channel;
    this.enabled = enabled;
    this.createdAt = Instant.now();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getScheduleType() {
    return scheduleType;
  }

  public Long getEverySeconds() {
    return everySeconds;
  }

  public String getCronExpr() {
    return cronExpr;
  }

  public Instant getRunAt() {
    return runAt;
  }

  public String getPrompt() {
    return prompt;
  }

  public String getSessionKey() {
    return sessionKey;
  }

  public boolean isDeliver() {
    return deliver;
  }

  public String getTo() {
    return to;
  }

  public String getChannel() {
    return channel;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

