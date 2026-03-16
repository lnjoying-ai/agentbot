package com.agentbot.core.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent configuration model loaded from config.json
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentConfig {
  
  private String id;
  private String name;
  
  @JsonProperty("displayName")
  private String displayName;
  
  private String description;
  private String avatar;
  private boolean enabled = true;
  
  @JsonProperty("createdAt")
  private String createdAt;
  
  @JsonProperty("updatedAt")
  private String updatedAt;
  
  private PersonalityConfig personality = new PersonalityConfig();
  private BehaviorConfig behavior = new BehaviorConfig();
  private CapabilitiesConfig capabilities = new CapabilitiesConfig();
  private MemoryConfig memory = new MemoryConfig();
  private SessionsConfig sessions = new SessionsConfig();
  private RoutingConfig routing = new RoutingConfig();
  private LlmConfig llm = new LlmConfig();
  private Map<String, Object> metadata = new HashMap<>();
  
  // Constructors
  public AgentConfig() {
    this.createdAt = Instant.now().toString();
    this.updatedAt = Instant.now().toString();
  }
  
  public AgentConfig(String id, String name) {
    this();
    this.id = id;
    this.name = name;
    this.displayName = name;
  }
  
  // Getters and Setters
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getDisplayName() {
    return displayName != null ? displayName : name;
  }
  
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public String getAvatar() {
    return avatar;
  }
  
  public void setAvatar(String avatar) {
    this.avatar = avatar;
  }
  
  public boolean isEnabled() {
    return enabled;
  }
  
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
  
  public String getCreatedAt() {
    return createdAt;
  }
  
  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }
  
  public String getUpdatedAt() {
    return updatedAt;
  }
  
  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }
  
  public PersonalityConfig getPersonality() {
    return personality;
  }
  
  public void setPersonality(PersonalityConfig personality) {
    this.personality = personality;
  }
  
  public BehaviorConfig getBehavior() {
    return behavior;
  }
  
  public void setBehavior(BehaviorConfig behavior) {
    this.behavior = behavior;
  }
  
  public CapabilitiesConfig getCapabilities() {
    return capabilities;
  }
  
  public void setCapabilities(CapabilitiesConfig capabilities) {
    this.capabilities = capabilities;
  }
  
  public MemoryConfig getMemory() {
    return memory;
  }
  
  public void setMemory(MemoryConfig memory) {
    this.memory = memory;
  }
  
  public SessionsConfig getSessions() {
    return sessions;
  }
  
  public void setSessions(SessionsConfig sessions) {
    this.sessions = sessions;
  }
  
  public RoutingConfig getRouting() {
    return routing;
  }
  
  public void setRouting(RoutingConfig routing) {
    this.routing = routing;
  }
  
  public LlmConfig getLlm() {
    return llm;
  }
  
  public void setLlm(LlmConfig llm) {
    this.llm = llm;
  }
  
  public Map<String, Object> getMetadata() {
    return metadata;
  }
  
  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }
  
  // Nested configuration classes
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PersonalityConfig {
    @JsonProperty("useSoul")
    private boolean useSoul = true;
    
    @JsonProperty("soulPath")
    private String soulPath;
    
    @JsonProperty("fallbackToSystem")
    private boolean fallbackToSystem = true;
    
    public boolean isUseSoul() {
      return useSoul;
    }
    
    public void setUseSoul(boolean useSoul) {
      this.useSoul = useSoul;
    }
    
    public String getSoulPath() {
      return soulPath;
    }
    
    public void setSoulPath(String soulPath) {
      this.soulPath = soulPath;
    }
    
    public boolean isFallbackToSystem() {
      return fallbackToSystem;
    }
    
    public void setFallbackToSystem(boolean fallbackToSystem) {
      this.fallbackToSystem = fallbackToSystem;
    }
  }
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class BehaviorConfig {
    @JsonProperty("useAgentMd")
    private boolean useAgentMd = true;
    
    @JsonProperty("agentMdPath")
    private String agentMdPath;
    
    @JsonProperty("fallbackToSystem")
    private boolean fallbackToSystem = true;
    
    public boolean isUseAgentMd() {
      return useAgentMd;
    }
    
    public void setUseAgentMd(boolean useAgentMd) {
      this.useAgentMd = useAgentMd;
    }
    
    public String getAgentMdPath() {
      return agentMdPath;
    }
    
    public void setAgentMdPath(String agentMdPath) {
      this.agentMdPath = agentMdPath;
    }
    
    public boolean isFallbackToSystem() {
      return fallbackToSystem;
    }
    
    public void setFallbackToSystem(boolean fallbackToSystem) {
      this.fallbackToSystem = fallbackToSystem;
    }
  }
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CapabilitiesConfig {
    private ToolsConfig tools = new ToolsConfig();
    private SkillsConfig skills = new SkillsConfig();
    
    public ToolsConfig getTools() {
      return tools;
    }
    
    public void setTools(ToolsConfig tools) {
      this.tools = tools;
    }
    
    public SkillsConfig getSkills() {
      return skills;
    }
    
    public void setSkills(SkillsConfig skills) {
      this.skills = skills;
    }
  }
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ToolsConfig {
    private List<String> inherited = new ArrayList<>();
    private List<String> disabled = new ArrayList<>();
    private List<String> custom = new ArrayList<>();
    
    public List<String> getInherited() {
      return inherited;
    }
    
    public void setInherited(List<String> inherited) {
      this.inherited = inherited;
    }
    
    public List<String> getDisabled() {
      return disabled;
    }
    
    public void setDisabled(List<String> disabled) {
      this.disabled = disabled;
    }
    
    public List<String> getCustom() {
      return custom;
    }
    
    public void setCustom(List<String> custom) {
      this.custom = custom;
    }
  }
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class SkillsConfig {
    private boolean inherited = true;
    
    @JsonProperty("customPath")
    private String customPath;
    
    @JsonProperty("entries")
    private Map<String, SkillEntryConfig> entries = new HashMap<>();
    
    public boolean isInherited() {
      return inherited;
    }
    
    public void setInherited(boolean inherited) {
      this.inherited = inherited;
    }
    
    public String getCustomPath() {
      return customPath;
    }
    
    public void setCustomPath(String customPath) {
      this.customPath = customPath;
    }
    
    public Map<String, SkillEntryConfig> getEntries() {
      return entries;
    }
    
    public void setEntries(Map<String, SkillEntryConfig> entries) {
      this.entries = entries;
    }
  }
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class SkillEntryConfig {
    private Boolean enabled = true;
    private String apiKey;
    private Map<String, String> env = new HashMap<>();
    
    public Boolean getEnabled() {
      return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
      this.enabled = enabled;
    }
    
    public String getApiKey() {
      return apiKey;
    }
    
    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }
    
    public Map<String, String> getEnv() {
      return env;
    }
    
    public void setEnv(Map<String, String> env) {
      this.env = env;
    }
  }

  
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class MemoryConfig {
    private boolean enabled = true;
    private String path;
    
    @JsonProperty("maxLongTermLines")
    private int maxLongTermLines = 500;
    
    @JsonProperty("maxDailyLines")
    private int maxDailyLines = 1000;
    
    public boolean isEnabled() {
      return enabled;
    }
    
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
    
    public String getPath() {
      return path;
    }
    
    public void setPath(String path) {
      this.path = path;
    }
    
    public int getMaxLongTermLines() {
      return maxLongTermLines;
    }
    
    public void setMaxLongTermLines(int maxLongTermLines) {
      this.maxLongTermLines = maxLongTermLines;
    }
    
    public int getMaxDailyLines() {
      return maxDailyLines;
    }
    
    public void setMaxDailyLines(int maxDailyLines) {
      this.maxDailyLines = maxDailyLines;
    }
  }
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class SessionsConfig {
    private String path;
    
    @JsonProperty("maxHistoryMessages")
    private int maxHistoryMessages = 20;
    
    @JsonProperty("retentionDays")
    private int retentionDays = 30;
    
    public String getPath() {
      return path;
    }
    
    public void setPath(String path) {
      this.path = path;
    }
    
    public int getMaxHistoryMessages() {
      return maxHistoryMessages;
    }
    
    public void setMaxHistoryMessages(int maxHistoryMessages) {
      this.maxHistoryMessages = maxHistoryMessages;
    }
    
    public int getRetentionDays() {
      return retentionDays;
    }
    
    public void setRetentionDays(int retentionDays) {
      this.retentionDays = retentionDays;
    }
  }
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RoutingConfig {
    private List<String> channels = new ArrayList<>();
    
    @JsonProperty("autoRoute")
    private boolean autoRoute = false;
    
    private List<String> keywords = new ArrayList<>();
    private int priority = 0;
    
    public List<String> getChannels() {
      return channels;
    }
    
    public void setChannels(List<String> channels) {
      this.channels = channels;
    }
    
    public boolean isAutoRoute() {
      return autoRoute;
    }
    
    public void setAutoRoute(boolean autoRoute) {
      this.autoRoute = autoRoute;
    }
    
    public List<String> getKeywords() {
      return keywords;
    }
    
    public void setKeywords(List<String> keywords) {
      this.keywords = keywords;
    }
    
    public int getPriority() {
      return priority;
    }
    
    public void setPriority(int priority) {
      this.priority = priority;
    }
  }
  
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class LlmConfig {
    private String provider;
    private String model;
    private double temperature = 0.7;
    
    @JsonProperty("maxTokens")
    private int maxTokens = 4096;
    
    @JsonProperty("overrideSystem")
    private boolean overrideSystem = false;
    
    @JsonProperty("maxToolRounds")
    private int maxToolRounds = 30;
    
    @JsonProperty("parallelTools")
    private boolean parallelTools = true;
    
    @JsonProperty("toolParallelism")
    private int toolParallelism = 3;
    
    public String getProvider() {
      return provider;
    }
    
    public void setProvider(String provider) {
      this.provider = provider;
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
    
    public int getMaxTokens() {
      return maxTokens;
    }
    
    public void setMaxTokens(int maxTokens) {
      this.maxTokens = maxTokens;
    }
    
    public boolean isOverrideSystem() {
      return overrideSystem;
    }
    
    public void setOverrideSystem(boolean overrideSystem) {
      this.overrideSystem = overrideSystem;
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
  }
}
