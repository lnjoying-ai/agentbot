package com.agentbot.core.tools.impl;

import com.agentbot.core.tools.ToolDefinition;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolWithDefinition;

import java.util.Map;

public class EchoTool implements ToolWithDefinition {
  @Override
  public String name() {
    return "echo";
  }

  @Override
  public ToolExecutionResult execute(Map<String, Object> args) {
    Object text = args.getOrDefault("text", "");
    return new ToolExecutionResult(true, String.valueOf(text));
  }

  @Override
  public ToolDefinition definition() {
    return new ToolDefinition(
        "echo",
        "Echo back provided text",
        Map.of(
            "type", "object",
            "properties", Map.of("text", Map.of("type", "string")),
            "required", java.util.List.of("text")
        )
    );
  }
}
