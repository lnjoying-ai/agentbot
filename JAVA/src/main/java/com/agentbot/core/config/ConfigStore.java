package com.agentbot.core.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigStore {
  private final ObjectMapper mapper;
  private final Path configPath;

  public ConfigStore(ObjectMapper mapper, Path configPath) {
    this.mapper = mapper;
    this.configPath = configPath;
  }

  public Path getConfigPath() {
    return configPath;
  }

  public Map<String, Object> load() {
    if (!Files.exists(configPath)) {
      return Collections.emptyMap();
    }
    try {
      return mapper.readValue(configPath.toFile(), new TypeReference<Map<String, Object>>() {});
    } catch (Exception ignored) {
      return Collections.emptyMap();
    }
  }

  public void save(Map<String, Object> payload) {
    try {
      Files.createDirectories(configPath.getParent());
      Map<String, Object> existing = load();
      Map<String, Object> normalized = normalizePayload(payload);
      Map<String, Object> merged = deepMerge(existing, normalized);
      mapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), merged);
    } catch (Exception ignored) {
      // ignore
    }
  }

  private Map<String, Object> normalizePayload(Map<String, Object> payload) {
    if (payload == null) {
      return Collections.emptyMap();
    }
    if (payload.containsKey("agentbot") || payload.containsKey("server") || payload.containsKey("logging")) {
      return payload;
    }
    if (looksLikeAgentbotConfig(payload)) {
      Map<String, Object> wrapped = new LinkedHashMap<>();
      wrapped.put("agentbot", payload);
      return wrapped;
    }
    return payload;
  }

  private boolean looksLikeAgentbotConfig(Map<String, Object> payload) {
    return payload.containsKey("channels")
        || payload.containsKey("llm")
        || payload.containsKey("heartbeat")
        || payload.containsKey("cron")
        || payload.containsKey("ops")
        || payload.containsKey("search")
        || payload.containsKey("approvals")
        || payload.containsKey("browser")
        || payload.containsKey("p2p")
        || payload.containsKey("agents")
        || payload.containsKey("bindings");
  }

  private Map<String, Object> deepMerge(Map<String, Object> base, Map<String, Object> patch) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (base != null) {
      result.putAll(base);
    }
    if (patch == null) {
      return result;
    }
    for (Map.Entry<String, Object> entry : patch.entrySet()) {
      String key = entry.getKey();
      Object patchValue = entry.getValue();
      Object baseValue = result.get(key);
      if (patchValue instanceof Map && baseValue instanceof Map) {
        result.put(key, deepMerge(castMap(baseValue), castMap(patchValue)));
        continue;
      }
      if (isMaskedString(patchValue) && baseValue != null) {
        continue;
      }
      result.put(key, patchValue);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> castMap(Object value) {
    return (Map<String, Object>) value;
  }

  private boolean isMaskedString(Object value) {
    if (!(value instanceof String)) {
      return false;
    }
    String text = (String) value;
    return text.contains("****");
  }

}
