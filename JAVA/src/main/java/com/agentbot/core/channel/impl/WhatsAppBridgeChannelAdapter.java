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
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WhatsAppBridgeChannelAdapter implements ChannelAdapter {
  private static final Logger log = LoggerFactory.getLogger(WhatsAppBridgeChannelAdapter.class);

  private final ExternalMessageBus messageBus;
  private final AgentbotProperties.WhatsApp config;
  private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  private final ObjectMapper mapper = new ObjectMapper();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private volatile WebSocket webSocket;
  private volatile boolean running = false;

  public WhatsAppBridgeChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    this.messageBus = messageBus;
    this.config = properties.getChannels().getWhatsapp();
  }


  @Override
  public String name() {
    return "whatsapp";
  }

  @Override
  public void start() {
    if (!config.isEnabled()) {
      log.info("whatsapp channel disabled");
      return;
    }
    if (config.getBridgeUrl() == null || config.getBridgeUrl().isBlank()) {
      log.warn("whatsapp bridge url missing");
      return;
    }
    running = true;
    log.info("whatsapp channel started: bridgeUrl={}", config.getBridgeUrl());
    connect();
  }

  @Override
  public void stop() {
    running = false;
    if (webSocket != null) {
      webSocket.abort();
    }
    scheduler.shutdownNow();
    log.info("whatsapp channel stopped");
  }


  @Override
  public void send(OutboundMessage message) {
    WebSocket ws = webSocket;
    if (ws == null) {
      log.warn("whatsapp bridge not connected, skip send: chatId={}", message.getChatId());
      return;
    }
    try {
      String payload = mapper.writeValueAsString(
          java.util.Map.of("type", "send", "to", message.getChatId(), "text", message.getContent())
      );
      ws.sendText(payload, true);
      log.info("whatsapp send: chatId={} length={}", message.getChatId(),
          message.getContent() == null ? 0 : message.getContent().length());
    } catch (Exception e) {
      log.warn("whatsapp send failed", e);
    }
  }


  private void connect() {
    if (!running) return;
    URI uri = URI.create(config.getBridgeUrl());
    CompletableFuture<WebSocket> future = httpClient.newWebSocketBuilder().buildAsync(uri, new Listener());
    future.whenComplete((ws, err) -> {
      if (err != null) {
        log.warn("whatsapp bridge connect failed", err);
        scheduleReconnect();
        return;
      }
      webSocket = ws;
      log.info("whatsapp bridge connected");
    });
  }

  private void scheduleReconnect() {
    if (!running) return;
    int delay = 5;
    log.info("whatsapp bridge reconnect scheduled: delay={}s", delay);
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
        handleBridgeMessage(data.toString());
      } catch (Exception e) {
        log.warn("whatsapp message parse failed", e);
      }
      webSocket.request(1);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
      log.warn("whatsapp bridge closed: {} {}", statusCode, reason);
      scheduleReconnect();
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
      log.warn("whatsapp bridge error", error);
      scheduleReconnect();
    }
  }

  private void handleBridgeMessage(String payload) throws Exception {
    JsonNode root = mapper.readTree(payload);
    String type = root.path("type").asText("");
    if ("message".equals(type)) {
      String sender = root.path("from").asText("");
      String chatId = root.path("chatId").asText("");
      if (chatId.isBlank()) chatId = sender;
      String text = root.path("text").asText("");
      if (!text.isBlank()) {
        log.info("whatsapp inbound: chatId={} senderId={} length={}", chatId, sender, text.length());
        String peerKind = chatId != null && chatId.endsWith("@g.us") ? "group" : "dm";
        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put(MessageEnvelope.META_ACCOUNT_ID, "default");
        metadata.put(MessageEnvelope.META_PEER_KIND, peerKind);
        metadata.put(MessageEnvelope.META_PEER_ID, chatId);
        metadata.put("raw", root);
        MessageEnvelope inbound = MessageEnvelope.externalInbound("whatsapp", sender, chatId, text, metadata);
        messageBus.publish(inbound);


      }
      return;
    }

    if ("status".equals(type)) {
      log.info("whatsapp status: {}", root.path("status").asText(""));
      return;
    }
    if ("qr".equals(type)) {
      log.info("whatsapp qr received; scan in bridge console");
      return;
    }
    if ("error".equals(type)) {
      log.warn("whatsapp bridge error: {}", root.path("message").asText(""));
    }
  }
}
