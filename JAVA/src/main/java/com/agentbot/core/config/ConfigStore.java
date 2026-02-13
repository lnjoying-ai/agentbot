package com.agentbot.core.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
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
      mapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), payload);
    } catch (Exception ignored) {
      // ignore
    }
  }
}
