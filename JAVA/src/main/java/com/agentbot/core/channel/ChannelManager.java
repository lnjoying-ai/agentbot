package com.agentbot.core.channel;

import com.agentbot.core.bus.MessageBus;
import com.agentbot.core.bus.events.OutboundMessage;

import java.util.Map;

public class ChannelManager {
  private final ChannelRegistry registry;
  private final MessageBus messageBus;

  public ChannelManager(ChannelRegistry registry, MessageBus messageBus) {
    this.registry = registry;
    this.messageBus = messageBus;
  }

  public void startAll() {
    for (ChannelAdapter adapter : registry.all().values()) {
      adapter.start();
      messageBus.subscribeOutbound(adapter.name(), adapter::send);
    }
  }

  public void stopAll() {
    for (ChannelAdapter adapter : registry.all().values()) {
      adapter.stop();
    }
  }

  public Map<String, ChannelAdapter> status() {
    return registry.all();
  }

  public void dispatch(OutboundMessage message) {
    messageBus.publishOutbound(message);
  }
}
