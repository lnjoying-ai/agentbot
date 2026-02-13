package com.agentbot.core.bus;

import com.agentbot.core.bus.events.InboundMessage;
import com.agentbot.core.bus.events.OutboundMessage;

import java.util.function.Consumer;

public interface MessageBus {
  void publishInbound(InboundMessage message);

  void publishOutbound(OutboundMessage message);

  void subscribeOutbound(String channel, Consumer<OutboundMessage> handler);

  void start();

  void stop();
}
