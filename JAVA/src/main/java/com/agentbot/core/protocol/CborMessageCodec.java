package com.agentbot.core.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

public class CborMessageCodec implements MessageCodec {
  private final ObjectMapper mapper;

  public CborMessageCodec() {
    this.mapper = new ObjectMapper(new CBORFactory()).findAndRegisterModules();
  }

  @Override
  public byte[] encode(Object obj) throws Exception {
    return mapper.writeValueAsBytes(obj);
  }

  @Override
  public <T> T decode(byte[] payload, Class<T> type) throws Exception {
    return mapper.readValue(payload, type);
  }

  @Override
  public String getName() {
    return ProtocolConstants.CONTENT_CBOR;
  }
}
