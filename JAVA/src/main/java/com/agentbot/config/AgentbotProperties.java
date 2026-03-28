package com.agentbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agentbot")
public class AgentbotProperties {

  private String heartbeatFile = "HEARTBEAT.md";
  private final Channels channels = new Channels();
  private final Agents agents = new Agents();
  private java.util.List<Binding> bindings = new java.util.ArrayList<>();

  private final Llm llm = new Llm();

  private final Heartbeat heartbeat = new Heartbeat();
  private final Cron cron = new Cron();
  private final Ops ops = new Ops();
  private final Search search = new Search();
  private final Approvals approvals = new Approvals();
  private final Browser browser = new Browser();
  private final P2p p2p = new P2p();
  private final Security security = new Security();


  public String getHeartbeatFile() {

    return heartbeatFile;
  }

  public void setHeartbeatFile(String heartbeatFile) {
    this.heartbeatFile = heartbeatFile;
  }

  public Channels getChannels() {
    return channels;
  }

  public Agents getAgents() {
    return agents;
  }

  public java.util.List<Binding> getBindings() {
    return bindings;
  }

  public void setBindings(java.util.List<Binding> bindings) {
    this.bindings = bindings == null ? new java.util.ArrayList<>() : bindings;
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

  public Approvals getApprovals() {
    return approvals;
  }

  public Browser getBrowser() {
    return browser;
  }

  public P2p getP2p() {
    return p2p;
  }

  public Security getSecurity() {
    return security;
  }

  public boolean isAuthEnabled() {
    return security.getAuth().isEnabled();
  }

  public String getAuthUsername() {
    return security.getAuth().getUsername();
  }

  public String getAuthPassword() {
    return security.getAuth().getPassword();
  }






  public static class Channels {



    private final Telegram telegram = new Telegram();
    private final WhatsApp whatsapp = new WhatsApp();
    private final WeChat wechat = new WeChat();
    private final Feishu feishu = new Feishu();
    private final Discord discord = new Discord();
    private final MaixCam maixcam = new MaixCam();
    private final QQ qq = new QQ();
    private final DingTalk dingtalk = new DingTalk();
    private final Slack slack = new Slack();
    private final Line line = new Line();
    private final OneBot onebot = new OneBot();

    public Telegram getTelegram() {
      return telegram;
    }

    public WhatsApp getWhatsapp() {
      return whatsapp;
    }

    public WeChat getWechat() {
      return wechat;
    }

    public Feishu getFeishu() {
      return feishu;
    }

    public Discord getDiscord() {
      return discord;
    }

    public MaixCam getMaixcam() {
      return maixcam;
    }

    public QQ getQq() {
      return qq;
    }

    public DingTalk getDingtalk() {
      return dingtalk;
    }

    public Slack getSlack() {
      return slack;
    }

    public Line getLine() {
      return line;
    }

    public OneBot getOnebot() {
      return onebot;
    }
  }

  public static class Agents {
    private final Defaults defaults = new Defaults();

    public Defaults getDefaults() {
      return defaults;
    }
  }

  public static class Defaults {
    private String defaultAgentId = "default";

    public String getDefaultAgentId() {
      return defaultAgentId;
    }

    public void setDefaultAgentId(String defaultAgentId) {
      this.defaultAgentId = defaultAgentId;
    }
  }

  public static class Binding {
    private String agentId = "";
    private Match match = new Match();

    public String getAgentId() {
      return agentId;
    }

    public void setAgentId(String agentId) {
      this.agentId = agentId;
    }

    public Match getMatch() {
      return match;
    }

    public void setMatch(Match match) {
      this.match = match == null ? new Match() : match;
    }
  }

  public static class Match {
    private String channel = "";
    private String accountId = "";
    private Peer peer;
    private String guildId = "";
    private String teamId = "";

    public String getChannel() {
      return channel;
    }

    public void setChannel(String channel) {
      this.channel = channel;
    }

    public String getAccountId() {
      return accountId;
    }

    public void setAccountId(String accountId) {
      this.accountId = accountId;
    }

    public Peer getPeer() {
      return peer;
    }

    public void setPeer(Peer peer) {
      this.peer = peer;
    }

    public String getGuildId() {
      return guildId;
    }

    public void setGuildId(String guildId) {
      this.guildId = guildId;
    }

    public String getTeamId() {
      return teamId;
    }

    public void setTeamId(String teamId) {
      this.teamId = teamId;
    }
  }

  public static class Peer {
    private String kind = "";
    private String id = "";

    public String getKind() {
      return kind;
    }

    public void setKind(String kind) {
      this.kind = kind;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
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

  public static class Feishu {
    private boolean enabled = false;
    private String appId = "";
    private String appSecret = "";
    private String verificationToken = "";
    private String encryptKey = "";
    private String domain = "https://open.feishu.cn";
    private boolean autoReconnect = true;

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

    public String getVerificationToken() {
      return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
      this.verificationToken = verificationToken;
    }

    public String getEncryptKey() {
      return encryptKey;
    }

    public void setEncryptKey(String encryptKey) {
      this.encryptKey = encryptKey;
    }

    public String getDomain() {
      return domain;
    }

    public void setDomain(String domain) {
      this.domain = domain;
    }

    public boolean isAutoReconnect() {
      return autoReconnect;
    }

    public void setAutoReconnect(boolean autoReconnect) {
      this.autoReconnect = autoReconnect;
    }
  }


  public static class Discord {
    private boolean enabled = false;
    private String botToken = "";
    private String gatewayUrl = "wss://gateway.discord.gg/?v=10&encoding=json";
    private int intents = 513;
    private int reconnectIntervalSeconds = 5;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getBotToken() {
      return botToken;
    }

    public void setBotToken(String botToken) {
      this.botToken = botToken;
    }

    public String getGatewayUrl() {
      return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
      this.gatewayUrl = gatewayUrl;
    }

    public int getIntents() {
      return intents;
    }

    public void setIntents(int intents) {
      this.intents = intents;
    }

    public int getReconnectIntervalSeconds() {
      return reconnectIntervalSeconds;
    }

    public void setReconnectIntervalSeconds(int reconnectIntervalSeconds) {
      this.reconnectIntervalSeconds = reconnectIntervalSeconds;
    }
  }


  public static class MaixCam {
    private boolean enabled = false;
    private String host = "0.0.0.0";
    private int port = 19032;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port;
    }
  }

  public static class QQ {
    private boolean enabled = false;
    private String appId = "";
    private String appSecret = "";
    private String botToken = "";
    private String apiBaseUrl = "https://api.sgroup.qq.com";
    private String wsUrl = "wss://api.sgroup.qq.com/websocket";
    private int intents = 1;
    private int shardId = 0;
    private int shardCount = 1;
    private int reconnectIntervalSeconds = 5;

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

    public String getBotToken() {
      return botToken;
    }

    public void setBotToken(String botToken) {
      this.botToken = botToken;
    }

    public String getApiBaseUrl() {
      return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
      this.apiBaseUrl = apiBaseUrl;
    }

    public String getWsUrl() {
      return wsUrl;
    }

    public void setWsUrl(String wsUrl) {
      this.wsUrl = wsUrl;
    }

    public int getIntents() {
      return intents;
    }

    public void setIntents(int intents) {
      this.intents = intents;
    }

    public int getShardId() {
      return shardId;
    }

    public void setShardId(int shardId) {
      this.shardId = shardId;
    }

    public int getShardCount() {
      return shardCount;
    }

    public void setShardCount(int shardCount) {
      this.shardCount = shardCount;
    }

    public int getReconnectIntervalSeconds() {
      return reconnectIntervalSeconds;
    }

    public void setReconnectIntervalSeconds(int reconnectIntervalSeconds) {
      this.reconnectIntervalSeconds = reconnectIntervalSeconds;
    }
  }



  public static class DingTalk {
    private boolean enabled = false;
    private String appKey = "";
    private String appSecret = "";
    private String token = "";
    private String aesKey = "";
    private String webhookUrl = "";
    private String webhookSecret = "";
    private String apiBaseUrl = "https://oapi.dingtalk.com";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getAppKey() {
      return appKey;
    }

    public void setAppKey(String appKey) {
      this.appKey = appKey;
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

    public String getWebhookUrl() {
      return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
      this.webhookUrl = webhookUrl;
    }

    public String getWebhookSecret() {
      return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
      this.webhookSecret = webhookSecret;
    }

    public String getApiBaseUrl() {
      return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
      this.apiBaseUrl = apiBaseUrl;
    }
  }


  public static class Slack {
    private boolean enabled = false;
    private String botToken = "";
    private String signingSecret = "";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getBotToken() {
      return botToken;
    }

    public void setBotToken(String botToken) {
      this.botToken = botToken;
    }

    public String getSigningSecret() {
      return signingSecret;
    }

    public void setSigningSecret(String signingSecret) {
      this.signingSecret = signingSecret;
    }
  }

  public static class Line {
    private boolean enabled = false;
    private String channelSecret = "";
    private String channelAccessToken = "";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getChannelSecret() {
      return channelSecret;
    }

    public void setChannelSecret(String channelSecret) {
      this.channelSecret = channelSecret;
    }

    public String getChannelAccessToken() {
      return channelAccessToken;
    }

    public void setChannelAccessToken(String channelAccessToken) {
      this.channelAccessToken = channelAccessToken;
    }
  }

  public static class OneBot {
    private boolean enabled = false;
    private String wsUrl = "";
    private String accessToken = "";
    private int reconnectIntervalSeconds = 5;
    private String groupTriggerPrefix = "";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getWsUrl() {
      return wsUrl;
    }

    public void setWsUrl(String wsUrl) {
      this.wsUrl = wsUrl;
    }

    public String getAccessToken() {
      return accessToken;
    }

    public void setAccessToken(String accessToken) {
      this.accessToken = accessToken;
    }

    public int getReconnectIntervalSeconds() {
      return reconnectIntervalSeconds;
    }

    public void setReconnectIntervalSeconds(int reconnectIntervalSeconds) {
      this.reconnectIntervalSeconds = reconnectIntervalSeconds;
    }

    public String getGroupTriggerPrefix() {
      return groupTriggerPrefix;
    }

    public void setGroupTriggerPrefix(String groupTriggerPrefix) {
      this.groupTriggerPrefix = groupTriggerPrefix;
    }
  }


  public static class Llm {
    private String provider = "openai";
    private String apiKey = "";
    private String apiBaseUrl = "https://api.openai.com/v1";
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o-mini";
    private double temperature = 0.7;
    private String fallbackOrder = "openai,openrouter,glm,kimi,qwen,minimax,apimesh";
    private int maxToolRounds = 30;
    private boolean parallelTools = true;
    private int toolParallelism = 3;
    private boolean logHttpRequest = false;
    private boolean logHttpResponse = true;
    private final Provider openai = new Provider("https://api.openai.com/v1");
    private final Provider openrouter = new Provider("https://openrouter.ai/api/v1");
    private final Provider glm = new Provider("https://open.bigmodel.cn/api/paas/v4");
    private final Provider kimi = new Provider("https://api.moonshot.cn/v1");
    private final Provider qwen = new Provider("https://dashscope.aliyuncs.com/compatible-mode/v1");
    private final Provider minimax = new Provider("https://api.minimax.chat/v1");
    private final Provider apimesh = new Provider("https://api.apimesh.io/api/v1/chat/completions");




    public String getProvider() {

      return provider;
    }

    public void setProvider(String provider) {
      this.provider = provider;
    }

    public Provider getActiveProvider() {
      if (provider == null) return openai;
      return switch (provider.trim().toLowerCase()) {
        case "openrouter" -> openrouter;
        case "glm" -> glm;
        case "kimi" -> kimi;
        case "qwen" -> qwen;
        case "minimax" -> minimax;
        case "apimesh" -> apimesh;
        default -> openai;
      };

    }

    public String getActiveModel() {
      Provider active = getActiveProvider();
      return active == null ? "" : active.getModel();
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

    public boolean isLogHttpRequest() {
      return logHttpRequest;
    }

    public void setLogHttpRequest(boolean logHttpRequest) {
      this.logHttpRequest = logHttpRequest;
    }

    public boolean isLogHttpResponse() {
      return logHttpResponse;
    }

    public void setLogHttpResponse(boolean logHttpResponse) {
      this.logHttpResponse = logHttpResponse;
    }

    public Provider getOpenai() {
      return openai;
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

    public Provider getQwen() {
      return qwen;
    }

    public Provider getMinimax() {
      return minimax;
    }

    public Provider getApimesh() {
      return apimesh;
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
    private int chatStreamBufferSize = 50;
    private String chatMode = "qa";

    public int getLogBufferSize() {
      return logBufferSize;
    }

    public void setLogBufferSize(int logBufferSize) {
      this.logBufferSize = logBufferSize;
    }

    public int getChatStreamBufferSize() {
      return chatStreamBufferSize;
    }

    public void setChatStreamBufferSize(int chatStreamBufferSize) {
      this.chatStreamBufferSize = chatStreamBufferSize;
    }

    public String getChatMode() {
      return chatMode;
    }

    public void setChatMode(String chatMode) {
      this.chatMode = chatMode;
    }
  }


  public static class Search {
    private String type = "bocha";
    private String braveApiKey = "";
    private String bochaApiKey = "";
    private String apimeshKey = "";

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

    public String getApimeshKey() {
      return apimeshKey;
    }

    public void setApimeshKey(String apimeshKey) {
      this.apimeshKey = apimeshKey;
    }
  }

  public static class Approvals {
    private final ToolApprovals tools = new ToolApprovals();

    public ToolApprovals getTools() {
      return tools;
    }
  }

  public static class Security {
    private final Auth auth = new Auth();

    public Auth getAuth() {
      return auth;
    }
  }

  public static class Auth {
    private boolean enabled = false;
    private String username = "lnjoying";
    private String password = "lnjoying";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }


  public static class ToolApprovals {
    private String security = "allowlist";
    private String ask = "on-miss";
    private String askFallback = "deny";
    private java.util.List<String> uiChannels = java.util.List.of("web");
    private java.util.List<AllowlistRule> allowlist = new java.util.ArrayList<>();

    public String getSecurity() {
      return security;
    }

    public void setSecurity(String security) {
      this.security = security;
    }

    public String getAsk() {
      return ask;
    }

    public void setAsk(String ask) {
      this.ask = ask;
    }

    public String getAskFallback() {
      return askFallback;
    }

    public void setAskFallback(String askFallback) {
      this.askFallback = askFallback;
    }

    public java.util.List<String> getUiChannels() {
      return uiChannels;
    }

    public void setUiChannels(java.util.List<String> uiChannels) {
      this.uiChannels = uiChannels;
    }

    public java.util.List<AllowlistRule> getAllowlist() {
      return allowlist;
    }

    public void setAllowlist(java.util.List<AllowlistRule> allowlist) {
      this.allowlist = allowlist;
    }
  }

  public static class AllowlistRule {
    private String tool = "";
    private java.util.Map<String, String> match = new java.util.HashMap<>();

    public String getTool() {
      return tool;
    }

    public void setTool(String tool) {
      this.tool = tool;
    }

    public java.util.Map<String, String> getMatch() {
      return match;
    }

    public void setMatch(java.util.Map<String, String> match) {
      this.match = match;
    }
  }

  public static class Browser {
    private boolean enabled = true;
    private int controlPort = 0;
    private String defaultProfile = "agentbot";
    private boolean headless = true;
    private boolean noSandbox = false;
    private boolean attachOnly = false;
    private int remoteCdpTimeoutMs = 15000;
    private int navigationTimeoutMs = 60000;
    private String sandboxBridgeUrl = "";


    private String nodeBridgeUrl = "";
    private String executablePath = "";
    private java.util.Map<String, BrowserProfile> profiles = new java.util.HashMap<>();

    private final AntiBot antiBot = new AntiBot();

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getControlPort() {
      return controlPort;
    }

    public void setControlPort(int controlPort) {
      this.controlPort = controlPort;
    }

    public String getDefaultProfile() {
      return defaultProfile;
    }

    public void setDefaultProfile(String defaultProfile) {
      this.defaultProfile = defaultProfile;
    }

    public boolean isHeadless() {
      return headless;
    }

    public void setHeadless(boolean headless) {
      this.headless = headless;
    }

    public boolean isNoSandbox() {
      return noSandbox;
    }

    public void setNoSandbox(boolean noSandbox) {
      this.noSandbox = noSandbox;
    }

    public boolean isAttachOnly() {
      return attachOnly;
    }

    public void setAttachOnly(boolean attachOnly) {
      this.attachOnly = attachOnly;
    }

    public int getRemoteCdpTimeoutMs() {
      return remoteCdpTimeoutMs;
    }

    public void setRemoteCdpTimeoutMs(int remoteCdpTimeoutMs) {
      this.remoteCdpTimeoutMs = remoteCdpTimeoutMs;
    }

    public int getNavigationTimeoutMs() {
      return navigationTimeoutMs;
    }

    public void setNavigationTimeoutMs(int navigationTimeoutMs) {
      this.navigationTimeoutMs = navigationTimeoutMs;
    }

    public String getSandboxBridgeUrl() {
      return sandboxBridgeUrl;
    }


    public void setSandboxBridgeUrl(String sandboxBridgeUrl) {
      this.sandboxBridgeUrl = sandboxBridgeUrl;
    }

    public String getNodeBridgeUrl() {
      return nodeBridgeUrl;
    }

    public void setNodeBridgeUrl(String nodeBridgeUrl) {
      this.nodeBridgeUrl = nodeBridgeUrl;
    }

    public String getExecutablePath() {
      return executablePath;
    }


    public void setExecutablePath(String executablePath) {
      this.executablePath = executablePath;
    }

    public java.util.Map<String, BrowserProfile> getProfiles() {
      return profiles;
    }

    public void setProfiles(java.util.Map<String, BrowserProfile> profiles) {
      this.profiles = profiles == null ? new java.util.HashMap<>() : profiles;
    }

    public AntiBot getAntiBot() {
      return antiBot;
    }
  }

  public static class BrowserProfile {
    private String driver = "agentbot";
    private String cdpUrl = "";
    private String userDataDir = "";
    private String color = "#FF4500";

    public String getDriver() {
      return driver;
    }

    public void setDriver(String driver) {
      this.driver = driver;
    }

    public String getCdpUrl() {
      return cdpUrl;
    }

    public void setCdpUrl(String cdpUrl) {
      this.cdpUrl = cdpUrl;
    }

    public String getUserDataDir() {
      return userDataDir;
    }

    public void setUserDataDir(String userDataDir) {
      this.userDataDir = userDataDir;
    }

    public String getColor() {
      return color;
    }

    public void setColor(String color) {
      this.color = color;
    }
  }

  public static class AntiBot {

    private String level = "basic";
    private String userAgent = "";
    private String locale = "zh-CN";
    private String timezoneId = "Asia/Shanghai";
    private java.util.Map<String, String> headers = new java.util.HashMap<>();
    private java.util.List<String> blockResourceTypes = new java.util.ArrayList<>();
    private java.util.List<String> blockUrlPatterns = new java.util.ArrayList<>();
    private java.util.List<String> proxies = new java.util.ArrayList<>();
    private boolean enableBehavior = true;
    private boolean enableDetection = true;
    private boolean enableStealth = true;
    private boolean enableResourceBlock = true;

    public String getLevel() {
      return level;
    }

    public void setLevel(String level) {
      this.level = level;
    }

    public String getUserAgent() {
      return userAgent;
    }

    public void setUserAgent(String userAgent) {
      this.userAgent = userAgent;
    }

    public String getLocale() {
      return locale;
    }

    public void setLocale(String locale) {
      this.locale = locale;
    }

    public String getTimezoneId() {
      return timezoneId;
    }

    public void setTimezoneId(String timezoneId) {
      this.timezoneId = timezoneId;
    }

    public java.util.Map<String, String> getHeaders() {
      return headers;
    }

    public void setHeaders(java.util.Map<String, String> headers) {
      this.headers = headers;
    }

    public java.util.List<String> getBlockResourceTypes() {
      return blockResourceTypes;
    }

    public void setBlockResourceTypes(java.util.List<String> blockResourceTypes) {
      this.blockResourceTypes = blockResourceTypes;
    }

    public java.util.List<String> getBlockUrlPatterns() {
      return blockUrlPatterns;
    }

    public void setBlockUrlPatterns(java.util.List<String> blockUrlPatterns) {
      this.blockUrlPatterns = blockUrlPatterns;
    }

    public java.util.List<String> getProxies() {
      return proxies;
    }

    public void setProxies(java.util.List<String> proxies) {
      this.proxies = proxies;
    }

    public boolean isEnableBehavior() {
      return enableBehavior;
    }

    public void setEnableBehavior(boolean enableBehavior) {
      this.enableBehavior = enableBehavior;
    }

    public boolean isEnableDetection() {
      return enableDetection;
    }

    public void setEnableDetection(boolean enableDetection) {
      this.enableDetection = enableDetection;
    }

    public boolean isEnableStealth() {
      return enableStealth;
    }

    public void setEnableStealth(boolean enableStealth) {
      this.enableStealth = enableStealth;
    }

    public boolean isEnableResourceBlock() {
      return enableResourceBlock;
    }

    public void setEnableResourceBlock(boolean enableResourceBlock) {
      this.enableResourceBlock = enableResourceBlock;
    }
  }

  public static class P2p {


    private boolean enabled = true;
    private int port = 190311;
    private String peersFile = "peers.yml";
    private int maxNeighbors = 8;
    private int getaddrLimit = 50;
    private int getaddrIntervalSeconds = 300;
    private double getaddrSampleRatio = 0.5;
    private int getaddrMaxPerMinute = 60;
    private int getaddrBackoffMaxSeconds = 1800;
    private int refreshSeconds = 300;
    private int persistSeconds = 900;

    private boolean skillExchangeEnabled = true;
    private int skillInvIntervalSeconds = 600;
    private double skillInvSampleRatio = 0.5;
    private int skillInvMaxPerRound = 100;
    private int skillGetdataMaxPerMinute = 120;
    private int skillMaxPackageBytes = 2097152;

    private java.util.List<String> seeds = java.util.List.of();

    private String regionId = "default";
    private int protocolVersion = 1;
    private int maxPayload = 1048576;
    private boolean requireEncryption = true;
    private String preferredCipherSuite = "CHACHA20_POLY1305";
    private String preferredContentType = "application/json";
    private java.util.List<String> supportedContentTypes = java.util.List.of();
    private java.util.List<String> supportedCompression = java.util.List.of("none");
    private java.util.List<String> supportedCipherSuites = java.util.List.of();
    private boolean authRequired = false;
    private boolean obfuscationEnabled = false;
    private String obfuscationAlgo = "elligator-swift-lite";
    private int flowWindow = 1024;
    private int maxInFlight = 256;
    private int idempotentWindow = 10000;
    private boolean chatAckEnabled = true;
    private boolean chatNackEnabled = true;
    private int chatTtlDefault = 5;
    private int chatFanout = 3;
    private long chatAckTimeoutMs = 60000;
    private int chatRetryMax = 2;
    private int chatDedupWindowMs = 600000;
    private int chatRateLimitQps = 100;
    private int chatMaxPayloadBytes = 65536;



    public boolean isEnabled() {

      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port;
    }

    public String getPeersFile() {
      return peersFile;
    }

    public void setPeersFile(String peersFile) {
      this.peersFile = peersFile;
    }

    public int getMaxNeighbors() {
      return maxNeighbors;
    }

    public void setMaxNeighbors(int maxNeighbors) {
      this.maxNeighbors = maxNeighbors;
    }

    public int getGetaddrLimit() {
      return getaddrLimit;
    }

    public void setGetaddrLimit(int getaddrLimit) {
      this.getaddrLimit = getaddrLimit;
    }

    public int getGetaddrIntervalSeconds() {
      return getaddrIntervalSeconds;
    }

    public void setGetaddrIntervalSeconds(int getaddrIntervalSeconds) {
      this.getaddrIntervalSeconds = getaddrIntervalSeconds;
    }

    public double getGetaddrSampleRatio() {
      return getaddrSampleRatio;
    }

    public void setGetaddrSampleRatio(double getaddrSampleRatio) {
      this.getaddrSampleRatio = getaddrSampleRatio;
    }

    public int getGetaddrMaxPerMinute() {
      return getaddrMaxPerMinute;
    }

    public void setGetaddrMaxPerMinute(int getaddrMaxPerMinute) {
      this.getaddrMaxPerMinute = getaddrMaxPerMinute;
    }

    public int getGetaddrBackoffMaxSeconds() {
      return getaddrBackoffMaxSeconds;
    }

    public void setGetaddrBackoffMaxSeconds(int getaddrBackoffMaxSeconds) {
      this.getaddrBackoffMaxSeconds = getaddrBackoffMaxSeconds;
    }

    public int getRefreshSeconds() {
      return refreshSeconds;
    }



    public void setRefreshSeconds(int refreshSeconds) {
      this.refreshSeconds = refreshSeconds;
    }

    public int getPersistSeconds() {
      return persistSeconds;
    }

    public void setPersistSeconds(int persistSeconds) {
      this.persistSeconds = persistSeconds;
    }

    public boolean isSkillExchangeEnabled() {
      return skillExchangeEnabled;
    }

    public void setSkillExchangeEnabled(boolean skillExchangeEnabled) {
      this.skillExchangeEnabled = skillExchangeEnabled;
    }

    public int getSkillInvIntervalSeconds() {
      return skillInvIntervalSeconds;
    }

    public void setSkillInvIntervalSeconds(int skillInvIntervalSeconds) {
      this.skillInvIntervalSeconds = skillInvIntervalSeconds;
    }

    public double getSkillInvSampleRatio() {
      return skillInvSampleRatio;
    }

    public void setSkillInvSampleRatio(double skillInvSampleRatio) {
      this.skillInvSampleRatio = skillInvSampleRatio;
    }

    public int getSkillInvMaxPerRound() {
      return skillInvMaxPerRound;
    }

    public void setSkillInvMaxPerRound(int skillInvMaxPerRound) {
      this.skillInvMaxPerRound = skillInvMaxPerRound;
    }

    public int getSkillGetdataMaxPerMinute() {
      return skillGetdataMaxPerMinute;
    }

    public void setSkillGetdataMaxPerMinute(int skillGetdataMaxPerMinute) {
      this.skillGetdataMaxPerMinute = skillGetdataMaxPerMinute;
    }

    public int getSkillMaxPackageBytes() {
      return skillMaxPackageBytes;
    }

    public void setSkillMaxPackageBytes(int skillMaxPackageBytes) {
      this.skillMaxPackageBytes = skillMaxPackageBytes;
    }

    public java.util.List<String> getSeeds() {
      return seeds;
    }


    public void setSeeds(java.util.List<String> seeds) {
      this.seeds = seeds;
    }

    public String getRegionId() {
      return regionId;
    }

    public void setRegionId(String regionId) {
      this.regionId = regionId;
    }

    public int getProtocolVersion() {
      return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
      this.protocolVersion = protocolVersion;
    }

    public int getMaxPayload() {
      return maxPayload;
    }

    public void setMaxPayload(int maxPayload) {
      this.maxPayload = maxPayload;
    }

    public boolean isRequireEncryption() {
      return requireEncryption;
    }

    public void setRequireEncryption(boolean requireEncryption) {
      this.requireEncryption = requireEncryption;
    }

    public String getPreferredCipherSuite() {
      return preferredCipherSuite;
    }

    public void setPreferredCipherSuite(String preferredCipherSuite) {
      this.preferredCipherSuite = preferredCipherSuite;
    }

    public String getPreferredContentType() {
      return preferredContentType;
    }

    public void setPreferredContentType(String preferredContentType) {
      this.preferredContentType = preferredContentType;
    }

    public java.util.List<String> getSupportedContentTypes() {
      return supportedContentTypes;
    }

    public void setSupportedContentTypes(java.util.List<String> supportedContentTypes) {
      this.supportedContentTypes = supportedContentTypes;
    }

    public java.util.List<String> getSupportedCompression() {
      return supportedCompression;
    }

    public void setSupportedCompression(java.util.List<String> supportedCompression) {
      this.supportedCompression = supportedCompression;
    }

    public java.util.List<String> getSupportedCipherSuites() {
      return supportedCipherSuites;
    }

    public void setSupportedCipherSuites(java.util.List<String> supportedCipherSuites) {
      this.supportedCipherSuites = supportedCipherSuites;
    }

    public boolean isAuthRequired() {
      return authRequired;
    }

    public void setAuthRequired(boolean authRequired) {
      this.authRequired = authRequired;
    }

    public boolean isObfuscationEnabled() {
      return obfuscationEnabled;
    }

    public void setObfuscationEnabled(boolean obfuscationEnabled) {
      this.obfuscationEnabled = obfuscationEnabled;
    }

    public String getObfuscationAlgo() {
      return obfuscationAlgo;
    }

    public void setObfuscationAlgo(String obfuscationAlgo) {
      this.obfuscationAlgo = obfuscationAlgo;
    }

    public int getFlowWindow() {
      return flowWindow;
    }

    public void setFlowWindow(int flowWindow) {
      this.flowWindow = flowWindow;
    }

    public int getMaxInFlight() {
      return maxInFlight;
    }

    public void setMaxInFlight(int maxInFlight) {
      this.maxInFlight = maxInFlight;
    }

    public int getIdempotentWindow() {
      return idempotentWindow;
    }

    public void setIdempotentWindow(int idempotentWindow) {
      this.idempotentWindow = idempotentWindow;
    }

    public boolean isChatAckEnabled() {
      return chatAckEnabled;
    }

    public void setChatAckEnabled(boolean chatAckEnabled) {
      this.chatAckEnabled = chatAckEnabled;
    }

    public boolean isChatNackEnabled() {
      return chatNackEnabled;
    }

    public void setChatNackEnabled(boolean chatNackEnabled) {
      this.chatNackEnabled = chatNackEnabled;
    }

    public int getChatTtlDefault() {
      return chatTtlDefault;
    }

    public void setChatTtlDefault(int chatTtlDefault) {
      this.chatTtlDefault = chatTtlDefault;
    }

    public int getChatFanout() {
      return chatFanout;
    }

    public void setChatFanout(int chatFanout) {
      this.chatFanout = chatFanout;
    }

    public long getChatAckTimeoutMs() {
      return chatAckTimeoutMs;
    }

    public void setChatAckTimeoutMs(long chatAckTimeoutMs) {
      this.chatAckTimeoutMs = chatAckTimeoutMs;
    }

    public int getChatRetryMax() {
      return chatRetryMax;
    }

    public void setChatRetryMax(int chatRetryMax) {
      this.chatRetryMax = chatRetryMax;
    }

    public int getChatDedupWindowMs() {
      return chatDedupWindowMs;
    }

    public void setChatDedupWindowMs(int chatDedupWindowMs) {
      this.chatDedupWindowMs = chatDedupWindowMs;
    }

    public int getChatRateLimitQps() {
      return chatRateLimitQps;
    }

    public void setChatRateLimitQps(int chatRateLimitQps) {
      this.chatRateLimitQps = chatRateLimitQps;
    }

    public int getChatMaxPayloadBytes() {
      return chatMaxPayloadBytes;
    }

    public void setChatMaxPayloadBytes(int chatMaxPayloadBytes) {
      this.chatMaxPayloadBytes = chatMaxPayloadBytes;
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



