package com.agentbot.core.p2p;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PeerSelector {
  public List<PeerAddress> select(List<PeerAddress> peers, int max) {
    if (peers == null || peers.isEmpty() || max <= 0) return List.of();
    List<PeerAddress> sorted = new ArrayList<>(peers);
    sorted.sort(Comparator.comparingDouble(this::score).reversed());
    if (sorted.size() <= max) return sorted;
    List<PeerAddress> head = new ArrayList<>(sorted.subList(0, Math.min(sorted.size(), max * 2)));
    Collections.shuffle(head);
    return head.subList(0, max);
  }

  private double score(PeerAddress peer) {
    double success = peer.getSuccessRate();
    double latency = peer.getLatencyMs() <= 0 ? 0.3 : Math.max(0.0, 1.0 - Math.min(peer.getLatencyMs(), 2000) / 2000.0);
    double failPenalty = Math.min(1.0, peer.getFailCount() / 8.0);
    return success * 0.6 + latency * 0.3 - failPenalty * 0.4;
  }
}
