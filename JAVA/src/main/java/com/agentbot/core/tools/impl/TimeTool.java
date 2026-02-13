package com.agentbot.core.tools.impl;

import com.agentbot.core.tools.ToolDefinition;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolWithDefinition;

import java.time.Instant;
import java.util.Map;

public class TimeTool implements ToolWithDefinition {
  @Override
  public String name() {
    return "time_now";
  }

  @Override
  public ToolExecutionResult execute(Map<String, Object> args) {
    return new ToolExecutionResult(true, Instant.now().toString());
  }

  @Override
  public ToolDefinition definition() {
    return new ToolDefinition(
        "time_now",
        "Get current UTC time",
        Map.of("type", "object", "properties", Map.of())
    );
  }
}
