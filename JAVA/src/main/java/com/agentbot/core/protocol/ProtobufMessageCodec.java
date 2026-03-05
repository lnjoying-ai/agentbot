package com.agentbot.core.protocol;

import com.google.protobuf.Message;

import java.lang.reflect.Method;

public class ProtobufMessageCodec implements MessageCodec {
  @Override
  public byte[] encode(Object obj) throws Exception {
    if (!(obj instanceof Message)) {
      throw new IllegalArgumentException("Payload is not a protobuf Message");
    }
    return ((Message) obj).toByteArray();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T decode(byte[] payload, Class<T> type) throws Exception {
    if (!Message.class.isAssignableFrom(type)) {
      throw new IllegalArgumentException("Type is not a protobuf Message");
    }
    Method parserMethod = type.getMethod("parser");
    Object parser = parserMethod.invoke(null);
    Method parseFrom = parser.getClass().getMethod("parseFrom", byte[].class);
    return (T) parseFrom.invoke(parser, payload);
  }

  @Override
  public String getName() {
    return ProtocolConstants.CONTENT_PROTO;
  }
}
