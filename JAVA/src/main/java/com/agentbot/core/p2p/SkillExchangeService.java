package com.agentbot.core.p2p;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.protocol.InvItem;
import com.agentbot.core.protocol.InvMessage;
import com.agentbot.core.protocol.MessageType;
import com.agentbot.core.skills.SkillStoreService;
import com.agentbot.core.skills.SkillStoreService.SkillDataPayload;
import com.agentbot.core.skills.SkillStoreService.SkillIndex;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


public class SkillExchangeService {
  private static final Logger log = LoggerFactory.getLogger(SkillExchangeService.class);
  private static final String INV_TYPE_SKILL = "skill";

  private final AgentbotProperties.P2p p2p;
  private final P2pSettings settings;
  private final SkillStoreService storeService;
  private final ObjectMapper mapper = new ObjectMapper();
  private long getdataWindowStartMs;
  private int getdataWindowCount;


  public SkillExchangeService(AgentbotProperties.P2p p2p, P2pSettings settings, SkillStoreService storeService) {
    this.p2p = p2p;
    this.settings = settings;
    this.storeService = storeService;
  }

  public List<InvItem> buildInvItems(int maxItems) {
    storeService.refreshIndex();
    List<SkillIndex> entries = new ArrayList<>();
    entries.addAll(storeService.listLocalSkills());
    entries.addAll(storeService.listStoreSkills());
    if (entries.isEmpty()) return List.of();

    entries.sort(Comparator.comparingLong(SkillIndex::getUpdatedAt).reversed());
    if (maxItems > 0 && entries.size() > maxItems) {
      entries = entries.subList(0, maxItems);
    }

    List<InvItem> items = new ArrayList<>();
    for (SkillIndex entry : entries) {
      InvItem item = new InvItem();
      item.setDataId(entry.getId());
      item.setDataType(INV_TYPE_SKILL);
      item.setPayloadHash(entry.getHash());

      item.setSize(entry.getSize());
      item.setOrigin(entry.getOrigin());
      item.setScope(entry.getScope());
      item.setUpdatedAt(entry.getUpdatedAt());
      item.setPriority(0);
      items.add(item);

    }
    return items;
  }

  public void broadcastInv(List<P2pConnection> connections) {
    if (connections == null || connections.isEmpty()) return;
    int maxInv = p2p.getSkillInvMaxPerRound();
    List<InvItem> items = buildInvItems(maxInv);
    if (items.isEmpty()) return;

    InvMessage inv = new InvMessage();
    inv.setItems(items);

    for (P2pConnection conn : connections) {
      if (conn == null || !conn.isHandshakeComplete()) continue;
      try {
        conn.send(MessageType.INV, inv);
        P2pMetrics.recordSkillInvSent(items.size());
      } catch (Exception e) {
        log.debug("Skill INV send failed: remote={}", conn.remoteHost(), e);
      }
    }
  }

  public boolean shouldFetch(InvItem item) {
    if (item == null || item.getDataId() == null || item.getDataId().isBlank()) return false;
    if (item.getDataType() == null || !INV_TYPE_SKILL.equalsIgnoreCase(item.getDataType())) {
      return true;
    }
    if (!p2p.isSkillExchangeEnabled()) return false;
    if (storeService.isIgnored(item.getDataId())) return false;

    if (storeService.hasSkillId(item.getDataId())) return false;
    if (item.getPayloadHash() != null && !item.getPayloadHash().isBlank()) {
      SkillId parsed = parseSkillId(item.getDataId());
      if (parsed != null && storeService.hasSkillHash(parsed.name, item.getPayloadHash())) {
        return false;
      }
    }

    if (p2p.getSkillMaxPackageBytes() > 0 && item.getSize() > p2p.getSkillMaxPackageBytes()) {
      return false;
    }
    return true;
  }

  public String buildDataPayload(String dataId) {
    SkillIndex entry = storeService.findSkillById(dataId).orElse(null);
    if (entry == null) return null;
    SkillDataPayload payload = storeService.buildSkillPayload(entry, settings == null ? null : settings.getNodeId());

    if (payload == null) return null;
    try {
      return mapper.writeValueAsString(payload);
    } catch (Exception e) {
      return null;
    }
  }

  public boolean handleIncomingData(String dataId, String payload) {
    if (payload == null || payload.isBlank()) return false;
    boolean ok = storeService.ingestSkillPayload(payload, dataId);

    if (ok) {
      P2pMetrics.recordSkillDataStored();
    } else {
      P2pMetrics.recordSkillDataRejected();
    }
    return ok;
  }

  public int sampleConnectionCount(int total) {
    double ratio = p2p.getSkillInvSampleRatio();
    if (total <= 0) return 0;
    if (ratio <= 0) return 0;
    if (ratio >= 1) return total;
    int count = (int) Math.ceil(total * ratio);
    return Math.max(1, Math.min(total, count));
  }

  public List<P2pConnection> pickRandomConnections(List<P2pConnection> candidates, int count) {
    if (candidates == null || candidates.isEmpty() || count <= 0) return List.of();
    List<P2pConnection> filtered = candidates.stream()
        .filter(c -> c != null && c.isHandshakeComplete())
        .collect(Collectors.toList());
    if (filtered.isEmpty()) return List.of();
    java.util.Collections.shuffle(filtered);
    if (count >= filtered.size()) return filtered;
    return new ArrayList<>(filtered.subList(0, count));
  }

  public int getGetdataMaxPerMinute() {
    return p2p.getSkillGetdataMaxPerMinute();
  }

  public int remainingGetdataBudget(long now) {
    int maxPerMinute = p2p.getSkillGetdataMaxPerMinute();
    if (maxPerMinute <= 0) return Integer.MAX_VALUE;
    if (getdataWindowStartMs <= 0 || now - getdataWindowStartMs >= 60_000L) {
      getdataWindowStartMs = now;
      getdataWindowCount = 0;
    }
    return Math.max(0, maxPerMinute - getdataWindowCount);
  }

  public void incrementGetdataWindowCount(int delta, long now) {
    int maxPerMinute = p2p.getSkillGetdataMaxPerMinute();
    if (maxPerMinute <= 0) return;
    if (getdataWindowStartMs <= 0 || now - getdataWindowStartMs >= 60_000L) {
      getdataWindowStartMs = now;
      getdataWindowCount = 0;
    }
    getdataWindowCount += delta;
  }

  public boolean isSkillItem(InvItem item, String dataId) {
    if (item != null && item.getDataType() != null) {
      return INV_TYPE_SKILL.equalsIgnoreCase(item.getDataType());
    }
    return parseSkillId(dataId) != null;
  }


  public static SkillId parseSkillId(String id) {
    if (id == null) return null;
    String[] parts = id.split(":", 3);
    if (parts.length < 3) return null;
    return new SkillId(parts[0], parts[1], parts[2]);
  }

  public record SkillId(String name, String version, String hash) {}
}

