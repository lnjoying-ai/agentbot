package com.agentbot.core.bus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class MessageEnvelope {
  public enum ChannelType {
    EXTERNAL,
    INTERNAL
  }

  public static final String TOPIC_EXTERNAL_INBOUND = "external.inbound";
  public static final String TOPIC_EXTERNAL_OUTBOUND = "external.outbound";
  public static final String TOPIC_INTERNAL_BROADCAST = "internal.broadcast";
  public static final String TOPIC_INTERNAL_DIRECT_PREFIX = "internal.agent.";

  public static final String META_ACCOUNT_ID = "accountId";
  public static final String META_PEER_KIND = "peerKind";
  public static final String META_PEER_ID = "peerId";
  public static final String META_GUILD_ID = "guildId";
  public static final String META_TEAM_ID = "teamId";

  //web
  private final String topic;
  private final String source;
  private final String target;
  private final ChannelType channelType;
  private final String agentId;
  private final String channel;
  private final String chatId;
  private final String senderId;

  //IM 
  private final String accountId;
  private final String peerKind;
  private final String peerId;
  private final String guildId;
  private final String teamId;
  private final String content;
  
  private final Map<String, Object> metadata;

  //P2P
  private final Instant timestamp;
  private final String msgId;
  private final String traceId;
  private final String regionId;
  private final int ttl;
  private final String payloadHash;
  private final int priority;
  private final Map<String, Object> features;


  private MessageEnvelope(Builder builder) {

    this.topic = builder.topic;
    this.source = builder.source;
    this.target = builder.target;
    this.channelType = builder.channelType;
    this.agentId = builder.agentId;
    this.channel = builder.channel;
    this.chatId = builder.chatId;
    this.senderId = builder.senderId;

    this.accountId = builder.accountId;
    this.peerKind = builder.peerKind;
    this.peerId = builder.peerId;
    this.guildId = builder.guildId;
    this.teamId = builder.teamId;
    this.content = builder.content;

    this.metadata = builder.metadata == null ? new HashMap<>() : new HashMap<>(builder.metadata);

    this.timestamp = builder.timestamp == null ? Instant.now() : builder.timestamp;
    this.msgId = builder.msgId;
    this.traceId = builder.traceId;
    this.regionId = builder.regionId;
    this.ttl = builder.ttl;
    this.payloadHash = builder.payloadHash;
    this.priority = builder.priority;
    this.features = builder.features == null ? new HashMap<>() : new HashMap<>(builder.features);

  }

  public static Builder builder() {
    return new Builder();
  }

  public static String topicExternalInbound(String channel) {
    if (channel == null || channel.isBlank()) {
      return TOPIC_EXTERNAL_INBOUND;
    }
    return TOPIC_EXTERNAL_INBOUND + "." + channel;
  }

  public static String topicExternalOutbound(String channel) {
    if (channel == null || channel.isBlank()) {
      return TOPIC_EXTERNAL_OUTBOUND;
    }
    return TOPIC_EXTERNAL_OUTBOUND + "." + channel;
  }

  public static String topicInternalAgent(String agentId) {
    if (agentId == null || agentId.isBlank()) {
      return TOPIC_INTERNAL_DIRECT_PREFIX;
    }
    return TOPIC_INTERNAL_DIRECT_PREFIX + agentId;
  }

  public static MessageEnvelope externalInbound(String channel, String senderId, String chatId, String content, Map<String, Object> metadata) {
    return builder()
        .topic(TOPIC_EXTERNAL_INBOUND)
        .channelType(ChannelType.EXTERNAL)
        .channel(channel)
        .senderId(senderId)
        .chatId(chatId)
        .source(senderId)
        .target(chatId)
        .content(content)
        .metadata(metadata)
        .accountId(readMeta(metadata, META_ACCOUNT_ID))
        .peerKind(readMeta(metadata, META_PEER_KIND))
        .peerId(readMeta(metadata, META_PEER_ID))
        .guildId(readMeta(metadata, META_GUILD_ID))
        .teamId(readMeta(metadata, META_TEAM_ID))
        .build();
  }


  public static MessageEnvelope externalOutbound(String channel, String chatId, String content, Map<String, Object> metadata) {
    return builder()
        .topic(TOPIC_EXTERNAL_OUTBOUND)
        .channelType(ChannelType.EXTERNAL)
        .channel(channel)
        .chatId(chatId)
        .target(chatId)
        .content(content)
        .metadata(metadata)
        .accountId(readMeta(metadata, META_ACCOUNT_ID))
        .peerKind(readMeta(metadata, META_PEER_KIND))
        .peerId(readMeta(metadata, META_PEER_ID))
        .guildId(readMeta(metadata, META_GUILD_ID))
        .teamId(readMeta(metadata, META_TEAM_ID))
        .build();
  }


  public MessageEnvelope withTopic(String newTopic) {
    return builder().from(this).topic(newTopic).build();
  }

  public String getTopic() {
    return topic;
  }

  public String getSource() {
    return source;
  }

  public String getTarget() {
    return target;
  }

  public ChannelType getChannelType() {
    return channelType;
  }

  public String getAgentId() {
    return agentId;
  }

  public String getChannel() {
    return channel;
  }

  public String getChatId() {
    return chatId;
  }

  public String getSenderId() {
    return senderId;
  }

  public String getAccountId() {
    return accountId;
  }

  public String getPeerKind() {
    return peerKind;
  }

  public String getPeerId() {
    return peerId;
  }

  public String getGuildId() {
    return guildId;
  }

  public String getTeamId() {
    return teamId;
  }

  public String getContent() {
    return content;
  }


  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getMsgId() {
    return msgId;
  }

  public String getTraceId() {
    return traceId;
  }

  public String getRegionId() {
    return regionId;
  }

  public int getTtl() {
    return ttl;
  }

  public String getPayloadHash() {
    return payloadHash;
  }

  public int getPriority() {
    return priority;
  }

  public Map<String, Object> getFeatures() {
    return features;
  }

  private static String readMeta(Map<String, Object> metadata, String key) {
    if (metadata == null || key == null) return null;
    Object value = metadata.get(key);
    return value == null ? null : String.valueOf(value);
  }

  public static class Builder {


    private String topic;
    private String source;
    private String target;
    private ChannelType channelType;
    private String agentId;
    private String channel;
    private String chatId;
    private String senderId;
    private String accountId;
    private String peerKind;
    private String peerId;
    private String guildId;
    private String teamId;
    private String content;
    private Map<String, Object> metadata;

    private Instant timestamp;
    private String msgId;
    private String traceId;
    private String regionId;
    private int ttl;
    private String payloadHash;
    private int priority;
    private Map<String, Object> features;


    public Builder from(MessageEnvelope envelope) {
      if (envelope == null) {
        return this;
      }
      this.topic = envelope.topic;
      this.source = envelope.source;
      this.target = envelope.target;
      this.channelType = envelope.channelType;
      this.agentId = envelope.agentId;
      this.channel = envelope.channel;
      this.chatId = envelope.chatId;
      this.senderId = envelope.senderId;
      this.accountId = envelope.accountId;
      this.peerKind = envelope.peerKind;
      this.peerId = envelope.peerId;
      this.guildId = envelope.guildId;
      this.teamId = envelope.teamId;
      this.content = envelope.content;
      this.metadata = envelope.metadata;

      this.timestamp = envelope.timestamp;
      this.msgId = envelope.msgId;
      this.traceId = envelope.traceId;
      this.regionId = envelope.regionId;
      this.ttl = envelope.ttl;
      this.payloadHash = envelope.payloadHash;
      this.priority = envelope.priority;
      this.features = envelope.features;
      return this;
    }


    public Builder topic(String topic) {
      this.topic = topic;
      return this;
    }

    public Builder source(String source) {
      this.source = source;
      return this;
    }

    public Builder target(String target) {
      this.target = target;
      return this;
    }

    public Builder channelType(ChannelType channelType) {
      this.channelType = channelType;
      return this;
    }

    public Builder agentId(String agentId) {
      this.agentId = agentId;
      return this;
    }

    public Builder channel(String channel) {
      this.channel = channel;
      return this;
    }

    public Builder chatId(String chatId) {
      this.chatId = chatId;
      return this;
    }

    public Builder senderId(String senderId) {
      this.senderId = senderId;
      return this;
    }

    public Builder accountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder peerKind(String peerKind) {
      this.peerKind = peerKind;
      return this;
    }

    public Builder peerId(String peerId) {
      this.peerId = peerId;
      return this;
    }

    public Builder guildId(String guildId) {
      this.guildId = guildId;
      return this;
    }

    public Builder teamId(String teamId) {
      this.teamId = teamId;
      return this;
    }

    public Builder content(String content) {
      this.content = content;
      return this;
    }


    public Builder metadata(Map<String, Object> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder msgId(String msgId) {
      this.msgId = msgId;
      return this;
    }

    public Builder traceId(String traceId) {
      this.traceId = traceId;
      return this;
    }

    public Builder regionId(String regionId) {
      this.regionId = regionId;
      return this;
    }

    public Builder ttl(int ttl) {
      this.ttl = ttl;
      return this;
    }

    public Builder payloadHash(String payloadHash) {
      this.payloadHash = payloadHash;
      return this;
    }

    public Builder priority(int priority) {
      this.priority = priority;
      return this;
    }

    public Builder features(Map<String, Object> features) {
      this.features = features;
      return this;
    }

    public MessageEnvelope build() {
      return new MessageEnvelope(this);
    }

  }
}
