package com.agentbot.core.bus.events;

import com.agentbot.core.bus.MessageEnvelope;

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
    String agentId = getMetadataString("agentId");
    String channelKey = normalizeToken(channel, "unknown");
    String accountId = normalizeToken(getAccountId(), "default");
    String peerKind = normalizeToken(getPeerKind(), "dm");
    String peerId = normalizeId(getPeerId());
    if (peerId == null || peerId.isBlank()) {
      peerId = normalizeId(chatId);
    }
    String baseKey = channelKey + ":" + accountId + ":" + peerKind + ":" + (peerId == null ? "unknown" : peerId);

    if (agentId == null || agentId.isBlank()) {
      return baseKey;
    }
    return "agent:" + agentId + ":" + baseKey;
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

  public String getAccountId() {
    return getMetadataString(MessageEnvelope.META_ACCOUNT_ID);
  }

  public String getPeerKind() {
    return getMetadataString(MessageEnvelope.META_PEER_KIND);
  }

  public String getPeerId() {
    return getMetadataString(MessageEnvelope.META_PEER_ID);
  }

  public String getGuildId() {
    return getMetadataString(MessageEnvelope.META_GUILD_ID);
  }

  public String getTeamId() {
    return getMetadataString(MessageEnvelope.META_TEAM_ID);
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  private String getMetadataString(String key) {
    if (key == null) return null;
    Object value = metadata.get(key);
    return value == null ? null : String.valueOf(value);
  }

  private String normalizeToken(String value, String fallback) {
    String trimmed = value == null ? "" : value.trim();
    if (!trimmed.isBlank()) return trimmed.toLowerCase();
    return fallback == null ? "" : fallback;
  }

  private String normalizeId(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isBlank() ? null : trimmed.toLowerCase();
  }
}
