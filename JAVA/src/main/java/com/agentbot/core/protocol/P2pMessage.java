package com.agentbot.core.protocol;

public class P2pMessage<T> {
  private final P2pHeader header;
  private final T payload;

  public P2pMessage(P2pHeader header, T payload) {
    this.header = header;
    this.payload = payload;
  }

  public P2pHeader getHeader() {
    return header;
  }

  public T getPayload() {
    return payload;
  }
}   
