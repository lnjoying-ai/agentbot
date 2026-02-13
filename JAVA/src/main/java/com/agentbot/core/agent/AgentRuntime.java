package com.agentbot.core.agent;

import com.agentbot.core.bus.events.InboundMessage;
import com.agentbot.core.bus.events.OutboundMessage;

public interface AgentRuntime {
  OutboundMessage handle(InboundMessage message);
}
