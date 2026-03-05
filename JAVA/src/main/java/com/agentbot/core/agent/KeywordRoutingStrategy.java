package com.agentbot.core.agent;

import com.agentbot.core.bus.events.InboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Keyword-based routing strategy.
 * Routes messages based on keywords defined in agent configurations.
 */
public class KeywordRoutingStrategy implements AgentRoutingStrategy {
  private static final Logger log = LoggerFactory.getLogger(KeywordRoutingStrategy.class);
  
  @Override
  public String route(InboundMessage message, AgentRegistry registry) {
    String content = message.getContent();
    if (content == null || content.isBlank()) {
      return null;
    }
    
    String contentLower = content.toLowerCase();
    
    // Collect all agents with matching keywords and their priority
    List<AgentMatch> matches = registry.getAllAgents().values().stream()
        .filter(agent -> agent.getConfig().isEnabled())
        .filter(agent -> agent.getConfig().getRouting().isAutoRoute())
        .flatMap(agent -> {
          List<String> keywords = agent.getConfig().getRouting().getKeywords();
          long matchCount = keywords.stream()
              .filter(keyword -> contentLower.contains(keyword.toLowerCase()))
              .count();
          
          if (matchCount > 0) {
            int priority = agent.getConfig().getRouting().getPriority();
            return java.util.stream.Stream.of(new AgentMatch(agent.getId(), priority, matchCount));
          }
          return java.util.stream.Stream.empty();
        })
        .collect(Collectors.toList());
    
    if (matches.isEmpty()) {
      return null;
    }
    
    // Sort by: 1. match count (descending), 2. priority (descending)
    matches.sort(Comparator
        .comparingLong(AgentMatch::getMatchCount).reversed()
        .thenComparingInt(AgentMatch::getPriority).reversed()
    );
    
    AgentMatch bestMatch = matches.get(0);
    log.debug("Keyword routing matched agent: {} (matches={}, priority={})", 
        bestMatch.getAgentId(), bestMatch.getMatchCount(), bestMatch.getPriority());
    
    return bestMatch.getAgentId();
  }
  
  /**
   * Helper class for tracking agent matches.
   */
  private static class AgentMatch {
    private final String agentId;
    private final int priority;
    private final long matchCount;
    
    public AgentMatch(String agentId, int priority, long matchCount) {
      this.agentId = agentId;
      this.priority = priority;
      this.matchCount = matchCount;
    }
    
    public String getAgentId() {
      return agentId;
    }
    
    public int getPriority() {
      return priority;
    }
    
    public long getMatchCount() {
      return matchCount;
    }
  }
}
