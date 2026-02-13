package com.agentbot.core.model;

import java.util.List;
import java.util.Map;

public class LLMResponse {
  private final String content;
  private final String reasoningContent;
  private final List<Map<String, Object>> toolCalls;

  public LLMResponse(String content, List<Map<String, Object>> toolCalls) {
    this(content, null, toolCalls);
  }

  public LLMResponse(String content, String reasoningContent, List<Map<String, Object>> toolCalls) {
    this.content = content;
    this.reasoningContent = reasoningContent;
    this.toolCalls = toolCalls;
  }

  public String getContent() {
    return content;
  }

  public String getReasoningContent() {
    return reasoningContent;
  }


  public List<Map<String, Object>> getToolCalls() {
    return toolCalls;
  }
}
