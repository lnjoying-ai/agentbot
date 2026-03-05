package com.agentbot.core.channel.impl;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.bus.ExternalMessageBus;
import com.agentbot.core.bus.MessageEnvelope;
import com.agentbot.core.bus.events.OutboundMessage;
import com.agentbot.core.channel.ChannelAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.EventMessage;
import com.lark.oapi.service.im.v1.model.EventSender;
import com.lark.oapi.service.im.v1.model.P2ChatAccessEventBotP2pChatEnteredV1;
import com.lark.oapi.service.im.v1.model.P2ChatAccessEventBotP2pChatEnteredV1Data;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1Data;
import com.lark.oapi.service.im.v1.model.UserId;
import com.lark.oapi.ws.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;



public class FeishuChannelAdapter implements ChannelAdapter {
  private static final Logger log = LoggerFactory.getLogger(FeishuChannelAdapter.class);
  private static final String API_BASE = "https://open.feishu.cn/open-apis";

  private final ExternalMessageBus messageBus;
  private final AgentbotProperties.Feishu config;
  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  private volatile String tenantAccessToken;
  private volatile Instant tokenExpireAt;
  private volatile Client wsClient;
  private volatile boolean running = false;


  //https://wiki.lckfb.com/zh-hans/tspi-3-rk3576/openclaw/openclaw-feishu-channel.html
  //https://github.com/larksuite/oapi-sdk-java
  public FeishuChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    this.messageBus = messageBus;
    this.config = properties.getChannels().getFeishu();
  }

  @Override
  public String name() {
    return "feishu";
  }

  @Override
  public void start() {
    if (!config.isEnabled()) {
      log.info("feishu channel disabled");
      return;
    }
    if (config.getAppId() == null || config.getAppId().isBlank()
        || config.getAppSecret() == null || config.getAppSecret().isBlank()) {
      log.warn("feishu appId/appSecret missing");
      return;
    }
    running = true;
    try {
      EventDispatcher dispatcher = EventDispatcher
          .newBuilder(config.getVerificationToken(), config.getEncryptKey())
          .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
            @Override
            public void handle(P2MessageReceiveV1 event) {
              handleMessageEvent(event);
            }
          })
          .onP2ChatAccessEventBotP2pChatEnteredV1(new ImService.P2ChatAccessEventBotP2pChatEnteredV1Handler() {
            @Override
            public void handle(P2ChatAccessEventBotP2pChatEnteredV1 event) {
              handleBotP2pChatEntered(event);
            }
          })
          .build();
      wsClient = new Client.Builder(config.getAppId(), config.getAppSecret())
          .eventHandler(dispatcher)
          .autoReconnect(config.isAutoReconnect())
          .domain(config.getDomain())
          .build();
      wsClient.start();
      log.info("feishu channel started (websocket): domain={} autoReconnect={}", config.getDomain(),
          config.isAutoReconnect());
    } catch (Exception e) {
      log.warn("feishu ws client start failed", e);
    }
  }

  @Override
  public void stop() {
    running = false;
    if (wsClient != null) {
      tryInvoke(wsClient, "disconnect");
    }
    log.info("feishu channel stopped");
  }

  @Override
  public void send(OutboundMessage message) {
    if (!config.isEnabled()) return;
    try {
      String token = tenantAccessToken();
      if (token == null || token.isBlank()) {
        log.warn("feishu token missing, skip send");
        return;
      }
      Map<String, Object> content = Map.of("text", message.getContent());
      Map<String, Object> body = Map.of(
          "receive_id", message.getChatId(),
          "msg_type", "text",
          "content", mapper.writeValueAsString(content)
      );
      String payload = mapper.writeValueAsString(body);
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(API_BASE + "/im/v1/messages?receive_id_type=chat_id"))
          .header("Authorization", "Bearer " + token)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(payload))
          .build();
      httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
          .thenAccept(response -> {
            int code = response.statusCode();
            if (code >= 300) {
              log.warn("feishu send failed: chatId={} status={}", message.getChatId(), code);
            } else {
              log.info("feishu send ok: chatId={} length={}", message.getChatId(),
                  message.getContent() == null ? 0 : message.getContent().length());
            }
          });
    } catch (Exception e) {
      log.warn("feishu send failed", e);
    }
  }

  private void handleMessageEvent(P2MessageReceiveV1 event) {
    if (!running || event == null) return;
    P2MessageReceiveV1Data data = event.getEvent();
    if (data == null) return;
    EventMessage message = data.getMessage();
    if (message == null) return;
    String msgType = message.getMessageType();
    if (!"text".equals(msgType)) return;
    String contentJson = message.getContent();
    String content = "";
    if (contentJson != null && !contentJson.isBlank()) {
      try {
        JsonNode root = mapper.readTree(contentJson);
        content = root.path("text").asText("");
      } catch (Exception ignored) {
        content = contentJson;
      }
    }
    if (content.isBlank()) return;
    String chatId = message.getChatId();
    String senderId = "";
    EventSender sender = data.getSender();
    if (sender != null) {
      senderId = resolveUserId(sender.getSenderId());
    }
    String peerKind = resolveFeishuPeerKind(message.getChatType());
    log.info("feishu inbound (ws): chatId={} senderId={} length={}", chatId, senderId, content.length());
    HashMap<String, Object> metadata = new HashMap<>();
    metadata.put(MessageEnvelope.META_ACCOUNT_ID, "default");
    metadata.put(MessageEnvelope.META_PEER_KIND, peerKind);
    metadata.put(MessageEnvelope.META_PEER_ID, chatId);
    metadata.put("raw", event);
    MessageEnvelope inbound = MessageEnvelope.externalInbound("feishu", senderId, chatId, content, metadata);
    messageBus.publish(inbound);

  }

  private void handleBotP2pChatEntered(P2ChatAccessEventBotP2pChatEnteredV1 event) {
    if (!running || event == null) return;
    P2ChatAccessEventBotP2pChatEnteredV1Data data = event.getEvent();
    if (data == null) return;
    String chatId = data.getChatId();
    String operatorId = resolveUserId(data.getOperatorId());
    log.info("feishu bot p2p chat entered: chatId={} operatorId={} lastMessageId={}",
        chatId, operatorId, data.getLastMessageId());
  }

  private String resolveUserId(UserId userId) {
    if (userId == null) return "";
    if (userId.getOpenId() != null && !userId.getOpenId().isBlank()) {
      return userId.getOpenId();
    }
    if (userId.getUserId() != null && !userId.getUserId().isBlank()) {
      return userId.getUserId();
    }
    if (userId.getUnionId() != null && !userId.getUnionId().isBlank()) {
      return userId.getUnionId();
    }
    return "";
  }

  private String resolveFeishuPeerKind(String chatType) {
    String type = chatType == null ? "" : chatType.trim().toLowerCase();
    if ("p2p".equals(type) || "private".equals(type)) return "dm";
    if ("group".equals(type) || "chat".equals(type)) return "group";
    return "dm";
  }


  @SuppressWarnings("unchecked")
  public void handleWebhook(Map<String, Object> payload) {
    if (!config.isEnabled()) return;
    if (payload == null) return;
    Object eventObj = payload.get("event");
    if (!(eventObj instanceof Map<?, ?>)) return;
    Map<String, Object> event = (Map<String, Object>) eventObj;
    Object messageObj = event.get("message");
    if (!(messageObj instanceof Map<?, ?>)) return;
    Map<String, Object> message = (Map<String, Object>) messageObj;
    String msgType = String.valueOf(message.getOrDefault("message_type", ""));
    if (!"text".equals(msgType)) return;

    String chatId = String.valueOf(message.getOrDefault("chat_id", ""));
    String senderId = "";
    Object senderObj = event.get("sender");
    if (senderObj instanceof Map<?, ?> sender) {
      Object senderIdObj = ((Map<String, Object>) sender).get("sender_id");
      if (senderIdObj instanceof Map<?, ?> senderIdMap) {
        senderId = String.valueOf(((Map<String, Object>) senderIdMap).getOrDefault("open_id", ""));
      }
    }

    String content = "";
    Object contentObj = message.get("content");
    if (contentObj != null) {
      try {
        JsonNode root = mapper.readTree(String.valueOf(contentObj));
        content = root.path("text").asText("");
      } catch (Exception ignored) {
        content = String.valueOf(contentObj);
      }
    }

    if (content.isBlank()) return;
    String peerKind = "group";
    Object chatTypeObj = event.get("chat_type");
    if (chatTypeObj != null) {
      peerKind = resolveFeishuPeerKind(String.valueOf(chatTypeObj));
    }
    log.info("feishu inbound: chatId={} senderId={} length={}", chatId, senderId, content.length());
    HashMap<String, Object> metadata = new HashMap<>();
    metadata.put(MessageEnvelope.META_ACCOUNT_ID, "default");
    metadata.put(MessageEnvelope.META_PEER_KIND, peerKind);
    metadata.put(MessageEnvelope.META_PEER_ID, chatId);
    metadata.put("raw", payload);
    MessageEnvelope inbound = MessageEnvelope.externalInbound("feishu", senderId, chatId, content, metadata);
    messageBus.publish(inbound);

  }

  public Map<String, Object> decodeWebhookPayload(Map<String, Object> payload) {
    if (payload == null) return null;
    Object encrypt = payload.get("encrypt");
    if (encrypt == null) {
      return payload;
    }
    if (config.getEncryptKey() == null || config.getEncryptKey().isBlank()) {
      log.warn("feishu encrypt payload received but encryptKey not configured");
      return payload;
    }

    String decrypted = decryptPayload(String.valueOf(encrypt));
    if (decrypted == null || decrypted.isBlank()) return payload;
    try {
      return mapper.readValue(decrypted, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

    } catch (Exception e) {
      return payload;
    }
  }

  public boolean verifyToken(Map<String, Object> payload) {
    if (payload == null) return false;
    String expected = config.getVerificationToken();
    if (expected == null || expected.isBlank()) return true;
    String token = extractToken(payload);
    return expected.equals(token);
  }

  public Object getChallenge(Map<String, Object> payload) {
    if (payload == null) return null;
    return payload.get("challenge");
  }

  private String extractToken(Map<String, Object> payload) {
    if (payload.containsKey("token")) {
      return String.valueOf(payload.get("token"));
    }
    Object headerObj = payload.get("header");
    if (headerObj instanceof Map<?, ?> header) {
      Object tokenObj = ((Map<?, ?>) header).get("token");
      if (tokenObj != null) return String.valueOf(tokenObj);
    }
    return "";
  }

  private String decryptPayload(String encrypted) {
    try {
      byte[] encryptedBytes = Base64.getDecoder().decode(encrypted);
      byte[] keyBytes = normalizeKey(config.getEncryptKey());
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(ivFromKey(keyBytes)));
      byte[] decrypted = cipher.doFinal(encryptedBytes);
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.warn("feishu decrypt failed", e);
      return null;
    }
  }

  private byte[] normalizeKey(String raw) {
    if (raw == null) return new byte[32];
    try {
      byte[] decoded = Base64.getDecoder().decode(raw);
      if (decoded.length == 16 || decoded.length == 24 || decoded.length == 32) return padKey(decoded);
    } catch (Exception ignored) {
    }
    return padKey(raw.getBytes(StandardCharsets.UTF_8));
  }

  private byte[] padKey(byte[] input) {
    byte[] out = new byte[32];
    int len = Math.min(input.length, out.length);
    System.arraycopy(input, 0, out, 0, len);
    return out;
  }

  private byte[] ivFromKey(byte[] key) {
    byte[] iv = new byte[16];
    System.arraycopy(key, 0, iv, 0, Math.min(16, key.length));
    return iv;
  }

  private String tenantAccessToken() {
    if (tenantAccessToken != null && tokenExpireAt != null && tokenExpireAt.isAfter(Instant.now().plusSeconds(60))) {
      return tenantAccessToken;
    }
    try {
      Map<String, Object> payload = Map.of(
          "app_id", config.getAppId(),
          "app_secret", config.getAppSecret()
      );
      String body = mapper.writeValueAsString(payload);
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(API_BASE + "/auth/v3/tenant_access_token/internal"))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      JsonNode root = mapper.readTree(response.body());
      String token = root.path("tenant_access_token").asText("");
      int expires = root.path("expire").asInt(0);
      if (!token.isBlank()) {
        tenantAccessToken = token;
        tokenExpireAt = Instant.now().plusSeconds(Math.max(60, expires));
      }
      return tenantAccessToken;
    } catch (Exception e) {
      log.warn("feishu token fetch failed", e);
      return null;
    }
  }

  private void tryInvoke(Object target, String method) {
    try {
      target.getClass().getDeclaredMethod(method).invoke(target);
      log.info("feishu ws client {} invoked", method);
    } catch (Exception ignored) {
    }
  }
}
