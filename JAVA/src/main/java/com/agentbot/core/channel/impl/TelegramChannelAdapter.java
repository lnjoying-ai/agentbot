package com.agentbot.core.channel.impl;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.bus.MessageBus;
import com.agentbot.core.bus.events.InboundMessage;
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
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelegramChannelAdapter implements ChannelAdapter {
  private static final Logger log = LoggerFactory.getLogger(TelegramChannelAdapter.class);

  private final MessageBus messageBus;
  private final AgentbotProperties.Telegram config;
  private final ObjectMapper mapper = new ObjectMapper();
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private volatile boolean running = false;
  private volatile HttpClient httpClient;
  private long offset = 0L;

  public TelegramChannelAdapter(MessageBus messageBus, AgentbotProperties properties) {
    this.messageBus = messageBus;
    this.config = properties.getChannels().getTelegram();
  }

  @Override
  public String name() {
    return "telegram";
  }

  @Override
  public void start() {
    if (!config.isEnabled()) {
      log.info("telegram channel disabled");
      return;
    }
    if (config.getToken() == null || config.getToken().isBlank()) {
      log.warn("telegram token missing");
      return;
    }
    running = true;
    worker.execute(this::pollLoop);
  }

  @Override
  public void stop() {
    running = false;
    worker.shutdownNow();
  }

  @Override
  public void send(OutboundMessage message) {
    if (!config.isEnabled()) return;
    String token = config.getToken();
    if (token == null || token.isBlank()) return;
    try {
      String url = "https://api.telegram.org/bot" + token + "/sendMessage";
      String payload = mapper.writeValueAsString(
          java.util.Map.of("chat_id", message.getChatId(), "text", message.getContent())
      );
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(payload))
          .build();
      httpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      log.warn("telegram send failed", e);
    }
  }

  private void pollLoop() {
    int pollSeconds = Math.max(1, config.getPollSeconds());
    while (running) {
      try {
        fetchUpdates(pollSeconds);
      } catch (Exception e) {
        log.warn("telegram poll failed", e);
        sleepSeconds(5);
      }
    }
  }

  private void fetchUpdates(int timeoutSeconds) throws Exception {
    String token = config.getToken();
    String url = "https://api.telegram.org/bot" + token + "/getUpdates?timeout=" + timeoutSeconds + "&offset=" + offset;
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .GET()
        .build();
    HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString());
    JsonNode root = mapper.readTree(response.body());
    if (!root.path("ok").asBoolean(false)) return;
    JsonNode result = root.path("result");
    if (!result.isArray()) return;
    for (JsonNode update : result) {
      long updateId = update.path("update_id").asLong();
      if (updateId >= offset) {
        offset = updateId + 1;
      }
      JsonNode message = update.path("message");
      if (message.isMissingNode()) continue;
      String text = message.path("text").asText("");
      if (text.isBlank()) continue;
      String senderId = message.path("from").path("id").asText("");
      String chatId = message.path("chat").path("id").asText("");
      InboundMessage inbound = new InboundMessage("telegram", senderId, chatId, text);
      messageBus.publishInbound(inbound);
    }
  }

  private void sleepSeconds(int seconds) {
    try {
      Thread.sleep(Math.max(1, seconds) * 1000L);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  private HttpClient httpClient() {
    if (httpClient != null) {
      return httpClient;
    }
    synchronized (this) {
      if (httpClient == null) {
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
      }
      return httpClient;
    }
  }
}
