package com.agentbot.core.protocol;

public class HandshakeMessage {
  private int v = 1;
  private String keyExchangePub;

  public int getV() {
    return v;
  }

  public void setV(int v) {
    this.v = v;
  }

  public int getVersion() {
    return v & 0xFFFF;
  }

  public int getPadding() {
    return (v >>> 16) & 0xFFFF;
  }

  public void setVersionWithPadding(int version, int paddingHigh16) {
    this.v = ((paddingHigh16 & 0xFFFF) << 16) | (version & 0xFFFF);
  }

  public String getKeyExchangePub() {
    return keyExchangePub;
  }

  public void setKeyExchangePub(String keyExchangePub) {
    this.keyExchangePub = keyExchangePub;
  }
}

