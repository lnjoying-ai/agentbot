package com.agentbot.core.channel.impl;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.bus.ExternalMessageBus;
import com.agentbot.core.bus.MessageEnvelope;
import com.agentbot.core.bus.events.OutboundMessage;
import com.agentbot.core.channel.ChannelAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OneBotChannelAdapter implements ChannelAdapter {
  private static final Logger log = LoggerFactory.getLogger(OneBotChannelAdapter.class);

  private final ExternalMessageBus messageBus;
  private final AgentbotProperties.OneBot config;
  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private volatile WebSocket webSocket;
  private volatile boolean running = false;

  public OneBotChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    this.messageBus = messageBus;
    this.config = properties.getChannels().getOnebot();
  }

  @Override
  public String name() {
    return "onebot";
  }

  @Override
  public void start() {
    if (!config.isEnabled()) {
      log.info("onebot channel disabled");
      return;
    }
    if (config.getWsUrl() == null || config.getWsUrl().isBlank()) {
      log.warn("onebot wsUrl missing");
      return;
    }
    running = true;
    log.info("onebot channel starting: wsUrl={}", config.getWsUrl());
    connect();
  }


  @Override
  public void stop() {
    running = false;
    if (webSocket != null) {
      webSocket.abort();
    }
    scheduler.shutdownNow();
    log.info("onebot channel stopped");
  }

  @Override
  public void send(OutboundMessage message) {
    WebSocket ws = webSocket;
    if (ws == null) {
      log.warn("onebot ws not connected, skip send: chatId={}", message.getChatId());
      return;
    }
    try {
      boolean isGroup = false;
      Object meta = message.getMetadata().get("isGroup");
      if (meta instanceof Boolean b) {
        isGroup = b;
      }
      String action = isGroup ? "send_group_msg" : "send_private_msg";
      Map<String, Object> params = isGroup
          ? Map.of("group_id", message.getChatId(), "message", message.getContent())
          : Map.of("user_id", message.getChatId(), "message", message.getContent());
      String payload = mapper.writeValueAsString(Map.of("action", action, "params", params));
      ws.sendText(payload, true);
      log.info("onebot send: action={} chatId={} length={}", action, message.getChatId(),
          message.getContent() == null ? 0 : message.getContent().length());
    } catch (Exception e) {
      log.warn("onebot send failed", e);
    }
  }


  private void connect() {
    if (!running) return;
    try {
      WebSocket.Builder builder = httpClient.newWebSocketBuilder();
      if (config.getAccessToken() != null && !config.getAccessToken().isBlank()) {
        builder.header("Authorization", "Bearer " + config.getAccessToken());
      }
      CompletableFuture<WebSocket> future = builder.buildAsync(URI.create(config.getWsUrl()), new Listener());
      future.whenComplete((ws, err) -> {
        if (err != null) {
          log.warn("onebot ws connect failed", err);
          scheduleReconnect();
          return;
        }
        webSocket = ws;
        log.info("onebot ws connected");
      });
    } catch (Exception e) {
      log.warn("onebot ws connect error", e);
      scheduleReconnect();
    }
  }

  private void scheduleReconnect() {
    if (!running) return;
    int delay = Math.max(1, config.getReconnectIntervalSeconds());
    log.info("onebot reconnect scheduled: delay={}s", delay);
    scheduler.schedule(this::connect, delay, TimeUnit.SECONDS);
  }


  private class Listener implements WebSocket.Listener {
    @Override
    public void onOpen(WebSocket webSocket) {
      WebSocket.Listener.super.onOpen(webSocket);
      webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
      try {
        handleEvent(data.toString());
      } catch (Exception e) {
        log.warn("onebot message parse failed", e);
      }
      webSocket.request(1);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
      log.warn("onebot ws closed: {} {}", statusCode, reason);
      scheduleReconnect();
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
      log.warn("onebot ws error", error);
      scheduleReconnect();
    }
  }

  private void handleEvent(String payload) throws Exception {
    JsonNode root = mapper.readTree(payload);
    String postType = root.path("post_type").asText("");
    if (!"message".equals(postType)) return;
    String messageType = root.path("message_type").asText("");
    String content = root.path("raw_message").asText(root.path("message").asText(""));
    if (content.isBlank()) return;

    String userId = root.path("user_id").asText("");
    String chatId;
    boolean isGroup = "group".equals(messageType);
    if (isGroup) {
      chatId = root.path("group_id").asText("");
    } else {
      chatId = userId;
    }

    if (isGroup && config.getGroupTriggerPrefix() != null && !config.getGroupTriggerPrefix().isBlank()) {
      String prefix = config.getGroupTriggerPrefix();
      if (!content.startsWith(prefix)) {
        return;
      }
      content = content.substring(prefix.length()).trim();
      if (content.isBlank()) return;
    }

    Map<String, Object> metadata = new java.util.HashMap<>();
    metadata.put("isGroup", isGroup);
    metadata.put(MessageEnvelope.META_ACCOUNT_ID, "default");
    metadata.put(MessageEnvelope.META_PEER_KIND, isGroup ? "group" : "dm");
    metadata.put(MessageEnvelope.META_PEER_ID, chatId);
    metadata.put("raw", payload);
    log.info("onebot inbound: chatId={} senderId={} group={} length={}", chatId, userId, isGroup, content.length());
    MessageEnvelope inbound = MessageEnvelope.externalInbound("onebot", userId, chatId, content, metadata);
    messageBus.publish(inbound);

  }

}
