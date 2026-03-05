package com.agentbot.core.bus;

import com.agentbot.core.bus.events.InboundMessage;
import com.agentbot.core.bus.events.OutboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ExternalMessageBus implements UnifiedMessageBus {
  private static final Logger log = LoggerFactory.getLogger(ExternalMessageBus.class);
  private final SimpleMessageBus delegate;

  private final Map<String, List<Consumer<MessageEnvelope>>> handlers = new ConcurrentHashMap<>();
  private final ExecutorService inboundDispatcher = Executors.newSingleThreadExecutor();
  private volatile boolean running = false;
  private volatile boolean inboundDispatching = false;


  public ExternalMessageBus(SimpleMessageBus delegate) {
    this.delegate = delegate;
  }

  @Override
  public void publish(MessageEnvelope envelope) {
    if (envelope == null) return;
    if (!isExternal(envelope)) return;

    if (isInbound(envelope)) {
      log.debug("Publish inbound: channel={}, chatId={}", envelope.getChannel(), envelope.getChatId());
      delegate.publishInbound(toInbound(envelope));
      return;
    }

    log.debug("Publish outbound: channel={}, chatId={}", envelope.getChannel(), envelope.getChatId());
    delegate.publishOutbound(toOutbound(envelope));
    dispatch(MessageEnvelope.TOPIC_EXTERNAL_OUTBOUND, envelope.withTopic(MessageEnvelope.TOPIC_EXTERNAL_OUTBOUND));
  }


  @Override
  public void subscribe(String topic, Consumer<MessageEnvelope> handler) {
    if (topic == null || handler == null) return;
    handlers.computeIfAbsent(topic, key -> new CopyOnWriteArrayList<>()).add(handler);
    log.debug("Subscribe external topic: {}", topic);


    if (topic.startsWith(MessageEnvelope.TOPIC_EXTERNAL_OUTBOUND + ".")) {
      String channel = topic.substring((MessageEnvelope.TOPIC_EXTERNAL_OUTBOUND + ".").length());
      if (!channel.isBlank()) {
        delegate.subscribeOutbound(channel, msg -> dispatch(topic, fromOutbound(msg).withTopic(topic)));
      }
    }

    if (topic.equals(MessageEnvelope.TOPIC_EXTERNAL_INBOUND)
        || topic.startsWith(MessageEnvelope.TOPIC_EXTERNAL_INBOUND + ".")) {
      ensureInboundDispatcher();
    }
  }

  @Override
  public void start() {
    delegate.start();
    if (running) return;
    running = true;
    log.info("ExternalMessageBus started");
    if (hasInboundSubscribers()) {
      ensureInboundDispatcher();
    }
  }


  @Override
  public void stop() {
    running = false;
    delegate.stop();
    inboundDispatcher.shutdownNow();
    log.info("ExternalMessageBus stopped");
  }


  private void ensureInboundDispatcher() {
    if (!running || inboundDispatching) return;
    inboundDispatching = true;
    log.info("Starting external inbound dispatcher");
    inboundDispatcher.execute(() -> {
      while (running) {
        try {
          InboundMessage inbound = delegate.inboundQueue().take();
          MessageEnvelope envelope = fromInbound(inbound);
          dispatch(MessageEnvelope.TOPIC_EXTERNAL_INBOUND, envelope.withTopic(MessageEnvelope.TOPIC_EXTERNAL_INBOUND));
          String channelTopic = MessageEnvelope.topicExternalInbound(inbound.getChannel());
          dispatch(channelTopic, envelope.withTopic(channelTopic));
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    });
  }


  private boolean hasInboundSubscribers() {
    if (handlers.containsKey(MessageEnvelope.TOPIC_EXTERNAL_INBOUND)) return true;
    for (String topic : handlers.keySet()) {
      if (topic.startsWith(MessageEnvelope.TOPIC_EXTERNAL_INBOUND + ".")) {
        return true;
      }
    }
    return false;
  }

  private void dispatch(String topic, MessageEnvelope envelope) {

    if (topic == null || envelope == null) return;
    List<Consumer<MessageEnvelope>> topicHandlers = handlers.get(topic);
    if (topicHandlers == null || topicHandlers.isEmpty()) return;
    for (Consumer<MessageEnvelope> handler : topicHandlers) {
      try {
        handler.accept(envelope);
      } catch (Exception error) {
        log.warn("ExternalMessageBus handler failed: topic={}", topic, error);
      }
    }

  }

  private boolean isExternal(MessageEnvelope envelope) {
    if (envelope.getChannelType() == MessageEnvelope.ChannelType.EXTERNAL) return true;
    String topic = envelope.getTopic() == null ? "" : envelope.getTopic();
    return topic.startsWith(MessageEnvelope.TOPIC_EXTERNAL_INBOUND)
        || topic.startsWith(MessageEnvelope.TOPIC_EXTERNAL_OUTBOUND);
  }

  private boolean isInbound(MessageEnvelope envelope) {
    String topic = envelope.getTopic() == null ? "" : envelope.getTopic();
    if (topic.startsWith(MessageEnvelope.TOPIC_EXTERNAL_INBOUND)) return true;
    if (topic.startsWith(MessageEnvelope.TOPIC_EXTERNAL_OUTBOUND)) return false;
    return envelope.getSenderId() != null && !envelope.getSenderId().isBlank();
  }

  private InboundMessage toInbound(MessageEnvelope envelope) {
    InboundMessage inbound = new InboundMessage(
        envelope.getChannel(),
        envelope.getSenderId(),
        envelope.getChatId(),
        envelope.getContent()
    );
    if (envelope.getMetadata() != null) {
      inbound.getMetadata().putAll(envelope.getMetadata());
    }
    applyRouteMetadata(envelope, inbound.getMetadata());
    return inbound;
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

  private MessageEnvelope fromInbound(InboundMessage inbound) {
    return MessageEnvelope.builder()
        .topic(MessageEnvelope.TOPIC_EXTERNAL_INBOUND)
        .channelType(MessageEnvelope.ChannelType.EXTERNAL)
        .channel(inbound.getChannel())
        .senderId(inbound.getSenderId())
        .chatId(inbound.getChatId())
        .source(inbound.getSenderId())
        .target(inbound.getChatId())
        .content(inbound.getContent())
        .metadata(inbound.getMetadata())
        .agentId(extractAgentId(inbound.getMetadata()))
        .accountId(inbound.getAccountId())
        .peerKind(inbound.getPeerKind())
        .peerId(inbound.getPeerId())
        .guildId(inbound.getGuildId())
        .teamId(inbound.getTeamId())
        .timestamp(inbound.getTimestamp())
        .build();
  }


  private MessageEnvelope fromOutbound(OutboundMessage outbound) {
    return MessageEnvelope.builder()
        .topic(MessageEnvelope.TOPIC_EXTERNAL_OUTBOUND)
        .channelType(MessageEnvelope.ChannelType.EXTERNAL)
        .channel(outbound.getChannel())
        .chatId(outbound.getChatId())
        .target(outbound.getChatId())
        .content(outbound.getContent())
        .metadata(outbound.getMetadata())
        .agentId(extractAgentId(outbound.getMetadata()))
        .timestamp(outbound.getTimestamp())
        .build();
  }

  private void applyRouteMetadata(MessageEnvelope envelope, Map<String, Object> metadata) {
    if (envelope == null || metadata == null) return;
    putIfAbsent(metadata, MessageEnvelope.META_ACCOUNT_ID, envelope.getAccountId());
    putIfAbsent(metadata, MessageEnvelope.META_PEER_KIND, envelope.getPeerKind());
    putIfAbsent(metadata, MessageEnvelope.META_PEER_ID, envelope.getPeerId());
    putIfAbsent(metadata, MessageEnvelope.META_GUILD_ID, envelope.getGuildId());
    putIfAbsent(metadata, MessageEnvelope.META_TEAM_ID, envelope.getTeamId());
  }

  private void putIfAbsent(Map<String, Object> metadata, String key, String value) {
    if (metadata == null || key == null || value == null || value.isBlank()) return;
    metadata.putIfAbsent(key, value);
  }

  private String extractAgentId(Map<String, Object> metadata) {
    if (metadata == null) return null;
    Object agentId = metadata.get("agentId");
    return agentId == null ? null : String.valueOf(agentId);
  }
}
