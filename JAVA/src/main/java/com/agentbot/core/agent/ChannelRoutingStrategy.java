package com.agentbot.core.agent;

import com.agentbot.core.bus.events.InboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Channel-based routing strategy.
 * Routes messages based on channel to agent mapping.
 */
public class ChannelRoutingStrategy implements AgentRoutingStrategy {
  private static final Logger log = LoggerFactory.getLogger(ChannelRoutingStrategy.class);
  
  private final Map<String, String> channelAgentMap = new ConcurrentHashMap<>();
  
  /**
   * Bind a channel to a specific agent.
   */
  public void bindChannel(String channel, String agentId) {
    channelAgentMap.put(channel, agentId);
    log.info("Bound channel {} to agent {}", channel, agentId);
  }
  
  /**
   * Unbind a channel.
   */
  public void unbindChannel(String channel) {
    String removed = channelAgentMap.remove(channel);
    if (removed != null) {
      log.info("Unbound channel {} from agent {}", channel, removed);
    }
  }
  
  @Override
  public String route(InboundMessage message, AgentRegistry registry) {
    String channel = message.getChannel();
    if (channel == null || channel.isBlank()) {
      return null;
    }
    
    String agentId = channelAgentMap.get(channel);
    if (agentId != null && registry.hasAgent(agentId)) {
      log.debug("Channel routing: {} -> {}", channel, agentId);
      return agentId;
    }
    
    return null;
  }
}
