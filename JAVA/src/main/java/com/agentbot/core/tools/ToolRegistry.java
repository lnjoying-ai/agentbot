package com.agentbot.core.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolRegistry {
  private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);
  private final Map<String, Tool> tools = new HashMap<>();
  private final Map<String, ToolDefinition> definitions = new HashMap<>();

  public void register(Tool tool) {
    if (tool == null) return;
    tools.put(tool.name(), tool);
    if (tool instanceof ToolWithDefinition withDefinition) {
      ToolDefinition definition = withDefinition.definition();
      if (definition != null) {
        log.debug("Registered tool: {}", definition.getName());
        definitions.put(definition.getName(), definition);
      }
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
      log.warn("Tool execution failed: Tool '{}' not found", name);
      return new ToolExecutionResult(false, "Tool not found: " + name);
    }
    
    log.info("Tool starting: name={}, args={}", name, args);
    long startTime = System.currentTimeMillis();
    try {
      ToolExecutionResult result = tool.execute(args);
      long duration = System.currentTimeMillis() - startTime;
      
      if (result.isOk()) {
        log.info("Tool completed: name={}, duration={}ms, success=true", name, duration);
      } else {
        log.warn("Tool completed: name={}, duration={}ms, success=false, error={}", name, duration, result.getOutput());
      }
      return result;
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("Tool crash: name={}, duration={}ms, error={}", name, duration, e.getMessage(), e);
      return new ToolExecutionResult(false, "Internal tool error: " + e.getMessage());
    }
  }
}

