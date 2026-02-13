package com.agentbot.core.automation;

import com.agentbot.core.agent.AgentRuntime;
import com.agentbot.core.bus.MessageBus;
import com.agentbot.core.bus.events.InboundMessage;
import com.agentbot.core.bus.events.OutboundMessage;

public class AutomationService {
  private final AgentRuntime runtime;
  private final MessageBus messageBus;

  public AutomationService(AgentRuntime runtime, MessageBus messageBus) {
    this.runtime = runtime;
    this.messageBus = messageBus;
  }

  public void triggerHeartbeat(String content) {
    trigger("heartbeat", "system", "heartbeat", content);
  }

  public void triggerCron(String sessionKey, String content) {
    String chatId = sessionKey == null || sessionKey.isBlank() ? "cron" : sessionKey;
    trigger("cron", "system", chatId, content);
  }

  private void trigger(String channel, String senderId, String chatId, String content) {
    InboundMessage inbound = new InboundMessage(channel, senderId, chatId, content);
    OutboundMessage outbound = runtime.handle(inbound);
    if (outbound != null) {
      messageBus.publishOutbound(outbound);
    }
  }
}
