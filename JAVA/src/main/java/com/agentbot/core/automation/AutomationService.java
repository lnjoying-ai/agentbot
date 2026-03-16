package com.agentbot.core.automation;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.agent.AgentRuntime;
import com.agentbot.core.bus.ExternalMessageBus;
import com.agentbot.core.bus.MessageEnvelope;
import com.agentbot.core.bus.events.InboundMessage;
import com.agentbot.core.bus.events.OutboundMessage;
import com.agentbot.core.session.ChatUnreadService;
import com.agentbot.core.session.SessionService;

public class AutomationService {
  private final AgentRuntime runtime;
  private final ExternalMessageBus messageBus;
  private final AgentbotProperties properties;
  private final SessionService sessionService;
  private final ChatUnreadService unreadService;

  public AutomationService(
      AgentRuntime runtime,
      ExternalMessageBus messageBus,
      AgentbotProperties properties,
      SessionService sessionService,
      ChatUnreadService unreadService
  ) {
    this.runtime = runtime;
    this.messageBus = messageBus;
    this.properties = properties;
    this.sessionService = sessionService;
    this.unreadService = unreadService;
  }



  public void triggerHeartbeat(String content) {
    trigger("heartbeat", "system", "heartbeat", content, null);
  }

  public void triggerCron(String sessionKey, String content) {
    String chatId = sessionKey == null || sessionKey.isBlank() ? "cron" : sessionKey;
    trigger("cron", "system", chatId, content, null);
  }

  public void triggerCronWithDelivery(String sessionKey, String content, DeliveryOptions delivery) {
    String chatId = sessionKey == null || sessionKey.isBlank() ? "cron" : sessionKey;
    trigger("cron", "system", chatId, content, delivery);
  }

  private void trigger(String channel, String senderId, String chatId, String content, DeliveryOptions delivery) {
    InboundMessage inbound = new InboundMessage(channel, senderId, chatId, content);
    OutboundMessage outbound = runtime.handle(inbound);
    if (outbound != null) {
      messageBus.publish(MessageEnvelope.externalOutbound(
          outbound.getChannel(),
          outbound.getChatId(),
          outbound.getContent(),
          outbound.getMetadata()
      ));
      deliverToChannelIfNeeded(outbound, delivery);
      recordWebMessageIfNeeded(outbound, delivery);

    }

  }


  private void deliverToChannelIfNeeded(OutboundMessage outbound, DeliveryOptions delivery) {
    if (outbound == null || delivery == null || !delivery.enabled) return;
    String channel = delivery.channel == null ? "" : delivery.channel.trim();
    if (channel.isBlank()) {
      channel = "web";
    }
    if ("web".equalsIgnoreCase(channel)) {
      deliverToWebIfNeeded(outbound, delivery);
      return;
    }
    String targetChatId = delivery.to == null || delivery.to.isBlank() ? outbound.getChatId() : delivery.to.trim();
    if (targetChatId == null || targetChatId.isBlank()) return;
    messageBus.publish(MessageEnvelope.externalOutbound(
        channel,
        targetChatId,
        outbound.getContent(),
        outbound.getMetadata()
    ));
  }

  private void deliverToWebIfNeeded(OutboundMessage outbound, DeliveryOptions delivery) {
    if (outbound == null || delivery == null || !delivery.enabled) return;
    String channel = delivery.channel == null ? "" : delivery.channel.trim();
    if (channel.isBlank()) {
      channel = "web";
    }
    if (!"web".equalsIgnoreCase(channel)) return;
    String targetAgentId = delivery.to == null || delivery.to.isBlank() ? "default" : delivery.to.trim();
    java.util.Map<String, Object> metadata = new java.util.HashMap<>();
    if (outbound.getMetadata() != null) {
      metadata.putAll(outbound.getMetadata());
    }
    metadata.put("agentId", targetAgentId);
    messageBus.publish(MessageEnvelope.externalOutbound(
        "web",
        targetAgentId,
        outbound.getContent(),
        metadata
    ));
  }


  private void recordWebMessageIfNeeded(OutboundMessage outbound, DeliveryOptions delivery) {
    if (outbound == null || !shouldRecordWeb(delivery)) return;
    if (sessionService == null) return;
    String chatId = resolveWebChatId(outbound, delivery);
    if (chatId == null || chatId.isBlank()) return;
    if (outbound.getContent() == null || outbound.getContent().isBlank()) return;
    String agentId = resolveWebAgentId(outbound, delivery);
    String sessionKey = buildWebSessionKey(agentId, chatId);
    sessionService.appendAssistantMessage(sessionKey, outbound.getContent());
    if (unreadService != null) {
      unreadService.increment("web", chatId);
    }
  }

  private String resolveWebAgentId(OutboundMessage outbound, DeliveryOptions delivery) {
    if (delivery != null && delivery.to != null && !delivery.to.isBlank()) {
      return delivery.to.trim();
    }
    if (outbound != null && outbound.getMetadata() != null) {
      Object agentId = outbound.getMetadata().get("agentId");
      if (agentId != null && !String.valueOf(agentId).isBlank()) {
        return String.valueOf(agentId).trim();
      }
    }
    return "default";
  }

  private String buildWebSessionKey(String agentId, String chatId) {
    String safeAgent = agentId == null || agentId.isBlank() ? "default" : agentId.trim().toLowerCase();
    String safeChatId = chatId == null || chatId.isBlank() ? "unknown" : chatId.trim().toLowerCase();
    return "agent:" + safeAgent + ":web:default:dm:" + safeChatId;
  }


  private boolean shouldRecordWeb(DeliveryOptions delivery) {
    if (isSseMode()) return false;
    if (delivery == null || !delivery.enabled) return false;
    String channel = delivery.channel == null ? "" : delivery.channel.trim();
    if (channel.isBlank()) channel = "web";
    return "web".equalsIgnoreCase(channel);
  }

  private String resolveWebChatId(OutboundMessage outbound, DeliveryOptions delivery) {
    if (delivery != null && delivery.to != null && !delivery.to.isBlank()) {
      return delivery.to.trim();
    }
    return outbound == null ? null : outbound.getChatId();
  }

  private boolean isSseMode() {
    String mode = properties == null || properties.getOps() == null ? null : properties.getOps().getChatMode();
    return mode != null && mode.trim().equalsIgnoreCase("sse");
  }

  public static class DeliveryOptions {

    public final boolean enabled;
    public final String channel;
    public final String to;

    public DeliveryOptions(boolean enabled, String channel, String to) {
      this.enabled = enabled;
      this.channel = channel;
      this.to = to;
    }
  }
}
