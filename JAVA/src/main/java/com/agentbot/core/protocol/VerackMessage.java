package com.agentbot.core.protocol;

public class VerackMessage {
  private String nodeId;
  private boolean accepted;
  private String reason;
  private String selectedCipher;
  private String selectedContentType;
  private String selectedCompression;
  private int flowWindow;
  private int maxInFlight;
  private String identityPubKey;
  private String identitySignature;


  public String getNodeId() {
    return nodeId;
  }

  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  public boolean isAccepted() {
    return accepted;
  }

  public void setAccepted(boolean accepted) {
    this.accepted = accepted;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getSelectedCipher() {
    return selectedCipher;
  }

  public void setSelectedCipher(String selectedCipher) {
    this.selectedCipher = selectedCipher;
  }

  public String getSelectedContentType() {
    return selectedContentType;
  }

  public void setSelectedContentType(String selectedContentType) {
    this.selectedContentType = selectedContentType;
  }

  public String getSelectedCompression() {
    return selectedCompression;
  }

  public void setSelectedCompression(String selectedCompression) {
    this.selectedCompression = selectedCompression;
  }

  public int getFlowWindow() {
    return flowWindow;
  }

  public void setFlowWindow(int flowWindow) {
    this.flowWindow = flowWindow;
  }

  public int getMaxInFlight() {
    return maxInFlight;
  }

  public void setMaxInFlight(int maxInFlight) {
    this.maxInFlight = maxInFlight;
  }

  public String getIdentityPubKey() {
    return identityPubKey;
  }


  public void setIdentityPubKey(String identityPubKey) {
    this.identityPubKey = identityPubKey;
  }

  public String getIdentitySignature() {
    return identitySignature;
  }

  public void setIdentitySignature(String identitySignature) {
    this.identitySignature = identitySignature;
  }
}

