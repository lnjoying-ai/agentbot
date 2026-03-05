package com.agentbot.core.bus;

import com.agentbot.core.agent.AgentMessage;
import com.agentbot.core.agent.AgentMessageBus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class InternalMessageBus implements UnifiedMessageBus {
  private final AgentMessageBus delegate;
  private final Map<String, List<Consumer<MessageEnvelope>>> handlers = new ConcurrentHashMap<>();

  public InternalMessageBus(AgentMessageBus delegate) {
    this.delegate = delegate;
  }

  @Override
  public void publish(MessageEnvelope envelope) {
    if (envelope == null) return;
    if (!isInternal(envelope)) return;

    if (isBroadcast(envelope)) {
      delegate.broadcast(toAgentMessage(envelope, true));
      return;
    }

    delegate.sendMessage(toAgentMessage(envelope, false));
  }

  @Override
  public void subscribe(String topic, Consumer<MessageEnvelope> handler) {
    if (topic == null || handler == null) return;
    handlers.computeIfAbsent(topic, key -> new CopyOnWriteArrayList<>()).add(handler);

    if (topic.startsWith(MessageEnvelope.TOPIC_INTERNAL_DIRECT_PREFIX)) {
      String agentId = topic.substring(MessageEnvelope.TOPIC_INTERNAL_DIRECT_PREFIX.length());
      if (!agentId.isBlank()) {
        delegate.registerHandler(agentId, msg -> dispatch(topic, fromAgentMessage(msg).withTopic(topic)));
      }
    }
  }

  @Override
  public void start() {
    delegate.start();
  }

  @Override
  public void stop() {
    delegate.stop();
  }

  private void dispatch(String topic, MessageEnvelope envelope) {
    List<Consumer<MessageEnvelope>> topicHandlers = handlers.get(topic);
    if (topicHandlers == null || topicHandlers.isEmpty()) return;
    for (Consumer<MessageEnvelope> handler : topicHandlers) {
      try {
        handler.accept(envelope);
      } catch (Exception ignored) {
        // ignore handler failures
      }
    }
  }

  private boolean isInternal(MessageEnvelope envelope) {
    if (envelope.getChannelType() == MessageEnvelope.ChannelType.INTERNAL) return true;
    String topic = envelope.getTopic() == null ? "" : envelope.getTopic();
    return topic.startsWith(MessageEnvelope.TOPIC_INTERNAL_DIRECT_PREFIX)
        || topic.startsWith(MessageEnvelope.TOPIC_INTERNAL_BROADCAST);
  }

  private boolean isBroadcast(MessageEnvelope envelope) {
    String topic = envelope.getTopic() == null ? "" : envelope.getTopic();
    if (topic.startsWith(MessageEnvelope.TOPIC_INTERNAL_BROADCAST)) return true;
    return envelope.getTarget() == null || envelope.getTarget().isBlank();
  }

  private AgentMessage toAgentMessage(MessageEnvelope envelope, boolean broadcast) {
    AgentMessage.Builder builder = AgentMessage.builder()
        .from(resolveSource(envelope))
        .content(envelope.getContent())
        .metadata(envelope.getMetadata());

    if (broadcast) {
      builder.type(AgentMessage.MessageType.BROADCAST);
    } else {
      builder.to(envelope.getTarget());
      builder.type(AgentMessage.MessageType.REQUEST);
    }

    return builder.build();
  }

  private MessageEnvelope fromAgentMessage(AgentMessage message) {
    String topic = message.isBroadcast()
        ? MessageEnvelope.TOPIC_INTERNAL_BROADCAST
        : MessageEnvelope.topicInternalAgent(message.getToAgentId());
    return MessageEnvelope.builder()
        .topic(topic)
        .channelType(MessageEnvelope.ChannelType.INTERNAL)
        .source(message.getFromAgentId())
        .target(message.getToAgentId())
        .agentId(message.getToAgentId())
        .content(message.getContent())
        .metadata(message.getMetadata())
        .timestamp(message.getTimestamp())
        .build();
  }

  private String resolveSource(MessageEnvelope envelope) {
    if (envelope.getSource() != null && !envelope.getSource().isBlank()) {
      return envelope.getSource();
    }
    if (envelope.getAgentId() != null && !envelope.getAgentId().isBlank()) {
      return envelope.getAgentId();
    }
    return "system";
  }
}
