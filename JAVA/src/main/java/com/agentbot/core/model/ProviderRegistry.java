package com.agentbot.core.model;

import java.util.HashMap;
import java.util.Map;

public class ProviderRegistry {
  private final Map<String, LLMProvider> providers = new HashMap<>();

  public void register(String name, LLMProvider provider) {
    providers.put(name, provider);
  }

  public LLMProvider get(String name) {
    return providers.get(name);
  }
}
