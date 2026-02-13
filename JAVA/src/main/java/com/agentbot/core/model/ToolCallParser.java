package com.agentbot.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;

public class ToolCallParser {
  private final ObjectMapper mapper = new ObjectMapper();

  public Map<String, Object> parseArguments(String raw) {
    if (raw == null || raw.isBlank()) return Collections.emptyMap();
    try {
      return mapper.readValue(raw, Map.class);
    } catch (Exception ignored) {
      return Map.of("raw", raw);
    }
  }
}
