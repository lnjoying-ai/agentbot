package com.agentbot.core.events;

import java.time.Instant;
import java.util.Map;

public class SystemEvent {
  private final String type;
  private final Instant timestamp;
  private final Map<String, Object> payload;

  public SystemEvent(String type, Map<String, Object> payload) {
    this.type = type;
    this.payload = payload;
    this.timestamp = Instant.now();
  }

  public String getType() {
    return type;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }
}
