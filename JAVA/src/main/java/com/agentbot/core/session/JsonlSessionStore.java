package com.agentbot.core.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JsonlSessionStore implements SessionStore {
  private final Path baseDir;
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  public JsonlSessionStore(Path baseDir) {
    this.baseDir = baseDir;
  }

  @Override
  public void append(String sessionKey, SessionMessage message) {
    try {
      Files.createDirectories(baseDir);
      Path file = sessionFile(sessionKey);
      try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
        writer.write(mapper.writeValueAsString(message));
        writer.newLine();
      }
    } catch (IOException ignored) {
      // TODO: add logging
    }
  }

  @Override
  public List<SessionMessage> loadRecent(String sessionKey, int limit) {
    Path file = sessionFile(sessionKey);
    if (!Files.exists(file)) return Collections.emptyList();
    List<SessionMessage> all = new ArrayList<>();
    try {
      List<String> lines = Files.readAllLines(file);
      for (String line : lines) {
        if (line == null || line.isBlank()) continue;
        all.add(mapper.readValue(line, SessionMessage.class));
      }
    } catch (IOException ignored) {
      return Collections.emptyList();
    }
    int start = Math.max(0, all.size() - limit);
    return all.subList(start, all.size());
  }

  @Override
  public void clear(String sessionKey) {
    try {
      Files.deleteIfExists(sessionFile(sessionKey));
    } catch (IOException ignored) {
      // ignore
    }
  }

  private Path sessionFile(String sessionKey) {
    String safe = sessionKey.replace(":", "_");
    return baseDir.resolve(safe + ".jsonl");
  }
}
