package com.agentbot.core.protocol;

public class AgentChatNackMessage {
  private String msgId;
  private String traceId;
  private String fromNodeId;
  private String toNodeId;
  private String reason;
  private String status;
  private long timestamp;
  private int ttl;
  private int hopCount;
  private String prevHopNodeId;


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

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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
}
