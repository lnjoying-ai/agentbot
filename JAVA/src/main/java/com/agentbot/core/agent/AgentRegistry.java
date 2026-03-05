package com.agentbot.core.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for managing multiple agents.
 */
public class AgentRegistry {
  private static final Logger log = LoggerFactory.getLogger(AgentRegistry.class);
  
  private final Map<String, AgentInstance> agents = new ConcurrentHashMap<>();
  private final Path workspacePath;
  private final AgentFactory factory;
  private final ObjectMapper objectMapper;
  
  public AgentRegistry(Path workspacePath, AgentFactory factory) {
    this.workspacePath = workspacePath;
    this.factory = factory;
    this.objectMapper = new ObjectMapper();
    this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
  }
  
  /**
   * Initialize registry by scanning workspace/agents/ directory.
   */
  public void initialize() {
    log.info("Initializing AgentRegistry from workspace: {}", workspacePath);
    
    // Ensure system and agents directories exist
    try {
      Files.createDirectories(workspacePath.resolve("system"));
      Files.createDirectories(workspacePath.resolve("agents"));
    } catch (IOException e) {
      log.error("Failed to create workspace directories", e);
      throw new RuntimeException("Failed to initialize AgentRegistry", e);
    }
    
    // 1. Load system default agent
    loadDefaultAgent();
    
    // 2. Scan and load all custom agents
    scanAndLoadAgents();
    
    log.info("AgentRegistry initialized with {} agents", agents.size());
  }
  
  /**
   * Load the default agent.
   */
  private void loadDefaultAgent() {
    Path defaultAgentDir = workspacePath.resolve("agents").resolve("default");
    Path configPath = defaultAgentDir.resolve("config.json");
    
    AgentConfig defaultConfig;
    
    if (Files.exists(configPath)) {
      // Load existing default agent config
      try {
        defaultConfig = objectMapper.readValue(configPath.toFile(), AgentConfig.class);
        log.info("Loaded existing default agent config");
      } catch (IOException e) {
        log.warn("Failed to load default agent config, creating new one", e);
        defaultConfig = createDefaultAgentConfig();
        saveConfig(configPath, defaultConfig);
      }
    } else {
      // Create new default agent config
      defaultConfig = createDefaultAgentConfig();
      saveConfig(configPath, defaultConfig);
      log.info("Created new default agent config");
    }
    
    AgentInstance defaultAgent = factory.createAgent(defaultConfig);
    agents.put("default", defaultAgent);
    log.info("Default agent loaded successfully");
  }
  
  /**
   * Create default agent configuration.
   */
  private AgentConfig createDefaultAgentConfig() {
    AgentConfig config = new AgentConfig("default", "Default Agent");
    config.setDisplayName("默认助手");
    config.setDescription("系统默认智能助手");
    config.setEnabled(true);
    
    // Configure to use system-level documents
    config.getPersonality().setUseSoul(true);
    config.getPersonality().setFallbackToSystem(true);
    
    config.getBehavior().setUseAgentMd(true);
    config.getBehavior().setFallbackToSystem(true);
    
    // Inherit all system tools
    config.getCapabilities().getSkills().setInherited(true);
    
    // Routing: accept all channels
    config.getRouting().setChannels(List.of("web", "telegram", "whatsapp", "cli"));
    config.getRouting().setAutoRoute(true);
    
    return config;
  }
  
