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
    response.put("effective", maskSecrets(properties));
    response.put("stored", store.load());
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
    llm.put("baseUrl", props.getLlm().getBaseUrl());
    llm.put("model", props.getLlm().getModel());
    llm.put("temperature", props.getLlm().getTemperature());
    llm.put("fallbackOrder", props.getLlm().getFallbackOrder());
    llm.put("apiKey", mask(props.getLlm().getApiKey()));
    llm.put("openrouterKey", mask(props.getLlm().getOpenrouter().getApiKey()));
    llm.put("glmKey", mask(props.getLlm().getGlm().getApiKey()));
    llm.put("kimiKey", mask(props.getLlm().getKimi().getApiKey()));


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
    data.put("workspaceDir", props.getWorkspaceDir());
    data.put("configFile", props.getConfigFile());
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

  private String mask(String value) {
    if (value == null || value.isBlank()) return "";
    if (value.length() <= 4) return "****";
    return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
  }
}
