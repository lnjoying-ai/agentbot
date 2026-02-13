package com.agentbot.core.bus.events;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class InboundMessage {
  private final String channel;
  private final String senderId;
  private final String chatId;
  private final String content;
  private final Instant timestamp;
  private final Map<String, Object> metadata = new HashMap<>();

  public InboundMessage(String channel, String senderId, String chatId, String content) {
    this.channel = channel;
    this.senderId = senderId;
    this.chatId = chatId;
    this.content = content;
    this.timestamp = Instant.now();
  }

  public String sessionKey() {
    return channel + ":" + chatId;
  }

  public String getChannel() {
    return channel;
  }

  public String getSenderId() {
    return senderId;
  }

  public String getChatId() {
    return chatId;
  }

  public String getContent() {
    return content;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }
}
