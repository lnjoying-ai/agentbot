package com.agentbot.core.tools.impl;

import com.agentbot.core.memory.MemoryStore;
import com.agentbot.core.tools.ToolDefinition;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolWithDefinition;

import java.util.List;
import java.util.Map;

public class MemoryGetTool implements ToolWithDefinition {
  private final MemoryStore store;

  public MemoryGetTool(MemoryStore store) {
    this.store = store;
  }

  @Override
  public String name() {
    return "memory_get";
  }

  @Override
  public ToolExecutionResult execute(Map<String, Object> args) {
    List<String> lines = store.readLongTerm();
    if (lines.isEmpty()) {
      return new ToolExecutionResult(true, "memory is empty");
    }
    return new ToolExecutionResult(true, String.join("\n", lines));
  }

  @Override
  public ToolDefinition definition() {
    return new ToolDefinition(
        "memory_get",
        "Get long-term memory content.",
        Map.of("type", "object", "properties", Map.of())
    );
  }
}
