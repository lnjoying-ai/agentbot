package com.agentbot.core.identity;

import java.time.Instant;

public class NodeIdentity {
  private String privateKeyHex;
  private String publicKeyHex;
  private boolean pubKeyCompressed;
  private String curve;
  private String nodeIdBech32;
  private String createdAt;

  public static NodeIdentity from(
      String privateKeyHex,
      String publicKeyHex,
      boolean pubKeyCompressed,
      String curve,
      String nodeIdBech32
  ) {
    NodeIdentity identity = new NodeIdentity();
    identity.privateKeyHex = privateKeyHex;
    identity.publicKeyHex = publicKeyHex;
    identity.pubKeyCompressed = pubKeyCompressed;
    identity.curve = curve;
    identity.nodeIdBech32 = nodeIdBech32;
    identity.createdAt = Instant.now().toString();
    return identity;
  }

  public String getPrivateKeyHex() {
    return privateKeyHex;
  }

  public void setPrivateKeyHex(String privateKeyHex) {
    this.privateKeyHex = privateKeyHex;
  }

  public String getPublicKeyHex() {
    return publicKeyHex;
  }

  public void setPublicKeyHex(String publicKeyHex) {
    this.publicKeyHex = publicKeyHex;
  }

  public boolean isPubKeyCompressed() {
    return pubKeyCompressed;
  }

  public void setPubKeyCompressed(boolean pubKeyCompressed) {
    this.pubKeyCompressed = pubKeyCompressed;
  }

  public String getCurve() {
    return curve;
  }

  public void setCurve(String curve) {
    this.curve = curve;
  }

  public String getNodeIdBech32() {
    return nodeIdBech32;
  }

  public void setNodeIdBech32(String nodeIdBech32) {
    this.nodeIdBech32 = nodeIdBech32;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }
}
