package com.agentbot.core.protocol;

import java.util.List;
import java.util.Map;

public class VersionMessage {
  private String nodeId;
  private String regionId;
  private long services;
  private int maxPayload;
  private List<String> compression;
  private Map<String, Object> features;
  private List<String> cipherSuites;
  private List<String> contentTypes;
  private FlowControlConfig flowControl;
  private String keyExchangePub;
  private String keyExchangeAlgo;



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

  public long getServices() {
    return services;
  }

  public void setServices(long services) {
    this.services = services;
  }

  public int getMaxPayload() {
    return maxPayload;
  }

  public void setMaxPayload(int maxPayload) {
    this.maxPayload = maxPayload;
  }

  public List<String> getCompression() {
    return compression;
  }

  public void setCompression(List<String> compression) {
    this.compression = compression;
  }

  public Map<String, Object> getFeatures() {
    return features;
  }

  public void setFeatures(Map<String, Object> features) {
    this.features = features;
  }

  public List<String> getCipherSuites() {
    return cipherSuites;
  }

  public void setCipherSuites(List<String> cipherSuites) {
    this.cipherSuites = cipherSuites;
  }

  public List<String> getContentTypes() {
    return contentTypes;
  }

  public void setContentTypes(List<String> contentTypes) {
    this.contentTypes = contentTypes;
  }

  public FlowControlConfig getFlowControl() {
    return flowControl;
  }

  public void setFlowControl(FlowControlConfig flowControl) {
    this.flowControl = flowControl;
  }

  public String getKeyExchangePub() {
    return keyExchangePub;
  }

  public void setKeyExchangePub(String keyExchangePub) {
    this.keyExchangePub = keyExchangePub;
  }

  public String getKeyExchangeAlgo() {
    return keyExchangeAlgo;
  }

  public void setKeyExchangeAlgo(String keyExchangeAlgo) {
    this.keyExchangeAlgo = keyExchangeAlgo;
  }
}


