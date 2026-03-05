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
  private final ToolApprovalPolicy approvalPolicy;

  public ToolRegistry() {
    this(null);
  }

  public ToolRegistry(ToolApprovalPolicy approvalPolicy) {
    this.approvalPolicy = approvalPolicy;
  }

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
    if (requiresApproval(name, args) && !Boolean.TRUE.equals(args.get("confirmed"))) {
      log.info("Tool requires approval: name={}", name);
      return new ToolExecutionResult(ToolExecutionResult.Status.PENDING_APPROVAL, "Approval required for tool: " + name);
    }

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

  public boolean requiresApproval(String name, Map<String, Object> args) {
    return approvalDecision(name, args, null) == ToolApprovalPolicy.Decision.ASK;
  }

  public ToolApprovalPolicy.Decision approvalDecision(String name, Map<String, Object> args, String channel) {
    Tool tool = tools.get(name);
    if (tool == null) {
      return ToolApprovalPolicy.Decision.ALLOW;
    }
    boolean toolRisky = tool.requiresApproval(args);
    if (approvalPolicy == null) {
      return toolRisky ? ToolApprovalPolicy.Decision.ASK : ToolApprovalPolicy.Decision.ALLOW;
    }
    return approvalPolicy.decide(name, args, toolRisky, channel);
  }

  public ToolApprovalPolicy getApprovalPolicy() {
    return approvalPolicy;
  }

  
  /**
   * Get a tool by name.
   */
  public Tool getTool(String name) {
    return tools.get(name);
  }

  /**
   * Get tool definition by name.
   */
  public ToolDefinition getDefinition(String name) {
    return name == null ? null : definitions.get(name);
  }
  
  /**
   * List all registered tool names.
   */
  public List<String> listToolNames() {
    return new ArrayList<>(tools.keySet());
  }
  
  /**
   * Check if a tool is registered.
   */
  public boolean hasTool(String name) {
    return tools.containsKey(name);
  }
  
  /**
   * Get count of registered tools.
   */
  public int getToolCount() {
    return tools.size();
  }
}


