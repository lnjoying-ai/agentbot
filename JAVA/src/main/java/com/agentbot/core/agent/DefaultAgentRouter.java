package com.agentbot.core.agent;

import com.agentbot.core.bus.events.InboundMessage;

public class DefaultAgentRouter implements AgentRouter {
  @Override
  public String resolveAgentId(InboundMessage message) {
    return "default";
  }
}
