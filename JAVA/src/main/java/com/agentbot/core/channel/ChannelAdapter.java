package com.agentbot.core.channel;

import com.agentbot.core.bus.events.OutboundMessage;

public interface ChannelAdapter {
  String name();

  void start();

  void stop();

  void send(OutboundMessage message);
}
