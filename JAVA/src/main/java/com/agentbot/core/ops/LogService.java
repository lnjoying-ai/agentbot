package com.agentbot.core.ops;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class LogService {
  private final int capacity;
  private final Deque<LogEntry> buffer = new ArrayDeque<>();

  public LogService(int capacity) {
    this.capacity = Math.max(50, capacity);
  }

  public synchronized void append(LogEntry entry) {
    if (entry == null) return;
    buffer.addLast(entry);
    while (buffer.size() > capacity) {
      buffer.removeFirst();
    }
  }

  public synchronized List<LogEntry> latest(int limit) {
    int size = Math.min(Math.max(1, limit), buffer.size());
    List<LogEntry> items = new ArrayList<>(size);
    int skip = buffer.size() - size;
    int index = 0;
    for (LogEntry entry : buffer) {
      if (index++ < skip) continue;
      items.add(entry);
    }
    return items;
  }
}
