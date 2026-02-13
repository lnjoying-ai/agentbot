package com.agentbot.core.workspace;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class WorkspaceInitializer {
  private final Path workspaceDir;

  public WorkspaceInitializer(Path workspaceDir) {
    this.workspaceDir = workspaceDir;
  }

  public Map<String, Object> initialize() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("AGENTS.md", writeIfMissing("AGENTS.md", "# Agents\n\n- default: 总控代理\n"));
    result.put("MEMORY.md", writeIfMissing("MEMORY.md", "# Memory\n\n"));
    result.put("HEARTBEAT.md", writeIfMissing("HEARTBEAT.md", ""));
    result.put("CONFIG.md", writeIfMissing("CONFIG.md", "# Config\n\n"));
    return result;
  }

  private boolean writeIfMissing(String name, String content) {
    try {
      Files.createDirectories(workspaceDir);
      Path file = workspaceDir.resolve(name);
      if (Files.exists(file)) return false;
      Files.writeString(file, content);
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }
}