  /**
   * Scan and load all agents from workspace/agents/ directory.
   */
  private void scanAndLoadAgents() {
    Path agentsDir = workspacePath.resolve("agents");
    
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(agentsDir)) {
      for (Path agentDir : stream) {
        if (!Files.isDirectory(agentDir)) {
          continue;
        }
        
        String agentId = agentDir.getFileName().toString();
        
        // Skip default agent (already loaded)
        if ("default".equals(agentId)) {
          continue;
        }
        
        Path configPath = agentDir.resolve("config.json");
        if (!Files.exists(configPath)) {
          log.warn("No config.json found for agent: {}, skipping", agentId);
          continue;
        }
        
        try {
          AgentConfig config = objectMapper.readValue(configPath.toFile(), AgentConfig.class);
          
          // Validate agent ID matches directory name
          if (!agentId.equals(config.getId())) {
            log.warn("Agent ID mismatch: directory={}, config={}, skipping", agentId, config.getId());
            continue;
          }
          
          AgentInstance instance = factory.createAgent(config);
          agents.put(agentId, instance);
          log.info("Loaded agent: {} ({})", agentId, config.getName());
          
        } catch (Exception e) {
          log.error("Failed to load agent: {}", agentId, e);
        }
      }
    } catch (IOException e) {
      log.error("Failed to scan agents directory", e);
    }
  }
  
  /**
   * Create a new agent.
   */
  public AgentInstance createAgent(AgentConfig config) {
    String agentId = config.getId();
    
    if (agents.containsKey(agentId)) {
      throw new IllegalArgumentException("Agent already exists: " + agentId);
    }
    
    if ("default".equals(agentId)) {
      throw new IllegalArgumentException("Cannot create agent with reserved ID: default");
    }
    
    // Validate agent ID (alphanumeric, hyphens, underscores only)
    if (!agentId.matches("^[a-z0-9_-]+$")) {
      throw new IllegalArgumentException("Invalid agent ID format: " + agentId);
    }
    
    Path agentDir = workspacePath.resolve("agents").resolve(agentId);
    
    if (Files.exists(agentDir)) {
      throw new IllegalArgumentException("Agent directory already exists: " + agentId);
    }
    
    // Set timestamps
    config.setCreatedAt(Instant.now().toString());
    config.setUpdatedAt(Instant.now().toString());
    
    // Create agent directory structure
    try {
      Files.createDirectories(agentDir);
      Files.createDirectories(agentDir.resolve("skills"));
      Files.createDirectories(agentDir.resolve("memory"));
      Files.createDirectories(agentDir.resolve("sessions"));

      Path defaultMemory = workspacePath.resolve("agents").resolve("default").resolve("memory").resolve("MEMORY.md");
      Path targetMemory = agentDir.resolve("memory").resolve("MEMORY.md");
      if (!Files.exists(targetMemory) && Files.exists(defaultMemory)) {
        Files.copy(defaultMemory, targetMemory);
        log.info("Copied default MEMORY.md to agent: {}", agentId);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to create agent directories", e);
    }
    
    // Write config.json

    Path configPath = agentDir.resolve("config.json");
    saveConfig(configPath, config);
    
    // Create agent instance
    AgentInstance instance = factory.createAgent(config);
    agents.put(agentId, instance);
    
    log.info("Created agent: {} ({})", agentId, config.getName());
    return instance;
  }
  
  /**
   * Get agent by ID.
   */
  public AgentInstance getAgent(String agentId) {
    return agents.get(agentId);
  }
  
  /**
   * Get agent config path by ID.
   */
  public Path getAgentConfigPath(String agentId) {
    return workspacePath.resolve("agents").resolve(agentId).resolve("config.json");
  }
  
  /**
   * Get all agents.
   */
  public Map<String, AgentInstance> getAllAgents() {
    return Collections.unmodifiableMap(agents);
  }

  
  /**
   * Update agent configuration.
   */
  public void updateAgent(String agentId, AgentConfig newConfig) {
    AgentInstance existing = agents.get(agentId);
    if (existing == null) {
      throw new IllegalArgumentException("Agent not found: " + agentId);
    }
    
    // Validate ID cannot be changed
    if (!agentId.equals(newConfig.getId())) {
      throw new IllegalArgumentException("Cannot change agent ID");
    }
    
    // Update timestamp
    newConfig.setUpdatedAt(Instant.now().toString());
    newConfig.setCreatedAt(existing.getConfig().getCreatedAt());
    
    // Update config file
    Path configPath = workspacePath.resolve("agents")
        .resolve(agentId).resolve("config.json");
    saveConfig(configPath, newConfig);
    
    // Reload agent
    AgentInstance updated = factory.createAgent(newConfig);
    agents.put(agentId, updated);
    
    // Shutdown old instance
    existing.shutdown();
    
    log.info("Updated agent: {}", agentId);
  }
  
  /**
   * Delete agent.
   */
  public void deleteAgent(String agentId) {
    if ("default".equals(agentId)) {
      throw new IllegalArgumentException("Cannot delete default agent");
    }
    
    AgentInstance agent = agents.remove(agentId);
    if (agent != null) {
      agent.shutdown();
      
      // Note: Not deleting directory to prevent data loss
      // Directory can be manually archived or deleted
      
      log.info("Deleted agent: {}", agentId);
    } else {
      throw new IllegalArgumentException("Agent not found: " + agentId);
    }
  }
  
  /**
   * List all agent IDs.
   */
  public List<String> listAgentIds() {
    return new ArrayList<>(agents.keySet());
  }
  
  /**
   * Check if agent exists.
   */
  public boolean hasAgent(String agentId) {
    return agents.containsKey(agentId);
  }
  
  /**
   * Get number of registered agents.
   */
  public int getAgentCount() {
    return agents.size();
  }
  
  /**
   * Save agent configuration to file.
   */
  private void saveConfig(Path configPath, AgentConfig config) {
    try {
      objectMapper.writeValue(configPath.toFile(), config);
      log.debug("Saved agent config: {}", configPath);
    } catch (IOException e) {
      log.error("Failed to save agent config: {}", configPath, e);
      throw new RuntimeException("Failed to save agent configuration", e);
    }
  }
}
