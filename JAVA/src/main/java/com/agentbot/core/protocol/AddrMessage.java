package com.agentbot.core.protocol;

import java.util.List;

public class AddrMessage {
  private List<NodeInfo> nodes;

  public List<NodeInfo> getNodes() {
    return nodes;
  }

  public void setNodes(List<NodeInfo> nodes) {
    this.nodes = nodes;
  }

  public static class NodeInfo {
    private String nodeId;
    private String regionId;
    private String endpoint;
    private long lastSeen;

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

    public String getEndpoint() {
      return endpoint;
    }

    public void setEndpoint(String endpoint) {
      this.endpoint = endpoint;
    }

    public long getLastSeen() {
      return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
      this.lastSeen = lastSeen;
    }
  }
}
