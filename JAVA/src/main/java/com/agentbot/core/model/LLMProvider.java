package com.agentbot.core.model;

import java.util.List;
import java.util.Map;

public interface LLMProvider {
  LLMResponse chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools);
}
