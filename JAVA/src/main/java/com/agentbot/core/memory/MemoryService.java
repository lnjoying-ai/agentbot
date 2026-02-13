package com.agentbot.core.memory;

import java.util.List;

public class MemoryService {
  private final MemoryStore store;

  public MemoryService(MemoryStore store) {
    this.store = store;
  }

  public List<String> loadContext() {
    return store.readLongTerm();
  }

  public void appendEvent(String line) {
    store.appendDaily(line);
  }
}
