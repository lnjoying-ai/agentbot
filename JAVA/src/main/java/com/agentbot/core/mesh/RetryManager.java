package com.agentbot.core.mesh;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RetryManager {
  private final Map<String, RetryState> retries = new ConcurrentHashMap<>();
  private final int maxAttempts;

  public RetryManager(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public void register(String msgId) {
    if (msgId == null || msgId.isBlank()) return;
    retries.putIfAbsent(msgId, new RetryState());
  }

  public void ack(String msgId) {
    if (msgId == null) return;
    retries.remove(msgId);
  }

  public void nack(String msgId) {
    if (msgId == null) return;
    RetryState state = retries.get(msgId);
    if (state != null) {
      state.attempts++;
    }
  }

  public boolean shouldRetry(String msgId) {
    RetryState state = retries.get(msgId);
    return state != null && state.attempts < maxAttempts;
  }

  private static class RetryState {
    private int attempts = 0;
  }
}
