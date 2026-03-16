package com.agentbot.gateway;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.config.ConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
  private static final Logger log = LoggerFactory.getLogger(ConfigController.class);
  private final AgentbotProperties properties;
  private final ConfigStore store;


  public ConfigController(AgentbotProperties properties, ConfigStore store) {
    this.properties = properties;
    this.store = store;
  }

  @GetMapping
  public Map<String, Object> getConfig() {
    log.debug("Fetching effective and stored configuration");
    Map<String, Object> response = new HashMap<>();
    Map<String, Object> stored = store.load();
    Map<String, Object> maskedStored = maskConfig(stored);
    if (maskedStored.isEmpty()) {
      maskedStored = new HashMap<>();
      maskedStored.put("agentbot", maskSecrets(properties));
    }
    response.put("effective", maskSecrets(properties));
    response.put("stored", maskedStored);
    response.put("path", store.getConfigPath().toString());
    return response;
  }

  @PostMapping
  public Map<String, Object> saveConfig(@RequestBody Map<String, Object> payload) {
    log.info("Saving new configuration to {}", store.getConfigPath());
    try {
      store.save(payload);
      return Map.of("ok", true, "path", store.getConfigPath().toString());
    } catch (Exception e) {
      log.error("Failed to save configuration", e);
      throw e;
    }
  }


  private Map<String, Object> maskSecrets(AgentbotProperties props) {
    Map<String, Object> llm = new HashMap<>();
    llm.put("provider", props.getLlm().getProvider());
    llm.put("temperature", props.getLlm().getTemperature());
    llm.put("fallbackOrder", props.getLlm().getFallbackOrder());
    llm.put("maxToolRounds", props.getLlm().getMaxToolRounds());
    llm.put("parallelTools", props.getLlm().isParallelTools());
    llm.put("toolParallelism", props.getLlm().getToolParallelism());

    llm.put("openai", Map.of(
        "apiKey", mask(props.getLlm().getOpenai().getApiKey()),
        "baseUrl", props.getLlm().getOpenai().getBaseUrl(),
        "model", props.getLlm().getOpenai().getModel()
    ));
    llm.put("openrouter", Map.of(
        "apiKey", mask(props.getLlm().getOpenrouter().getApiKey()),
        "baseUrl", props.getLlm().getOpenrouter().getBaseUrl(),
        "model", props.getLlm().getOpenrouter().getModel()
    ));
    llm.put("glm", Map.of(
        "apiKey", mask(props.getLlm().getGlm().getApiKey()),
        "baseUrl", props.getLlm().getGlm().getBaseUrl(),
        "model", props.getLlm().getGlm().getModel()
    ));
    llm.put("kimi", Map.of(
        "apiKey", mask(props.getLlm().getKimi().getApiKey()),
        "baseUrl", props.getLlm().getKimi().getBaseUrl(),
        "model", props.getLlm().getKimi().getModel()
    ));
    llm.put("qwen", Map.of(
        "apiKey", mask(props.getLlm().getQwen().getApiKey()),
        "baseUrl", props.getLlm().getQwen().getBaseUrl(),
        "model", props.getLlm().getQwen().getModel()
    ));
    llm.put("minimax", Map.of(
        "apiKey", mask(props.getLlm().getMinimax().getApiKey()),
        "baseUrl", props.getLlm().getMinimax().getBaseUrl(),
        "model", props.getLlm().getMinimax().getModel()
    ));
    llm.put("apimesh", Map.of(
        "apiKey", mask(props.getLlm().getApimesh().getApiKey()),
        "baseUrl", props.getLlm().getApimesh().getBaseUrl(),
        "model", props.getLlm().getApimesh().getModel()
    ));

    Map<String, Object> channels = new HashMap<>();

    channels.put("telegram", Map.of(
        "enabled", props.getChannels().getTelegram().isEnabled(),
        "token", mask(props.getChannels().getTelegram().getToken())
    ));
    channels.put("whatsapp", Map.of(
        "enabled", props.getChannels().getWhatsapp().isEnabled(),
        "bridgeUrl", props.getChannels().getWhatsapp().getBridgeUrl()
    ));
    channels.put("wechat", Map.of(
        "enabled", props.getChannels().getWechat().isEnabled(),
        "appId", props.getChannels().getWechat().getAppId(),
        "token", mask(props.getChannels().getWechat().getToken())
    ));

    Map<String, Object> data = new HashMap<>();
    data.put("channels", channels);

    data.put("llm", llm);

    data.put("heartbeat", Map.of(
        "enabled", props.getHeartbeat().isEnabled(),
        "intervalSeconds", props.getHeartbeat().getIntervalSeconds()
    ));
    data.put("cron", Map.of(
        "enabled", props.getCron().isEnabled(),
        "defaultIntervalSeconds", props.getCron().getDefaultIntervalSeconds(),
        "defaultPrompt", props.getCron().getDefaultPrompt()
    ));
    return data;
  }

  private Map<String, Object> maskConfig(Map<String, Object> raw) {
    if (raw == null) {
      return new HashMap<>();
    }
    Map<String, Object> masked = new HashMap<>();
    for (Map.Entry<String, Object> entry : raw.entrySet()) {
      masked.put(entry.getKey(), maskValue(entry.getKey(), entry.getValue()));
    }
    return masked;
  }

  private Object maskValue(String key, Object value) {
    if (value instanceof Map) {
      Map<String, Object> nested = new HashMap<>();
      Map<?, ?> mapValue = (Map<?, ?>) value;
      for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
        String childKey = String.valueOf(entry.getKey());
        nested.put(childKey, maskValue(childKey, entry.getValue()));
      }
      return nested;
    }
    if (value instanceof Iterable) {
      java.util.List<Object> list = new java.util.ArrayList<>();
      for (Object item : (Iterable<?>) value) {
        list.add(maskValue(key, item));
      }
      return list;
    }
    if (value instanceof String && isSecretKey(key)) {
      return mask((String) value);
    }
    return value;
  }

  private boolean isSecretKey(String key) {
    String lower = key.toLowerCase();
    return lower.contains("key")
        || lower.contains("token")
        || lower.contains("secret")
        || lower.contains("password");
  }

  private String mask(String value) {
    if (value == null || value.isBlank()) return "";
    if (value.length() <= 4) return "****";
    return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
  }
}
