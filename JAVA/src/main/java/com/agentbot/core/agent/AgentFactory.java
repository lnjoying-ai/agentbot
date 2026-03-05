package com.agentbot.core.agent;

import com.agentbot.core.memory.MemoryService;
import com.agentbot.core.memory.MemoryStore;
import com.agentbot.core.model.LLMProvider;
import com.agentbot.core.model.ToolCallParser;
import com.agentbot.core.session.JsonlSessionStore;
import com.agentbot.core.session.SessionService;
import com.agentbot.core.session.SessionStore;
import com.agentbot.core.skills.Skill;
import com.agentbot.core.skills.SkillLoader;
import com.agentbot.core.events.SystemEventBus;
import com.agentbot.core.tools.Tool;
import com.agentbot.core.tools.ToolRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Factory for creating agent instances with proper dependency injection.
 */
public class AgentFactory {
  private static final Logger log = LoggerFactory.getLogger(AgentFactory.class);
  
  private final Path workspacePath;
  private final LLMProvider llmProvider;
  private final ToolRegistry systemTools;
  private final SkillLoader systemSkills;
  private final ToolCallParser toolCallParser;
  private final PendingActionStore pendingActionStore;
  private final SystemEventBus eventBus;
  
  public AgentFactory(
      Path workspacePath,
      LLMProvider llmProvider,
      ToolRegistry systemTools,
      SkillLoader systemSkills,
      ToolCallParser toolCallParser,
      PendingActionStore pendingActionStore,
      SystemEventBus eventBus
  ) {

    this.workspacePath = workspacePath;
    this.llmProvider = llmProvider;
    this.systemTools = systemTools;
    this.systemSkills = systemSkills;
    this.toolCallParser = toolCallParser;
    this.pendingActionStore = pendingActionStore;
    this.eventBus = eventBus;
  }

  
  /**
   * Create agent instance from configuration.
   */
  public AgentInstance createAgent(AgentConfig config) {
    String agentId = config.getId();
    log.info("Creating agent instance: {} ({})", agentId, config.getName());
    
    Path agentDir = workspacePath.resolve("agents").resolve(agentId);
    
    // Ensure directory structure exists
    ensureDirectoryStructure(agentDir);
    
    // 1. Create agent-specific guidelines service
    Path systemDir = workspacePath.resolve("system");
    AgentGuidelinesService guidelines = new MultiLevelGuidelinesService(systemDir, agentDir, config);
    
    // 2. Create agent-specific memory
    Path memoryDir = agentDir.resolve("memory");
    MemoryStore memoryStore = new MemoryStore(memoryDir);
    MemoryService memory = new MemoryService(memoryStore);
    
    // 3. Create agent-specific sessions
    Path sessionsDir = agentDir.resolve("sessions");
    SessionStore sessionStore = new JsonlSessionStore(sessionsDir);
    SessionService sessions = new SessionService(sessionStore);
    
    // 4. Create agent-specific skills
    Path skillsDir = agentDir.resolve("skills");
    SkillLoader skills = new SkillLoader(skillsDir);
    List<Skill> loadedSkills = new ArrayList<>();
    // agent skills
    loadedSkills.addAll(skills.loadSkills());
    // custom path skills
    String customPath = config.getCapabilities().getSkills().getCustomPath();
    if (customPath != null && !customPath.isBlank()) {
      SkillLoader customLoader = new SkillLoader(Path.of(customPath));
      loadedSkills.addAll(customLoader.loadSkills());
    }
    
    // Add system skills if inherited
    if (config.getCapabilities().getSkills().isInherited()) {
      List<Skill> systemSkillsList = systemSkills.loadSkills();
      log.info("Agent {} inheriting {} system skills", agentId, systemSkillsList.size());
      loadedSkills.addAll(systemSkillsList);
    }
    
    // Filter & inject skill config
    Map<String, AgentConfig.SkillEntryConfig> skillEntries =
        config.getCapabilities().getSkills().getEntries() == null
            ? Map.of()
            : config.getCapabilities().getSkills().getEntries();
    loadedSkills = loadedSkills.stream()
        .filter(skill -> {
          AgentConfig.SkillEntryConfig entry = skillEntries.getOrDefault(skill.getName(), null);
          if (entry != null && Boolean.FALSE.equals(entry.getEnabled())) {
            log.debug("Skill {} disabled by config", skill.getName());
            return false;
          }
          return true;
        })
        .peek(skill -> {
          AgentConfig.SkillEntryConfig entry = skillEntries.get(skill.getName());
          if (entry != null) {
            if (entry.getApiKey() != null && !entry.getApiKey().isBlank()) {
              skill.getMetadata().put("apiKey", entry.getApiKey().trim());
            }
            if (entry.getEnv() != null && !entry.getEnv().isEmpty()) {
              skill.getMetadata().put("env", new HashMap<>(entry.getEnv()));
            }
          }
        })
        .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
    
    // 5. Create agent-specific tools

    ToolRegistry tools = createToolRegistry(config);
    
    // Add agent-specific memory tools
    addMemoryTools(tools, memoryStore, memory);
    
    // 6. Determine LLM configuration
    int maxToolRounds = config.getLlm().getMaxToolRounds() > 0 
        ? config.getLlm().getMaxToolRounds() : 5;
    boolean parallelTools = config.getLlm().isParallelTools();
    int toolParallelism = config.getLlm().getToolParallelism() > 0 
        ? config.getLlm().getToolParallelism() : 4;
    
    // 7. Create runtime
    AgentRuntime runtime = new DefaultAgentRuntime(
        llmProvider,
        tools,
        toolCallParser,
        sessions,
        memory,
        pendingActionStore,
        loadedSkills,
        guidelines,
        eventBus,
        maxToolRounds,
        parallelTools,
        toolParallelism
    );

    
    log.info("Agent instance created successfully: {}", agentId);
    
    return new AgentInstance(
        agentId,
        config,
        runtime,
        memory,
        sessions,
        skills,
        tools,
        loadedSkills
    );

  }
  
