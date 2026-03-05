package com.agentbot.core.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-level guidelines service that loads configuration from both system-level and agent-level.
 * Priority: Agent-level > System-level
 */
public class MultiLevelGuidelinesService extends AgentGuidelinesService {
  private static final Logger log = LoggerFactory.getLogger(MultiLevelGuidelinesService.class);
  
  private final Path systemDir;
  private final Path agentDir;
  private final AgentConfig config;
  
  public MultiLevelGuidelinesService(Path systemDir, Path agentDir, AgentConfig config) {
    super(systemDir);
    this.systemDir = systemDir;
    this.agentDir = agentDir;
    this.config = config;
  }
  
  @Override
  public String buildSystemPrompt() {
    List<String> sections = new ArrayList<>();
    
    // 1. SOUL.md - Personality (Agent-level > System-level)
    String soul = loadSoul();
    if (soul != null && !soul.isBlank()) {
      sections.add(soul);
    }
    
    // 2. AGENT.md / AGENTS.md - Behavior rules (Agent-level > System-level)
    String agentMd = loadAgentBehavior();
    if (agentMd != null && !agentMd.isBlank()) {
      sections.add(agentMd);
    }
    
    // 3. TOOLS.md - System tools documentation (only system-level)
    String tools = loadDocument(systemDir, "TOOLS.md");
    if (tools != null && !tools.isBlank()) {
      sections.add(tools);
    }
    
    // 4. USER.md - User preferences (Agent-level > System-level)
    String user = loadUserPreferences();
    if (user != null && !user.isBlank()) {
      sections.add(user);
    }
    
    return String.join("\n\n---\n\n", sections);
  }
  
  /**
   * Load SOUL.md (personality)
   * Priority: Agent-level > System-level
   */
  private String loadSoul() {
    if (!config.getPersonality().isUseSoul()) {
      log.debug("SOUL.md loading disabled for agent: {}", config.getId());
      return null;
    }
    
    // Try agent-level SOUL.md first
    String agentSoul = loadDocument(agentDir, "SOUL.md");
    if (agentSoul != null && !agentSoul.isBlank()) {
      log.info("Loaded agent-level SOUL.md for agent: {}", config.getId());
      return agentSoul;
    }
    
    // Fallback to system-level
    if (config.getPersonality().isFallbackToSystem()) {
      String systemSoul = loadDocument(systemDir, "SOUL.md");
      if (systemSoul != null && !systemSoul.isBlank()) {
        log.info("Loaded system-level SOUL.md for agent: {}", config.getId());
        return systemSoul;
      }
    }
    
    return null;
  }
  
  /**
   * Load AGENT.md / AGENTS.md (behavior rules)
   * Priority: Agent-level AGENT.md > System-level AGENTS.md
   */
  private String loadAgentBehavior() {
    if (!config.getBehavior().isUseAgentMd()) {
      log.debug("AGENT.md loading disabled for agent: {}", config.getId());
      return null;
    }
    
    // Try agent-level AGENT.md first
    String agentMd = loadDocument(agentDir, "AGENT.md");
    if (agentMd != null && !agentMd.isBlank()) {
      log.info("Loaded agent-level AGENT.md for agent: {}", config.getId());
      return agentMd;
    }
    
    // Fallback to system-level AGENTS.md
    if (config.getBehavior().isFallbackToSystem()) {
      String systemAgents = loadDocument(systemDir, "AGENTS.md");
      if (systemAgents != null && !systemAgents.isBlank()) {
        log.info("Loaded system-level AGENTS.md for agent: {}", config.getId());
        return systemAgents;
      }
    }
    
    return null;
  }
  
  /**
   * Load USER.md (user preferences)
   * Priority: Agent-level > System-level
   */
  private String loadUserPreferences() {
    // Try agent-level USER.md first
    String agentUser = loadDocument(agentDir, "USER.md");
    if (agentUser != null && !agentUser.isBlank()) {
      log.info("Loaded agent-level USER.md for agent: {}", config.getId());
      return agentUser;
    }
    
    // Fallback to system-level
    String systemUser = loadDocument(systemDir, "USER.md");
    if (systemUser != null && !systemUser.isBlank()) {
      log.info("Loaded system-level USER.md for agent: {}", config.getId());
      return systemUser;
    }
    
    return null;
  }
  
  /**
   * Load a document from specified directory.
   */
  private String loadDocument(Path dir, String filename) {
    if (dir == null || filename == null) {
      return null;
    }
    
    Path filePath = dir.resolve(filename);
    if (!Files.exists(filePath)) {
      return null;
    }
    
    try {
      String content = Files.readString(filePath);
      log.debug("Loaded document: {} ({} chars)", filePath, content.length());
      return content;
    } catch (IOException e) {
      log.warn("Failed to read document: {}", filePath, e);
      return null;
    }
  }
}
