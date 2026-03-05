package com.agentbot.core.p2p.crypto;

public class SessionKeys {
  private final byte[] sendKey;
  private final byte[] recvKey;
  private final byte[] sessionId;

  public SessionKeys(byte[] sendKey, byte[] recvKey, byte[] sessionId) {
    this.sendKey = sendKey;
    this.recvKey = recvKey;
    this.sessionId = sessionId;
  }

  public byte[] getSendKey() {
    return sendKey;
  }

  public byte[] getRecvKey() {
    return recvKey;
  }

  public byte[] getSessionId() {
    return sessionId;
  }
}
