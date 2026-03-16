package com.agentbot.config;

import com.agentbot.core.agent.*;
import com.agentbot.core.browser.BrowserControlServer;
import com.agentbot.core.browser.BrowserService;
import com.agentbot.core.bus.ExternalMessageBus;
import com.agentbot.core.bus.InternalMessageBus;


import com.agentbot.core.heartbeat.HeartbeatService;
import com.agentbot.core.heartbeat.HeartbeatTaskExecutor;
import com.agentbot.core.memory.MemorySearch;
import com.agentbot.core.memory.MemoryStore;
import com.agentbot.core.model.FallbackLlmProvider;
import com.agentbot.core.model.LLMProvider;
import com.agentbot.core.model.OpenAiCompatibleProvider;
import com.agentbot.core.model.ToolCallParser;
import com.agentbot.core.monitor.AgentHealthMonitor;
import com.agentbot.core.monitor.AgentMetricsCollector;
import com.agentbot.core.security.AgentAccessControl;
import com.agentbot.core.security.AgentRateLimiter;
import com.agentbot.core.session.ChatHistoryService;
import com.agentbot.core.util.ConfigPathResolver;

import com.agentbot.core.session.ChatUnreadService;
import com.agentbot.core.session.JsonlSessionStore;
import com.agentbot.core.session.SessionService;
import com.agentbot.core.session.SessionStore;

import com.agentbot.core.skills.Skill;
import com.agentbot.core.skills.SkillLoader;
import com.agentbot.core.tools.AntiBotConfig;
import com.agentbot.core.tools.ToolRegistry;
import com.agentbot.core.tools.ToolApprovalPolicy;
import com.agentbot.core.tools.impl.*;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;



@Configuration
@EnableScheduling
public class AgentConfiguration {

  @Bean
  public SkillLoader skillLoader(AgentbotProperties properties) {
    Path workspace = resolveWorkspaceDir();

    List<Path> candidates = new ArrayList<>();
    candidates.add(workspace.resolve("skills"));
    candidates.add(workspace.resolve("system").resolve("skills"));

    return new SkillLoader(candidates);
  }

  @Bean
  public List<Skill> loadedSkills(SkillLoader loader) {
    return loader.loadSkills();
  }

  @Bean
  public SubAgentManager subAgentManager() {
    return new SubAgentManager();
  }
  
  @Bean
  public AgentGuidelinesService agentGuidelinesService(AgentbotProperties properties) {
    return new AgentGuidelinesService(resolveWorkspaceDir());
  }


  @Bean
  public ToolApprovalPolicy toolApprovalPolicy(AgentbotProperties properties) {
    return new ToolApprovalPolicy(properties.getApprovals().getTools());
  }

  @Bean
  public BrowserService browserService(AgentbotProperties properties, @Value("${server.port:8080}") int serverPort) {
    AntiBotConfig antiBotConfig = buildAntiBotConfig(properties);
    return new BrowserService(resolveWorkspaceDir(), properties.getBrowser(), antiBotConfig, serverPort);
  }


  @Bean(initMethod = "start", destroyMethod = "stop")
  public BrowserControlServer browserControlServer(BrowserService browserService) {
    return new BrowserControlServer(browserService, browserService.getControlPort());
  }

  @Bean
  public ToolRegistry toolRegistry(ExternalMessageBus messageBus, SubAgentManager subAgentManager, AgentbotProperties properties, ToolApprovalPolicy toolApprovalPolicy, BrowserService browserService) {

    ToolRegistry registry = new ToolRegistry(toolApprovalPolicy);


    registry.register(new EchoTool());
    registry.register(new TimeTool());
    
    // Memory tools will use default agent's memory - initialized after AgentRegistry
    // These tools will be re-initialized in AgentFactory for each agent
    
    registry.register(new ShellTool());
    registry.register(new FileReadTool(resolveWorkspaceDir()));
    registry.register(new FileWriteTool(resolveWorkspaceDir()));
    registry.register(new ListDirTool(resolveWorkspaceDir()));



    
    AgentbotProperties.Search searchConfig = properties.getSearch();
    if ("brave".equalsIgnoreCase(searchConfig.getType())) {
      registry.register(new BraveSearchTool(searchConfig.getBraveApiKey()));
    } else if ("apimesh".equalsIgnoreCase(searchConfig.getType())) {
      registry.register(new ApimeshSearchTool(searchConfig.getApimeshKey()));
    } else {
      registry.register(new BochaSearchTool(searchConfig.getBochaApiKey()));
    }


    registry.register(new MessageTool(messageBus));
    registry.register(new P2pMessageTool(messageBus));

    registry.register(new SpawnTool(subAgentManager));

    registry.register(new BrowserTool(browserService));
    return registry;
  }


