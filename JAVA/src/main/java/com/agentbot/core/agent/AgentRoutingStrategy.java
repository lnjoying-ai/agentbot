package com.agentbot.core.agent;

import com.agentbot.core.bus.events.InboundMessage;

/**
 * Strategy interface for routing messages to agents.
 */
public interface AgentRoutingStrategy {
  /**
   * Determine which agent should handle the message.
   * @param message The incoming message
   * @param registry The agent registry
   * @return Agent ID, or null if this strategy cannot determine the agent
   */
  String route(InboundMessage message, AgentRegistry registry);
}
