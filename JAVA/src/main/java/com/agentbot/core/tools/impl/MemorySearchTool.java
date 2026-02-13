package com.agentbot.core.tools.impl;

import com.agentbot.core.memory.MemorySearch;
import com.agentbot.core.tools.ToolDefinition;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolWithDefinition;

import java.util.List;
import java.util.Map;

public class MemorySearchTool implements ToolWithDefinition {
  private final MemorySearch memorySearch;

  public MemorySearchTool(MemorySearch memorySearch) {
    this.memorySearch = memorySearch;
  }

  @Override
  public String name() {
    return "memory_search";
  }

  @Override
  public ToolExecutionResult execute(Map<String, Object> args) {
    String query = String.valueOf(args.getOrDefault("query", "")).trim();
    if (query.isEmpty()) {
      return new ToolExecutionResult(false, "query is required");
    }
    int limit = 5;
    Object limitArg = args.get("limit");
    if (limitArg instanceof Number number) {
      limit = Math.max(1, number.intValue());
    }
    List<String> matches = memorySearch.search(query, limit);
    if (matches.isEmpty()) {
      return new ToolExecutionResult(true, "no memory matched");
    }
    return new ToolExecutionResult(true, String.join("\n", matches));
  }

  @Override
  public ToolDefinition definition() {
    return new ToolDefinition(
        "memory_search",
        "Search long-term memory and daily logs.",
        Map.of(
            "type", "object",
            "properties", Map.of(
                "query", Map.of("type", "string"),
                "limit", Map.of("type", "integer")
            ),
            "required", java.util.List.of("query")
        )
    );
  }
}
