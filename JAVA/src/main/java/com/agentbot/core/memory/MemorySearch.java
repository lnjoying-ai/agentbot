package com.agentbot.core.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MemorySearch {
  private final MemoryStore store;

  public MemorySearch(MemoryStore store) {
    this.store = store;
  }

  public List<String> search(String query, int limit) {
    if (query == null || query.isBlank()) return Collections.emptyList();
    String needle = query.trim().toLowerCase();
    List<String> results = new ArrayList<>();
    for (String line : store.readAll()) {
      if (line == null || line.isBlank()) continue;
      if (line.toLowerCase().contains(needle)) {
        results.add(line);
      }
      if (results.size() >= Math.max(1, limit)) break;
    }
    return results;
  }
}

