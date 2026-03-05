package com.agentbot.core.agent;

import com.agentbot.core.bus.events.InboundMessage;
import com.agentbot.core.bus.events.OutboundMessage;
import com.agentbot.core.memory.MemoryService;
import com.agentbot.core.session.SessionService;
import com.agentbot.core.skills.SkillLoader;
import com.agentbot.core.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a single agent instance with its own runtime, memory, and configuration.
 */
public class AgentInstance {
  private static final Logger log = LoggerFactory.getLogger(AgentInstance.class);
  
  private final String id;
  private final AgentConfig config;
  private final AgentRuntime runtime;
  private final MemoryService memory;
  private final SessionService sessions;
  private final SkillLoader skills;
  private final ToolRegistry tools;
  private final java.util.List<com.agentbot.core.skills.Skill> loadedSkills;
  
  public AgentInstance(
      String id,
      AgentConfig config,
      AgentRuntime runtime,
      MemoryService memory,
      SessionService sessions,
      SkillLoader skills,
      ToolRegistry tools,
      java.util.List<com.agentbot.core.skills.Skill> loadedSkills
  ) {
    this.id = id;
    this.config = config;
    this.runtime = runtime;
    this.memory = memory;
    this.sessions = sessions;
    this.skills = skills;
    this.tools = tools;
    this.loadedSkills = loadedSkills;
  }

  
  public String getId() {
    return id;
  }
  
  public AgentConfig getConfig() {
    return config;
  }
  
  public AgentRuntime getRuntime() {
    return runtime;
  }
  
  public MemoryService getMemory() {
    return memory;
  }
  
  public SessionService getSessions() {
    return sessions;
  }
  
  public SkillLoader getSkills() {
    return skills;
  }
  
  public ToolRegistry getTools() {
    return tools;
  }
  
  public java.util.List<com.agentbot.core.skills.Skill> getLoadedSkills() {
    return loadedSkills;
  }

  
  /**
   * Handle an incoming message.
   */
  public OutboundMessage handle(InboundMessage message) {
    if (!config.isEnabled()) {
      log.warn("Agent {} is disabled, ignoring message", id);
      return new OutboundMessage(
          message.getChannel(),
          message.getChatId(),
          "This agent is currently disabled."
      );
    }
    return runtime.handle(message);
  }
  
  /**
   * Check if agent is healthy and operational.
   */
  public boolean isHealthy() {
    return config.isEnabled() && runtime != null;
  }
  
  /**
   * Shutdown agent gracefully.
   */
  public void shutdown() {
    log.info("Shutting down agent: {}", id);
    // Cleanup resources if needed
  }
}
