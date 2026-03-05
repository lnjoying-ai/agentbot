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
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DiscordChannelAdapter implements ChannelAdapter {
  private static final Logger log = LoggerFactory.getLogger(DiscordChannelAdapter.class);
  private static final String API_BASE = "https://discord.com/api/v10";

  private final ExternalMessageBus messageBus;
  private final AgentbotProperties.Discord config;
  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  private volatile WebSocket webSocket;
  private volatile boolean running = false;
  private volatile Long lastSeq;
  private volatile String sessionId;
  private volatile String resumeGatewayUrl;
  private volatile long heartbeatIntervalMs = 45000L;
  private volatile long lastHeartbeatAck = System.currentTimeMillis();
  private volatile ScheduledFuture<?> heartbeatTask;

  public DiscordChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    this.messageBus = messageBus;
    this.config = properties.getChannels().getDiscord();
  }

  @Override
  public String name() {
    return "discord";
  }

  @Override
  public void start() {
    if (!config.isEnabled()) {
      log.info("discord channel disabled");
      return;
    }
    if (config.getBotToken() == null || config.getBotToken().isBlank()) {
      log.warn("discord bot token missing");
      return;
    }
    running = true;
    String url = resolveGatewayUrl();
    log.info("discord channel starting: gatewayUrl={}", url);
    connectGateway(url);
    log.info("discord channel started (gateway)");
  }


  @Override
  public void stop() {
    running = false;
    if (heartbeatTask != null) {
      heartbeatTask.cancel(true);
    }
    scheduler.shutdownNow();
    if (webSocket != null) {
      webSocket.abort();
    }
    log.info("discord channel stopped");
  }

  @Override
  public void send(OutboundMessage message) {
    if (!config.isEnabled()) return;
    String token = config.getBotToken();
    if (token == null || token.isBlank()) return;
    try {
      Map<String, Object> payload = Map.of("content", message.getContent());
      String body = mapper.writeValueAsString(payload);
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(API_BASE + "/channels/" + message.getChatId() + "/messages"))
          .header("Authorization", "Bot " + token)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
          .thenAccept(response -> {
            int code = response.statusCode();
            if (code >= 300) {
              log.warn("discord send failed: chatId={} status={}", message.getChatId(), code);
            } else {
              log.info("discord send ok: chatId={} length={}", message.getChatId(),
                  message.getContent() == null ? 0 : message.getContent().length());
            }
          });
    } catch (Exception e) {
      log.warn("discord send failed", e);
    }
  }


  private void connectGateway(String url) {
    if (!running) return;
    try {
      CompletableFuture<WebSocket> future = httpClient.newWebSocketBuilder()
          .connectTimeout(Duration.ofSeconds(10))
          .buildAsync(URI.create(url), new Listener());
      future.whenComplete((ws, err) -> {
        if (err != null) {
          log.warn("discord gateway connect failed", err);
          scheduleReconnect();
          return;
        }
        webSocket = ws;
        log.info("discord gateway connected");
      });

    } catch (Exception e) {
      log.warn("discord gateway connect error", e);
      scheduleReconnect();
    }
  }

  private String resolveGatewayUrl() {
    if (resumeGatewayUrl != null && !resumeGatewayUrl.isBlank()) {
      if (resumeGatewayUrl.contains("?")) return resumeGatewayUrl;
      return resumeGatewayUrl + "?v=10&encoding=json";
    }
    return config.getGatewayUrl();
  }

  private void scheduleReconnect() {
    if (!running) return;
    int delay = Math.max(1, config.getReconnectIntervalSeconds());
    log.info("discord gateway reconnect scheduled: delay={}s", delay);
    scheduler.schedule(() -> connectGateway(resolveGatewayUrl()), delay, TimeUnit.SECONDS);
  }


  private void startHeartbeat() {
    if (heartbeatTask != null) {
      heartbeatTask.cancel(true);
    }
    heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
      if (webSocket == null) return;
      long now = System.currentTimeMillis();
      if (now - lastHeartbeatAck > heartbeatIntervalMs * 2) {
        log.warn("discord heartbeat timeout, reconnecting");
        scheduleReconnect();
        return;
      }
      sendPayload(Map.of("op", 1, "d", lastSeq == null ? null : lastSeq));
    }, heartbeatIntervalMs, heartbeatIntervalMs, TimeUnit.MILLISECONDS);
  }

  private void sendIdentifyOrResume() {
    if (sessionId != null && lastSeq != null) {
      Map<String, Object> d = Map.of(
          "token", "Bot " + config.getBotToken(),
          "session_id", sessionId,
          "seq", lastSeq
      );
      log.info("discord gateway resume: sessionId={} seq={}", sessionId, lastSeq);
      sendPayload(Map.of("op", 6, "d", d));
      return;
    }

    Map<String, Object> props = Map.of(
        "os", System.getProperty("os.name", "unknown"),
        "browser", "agentbot",
        "device", "agentbot"
    );
    Map<String, Object> d = Map.of(
        "token", "Bot " + config.getBotToken(),
        "intents", config.getIntents(),
        "properties", props
    );
    log.info("discord gateway identify: intents={}", config.getIntents());
    sendPayload(Map.of("op", 2, "d", d));
  }


  private void sendPayload(Map<String, Object> payload) {
    WebSocket ws = webSocket;
    if (ws == null) return;
    try {
      String text = mapper.writeValueAsString(payload);
      ws.sendText(text, true);
    } catch (Exception e) {
      log.warn("discord gateway send failed", e);
    }
  }

  private void handleGatewayMessage(String payload) {
    try {
      JsonNode root = mapper.readTree(payload);
      JsonNode seqNode = root.get("s");
      if (seqNode != null && !seqNode.isNull()) {
        lastSeq = seqNode.asLong();
      }
      int op = root.path("op").asInt();
      String t = root.path("t").asText("");
      JsonNode data = root.path("d");
      switch (op) {
        case 10 -> {
          heartbeatIntervalMs = data.path("heartbeat_interval").asLong(45000L);
          lastHeartbeatAck = System.currentTimeMillis();
          startHeartbeat();
          sendIdentifyOrResume();
        }
        case 11 -> lastHeartbeatAck = System.currentTimeMillis();
        case 7 -> {
          log.warn("discord gateway requested reconnect");
          scheduleReconnect();
        }
        case 9 -> {
          log.warn("discord invalid session, resetting");
          sessionId = null;
          resumeGatewayUrl = null;
          scheduleReconnect();
        }
        case 0 -> handleDispatch(t, data);
        default -> {
        }
      }
    } catch (Exception e) {
      log.warn("discord gateway parse failed", e);
    }
  }

  private void handleDispatch(String type, JsonNode data) {
    if ("READY".equals(type)) {
      sessionId = data.path("session_id").asText("");
      resumeGatewayUrl = data.path("resume_gateway_url").asText("");
      log.info("discord gateway ready: sessionId={}", sessionId);
      return;
    }

    if (!"MESSAGE_CREATE".equals(type)) return;
    JsonNode author = data.path("author");
    if (author.path("bot").asBoolean(false)) return;
    String content = data.path("content").asText("");
    if (content.isBlank()) return;
    String senderId = author.path("id").asText("");
    String chatId = data.path("channel_id").asText("");
    String guildId = data.path("guild_id").asText("");
    String peerKind = guildId == null || guildId.isBlank() ? "dm" : "channel";
    log.info("discord inbound: chatId={} senderId={} length={}", chatId, senderId, content.length());
    HashMap<String, Object> metadata = new HashMap<>();
    metadata.put(MessageEnvelope.META_ACCOUNT_ID, "default");
    metadata.put(MessageEnvelope.META_PEER_KIND, peerKind);
    metadata.put(MessageEnvelope.META_PEER_ID, chatId);
    if (guildId != null && !guildId.isBlank()) {
      metadata.put(MessageEnvelope.META_GUILD_ID, guildId);
    }
    metadata.put("raw", data);
    MessageEnvelope inbound = MessageEnvelope.externalInbound("discord", senderId, chatId, content, metadata);
    messageBus.publish(inbound);

  }


  private class Listener implements WebSocket.Listener {
    @Override
    public void onOpen(WebSocket webSocket) {
      WebSocket.Listener.super.onOpen(webSocket);
      webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
      handleGatewayMessage(data.toString());
      webSocket.request(1);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
      log.warn("discord gateway closed: {} {}", statusCode, reason);
      scheduleReconnect();
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
      log.warn("discord gateway error", error);
      scheduleReconnect();
    }
  }

  @SuppressWarnings("unchecked")
  public void handleWebhook(Map<String, Object> payload) {
    if (!config.isEnabled()) return;
    if (payload == null) return;
    Object authorObj = payload.get("author");
    String senderId = "";
    if (authorObj instanceof Map<?, ?> author) {
      senderId = String.valueOf(((Map<String, Object>) author).getOrDefault("id", ""));
    }
    String chatId = String.valueOf(payload.getOrDefault("channel_id", ""));
    String content = String.valueOf(payload.getOrDefault("content", ""));
    if (content.isBlank()) return;
    String guildId = String.valueOf(payload.getOrDefault("guild_id", ""));
    String peerKind = guildId == null || guildId.isBlank() ? "dm" : "channel";
    log.info("discord webhook inbound: chatId={} senderId={} length={}", chatId, senderId, content.length());
    HashMap<String, Object> metadata = new HashMap<>();
    metadata.put(MessageEnvelope.META_ACCOUNT_ID, "default");
    metadata.put(MessageEnvelope.META_PEER_KIND, peerKind);
    metadata.put(MessageEnvelope.META_PEER_ID, chatId);
    if (guildId != null && !guildId.isBlank()) {
      metadata.put(MessageEnvelope.META_GUILD_ID, guildId);
    }
    metadata.put("raw", payload);
    MessageEnvelope inbound = MessageEnvelope.externalInbound("discord", senderId, chatId, content, metadata);
    messageBus.publish(inbound);

  }

}
