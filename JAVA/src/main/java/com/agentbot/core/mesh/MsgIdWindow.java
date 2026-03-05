package com.agentbot.core.mesh;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MsgIdWindow {
  private static final long DEFAULT_WINDOW_MS = 60_000L;

  private final long windowMs;
  private final Map<String, Long> seen = new ConcurrentHashMap<>();
  private volatile long lastSweepMs;

  public MsgIdWindow(int windowConfig) {
    long ms;
    if (windowConfig <= 0) {
      ms = DEFAULT_WINDOW_MS;
    } else if (windowConfig < 1000) {
      ms = windowConfig * 1000L;
    } else {
      ms = windowConfig;
    }
    this.windowMs = ms;
  }

  public boolean markIfNew(String msgId) {
    if (msgId == null || msgId.isBlank()) {
      return true;
    }
    long now = System.currentTimeMillis();
    sweepIfNeeded(now);
    Long existing = seen.get(msgId);
    if (existing != null && now - existing <= windowMs) {
      return false;
    }
    seen.put(msgId, now);
    return true;
  }

  private void sweepIfNeeded(long now) {
    if (now - lastSweepMs < windowMs) {
      return;
    }
    lastSweepMs = now;
    seen.entrySet().removeIf(entry -> now - entry.getValue() > windowMs);
  }
}
