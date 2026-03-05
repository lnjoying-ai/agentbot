package com.agentbot.core.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * MemoryService manages long-term and daily memory for the agent.
 * 
 * Memory Structure:
 * - MEMORY.md: Long-term persistent memory (manually curated)
 * - memory.log: Daily event log (automatically appended)
 * 
 * Inspired by nanobot's memory system.
 */
public class MemoryService {
  private static final Logger log = LoggerFactory.getLogger(MemoryService.class);
  private final MemoryStore store;

  public MemoryService(MemoryStore store) {
    this.store = store;
  }

  /**
   * Load context from long-term memory (MEMORY.md).
   * This is automatically included in every conversation.
   */
  public List<String> loadContext() {
    List<String> memory = store.readLongTerm();
    if (!memory.isEmpty()) {
      log.debug("Loaded {} lines from long-term memory", memory.size());
    }
    return memory;
  }

  /**
   * Append an event to the daily memory log.
   * 
   * @param line Event description
   */
  public void appendEvent(String line) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    String logEntry = String.format("[%s] %s", timestamp, line);
    store.appendDaily(logEntry);
    log.debug("Appended to daily memory: {}", line);
  }

  /**
   * Append important information to long-term memory (MEMORY.md).
   * This should be used sparingly for truly important facts.
   * 
   * @param section Section to append to (e.g., "User Information", "Project Context")
   * @param content Content to append
   * @return true if successful
   */
  public boolean appendToLongTerm(String section, String content) {
    return store.appendToMemoryMd(section, content);
  }

  /**
   * Load all memory (both long-term and daily).
   */
  public List<String> loadAll() {
    return store.readAll();
  }

  /**
   * Search memory for specific patterns or keywords.
   */
  public List<String> search(String keyword) {
    return store.search(keyword);
  }

  /**
   * Clear the daily memory log (useful for maintenance).
   */
  public void clearDailyLog() {
    store.clearDaily();
    log.info("Cleared daily memory log");
  }

  /**
   * Get memory statistics.
   */
  public MemoryStats getStats() {
    return store.getStats();
  }
}

/**
 * Memory statistics.
 */
class MemoryStats {
  private final int longTermLines;
  private final int dailyLines;
  private final long longTermSizeBytes;
  private final long dailySizeBytes;

  public MemoryStats(int longTermLines, int dailyLines, long longTermSizeBytes, long dailySizeBytes) {
    this.longTermLines = longTermLines;
    this.dailyLines = dailyLines;
    this.longTermSizeBytes = longTermSizeBytes;
    this.dailySizeBytes = dailySizeBytes;
  }

  public int getLongTermLines() { return longTermLines; }
  public int getDailyLines() { return dailyLines; }
  public long getLongTermSizeBytes() { return longTermSizeBytes; }
  public long getDailySizeBytes() { return dailySizeBytes; }

  @Override
  public String toString() {
    return String.format("MemoryStats{longTerm=%d lines (%.1f KB), daily=%d lines (%.1f KB)}",
        longTermLines, longTermSizeBytes / 1024.0,
        dailyLines, dailySizeBytes / 1024.0);
  }
}
