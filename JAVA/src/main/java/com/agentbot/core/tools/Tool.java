package com.agentbot.core.tools;

import java.util.Map;

public interface Tool {
  String name();

  ToolExecutionResult execute(Map<String, Object> args);
}