  @Bean
  public PendingActionStore pendingActionStore() {
    return new PendingActionStore();
  }

  @Bean
  public ToolCallParser toolCallParser() {

    return new ToolCallParser();
  }

  @Bean
  public LLMProvider llmProvider(AgentbotProperties properties) {
    AgentbotProperties.Llm llm = properties.getLlm();
    String primaryProvider = llm.getProvider() == null ? "openai" : llm.getProvider().toLowerCase();

    List<String> order = parseFallbackOrder(llm.getFallbackOrder());
    List<FallbackLlmProvider.ProviderEntry> providers = new ArrayList<>();
    java.util.Set<String> seen = new java.util.HashSet<>();

    FallbackLlmProvider.ProviderEntry primary = buildProviderEntry(primaryProvider, null, llm);
    if (primary != null && seen.add(primary.getId())) {
      providers.add(primary);
    }

    for (String entry : order) {
      ProviderSpec spec = parseProviderSpec(entry, primaryProvider);
      if (spec == null) continue;
      FallbackLlmProvider.ProviderEntry fallback = buildProviderEntry(spec.providerName, spec.modelOverride, llm);
      if (fallback != null && seen.add(fallback.getId())) {
        providers.add(fallback);
      }
    }

    return new FallbackLlmProvider(providers);
  }


  @Bean
  public AgentFactory agentFactory(
      AgentbotProperties properties,
      LLMProvider llmProvider,
      ToolRegistry toolRegistry,
      SkillLoader skillLoader,
      ToolCallParser toolCallParser,
      PendingActionStore pendingActionStore,
      com.agentbot.core.events.SystemEventBus eventBus
  ) {
    return new AgentFactory(
        resolveWorkspaceDir(),
        properties,
        llmProvider,
        toolRegistry,
        skillLoader,
        toolCallParser,
        pendingActionStore,
        eventBus
    );

  }


  @Bean
  public AgentMessageBus agentMessageBus(AgentRegistry registry) {
    AgentMessageBus messageBus = new AgentMessageBus(registry, 10);
    messageBus.start();
    return messageBus;
  }

  @Bean
  public InternalMessageBus internalMessageBus(AgentMessageBus agentMessageBus) {
    return new InternalMessageBus(agentMessageBus);
  }
  
  @Bean
  public AgentRegistry agentRegistry(AgentbotProperties properties, AgentFactory factory) {

    AgentRegistry registry = new AgentRegistry(
        resolveWorkspaceDir(),
        factory
    );

    registry.initialize();
    return registry;
  }

  @Bean
  public SessionRoutingStrategy sessionRoutingStrategy() {
    return new SessionRoutingStrategy();
  }

  @Bean
  public KeywordRoutingStrategy keywordRoutingStrategy() {
    return new KeywordRoutingStrategy();
  }

  @Bean
  public ChannelRoutingStrategy channelRoutingStrategy() {
    return new ChannelRoutingStrategy();
  }

  @Bean
  public BindingRoutingStrategy bindingRoutingStrategy(AgentbotProperties properties) {
    return new BindingRoutingStrategy(properties);
  }

  @Bean
  public GeoRoutingStrategy geoRoutingStrategy(AgentbotProperties properties) {
    return new GeoRoutingStrategy(properties.getP2p().getRegionId());
  }

