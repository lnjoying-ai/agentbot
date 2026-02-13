package com.agentbot.core.memory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class MemoryStore {
  private final Path memoryDir;

  public MemoryStore(Path memoryDir) {
    this.memoryDir = memoryDir;
  }

  public List<String> readLongTerm() {
    Path memoryFile = memoryDir.resolve("MEMORY.md");
    if (!Files.exists(memoryFile)) return Collections.emptyList();
    try {
      return Files.readAllLines(memoryFile);
    } catch (Exception ignored) {
      return Collections.emptyList();
    }
  }

  public List<String> readDaily() {
    Path memoryFile = memoryDir.resolve("memory.log");
    if (!Files.exists(memoryFile)) return Collections.emptyList();
    try {
      return Files.readAllLines(memoryFile);
    } catch (Exception ignored) {
      return Collections.emptyList();
    }
  }

  public List<String> readAll() {
    List<String> combined = new java.util.ArrayList<>();
    combined.addAll(readLongTerm());
    combined.addAll(readDaily());
    return combined;
  }

  public void appendDaily(String line) {
    try {
      Files.createDirectories(memoryDir);
      Path file = memoryDir.resolve("memory.log");
      Files.writeString(file, line + System.lineSeparator(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    } catch (Exception ignored) {
      // ignore
    }
  }

}
