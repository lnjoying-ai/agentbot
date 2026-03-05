package com.agentbot.core.channel;

import com.agentbot.core.bus.ExternalMessageBus;
import com.agentbot.core.bus.MessageEnvelope;
import com.agentbot.core.bus.events.OutboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ChannelManager {
  private static final Logger log = LoggerFactory.getLogger(ChannelManager.class);
  private final ChannelRegistry registry;
  private final ExternalMessageBus messageBus;


  public ChannelManager(ChannelRegistry registry, ExternalMessageBus messageBus) {
    this.registry = registry;
    this.messageBus = messageBus;
  }

  public void startAll() {
    log.info("Starting all channels: total={}", registry.all().size());
    for (ChannelAdapter adapter : registry.all().values()) {
      log.info("Starting channel adapter: {}", adapter.name());
      adapter.start();
      String topic = MessageEnvelope.topicExternalOutbound(adapter.name());
      messageBus.subscribe(topic, envelope -> {
        adapter.send(toOutbound(envelope));
      });
      log.info("Subscribed outbound topic: {}", topic);
    }
  }

  public void stopAll() {
    log.info("Stopping all channels: total={}", registry.all().size());
    for (ChannelAdapter adapter : registry.all().values()) {
      log.info("Stopping channel adapter: {}", adapter.name());
      adapter.stop();
    }
  }


  public Map<String, ChannelAdapter> status() {
    return registry.all();
  }

  public void dispatch(OutboundMessage message) {
    if (message == null) {
      log.warn("Outbound message is null, skip dispatch");
      return;
    }
    log.info("Dispatch outbound: channel={} chatId={} length={}", message.getChannel(), message.getChatId(),
        message.getContent() == null ? 0 : message.getContent().length());
    messageBus.publish(MessageEnvelope.externalOutbound(
        message.getChannel(),
        message.getChatId(),
        message.getContent(),
        message.getMetadata()
    ));
  }


  private OutboundMessage toOutbound(MessageEnvelope envelope) {
    OutboundMessage outbound = new OutboundMessage(
        envelope.getChannel(),
        envelope.getChatId(),
        envelope.getContent()
    );
    if (envelope.getMetadata() != null) {
      outbound.getMetadata().putAll(envelope.getMetadata());
    }
    return outbound;
  }
}
