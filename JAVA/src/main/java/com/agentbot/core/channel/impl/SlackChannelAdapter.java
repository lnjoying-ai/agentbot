package com.agentbot.core.channel.impl;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.bus.ExternalMessageBus;
import com.agentbot.core.bus.MessageEnvelope;
import com.agentbot.core.bus.events.OutboundMessage;
import com.agentbot.core.channel.ChannelAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


public class SlackChannelAdapter implements ChannelAdapter {
  private static final Logger log = LoggerFactory.getLogger(SlackChannelAdapter.class);
  private static final String API_BASE = "https://slack.com/api";

  private final ExternalMessageBus messageBus;
  private final AgentbotProperties.Slack config;
  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public SlackChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    this.messageBus = messageBus;
    this.config = properties.getChannels().getSlack();
  }

  @Override
  public String name() {
    return "slack";
  }

  @Override
  public void start() {
    if (!config.isEnabled()) {
      log.info("slack channel disabled");
      return;
    }
    if (config.getBotToken() == null || config.getBotToken().isBlank()) {
      log.warn("slack bot token missing");
      return;
    }
    log.info("slack channel started");
  }

  @Override
  public void stop() {
    log.info("slack channel stopped");
  }

  @Override
  public void send(OutboundMessage message) {
    if (!config.isEnabled()) return;
    String token = config.getBotToken();
    if (token == null || token.isBlank()) {
      log.warn("slack bot token missing, skip send");
      return;
    }
    try {
      Map<String, Object> payload = Map.of(
          "channel", message.getChatId(),
          "text", message.getContent()
      );
      String body = mapper.writeValueAsString(payload);
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(API_BASE + "/chat.postMessage"))
          .header("Authorization", "Bearer " + token)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
          .thenAccept(response -> {
            int code = response.statusCode();
            if (code >= 300) {
              log.warn("slack send failed: chatId={} status={}", message.getChatId(), code);
            } else {
              log.info("slack send ok: chatId={} length={}", message.getChatId(),
                  message.getContent() == null ? 0 : message.getContent().length());
            }
          });
    } catch (Exception e) {
      log.warn("slack send failed", e);
    }
  }


  @SuppressWarnings("unchecked")
  public void handleWebhook(Map<String, Object> payload) {
    if (!config.isEnabled()) return;
    if (payload == null) return;

    Object eventObj = payload.get("event");
    if (!(eventObj instanceof Map<?, ?>)) return;
    Map<String, Object> event = (Map<String, Object>) eventObj;
    String type = asText(event.get("type"));
    if (!"message".equals(type)) return;
    if (event.containsKey("bot_id")) return;
    String subtype = asText(event.get("subtype"));
    if (subtype != null && !subtype.isBlank()) return;

    String senderId = asText(event.get("user"));
    String chatId = asText(event.get("channel"));
    String text = asText(event.get("text"));
    if (text == null || text.isBlank()) return;
    String channelType = asText(event.get("channel_type"));
    String peerKind;
    if ("im".equalsIgnoreCase(channelType)) {
      peerKind = "dm";
    } else if ("channel".equalsIgnoreCase(channelType)) {
      peerKind = "channel";
    } else if ("mpim".equalsIgnoreCase(channelType) || "group".equalsIgnoreCase(channelType)) {
      peerKind = "group";
    } else if (chatId != null && chatId.startsWith("D")) {
      peerKind = "dm";
    } else {
      peerKind = "channel";
    }
    String teamId = asText(payload.get("team_id"));
    log.info("slack inbound: chatId={} senderId={} length={}", chatId, senderId, text.length());
    HashMap<String, Object> metadata = new HashMap<>();
    metadata.put(MessageEnvelope.META_ACCOUNT_ID, "default");
    metadata.put(MessageEnvelope.META_PEER_KIND, peerKind);
    metadata.put(MessageEnvelope.META_PEER_ID, chatId);
    if (teamId != null && !teamId.isBlank()) {
      metadata.put(MessageEnvelope.META_TEAM_ID, teamId);
    }
    metadata.put("raw", payload);
    MessageEnvelope inbound = MessageEnvelope.externalInbound("slack", senderId, chatId, text, metadata);
    messageBus.publish(inbound);

  }


  private String asText(Object value) {
    return value == null ? "" : String.valueOf(value);
  }
}
