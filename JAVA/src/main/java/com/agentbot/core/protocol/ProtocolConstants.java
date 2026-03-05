package com.agentbot.core.protocol;

public final class ProtocolConstants {
  private ProtocolConstants() {}

  public static final int MAGIC = 0x41474E54; // "AGNT"
  public static final int DEFAULT_PROTOCOL_VERSION = 1;
  public static final String CONTENT_JSON = "application/json";
  public static final String CONTENT_CBOR = "application/cbor";
  public static final String CONTENT_PROTO = "application/x-protobuf";
}
