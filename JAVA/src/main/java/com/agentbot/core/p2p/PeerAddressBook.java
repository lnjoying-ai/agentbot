package com.agentbot.core.p2p;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerAddressBook {
  private final ObjectMapper yamlMapper;
  private final Path peersFile;
  private final Map<String, PeerAddress> peers = new ConcurrentHashMap<>();

  public PeerAddressBook(ObjectMapper yamlMapper, Path peersFile) {
    this.yamlMapper = yamlMapper;
    this.peersFile = peersFile;
  }

  public void load() {
    if (!Files.exists(peersFile)) {
      return;
    }
    try {
      List<PeerAddress> list = yamlMapper.readValue(peersFile.toFile(), new TypeReference<List<PeerAddress>>() {});
      if (list == null) return;
      for (PeerAddress peer : list) {
        if (peer == null || peer.getHost() == null || peer.getHost().isBlank() || peer.getPort() <= 0) {
          continue;
        }
        peers.put(peer.key(), peer);
      }
    } catch (Exception ignored) {
      // ignore invalid peers file
    }
  }

  public void save() {
    try {
      Files.createDirectories(peersFile.getParent());
      List<PeerAddress> list = new ArrayList<>(peers.values());
      list.sort(Comparator.comparing(PeerAddress::getHost).thenComparingInt(PeerAddress::getPort));
      yamlMapper.writerWithDefaultPrettyPrinter().writeValue(peersFile.toFile(), list);
    } catch (Exception ignored) {
      // ignore persistence failures
    }
  }

  public List<PeerAddress> list() {
    List<PeerAddress> list = new ArrayList<>(peers.values());
    list.sort(Comparator.comparingDouble(this::score).reversed());
    return Collections.unmodifiableList(list);
  }

  public void upsert(PeerAddress incoming) {
    if (incoming == null || incoming.getHost() == null || incoming.getHost().isBlank() || incoming.getPort() <= 0) {
      return;
    }
    String key = incoming.key();
    peers.compute(key, (k, existing) -> merge(existing, incoming));
  }

  public void markSuccess(PeerAddress peer, long latencyMs) {
    if (peer == null) return;
    peer.setLastSeen(System.currentTimeMillis());
    peer.setFailCount(Math.max(0, peer.getFailCount() - 1));
    peer.setSuccessRate(peer.getSuccessRate() * 0.8 + 0.2);
    peer.setLatencyMs(latencyMs);
    upsert(peer);
  }

  public void markFailure(PeerAddress peer) {
    if (peer == null) return;
    peer.setFailCount(peer.getFailCount() + 1);
    peer.setSuccessRate(peer.getSuccessRate() * 0.8);
    upsert(peer);
  }

  public void markSuccessByNodeId(String nodeId, long latencyMs) {
    PeerAddress peer = findByNodeId(nodeId);
    if (peer == null) return;
    markSuccess(peer, latencyMs);
  }

  public void markFailureByNodeId(String nodeId) {
    PeerAddress peer = findByNodeId(nodeId);
    if (peer == null) return;
    markFailure(peer);
  }

  private PeerAddress findByNodeId(String nodeId) {
    if (nodeId == null || nodeId.isBlank()) return null;
    for (PeerAddress peer : peers.values()) {
      if (nodeId.equals(peer.getNodeId())) {
        return peer;
      }
    }
    return null;
  }


  private PeerAddress merge(PeerAddress existing, PeerAddress incoming) {
    if (existing == null) {
      normalize(incoming);
      return incoming;
    }
    if (incoming.getLastSeen() > existing.getLastSeen()) {
      existing.setLastSeen(incoming.getLastSeen());
    }
    if (incoming.getSuccessRate() > 0) {
      existing.setSuccessRate(Math.max(existing.getSuccessRate(), incoming.getSuccessRate()));
    }
    if (incoming.getFailCount() > existing.getFailCount()) {
      existing.setFailCount(incoming.getFailCount());
    }
    if (incoming.getLatencyMs() > 0) {
      existing.setLatencyMs(incoming.getLatencyMs());
    }
    if (sourcePriority(incoming.getSource()) > sourcePriority(existing.getSource())) {
      existing.setSource(incoming.getSource());
    }
    if (existing.getNodeId() == null || existing.getNodeId().isBlank()) {
      existing.setNodeId(incoming.getNodeId());
    }
    normalize(existing);
    return existing;
  }

  private void normalize(PeerAddress peer) {
    if (peer.getSuccessRate() <= 0) {
      peer.setSuccessRate(0.3);
    }
    if (peer.getLastSeen() <= 0) {
      peer.setLastSeen(System.currentTimeMillis());
    }
  }

  private double score(PeerAddress peer) {
    long now = System.currentTimeMillis();
    long age = now - peer.getLastSeen();
    double recency = age <= 0 ? 1.0 : Math.max(0.0, 1.0 - (double) age / (7L * 24 * 3600 * 1000));
    double sourceWeight = sourcePriority(peer.getSource()) / 4.0;
    double success = peer.getSuccessRate();
    double failPenalty = Math.min(1.0, peer.getFailCount() / 10.0);
    return success * 0.5 + recency * 0.3 + sourceWeight * 0.2 - failPenalty * 0.2;
  }

  private int sourcePriority(String source) {
    if (source == null) return 1;
    String s = source.toLowerCase();
    if ("manual".equals(s)) return 4;
    if ("peers".equals(s)) return 3;
    if ("addr".equals(s)) return 2;
    return 1;
  }
}
