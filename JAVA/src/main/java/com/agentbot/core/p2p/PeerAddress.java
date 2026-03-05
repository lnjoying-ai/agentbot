package com.agentbot.core.p2p;

public class PeerAddress {
  private String host;
  private int port;
  private String nodeId;
  private String regionId;
  private long lastSeen;

  private double successRate;
  private int failCount;
  private String source;
  private long latencyMs;

  public PeerAddress() {
  }

  public PeerAddress(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getNodeId() {
    return nodeId;
  }

  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  public String getRegionId() {
    return regionId;
  }

  public void setRegionId(String regionId) {
    this.regionId = regionId;
  }

  public long getLastSeen() {
    return lastSeen;
  }


  public void setLastSeen(long lastSeen) {
    this.lastSeen = lastSeen;
  }

  public double getSuccessRate() {
    return successRate;
  }

  public void setSuccessRate(double successRate) {
    this.successRate = successRate;
  }

  public int getFailCount() {
    return failCount;
  }

  public void setFailCount(int failCount) {
    this.failCount = failCount;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public long getLatencyMs() {
    return latencyMs;
  }

  public void setLatencyMs(long latencyMs) {
    this.latencyMs = latencyMs;
  }

  public String key() {
    String safeNodeId = nodeId == null ? "" : nodeId;
    return host + ":" + port + "|" + safeNodeId;
  }
}
