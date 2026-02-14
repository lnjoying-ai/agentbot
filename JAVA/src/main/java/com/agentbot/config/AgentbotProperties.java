package com.agentbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agentbot")
public class AgentbotProperties {
  private String workspaceDir = "./workspace";
  private String heartbeatFile = "HEARTBEAT.md";
  private String configFile = "agentbot-config.json";
  private final Channels channels = new Channels();
  private final Llm llm = new Llm();
  private final Heartbeat heartbeat = new Heartbeat();
  private final Cron cron = new Cron();
  private final Ops ops = new Ops();
  private final Search search = new Search();



  public String getWorkspaceDir() {
    return workspaceDir;
  }

  public void setWorkspaceDir(String workspaceDir) {
    this.workspaceDir = workspaceDir;
  }

  public String getHeartbeatFile() {
    return heartbeatFile;
  }

  public void setHeartbeatFile(String heartbeatFile) {
    this.heartbeatFile = heartbeatFile;
  }

  public String getConfigFile() {
    return configFile;
  }

  public void setConfigFile(String configFile) {
    this.configFile = configFile;
  }

  public Channels getChannels() {
    return channels;
  }


  public Llm getLlm() {
    return llm;
  }

  public Heartbeat getHeartbeat() {
    return heartbeat;
  }

  public Cron getCron() {
    return cron;
  }

  public Ops getOps() {
    return ops;
  }

  public Search getSearch() {
    return search;
  }


  public static class Channels {


    private final Telegram telegram = new Telegram();
    private final WhatsApp whatsapp = new WhatsApp();
    private final WeChat wechat = new WeChat();

    public Telegram getTelegram() {
      return telegram;
    }

    public WhatsApp getWhatsapp() {
      return whatsapp;
    }

    public WeChat getWechat() {
      return wechat;
    }
  }

  public static class Telegram {
    private boolean enabled = false;
    private String token = "";
    private int pollSeconds = 2;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public int getPollSeconds() {
      return pollSeconds;
    }

    public void setPollSeconds(int pollSeconds) {
      this.pollSeconds = pollSeconds;
    }
  }

  public static class WhatsApp {
    private boolean enabled = false;
    private String bridgeUrl = "ws://127.0.0.1:3001";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getBridgeUrl() {
      return bridgeUrl;
    }

    public void setBridgeUrl(String bridgeUrl) {
      this.bridgeUrl = bridgeUrl;
    }
  }

  public static class WeChat {
    private boolean enabled = false;
    private String appId = "";
    private String appSecret = "";
    private String token = "";
    private String aesKey = "";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getAppId() {
      return appId;
    }

    public void setAppId(String appId) {
      this.appId = appId;
    }

    public String getAppSecret() {
      return appSecret;
    }

    public void setAppSecret(String appSecret) {
      this.appSecret = appSecret;
    }

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public String getAesKey() {
      return aesKey;
    }

    public void setAesKey(String aesKey) {
      this.aesKey = aesKey;
    }
  }

  public static class Llm {
    private String provider = "openai";
    private String apiKey = "";
    private String apiBaseUrl = "https://api.openai.com/v1";
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o-mini";
    private double temperature = 0.2;
    private String fallbackOrder = "openai,openrouter,glm,kimi";
    private int maxToolRounds = 2;
    private boolean parallelTools = true;
    private int toolParallelism = 4;
    private final Provider openrouter = new Provider("https://openrouter.ai/api/v1");
    private final Provider glm = new Provider("https://open.bigmodel.cn/api/paas/v4");
    private final Provider kimi = new Provider("https://api.moonshot.cn/v1");


    public String getProvider() {
      return provider;
    }

    public void setProvider(String provider) {
      this.provider = provider;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public String getApiBaseUrl() {
      return apiBaseUrl != null && !apiBaseUrl.equals("https://api.openai.com/v1") ? apiBaseUrl : baseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
      this.apiBaseUrl = apiBaseUrl;
      this.baseUrl = apiBaseUrl;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      this.apiBaseUrl = baseUrl;
    }


    public String getModel() {
      return model;
    }

    public void setModel(String model) {
      this.model = model;
    }

    public double getTemperature() {
      return temperature;
    }

    public void setTemperature(double temperature) {
      this.temperature = temperature;
    }

    public String getFallbackOrder() {
      return fallbackOrder;
    }

    public void setFallbackOrder(String fallbackOrder) {
      this.fallbackOrder = fallbackOrder;
    }

    public int getMaxToolRounds() {
      return maxToolRounds;
    }

    public void setMaxToolRounds(int maxToolRounds) {
      this.maxToolRounds = maxToolRounds;
    }

    public boolean isParallelTools() {
      return parallelTools;
    }

    public void setParallelTools(boolean parallelTools) {
      this.parallelTools = parallelTools;
    }

    public int getToolParallelism() {
      return toolParallelism;
    }

    public void setToolParallelism(int toolParallelism) {
      this.toolParallelism = toolParallelism;
    }

    public Provider getOpenrouter() {
      return openrouter;
    }


    public Provider getGlm() {
      return glm;
    }

    public Provider getKimi() {
      return kimi;
    }
  }

  public static class Heartbeat {
    private boolean enabled = false;
    private int intervalSeconds = 60;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getIntervalSeconds() {
      return intervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds) {
      this.intervalSeconds = intervalSeconds;
    }
  }

  public static class Cron {
    private boolean enabled = false;
    private int defaultIntervalSeconds = 3600;
    private String defaultPrompt = "";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getDefaultIntervalSeconds() {
      return defaultIntervalSeconds;
    }

    public void setDefaultIntervalSeconds(int defaultIntervalSeconds) {
      this.defaultIntervalSeconds = defaultIntervalSeconds;
    }

    public String getDefaultPrompt() {
      return defaultPrompt;
    }

    public void setDefaultPrompt(String defaultPrompt) {
      this.defaultPrompt = defaultPrompt;
    }
  }

  public static class Ops {
    private int logBufferSize = 200;

    public int getLogBufferSize() {
      return logBufferSize;
    }

    public void setLogBufferSize(int logBufferSize) {
      this.logBufferSize = logBufferSize;
    }
  }

  public static class Search {
    private String type = "bocha";
    private String braveApiKey = "";
    private String bochaApiKey = "";

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getBraveApiKey() {
      return braveApiKey;
    }

    public void setBraveApiKey(String braveApiKey) {
      this.braveApiKey = braveApiKey;
    }

    public String getBochaApiKey() {
      return bochaApiKey;
    }

    public void setBochaApiKey(String bochaApiKey) {
      this.bochaApiKey = bochaApiKey;
    }
  }


  public static class Provider {



    private String apiKey = "";
    private String baseUrl;
    private String model = "";

    public Provider(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getModel() {
      return model;
    }

    public void setModel(String model) {
      this.model = model;
    }
  }
}



