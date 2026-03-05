package com.agentbot.core.protocol;

public class ProtocolFrame {
  private final byte[] headerBytes;
  private final byte[] payloadBytes;
  private final P2pHeader header;
  private final Object payload;

  public ProtocolFrame(byte[] headerBytes, byte[] payloadBytes, P2pHeader header, Object payload) {
    this.headerBytes = headerBytes;
    this.payloadBytes = payloadBytes;
    this.header = header;
    this.payload = payload;
  }

  public byte[] getHeaderBytes() {
    return headerBytes;
  }

  public byte[] getPayloadBytes() {
    return payloadBytes;
  }

  public P2pHeader getHeader() {
    return header;
  }

  public Object getPayload() {
    return payload;
  }
}
