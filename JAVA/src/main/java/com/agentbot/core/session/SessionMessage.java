package com.agentbot.core.session;

import java.time.Instant;

public class SessionMessage {
  private String role;
  private String content;
  private Instant timestamp;

  public SessionMessage() {}

  public SessionMessage(String role, String content, Instant timestamp) {
    this.role = role;
    this.content = content;
    this.timestamp = timestamp;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }
}
