package com.agentbot.core.mesh;

import com.agentbot.core.protocol.InvItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class InvStore {
  private final Map<String, InvItem> invIndex = new ConcurrentHashMap<>();
  private final Map<String, String> dataStore = new ConcurrentHashMap<>();

  public List<String> recordInv(List<InvItem> items) {
    return recordInv(items, item -> true);
  }

  public List<String> recordInv(List<InvItem> items, Predicate<InvItem> shouldFetch) {
    List<String> toFetch = new ArrayList<>();
    if (items == null) return toFetch;
    for (InvItem item : items) {
      if (item == null || item.getDataId() == null) continue;
      boolean isNew = invIndex.putIfAbsent(item.getDataId(), item) == null;
      if (isNew && shouldFetch != null && shouldFetch.test(item)) {
        toFetch.add(item.getDataId());
      }
    }
    return toFetch;
  }




  public void storeData(String msgId, String payload) {
    if (msgId == null || msgId.isBlank()) return;
    dataStore.put(msgId, payload);
  }

  public String getData(String msgId) {
    return msgId == null ? null : dataStore.get(msgId);
  }

  public InvItem getInv(String msgId) {
    return msgId == null ? null : invIndex.get(msgId);
  }

  public boolean hasInv(String msgId) {
    return msgId != null && invIndex.containsKey(msgId);
  }
}