  @Bean
  public AgentRouter agentRouter(
      AgentRegistry registry,
      AgentbotProperties properties,
      BindingRoutingStrategy bindingRoutingStrategy,
      SessionRoutingStrategy sessionStrategy,
      KeywordRoutingStrategy keywordStrategy,
      GeoRoutingStrategy geoRoutingStrategy,
      ChannelRoutingStrategy channelStrategy
  ) {
    // Routing priority: Bindings > Session > Keyword > Geo > Channel
    String defaultAgentId = properties.getAgents().getDefaults().getDefaultAgentId();
    return new MultiAgentRouter(registry, List.of(
        bindingRoutingStrategy,
        sessionStrategy,
        keywordStrategy,
        geoRoutingStrategy,
        channelStrategy
    ), defaultAgentId);
  }


  @Bean
  public AgentDispatcher agentDispatcher(ExternalMessageBus bus, AgentRouter router, AgentRegistry registry) {
    AgentDispatcher dispatcher = new AgentDispatcher(bus, router, registry);
    dispatcher.start();
    return dispatcher;
  }



  @Bean
  public HeartbeatTaskExecutor heartbeatTaskExecutor(AgentRegistry registry) {
    // Use default agent's runtime for heartbeat tasks
    AgentInstance defaultAgent = registry.getAgent("default");
    if (defaultAgent == null) {
      throw new RuntimeException("Default agent not found for HeartbeatTaskExecutor");
    }
    return new HeartbeatTaskExecutor(defaultAgent);

  }

  @Bean
  public HeartbeatService heartbeatService(AgentbotProperties properties, HeartbeatTaskExecutor executor) {
    return new HeartbeatService(
        resolveWorkspaceDir().resolve("system"),
        executor,
        properties.getHeartbeat().isEnabled(),
        properties.getHeartbeat().getIntervalSeconds()
    );

  }
  
  @Bean
  public AgentRuntime agentRuntime(AgentRegistry registry) {
    // Provide default agent's runtime for CLI and other services (wrap to update session state)
    AgentInstance defaultAgent = registry.getAgent("default");
    if (defaultAgent == null) {
      throw new RuntimeException("Default agent not found for AgentRuntime bean");
    }
    return defaultAgent::handle;
  }


  @Bean
  public MemorySearch memorySearch(AgentRegistry registry) {
    // Use default agent's memory service to get the memory store
    AgentInstance defaultAgent = registry.getAgent("default");
    if (defaultAgent == null) {
      throw new RuntimeException("Default agent not found for MemorySearch bean");
    }
    // Create MemoryStore from default agent's memory directory
    Path memoryDir = resolveWorkspaceDir()
        .resolve("agents")
        .resolve("default")

        .resolve("memory");
    MemoryStore memoryStore = new MemoryStore(memoryDir);
    return new MemorySearch(memoryStore);
  }

  @Bean(initMethod = "start", destroyMethod = "stop")
  public AgentHealthMonitor agentHealthMonitor(AgentRegistry registry) {
    return new AgentHealthMonitor(registry);
  }

  @Bean
  public AgentMetricsCollector agentMetricsCollector(AgentRegistry registry) {
    return new AgentMetricsCollector(registry);
  }

  @Bean(destroyMethod = "shutdown")
  public AgentRateLimiter agentRateLimiter() {
    return new AgentRateLimiter();
  }

  @Bean
  public AgentAccessControl agentAccessControl() {
    return new AgentAccessControl();
  }

  @Bean
  public SessionStore sessionStore(AgentbotProperties properties) {
    Path sessionsDir = resolveWorkspaceDir()
        .resolve("agents")

        .resolve("default")
        .resolve("sessions");
    return new JsonlSessionStore(sessionsDir);
  }

  @Bean
  public SessionService sessionService(SessionStore sessionStore) {
    return new SessionService(sessionStore);
  }

  @Bean
  public ChatHistoryService chatHistoryService(AgentRegistry registry) {
    return new ChatHistoryService(registry);
  }


  @Bean
  public ChatUnreadService chatUnreadService() {
    return new ChatUnreadService();
  }


  private Path resolveWorkspaceDir() {
    return ConfigPathResolver.resolveUserDataDir().resolve("workspace").toAbsolutePath().normalize();
  }

