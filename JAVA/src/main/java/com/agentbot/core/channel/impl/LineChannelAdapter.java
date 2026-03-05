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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class LineChannelAdapter implements ChannelAdapter {
  private static final Logger log = LoggerFactory.getLogger(LineChannelAdapter.class);
  private static final String API_BASE = "https://api.line.me/v2/bot/message";

  private final ExternalMessageBus messageBus;
  private final AgentbotProperties.Line config;
  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public LineChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    this.messageBus = messageBus;
    this.config = properties.getChannels().getLine();
  }

  @Override
  public String name() {
    return "line";
  }

  @Override
  public void start() {
    if (!config.isEnabled()) {
      log.info("line channel disabled");
      return;
    }
    if (config.getChannelAccessToken() == null || config.getChannelAccessToken().isBlank()) {
      log.warn("line access token missing");
      return;
    }
    log.info("line channel started (webhook inbound)");
  }

  @Override
  public void stop() {
    log.info("line channel stopped");
  }

  @Override
  public void send(OutboundMessage message) {
    if (!config.isEnabled()) return;
    String token = config.getChannelAccessToken();
    if (token == null || token.isBlank()) return;
    try {
      Map<String, Object> msg = Map.of("type", "text", "text", message.getContent());
      Object replyToken = message.getMetadata().get("replyToken");
      String endpoint;
      Map<String, Object> payload;
      if (replyToken != null) {
        endpoint = API_BASE + "/reply";
        payload = Map.of("replyToken", String.valueOf(replyToken), "messages", java.util.List.of(msg));
      } else {
        endpoint = API_BASE + "/push";
        payload = Map.of("to", message.getChatId(), "messages", java.util.List.of(msg));
      }
      String body = mapper.writeValueAsString(payload);
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(endpoint))
          .header("Authorization", "Bearer " + token)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
          .thenAccept(response -> {
            int code = response.statusCode();
            if (code >= 300) {
              log.warn("line send failed: chatId={} status={}", message.getChatId(), code);
            } else {
              log.info("line send ok: chatId={} length={}", message.getChatId(),
                  message.getContent() == null ? 0 : message.getContent().length());
            }
          });
    } catch (Exception e) {
      log.warn("line send failed", e);
    }
  }


  public boolean verifySignature(String body, String signature) {
    if (signature == null || signature.isBlank()) return false;
    if (config.getChannelSecret() == null || config.getChannelSecret().isBlank()) return true;
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(config.getChannelSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
      String encoded = Base64.getEncoder().encodeToString(digest);
      return encoded.equals(signature);
    } catch (Exception e) {
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  public void handleWebhook(Map<String, Object> payload) {
    if (!config.isEnabled()) return;
    if (payload == null) return;
    Object eventsObj = payload.get("events");
    if (!(eventsObj instanceof java.util.List<?> events)) return;
    for (Object evtObj : events) {
      if (!(evtObj instanceof Map<?, ?> evt)) continue;
      Map<String, Object> event = (Map<String, Object>) evt;
      Object messageObj = event.get("message");
      if (!(messageObj instanceof Map<?, ?> message)) continue;
      String type = String.valueOf(((Map<String, Object>) message).getOrDefault("type", ""));
      if (!"text".equals(type)) continue;
      String content = String.valueOf(((Map<String, Object>) message).getOrDefault("text", ""));
      if (content.isBlank()) continue;
      Map<String, Object> source = (Map<String, Object>) event.getOrDefault("source", Map.of());
      String senderId = String.valueOf(source.getOrDefault("userId", ""));
      String chatId = String.valueOf(source.getOrDefault("groupId", source.getOrDefault("roomId", senderId)));
      String sourceType = String.valueOf(source.getOrDefault("type", "user"));
      String peerKind = "user".equalsIgnoreCase(sourceType) ? "dm" : "group";
      String replyToken = String.valueOf(event.getOrDefault("replyToken", ""));
      Map<String, Object> metadata = new HashMap<>();
      if (!replyToken.isBlank()) metadata.put("replyToken", replyToken);
      metadata.put(MessageEnvelope.META_ACCOUNT_ID, "default");
      metadata.put(MessageEnvelope.META_PEER_KIND, peerKind);
      metadata.put(MessageEnvelope.META_PEER_ID, chatId);
      metadata.put("raw", payload);
      log.info("line inbound: chatId={} senderId={} length={}", chatId, senderId, content.length());
      MessageEnvelope inbound = MessageEnvelope.externalInbound("line", senderId, chatId, content, metadata);
      messageBus.publish(inbound);

    }
  }

}
