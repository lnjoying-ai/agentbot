package com.agentbot.core.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FallbackLlmProvider implements LLMProvider {
  private static final Logger log = LoggerFactory.getLogger(FallbackLlmProvider.class);
  private final List<ProviderEntry> providers = new ArrayList<>();
  private int primaryIndex = 0;
  private int primaryConsecutiveFailures = 0;
  private long totalFallbacks = 0;
  private final Map<String, Integer> fallbackCounts = new HashMap<>();
  private final Map<String, Integer> failureCounts = new HashMap<>();
  private String lastFallbackReason = "";
  private String lastFallbackFrom = "";
  private String lastFallbackTo = "";
  private long lastFallbackAt = 0;
  private String lastPrimarySwitchReason = "";
  private String lastPrimarySwitchFrom = "";
  private String lastPrimarySwitchTo = "";
  private long lastPrimarySwitchAt = 0;

  public static class ProviderEntry {
    private final String providerName;
    private final String model;
    private final LLMProvider provider;

    public ProviderEntry(String providerName, String model, LLMProvider provider) {
      this.providerName = providerName == null ? "" : providerName;
      this.model = model == null ? "" : model;
      this.provider = provider;
    }

    public String getProviderName() {
      return providerName;
    }

    public String getModel() {
      return model;
    }

    public LLMProvider getProvider() {
      return provider;
    }

    public String getId() {
      return providerName + " / " + model;
    }
  }

  public FallbackLlmProvider(List<ProviderEntry> orderedProviders) {
    if (orderedProviders != null) {
      for (ProviderEntry entry : orderedProviders) {
        if (entry != null && entry.getProvider() != null) {
          providers.add(entry);
        }
      }
    }
  }

  @Override
  public synchronized LLMResponse chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
    if (providers.isEmpty()) {
      return new LLMResponse("[LLM_ERROR] no providers available", List.of());
    }

    LLMResponse last = new LLMResponse("[LLM_ERROR] no providers available", List.of());
    int startIndex = primaryIndex;

    for (int i = 0; i < providers.size(); i++) {
      int index = (startIndex + i) % providers.size();
      ProviderEntry entry = providers.get(index);
      if (entry == null || entry.getProvider() == null) continue;

      LLMResponse response = entry.getProvider().chat(messages, tools);
      if (response == null) continue;
      String content = response.getContent() == null ? "" : response.getContent();
      boolean hasError = content.startsWith("[LLM_ERROR]");
      boolean hasToolCalls = response.getToolCalls() != null && !response.getToolCalls().isEmpty();

      if (!hasError || hasToolCalls) {
        if (i == 0) {
          primaryConsecutiveFailures = 0;
        }
        return response;
      }

      String reason = extractErrorReason(content);
      failureCounts.merge(entry.getId(), 1, Integer::sum);

      if (i == 0) {
        primaryConsecutiveFailures++;
        if (primaryConsecutiveFailures > 3 && providers.size() > 1) {
          promotePrimary(reason);
        }
      }

      if (i < providers.size() - 1) {
        totalFallbacks++;
        fallbackCounts.merge(entry.getId(), 1, Integer::sum);
        ProviderEntry nextEntry = providers.get((index + 1) % providers.size());
        lastFallbackFrom = entry.getId();
        lastFallbackTo = nextEntry == null ? "" : nextEntry.getId();
        lastFallbackReason = reason;
        lastFallbackAt = System.currentTimeMillis();
        log.warn("LLM fallback: from={} to={} reason={}", lastFallbackFrom, lastFallbackTo, reason);
      }

      last = response;
    }

    return last;
  }

  public synchronized Map<String, Object> getStatus() {
    Map<String, Object> data = new HashMap<>();
    ProviderEntry current = providers.isEmpty() ? null : providers.get(primaryIndex);
    data.put("currentProvider", current == null ? "" : current.getProviderName());
    data.put("currentModel", current == null ? "" : current.getModel());
    data.put("primaryIndex", primaryIndex);
    data.put("primaryConsecutiveFailures", primaryConsecutiveFailures);
    data.put("totalFallbacks", totalFallbacks);
    data.put("fallbackCounts", new HashMap<>(fallbackCounts));
    data.put("failureCounts", new HashMap<>(failureCounts));

    Map<String, Object> lastFallback = new HashMap<>();
    lastFallback.put("from", lastFallbackFrom);
    lastFallback.put("to", lastFallbackTo);
    lastFallback.put("reason", lastFallbackReason);
    lastFallback.put("at", lastFallbackAt);
    data.put("lastFallback", lastFallback);

    Map<String, Object> lastSwitch = new HashMap<>();
    lastSwitch.put("from", lastPrimarySwitchFrom);
    lastSwitch.put("to", lastPrimarySwitchTo);
    lastSwitch.put("reason", lastPrimarySwitchReason);
    lastSwitch.put("at", lastPrimarySwitchAt);
    data.put("lastPrimarySwitch", lastSwitch);

    List<Map<String, Object>> order = new ArrayList<>();
    for (ProviderEntry entry : providers) {
      if (entry == null) continue;
      Map<String, Object> item = new HashMap<>();
      item.put("provider", entry.getProviderName());
      item.put("model", entry.getModel());
      item.put("id", entry.getId());
      order.add(item);
    }
    data.put("order", order);

    return data;
  }

  private void promotePrimary(String reason) {
    int previous = primaryIndex;
    primaryIndex = (primaryIndex + 1) % providers.size();
    primaryConsecutiveFailures = 0;
    ProviderEntry from = providers.get(previous);
    ProviderEntry to = providers.get(primaryIndex);
    lastPrimarySwitchFrom = from == null ? "" : from.getId();
    lastPrimarySwitchTo = to == null ? "" : to.getId();
    lastPrimarySwitchReason = reason == null ? "" : reason;
    lastPrimarySwitchAt = System.currentTimeMillis();
    log.warn("LLM primary switched: from={} to={} reason={}", lastPrimarySwitchFrom, lastPrimarySwitchTo, lastPrimarySwitchReason);
  }

  private String extractErrorReason(String content) {
    if (content == null) return "";
    String trimmed = content.trim();
    if (trimmed.startsWith("[LLM_ERROR]")) {
      return trimmed.substring("[LLM_ERROR]".length()).trim();
    }
    return trimmed;
  }
}
