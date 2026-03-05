package com.agentbot.core.agent;

import com.agentbot.core.bus.events.InboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Routes messages to appropriate agents based on multiple strategies.
 */
public class MultiAgentRouter implements AgentRouter {
  private static final Logger log = LoggerFactory.getLogger(MultiAgentRouter.class);
  
  private final AgentRegistry registry;
  private final List<AgentRoutingStrategy> strategies;
  private final String defaultAgentId;
  
  public MultiAgentRouter(AgentRegistry registry, List<AgentRoutingStrategy> strategies) {
    this(registry, strategies, "default");
  }

  public MultiAgentRouter(AgentRegistry registry, List<AgentRoutingStrategy> strategies, String defaultAgentId) {
    this.registry = registry;
    this.strategies = strategies;
    this.defaultAgentId = defaultAgentId == null || defaultAgentId.isBlank() ? "default" : defaultAgentId;
  }

  
  @Override
  public String resolveAgentId(InboundMessage message) {
    // 1. Check if message explicitly specifies agent
    String explicitAgentId = (String) message.getMetadata().get("agentId");
    if (explicitAgentId != null && !explicitAgentId.isBlank()) {
      if (registry.hasAgent(explicitAgentId)) {
        log.debug("Routing to explicitly specified agent: {}", explicitAgentId);
        return explicitAgentId;
      } else {
        log.warn("Explicit agent {} not found, falling back to routing strategies", explicitAgentId);
      }
    }
    
    // 2. Try each routing strategy in order
    for (AgentRoutingStrategy strategy : strategies) {
      String agentId = strategy.route(message, registry);
      if (agentId != null && !agentId.isBlank() && registry.hasAgent(agentId)) {
        log.debug("Routing via {}: {}", strategy.getClass().getSimpleName(), agentId);
        return agentId;
      }
    }
    
    // 3. Fallback to default agent
    String fallback = defaultAgentId;
    if (!registry.hasAgent(fallback)) {
      fallback = "default";
    }
    log.debug("Routing to default agent (no strategy matched): {}", fallback);
    return fallback;

  }
}
