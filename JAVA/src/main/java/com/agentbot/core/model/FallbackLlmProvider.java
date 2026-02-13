package com.agentbot.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FallbackLlmProvider implements LLMProvider {
  private final List<LLMProvider> providers = new ArrayList<>();

  public FallbackLlmProvider(List<LLMProvider> orderedProviders) {
    if (orderedProviders != null) {
      providers.addAll(orderedProviders);
    }
  }

  @Override
  public LLMResponse chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
    LLMResponse last = new LLMResponse("[LLM_ERROR] no providers available", List.of());
    for (LLMProvider provider : providers) {
      if (provider == null) continue;
      LLMResponse response = provider.chat(messages, tools);
      if (response == null) continue;
      String content = response.getContent() == null ? "" : response.getContent();
      boolean hasError = content.startsWith("[LLM_ERROR]");
      boolean hasToolCalls = response.getToolCalls() != null && !response.getToolCalls().isEmpty();
      if (!hasError || hasToolCalls) {
        return response;
      }
      last = response;
    }
    return last;
  }
}
