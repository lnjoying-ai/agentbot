package com.agentbot.core.agent;

import com.agentbot.core.bus.events.InboundMessage;

public interface AgentRouter {
  String resolveAgentId(InboundMessage message);
}
