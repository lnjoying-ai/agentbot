package com.agentbot.core.protocol;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class PingMessage {
  private String nonce;
  private long sentAt;

  public String getNonce() {
    return nonce;
  }

  public void setNonce(String nonce) {
    this.nonce = nonce;
  }

  public long getSentAt() {
    return sentAt;
  }

  public void setSentAt(long sentAt) {
    this.sentAt = sentAt;
  }
}