  private AntiBotConfig buildAntiBotConfig(AgentbotProperties properties) {

    AgentbotProperties.AntiBot antiBot = properties == null || properties.getBrowser() == null
        ? null
        : properties.getBrowser().getAntiBot();
    if (antiBot == null) {
      return new AntiBotConfig(
          "basic",
          "",
          "zh-CN",
          "Asia/Shanghai",
          java.util.Map.of(),
          java.util.List.of("image", "font", "media"),
          java.util.List.of(),
          java.util.List.of(),
          true,
          true,
          true,
          true
      );
    }
    return new AntiBotConfig(
        antiBot.getLevel(),
        antiBot.getUserAgent(),
        antiBot.getLocale(),
        antiBot.getTimezoneId(),
        antiBot.getHeaders(),
        antiBot.getBlockResourceTypes(),
        antiBot.getBlockUrlPatterns(),
        antiBot.getProxies(),
        antiBot.isEnableBehavior(),
        antiBot.isEnableDetection(),
        antiBot.isEnableStealth(),
        antiBot.isEnableResourceBlock()
    );
  }

  private static class ProviderSpec {

    private final String providerName;
    private final String modelOverride;

    private ProviderSpec(String providerName, String modelOverride) {
      this.providerName = providerName;
      this.modelOverride = modelOverride;
    }
  }

