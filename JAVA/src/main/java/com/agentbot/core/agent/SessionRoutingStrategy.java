package com.agentbot.core.agent;

import com.agentbot.core.bus.events.InboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session-based routing strategy (sticky sessions).
 * Once a session is assigned to an agent, all subsequent messages from that session go to the same agent.
 */
public class SessionRoutingStrategy implements AgentRoutingStrategy {
  private static final Logger log = LoggerFactory.getLogger(SessionRoutingStrategy.class);
  
  private final Map<String, String> sessionAgentMap = new ConcurrentHashMap<>();
  
  /**
   * Bind a session to a specific agent.
   */
  public void bindSession(String sessionKey, String agentId) {
    sessionAgentMap.put(sessionKey, agentId);
    log.debug("Bound session {} to agent {}", sessionKey, agentId);
  }
  
  /**
   * Unbind a session.
   */
  public void unbindSession(String sessionKey) {
    String removed = sessionAgentMap.remove(sessionKey);
    if (removed != null) {
      log.debug("Unbound session {} from agent {}", sessionKey, removed);
    }
  }
  
  /**
   * Get the agent for a session.
   */
  public String getAgentForSession(String sessionKey) {
    return sessionAgentMap.get(sessionKey);
  }
  
  @Override
  public String route(InboundMessage message, AgentRegistry registry) {
    String sessionKey = message.sessionKey();
    if (sessionKey == null || sessionKey.isBlank()) {
      return null;
    }
    
    String agentId = sessionAgentMap.get(sessionKey);
    if (agentId != null && registry.hasAgent(agentId)) {
      log.debug("Session routing: {} -> {}", sessionKey, agentId);
      return agentId;
    }
    
    return null;
  }
}
