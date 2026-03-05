package com.agentbot.core.channel.impl;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.bus.ExternalMessageBus;
import com.agentbot.core.bus.MessageEnvelope;
import com.agentbot.core.bus.events.OutboundMessage;
import com.agentbot.core.channel.ChannelAdapter;
import com.alibaba.fastjson.JSONObject;
import com.dingtalk.open.app.api.OpenDingTalkClient;
import com.dingtalk.open.app.api.OpenDingTalkStreamClientBuilder;
import com.dingtalk.open.app.api.callback.DingTalkStreamTopics;
import com.dingtalk.open.app.api.callback.OpenDingTalkCallbackListener;
import com.dingtalk.open.app.api.models.bot.ChatbotMessage;
import com.dingtalk.open.app.api.models.bot.MessageContent;
import com.dingtalk.open.app.api.security.AuthClientCredential;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class DingTalkChannelAdapter implements ChannelAdapter {
  private static final Logger log = LoggerFactory.getLogger(DingTalkChannelAdapter.class);

  private final ExternalMessageBus messageBus;
  private final AgentbotProperties.DingTalk config;
  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  private volatile OpenDingTalkClient streamClient;
  private volatile boolean running = false;

  public DingTalkChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    this.messageBus = messageBus;
    this.config = properties.getChannels().getDingtalk();
  }

  @Override
  public String name() {
    return "dingtalk";
  }

  @Override
  public void start() {
    if (!config.isEnabled()) {
      log.info("dingtalk channel disabled");
      return;
    }
    if (config.getAppKey() == null || config.getAppKey().isBlank()
        || config.getAppSecret() == null || config.getAppSecret().isBlank()) {
      log.warn("dingtalk appKey/appSecret missing");
      return;
    }
    running = true;
    try {
      streamClient = OpenDingTalkStreamClientBuilder
          .custom()
          .credential(new AuthClientCredential(config.getAppKey(), config.getAppSecret()))
          .registerCallbackListener(DingTalkStreamTopics.BOT_MESSAGE_TOPIC, new ChatbotMessageListener())
          .build();
      streamClient.start();
      log.info("dingtalk stream client started");
    } catch (Exception e) {
      log.warn("dingtalk stream client start failed", e);
    }
  }

  @Override
  public void stop() {
    running = false;
    if (streamClient != null) {
      tryInvoke(streamClient, "shutdown");
      tryInvoke(streamClient, "close");
    }
    log.info("dingtalk channel stopped");
  }

  @Override
  public void send(OutboundMessage message) {
    if (!config.isEnabled()) return;
    String webhookUrl = resolveWebhookUrl(message);
    if (webhookUrl == null || webhookUrl.isBlank()) {
      log.warn("dingtalk send skipped: no sessionWebhook or webhookUrl");
      return;
    }
    try {
      String url = signWebhookUrl(webhookUrl, config.getWebhookSecret());
      Map<String, Object> payload = Map.of(
          "msgtype", "text",
          "text", Map.of("content", message.getContent())
      );
      String body = mapper.writeValueAsString(payload);
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
          .thenAccept(response -> {
            int code = response.statusCode();
            if (code >= 300) {
              log.warn("dingtalk send failed: chatId={} status={}", message.getChatId(), code);
            } else {
              log.info("dingtalk send ok: chatId={} length={}", message.getChatId(),
                  message.getContent() == null ? 0 : message.getContent().length());
            }
          });
    } catch (Exception e) {
      log.warn("dingtalk send failed", e);
    }
  }

  public void handleWebhook(Map<String, Object> payload) {
    if (!config.isEnabled()) return;
    if (payload == null) return;
    String senderId = String.valueOf(payload.getOrDefault("senderStaffId", payload.getOrDefault("senderId", "")));
    String chatId = String.valueOf(payload.getOrDefault("conversationId", senderId));
    String content = "";
    Object textObj = payload.get("text");
    if (textObj instanceof Map<?, ?> textMap) {
      Object c = ((Map<?, ?>) textMap).get("content");
      if (c != null) content = String.valueOf(c);
    }
    if (content.isBlank()) return;
    String conversationType = String.valueOf(payload.getOrDefault("conversationType", ""));
    String peerKind = resolvePeerKind(conversationType);
    log.info("dingtalk inbound (webhook): chatId={} senderId={} length={}", chatId, senderId, content.length());
    Map<String, Object> metadata = new HashMap<>();
    metadata.put(MessageEnvelope.META_ACCOUNT_ID, "default");
    metadata.put(MessageEnvelope.META_PEER_KIND, peerKind);
    metadata.put(MessageEnvelope.META_PEER_ID, chatId);
    metadata.put("raw", payload);
    MessageEnvelope inbound = MessageEnvelope.externalInbound("dingtalk", senderId, chatId, content, metadata);
    messageBus.publish(inbound);

  }

  private String resolveWebhookUrl(OutboundMessage message) {
    if (message == null) return null;
    if (message.getMetadata() != null) {
      Object sessionWebhook = message.getMetadata().get("sessionWebhook");
      if (sessionWebhook != null && !String.valueOf(sessionWebhook).isBlank()) {
        return String.valueOf(sessionWebhook);
      }
    }
    String chatId = message.getChatId();
    if (chatId != null && (chatId.startsWith("http://") || chatId.startsWith("https://"))) {
      return chatId;
    }
    return config.getWebhookUrl();
  }

  private String signWebhookUrl(String webhookUrl, String secret) {
    if (secret == null || secret.isBlank()) return webhookUrl;
    try {
      long timestamp = System.currentTimeMillis();
      String stringToSign = timestamp + "\n" + secret;
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String sign = java.util.Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
      String encodedSign = URLEncoder.encode(sign, StandardCharsets.UTF_8);
      String sep = webhookUrl.contains("?") ? "&" : "?";
      return webhookUrl + sep + "timestamp=" + timestamp + "&sign=" + encodedSign;
    } catch (Exception e) {
      return webhookUrl;
    }
  }

  private void tryInvoke(Object target, String method) {
    try {
      target.getClass().getMethod(method).invoke(target);
      log.info("dingtalk stream client {} invoked", method);
    } catch (Exception ignored) {
    }
  }

  private String resolvePeerKind(String conversationType) {
    String type = conversationType == null ? "" : conversationType.trim().toLowerCase();
    if ("1".equals(type) || "single".equals(type) || "p2p".equals(type)) {
      return "dm";
    }
    if ("2".equals(type) || "group".equals(type) || "chat".equals(type)) {
      return "group";
    }
    return "group";
  }

  private class ChatbotMessageListener implements OpenDingTalkCallbackListener<ChatbotMessage, JSONObject> {

    @Override
    public JSONObject execute(ChatbotMessage message) {
      if (!running) return new JSONObject();
      try {
        if (message == null) return new JSONObject();
        String msgType = message.getMsgtype();
        MessageContent text = message.getText();
        String content = text == null ? "" : String.valueOf(text.getContent());
        if (!"text".equals(msgType) || content.isBlank()) {
          return new JSONObject();
        }
        String senderId = message.getSenderId();
        String chatId = message.getConversationId();
        String peerKind = resolvePeerKind(message.getConversationType());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(MessageEnvelope.META_ACCOUNT_ID, "default");
        metadata.put(MessageEnvelope.META_PEER_KIND, peerKind);
        metadata.put(MessageEnvelope.META_PEER_ID, chatId);
        metadata.put("raw", message);
        log.info("dingtalk inbound (stream): chatId={} senderId={} length={}", chatId, senderId, content.length());
        MessageEnvelope inbound = MessageEnvelope.externalInbound("dingtalk", senderId, chatId, content, metadata);
        messageBus.publish(inbound);

      } catch (Exception e) {
        log.warn("dingtalk stream message handle failed", e);
      }
      return new JSONObject();
    }
  }
}
