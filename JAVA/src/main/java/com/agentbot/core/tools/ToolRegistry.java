package com.agentbot.core.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolRegistry {
  private final Map<String, Tool> tools = new HashMap<>();
  private final Map<String, ToolDefinition> definitions = new HashMap<>();

  public void register(Tool tool) {
    if (tool == null) return;
    tools.put(tool.name(), tool);
    if (tool instanceof ToolWithDefinition withDefinition) {
      ToolDefinition definition = withDefinition.definition();
      if (definition != null) definitions.put(definition.getName(), definition);
    }
  }

  public List<Map<String, Object>> definitionsForLlm() {
    List<Map<String, Object>> list = new ArrayList<>();
    for (ToolDefinition definition : definitions.values()) {
      list.add(Map.of(
          "type", "function",
          "function", Map.of(
              "name", definition.getName(),
              "description", definition.getDescription(),
              "parameters", definition.getParameters()
          )
      ));
    }
    return list;
  }

  public ToolExecutionResult execute(String name, Map<String, Object> args) {
    Tool tool = tools.get(name);
    if (tool == null) {
      return new ToolExecutionResult(false, "Tool not found: " + name);
    }
    return tool.execute(args);
  }
}

