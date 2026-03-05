package com.agentbot.core.protocol;

public interface MessageCodec {
  byte[] encode(Object obj) throws Exception;

  <T> T decode(byte[] payload, Class<T> type) throws Exception;

  String getName();
}
