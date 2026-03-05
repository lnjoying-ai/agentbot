package com.agentbot.core.mesh;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerManager {
  private final Map<String, PeerStats> peers = new ConcurrentHashMap<>();

  public void recordLatency(String nodeId, long latencyMs) {
    if (nodeId == null || nodeId.isBlank()) return;
    peers.computeIfAbsent(nodeId, key -> new PeerStats()).latencyMs = latencyMs;
  }

  public void recordDrop(String nodeId) {
    if (nodeId == null || nodeId.isBlank()) return;
    peers.computeIfAbsent(nodeId, key -> new PeerStats()).drops++;
  }

  public Map<String, PeerStats> snapshot() {
    return Map.copyOf(peers);
  }

  public static class PeerStats {
    private long latencyMs;
    private long drops;

    public long getLatencyMs() {
      return latencyMs;
    }

    public long getDrops() {
      return drops;
    }
  }
}
