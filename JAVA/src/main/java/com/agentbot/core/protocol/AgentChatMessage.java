package com.agentbot.core.protocol;

public class AgentChatMessage {
  private String msgId;
  private String traceId;
  private String fromNodeId;
  private String toNodeId;
  private String fromAgentId;
  private String toAgentId;
  private String regionId;
  private long timestamp;
  private int ttl;
  private int hopCount;
  private String prevHopNodeId;
  private String cipher;
  private String payload;
  private String senderPubKey;
  private boolean ackRequired = true;


  public String getMsgId() {
    return msgId;
  }

  public void setMsgId(String msgId) {
    this.msgId = msgId;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public String getFromNodeId() {
    return fromNodeId;
  }

  public void setFromNodeId(String fromNodeId) {
    this.fromNodeId = fromNodeId;
  }

  public String getToNodeId() {
    return toNodeId;
  }

  public void setToNodeId(String toNodeId) {
    this.toNodeId = toNodeId;
  }

  public String getFromAgentId() {
    return fromAgentId;
  }

  public void setFromAgentId(String fromAgentId) {
    this.fromAgentId = fromAgentId;
  }

  public String getToAgentId() {
    return toAgentId;
  }

  public void setToAgentId(String toAgentId) {
    this.toAgentId = toAgentId;
  }

  public String getRegionId() {
    return regionId;
  }

  public void setRegionId(String regionId) {
    this.regionId = regionId;
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

  public int getHopCount() {
    return hopCount;
  }

  public void setHopCount(int hopCount) {
    this.hopCount = hopCount;
  }

  public String getPrevHopNodeId() {
    return prevHopNodeId;
  }

  public void setPrevHopNodeId(String prevHopNodeId) {
    this.prevHopNodeId = prevHopNodeId;
  }

  public String getCipher() {
    return cipher;
  }

  public void setCipher(String cipher) {
    this.cipher = cipher;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public boolean isAckRequired() {
    return ackRequired;
  }

  public void setAckRequired(boolean ackRequired) {
    this.ackRequired = ackRequired;
  }

  public String getSenderPubKey() {
    return senderPubKey;
  }

  public void setSenderPubKey(String senderPubKey) {
    this.senderPubKey = senderPubKey;
  }

}