  private List<String> parseFallbackOrder(String raw) {
    if (raw == null) return List.of();
    String trimmed = raw.trim();
    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
      trimmed = trimmed.substring(1, trimmed.length() - 1);
    }
    if (trimmed.isBlank()) return List.of();
    String[] parts = trimmed.split(",");
    List<String> result = new ArrayList<>();
    for (String part : parts) {
      String cleaned = stripQuotes(part);
      if (!cleaned.isBlank()) result.add(cleaned);
    }
    return result;
  }

  private ProviderSpec parseProviderSpec(String entry, String defaultProvider) {
    if (entry == null) return null;
    String cleaned = stripQuotes(entry);
    if (cleaned.isBlank()) return null;

    String provider = null;
    String modelOverride = null;

    if (cleaned.contains("/")) {
      String[] parts = cleaned.split("/", 2);
      provider = stripQuotes(parts[0]).trim();
      modelOverride = stripQuotes(parts[1]).trim();
    } else if (cleaned.contains(":")) {
      String[] parts = cleaned.split(":", 2);
      provider = stripQuotes(parts[0]).trim();
      modelOverride = stripQuotes(parts[1]).trim();
    } else if (isKnownProvider(cleaned)) {
      provider = cleaned.trim();
    } else {
      provider = defaultProvider;
      modelOverride = cleaned.trim();
    }

    if (provider == null || provider.isBlank()) {
      provider = defaultProvider;
    }

    if (modelOverride != null && modelOverride.isBlank()) {
      modelOverride = null;
    }

    return new ProviderSpec(provider.toLowerCase(), modelOverride);
  }

  private String stripQuotes(String value) {
    if (value == null) return "";
    String trimmed = value.trim();
    if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
        || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
      return trimmed.substring(1, trimmed.length() - 1).trim();
    }
    return trimmed;
  }

  private boolean isKnownProvider(String key) {
    if (key == null) return false;
    String lower = key.trim().toLowerCase();
    return "openai".equals(lower)
        || "openrouter".equals(lower)
        || "glm".equals(lower)
        || "kimi".equals(lower)
        || "qwen".equals(lower)
        || "minimax".equals(lower)
        || "apimesh".equals(lower);

  }


  private FallbackLlmProvider.ProviderEntry buildProviderEntry(
      String providerName,
      String modelOverride,
      AgentbotProperties.Llm llm
  ) {
    String key = providerName == null ? "" : providerName.trim().toLowerCase();
    if (key.isEmpty()) return null;

    if ("openrouter".equals(key)) {
      AgentbotProperties.Provider p = llm.getOpenrouter();
      String model = resolveModel(modelOverride, p.getModel(), null);
      return new FallbackLlmProvider.ProviderEntry(
          key,
          model,
          new OpenAiCompatibleProvider(
              p.getBaseUrl(),
              p.getApiKey(),
              model,
              llm.getTemperature(),
              llm.isLogHttpRequest(),
              llm.isLogHttpResponse(),
              Map.of("HTTP-Referer", "http://localhost", "X-Title", "agentbot")
          )
      );
    }

    if ("glm".equals(key)) {
      AgentbotProperties.Provider p = llm.getGlm();
      String model = resolveModel(modelOverride, p.getModel(), null);
      return new FallbackLlmProvider.ProviderEntry(
          key,
          model,
          new OpenAiCompatibleProvider(
              p.getBaseUrl(),
              p.getApiKey(),
              model,
              llm.getTemperature(),
              llm.isLogHttpRequest(),
              llm.isLogHttpResponse(),
              Map.of()
          )
      );
    }

    if ("kimi".equals(key)) {
      AgentbotProperties.Provider p = llm.getKimi();
      String model = resolveModel(modelOverride, p.getModel(), null);
      return new FallbackLlmProvider.ProviderEntry(
          key,
          model,
          new OpenAiCompatibleProvider(
              p.getBaseUrl(),
              p.getApiKey(),
              model,
              llm.getTemperature(),
              llm.isLogHttpRequest(),
              llm.isLogHttpResponse(),
              Map.of()
          )
      );
    }

    if ("qwen".equals(key)) {
      AgentbotProperties.Provider p = llm.getQwen();
      String model = resolveModel(modelOverride, p.getModel(), null);
      return new FallbackLlmProvider.ProviderEntry(
          key,
          model,
          new OpenAiCompatibleProvider(
              p.getBaseUrl(),
              p.getApiKey(),
              model,
              llm.getTemperature(),
              llm.isLogHttpRequest(),
              llm.isLogHttpResponse(),
              Map.of()
          )
      );
    }

    if ("minimax".equals(key)) {
      AgentbotProperties.Provider p = llm.getMinimax();
      String model = resolveModel(modelOverride, p.getModel(), null);
      return new FallbackLlmProvider.ProviderEntry(
          key,
          model,
          new OpenAiCompatibleProvider(
              p.getBaseUrl(),
              p.getApiKey(),
              model,
              llm.getTemperature(),
              llm.isLogHttpRequest(),
              llm.isLogHttpResponse(),
              Map.of()
          )
      );
    }

    if ("apimesh".equals(key)) {
      AgentbotProperties.Provider p = llm.getApimesh();
      String model = resolveModel(modelOverride, p.getModel(), null);
      return new FallbackLlmProvider.ProviderEntry(
          key,
          model,
          new OpenAiCompatibleProvider(
              p.getBaseUrl(),
              p.getApiKey(),
              model,
              llm.getTemperature(),
              llm.isLogHttpRequest(),
              llm.isLogHttpResponse(),
              Map.of()
          )
      );
    }

    if ("openai".equals(key)) {
      AgentbotProperties.Provider p = llm.getOpenai();
      String model = resolveModel(modelOverride, p.getModel(), null);
      return new FallbackLlmProvider.ProviderEntry(
          key,
          model,
          new OpenAiCompatibleProvider(
              p.getBaseUrl(),
              p.getApiKey(),
              model,
              llm.getTemperature(),
              llm.isLogHttpRequest(),
              llm.isLogHttpResponse(),
              Map.of()
          )
      );
    }


    return null;
  }


  private String normalizeBaseUrl(String raw) {
    if (raw == null) return "";
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) return "";
    if (trimmed.endsWith("/chat/completions")) {
      return trimmed.substring(0, trimmed.length() - "/chat/completions".length());
    }
    if (trimmed.endsWith("/text/chatcompletion_v2")) {
      return trimmed.substring(0, trimmed.length() - "/text/chatcompletion_v2".length());
    }
    return trimmed;
  }


  private String resolveModel(String override, String providerModel, String defaultModel) {
    if (override != null && !override.isBlank()) return override;
    if (providerModel != null && !providerModel.isBlank()) return providerModel;
    return defaultModel == null ? "" : defaultModel;
  }
}

