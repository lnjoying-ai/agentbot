package com.agentbot.config;

import com.agentbot.core.agent.AgentDispatcher;
import com.agentbot.core.agent.AgentRouter;
import com.agentbot.core.agent.AgentRuntime;
import com.agentbot.core.agent.DefaultAgentRouter;
import com.agentbot.core.agent.DefaultAgentRuntime;
import com.agentbot.core.agent.SubAgentManager;
import com.agentbot.core.bus.MessageBus;
import com.agentbot.core.memory.MemorySearch;
import com.agentbot.core.memory.MemoryService;
import com.agentbot.core.memory.MemoryStore;
import com.agentbot.core.model.FallbackLlmProvider;
import com.agentbot.core.model.LLMProvider;
import com.agentbot.core.model.OpenAiCompatibleProvider;
import com.agentbot.core.model.ToolCallParser;
import com.agentbot.core.session.JsonlSessionStore;
import com.agentbot.core.session.SessionService;
import com.agentbot.core.session.SessionStore;
import com.agentbot.core.skills.Skill;
import com.agentbot.core.skills.SkillLoader;
import com.agentbot.core.tools.ToolRegistry;
import com.agentbot.core.tools.impl.*;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;


@Configuration
public class AgentConfiguration {

  @Bean
  public SkillLoader skillLoader(AgentbotProperties properties) {
    return new SkillLoader(Path.of(properties.getWorkspaceDir()).resolve("skills"));
  }

  @Bean
  public List<Skill> loadedSkills(SkillLoader loader) {
    return loader.loadSkills();
  }

  @Bean
  public SessionStore sessionStore(AgentbotProperties properties) {
    return new JsonlSessionStore(Path.of(properties.getWorkspaceDir()).resolve("sessions"));
  }

  @Bean
  public SessionService sessionService(SessionStore store) {
    return new SessionService(store);
  }

  @Bean
  public MemoryStore memoryStore(AgentbotProperties properties) {
    return new MemoryStore(Path.of(properties.getWorkspaceDir()).resolve("memory"));
  }

  @Bean
  public MemoryService memoryService(MemoryStore store) {
    return new MemoryService(store);
  }

  @Bean
  public MemorySearch memorySearch(MemoryStore store) {
    return new MemorySearch(store);
  }

  @Bean
  public SubAgentManager subAgentManager() {
    return new SubAgentManager();
  }

  @Bean
  public ToolRegistry toolRegistry(MemorySearch memorySearch, MemoryStore memoryStore, MessageBus messageBus, SubAgentManager subAgentManager, AgentbotProperties properties) {
    ToolRegistry registry = new ToolRegistry();
    registry.register(new EchoTool());
    registry.register(new TimeTool());
    registry.register(new MemorySearchTool(memorySearch));
    registry.register(new MemoryGetTool(memoryStore));
    registry.register(new ShellTool());
    registry.register(new FileReadTool());
    registry.register(new FileWriteTool());
    
    AgentbotProperties.Search searchConfig = properties.getSearch();
    if ("brave".equalsIgnoreCase(searchConfig.getType())) {
      registry.register(new BraveSearchTool(searchConfig.getBraveApiKey()));
    } else {
      registry.register(new BochaSearchTool(searchConfig.getBochaApiKey()));
    }

    registry.register(new MessageTool(messageBus));

    registry.register(new SpawnTool(subAgentManager));
    registry.register(new BrowserTool(Path.of(properties.getWorkspaceDir())));
    return registry;
  }




  @Bean
  public ToolCallParser toolCallParser() {
    return new ToolCallParser();
  }

  @Bean
  public LLMProvider llmProvider(AgentbotProperties properties) {
    AgentbotProperties.Llm llm = properties.getLlm();
    String provider = llm.getProvider() == null ? "openai" : llm.getProvider().toLowerCase();

    List<String> order = List.of((llm.getFallbackOrder() == null ? "" : llm.getFallbackOrder()).split(","));
    List<LLMProvider> providers = new java.util.ArrayList<>();

    java.util.function.Function<String, LLMProvider> buildProvider = (name) -> {
      String key = name == null ? "" : name.trim().toLowerCase();
      if (key.isEmpty()) return null;
      if ("openrouter".equals(key)) {
        AgentbotProperties.Provider p = llm.getOpenrouter();
        String model = p.getModel().isBlank() ? llm.getModel() : p.getModel();
        return new OpenAiCompatibleProvider(
            p.getBaseUrl(),
            p.getApiKey().isBlank() ? llm.getApiKey() : p.getApiKey(),
            model,
            llm.getTemperature(),
            Map.of("HTTP-Referer", "http://localhost", "X-Title", "agentbot")
        );
      }
      if ("glm".equals(key)) {
        AgentbotProperties.Provider p = llm.getGlm();
        String model = p.getModel().isBlank() ? llm.getModel() : p.getModel();
        return new OpenAiCompatibleProvider(
            p.getBaseUrl(),
            p.getApiKey().isBlank() ? llm.getApiKey() : p.getApiKey(),
            model,
            llm.getTemperature(),
            Map.of()
        );
      }
      if ("kimi".equals(key)) {
        AgentbotProperties.Provider p = llm.getKimi();
        String model = p.getModel().isBlank() ? llm.getModel() : p.getModel();
        return new OpenAiCompatibleProvider(
            p.getBaseUrl(),
            p.getApiKey().isBlank() ? llm.getApiKey() : p.getApiKey(),
            model,
            llm.getTemperature(),
            Map.of()
        );
      }
      if ("openai".equals(key)) {
        return new OpenAiCompatibleProvider(
            llm.getBaseUrl(),
            llm.getApiKey(),
            llm.getModel(),
            llm.getTemperature(),
            Map.of()
        );
      }
      return null;
    };

    LLMProvider primary = buildProvider.apply(provider);
    if (primary != null) providers.add(primary);

    for (String entry : order) {
      String key = entry == null ? "" : entry.trim().toLowerCase();
      if (key.isEmpty() || key.equals(provider)) continue;
      LLMProvider fallback = buildProvider.apply(key);
      if (fallback != null) providers.add(fallback);
    }

    return new FallbackLlmProvider(providers);
  }



  @Bean
  public AgentRuntime agentRuntime(
      LLMProvider provider,
      ToolRegistry toolRegistry,
      ToolCallParser toolCallParser,
      SessionService sessionService,
      MemoryService memoryService,
      List<Skill> skills,
      AgentbotProperties properties
  ) {
    AgentbotProperties.Llm llm = properties.getLlm();
    return new DefaultAgentRuntime(
        provider,
        toolRegistry,
        toolCallParser,
        sessionService,
        memoryService,
        skills,
        llm.getMaxToolRounds(),
        llm.isParallelTools(),
        llm.getToolParallelism()
    );
  }


  @Bean
  public AgentRouter agentRouter() {
    return new DefaultAgentRouter();
  }

  @Bean
  public AgentDispatcher agentDispatcher(MessageBus bus, AgentRouter router, AgentRuntime runtime) {
    AgentDispatcher dispatcher = new AgentDispatcher(bus, router, Map.of("default", runtime));
    dispatcher.start();
    return dispatcher;
  }
}
