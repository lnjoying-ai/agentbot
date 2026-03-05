package com.agentbot.core.protocol;

import java.util.Map;
import java.util.UUID;

public class P2pHeader {
  private int magic = ProtocolConstants.MAGIC;
  private int protocolVersion = ProtocolConstants.DEFAULT_PROTOCOL_VERSION;
  private MessageType msgType = MessageType.UNKNOWN;
  private String msgId = UUID.randomUUID().toString();
  private long timestamp = System.currentTimeMillis();
  private int ttl = 0;
  private String traceId;
  private String regionId;
  private String signature;
  private String contentType = ProtocolConstants.CONTENT_JSON;
  private String payloadHash;
  private int priority = 5;
  private Map<String, Object> features;

  public int getMagic() {
    return magic;
  }

  public void setMagic(int magic) {
    this.magic = magic;
  }

  public int getProtocolVersion() {
    return protocolVersion;
  }

  public void setProtocolVersion(int protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  public MessageType getMsgType() {
    return msgType;
  }

  public void setMsgType(MessageType msgType) {
    this.msgType = msgType;
  }

  public String getMsgId() {
    return msgId;
  }

  public void setMsgId(String msgId) {
    this.msgId = msgId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public int getTtl() {
    return ttl;
  }

  public void setTtl(int ttl) {
    this.ttl = ttl;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public String getRegionId() {
    return regionId;
  }

  public void setRegionId(String regionId) {
    this.regionId = regionId;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getPayloadHash() {
    return payloadHash;
  }

  public void setPayloadHash(String payloadHash) {
    this.payloadHash = payloadHash;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public Map<String, Object> getFeatures() {
    return features;
  }

  public void setFeatures(Map<String, Object> features) {
    this.features = features;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final P2pHeader header = new P2pHeader();

    public Builder magic(int magic) {
      header.setMagic(magic);
      return this;
    }

    public Builder protocolVersion(int protocolVersion) {
      header.setProtocolVersion(protocolVersion);
      return this;
    }

    public Builder msgType(MessageType msgType) {
      header.setMsgType(msgType);
      return this;
    }

    public Builder msgId(String msgId) {
      header.setMsgId(msgId);
      return this;
    }

    public Builder timestamp(long timestamp) {
      header.setTimestamp(timestamp);
      return this;
    }

    public Builder ttl(int ttl) {
      header.setTtl(ttl);
      return this;
    }

    public Builder traceId(String traceId) {
      header.setTraceId(traceId);
      return this;
    }

    public Builder regionId(String regionId) {
      header.setRegionId(regionId);
      return this;
    }

    public Builder signature(String signature) {
      header.setSignature(signature);
      return this;
    }

    public Builder contentType(String contentType) {
      header.setContentType(contentType);
      return this;
    }

    public Builder payloadHash(String payloadHash) {
      header.setPayloadHash(payloadHash);
      return this;
    }

    public Builder priority(int priority) {
      header.setPriority(priority);
      return this;
    }

    public Builder features(Map<String, Object> features) {
      header.setFeatures(features);
      return this;
    }

    public P2pHeader build() {
      return header;
    }
  }
}
