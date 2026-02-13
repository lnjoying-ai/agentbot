package com.agentbot.core.ops;

import java.time.Instant;
import java.util.Map;

public class LogEntry {
  private final Instant timestamp;
  private final String type;
  private final Map<String, Object> payload;

  public LogEntry(String type, Map<String, Object> payload) {
    this.timestamp = Instant.now();
    this.type = type;
    this.payload = payload;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getType() {
    return type;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }
}
