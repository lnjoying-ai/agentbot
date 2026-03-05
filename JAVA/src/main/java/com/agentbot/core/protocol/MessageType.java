package com.agentbot.core.protocol;

public enum MessageType {
  HANDSHAKE,
  VERSION,
  VERACK,
  GETADDR,
  ADDR,
  INV,
  GETDATA,
  DATA,
  PING,
  PONG,
  ACK,
  NACK,
  AGENT_CHAT,
  AGENT_CHAT_ACK,
  AGENT_CHAT_NACK,
  UNKNOWN;



  public static MessageType from(String raw) {
    if (raw == null || raw.isBlank()) {
      return UNKNOWN;
    }
    try {
      return MessageType.valueOf(raw.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      return UNKNOWN;
    }
  }
}