  /**
   * Create agent-specific tool registry.
   */
  private ToolRegistry createToolRegistry(AgentConfig config) {
    ToolRegistry registry = new ToolRegistry(systemTools.getApprovalPolicy());

    
    List<String> inherited = config.getCapabilities().getTools().getInherited();
    List<String> disabled = config.getCapabilities().getTools().getDisabled();
    
    // Add inherited system tools (excluding disabled ones)
    for (String toolName : inherited) {
      if (disabled.contains(toolName)) {
        log.debug("Tool {} is disabled for agent {}", toolName, config.getId());
        continue;
      }
      
      Tool tool = systemTools.getTool(toolName);
      if (tool != null) {
        registry.register(tool);
        log.debug("Agent {} inherited tool: {}", config.getId(), toolName);
      } else {
        log.warn("System tool not found: {}", toolName);
      }
    }
    
    // If no specific tools inherited, inherit all except disabled
    if (inherited.isEmpty()) {
      for (String toolName : systemTools.listToolNames()) {
        if (!disabled.contains(toolName)) {
          Tool tool = systemTools.getTool(toolName);
          if (tool != null) {
            registry.register(tool);
          }
        }
      }
    }
    
    // TODO: Add custom agent-specific tools
    
    log.info("Agent {} tool registry created with {} tools", 
        config.getId(), registry.listToolNames().size());
    
    return registry;
  }
  
  /**
   * Add agent-specific memory tools to registry.
   */
  private void addMemoryTools(ToolRegistry registry, MemoryStore memoryStore, MemoryService memoryService) {
    // Add memory search and get tools specific to this agent
    registry.register(new com.agentbot.core.tools.impl.MemorySearchTool(
        new com.agentbot.core.memory.MemorySearch(memoryStore)
    ));
    registry.register(new com.agentbot.core.tools.impl.MemoryGetTool(memoryStore));
  }
  
  /**
   * Ensure agent directory structure exists.
   */
  private void ensureDirectoryStructure(Path agentDir) {
    try {
      Files.createDirectories(agentDir);
      Files.createDirectories(agentDir.resolve("skills"));
      Files.createDirectories(agentDir.resolve("memory"));
      Files.createDirectories(agentDir.resolve("sessions"));
      log.debug("Ensured directory structure for: {}", agentDir);
    } catch (IOException e) {
      log.error("Failed to create directory structure: {}", agentDir, e);
      throw new RuntimeException("Failed to create agent directories", e);
    }
  }
}
