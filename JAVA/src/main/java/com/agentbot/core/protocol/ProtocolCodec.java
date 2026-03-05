package com.agentbot.core.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;



public class ProtocolCodec {
  private final MessageCodecRegistry codecRegistry;
  private final ObjectMapper headerMapper;

  public ProtocolCodec(MessageCodecRegistry codecRegistry) {
    this.codecRegistry = codecRegistry;
    this.headerMapper = new ObjectMapper().findAndRegisterModules();
  }

  public byte[] encode(P2pHeader header, Object payload) throws Exception {
    byte[] headerBytes = encodeHeader(header);
    byte[] payloadBytes = encodePayload(header, payload);
    return FrameCodec.encode(headerBytes, payloadBytes);
  }

  public byte[] encodeHeader(P2pHeader header) throws Exception {
    return headerMapper.writeValueAsBytes(header);
  }

  public byte[] encodePayload(P2pHeader header, Object payload) throws Exception {
    MessageCodec codec = codecRegistry.resolve(header.getContentType());
    return payload == null ? new byte[0] : codec.encode(payload);
  }

  public P2pHeader decodeHeader(byte[] headerBytes) throws Exception {
    return headerMapper.readValue(headerBytes, P2pHeader.class);
  }

  public Object decodePayload(P2pHeader header, byte[] payloadBytes) throws Exception {
    if (payloadBytes == null || payloadBytes.length == 0) {
      return null;
    }
    MessageCodec codec = codecRegistry.resolve(header.getContentType());
    Class<?> type = resolvePayloadType(header.getMsgType());
    return codec.decode(payloadBytes, type);
  }

  public P2pMessage<?> decode(byte[] frameBytes) throws Exception {
    ProtocolFrame frame = decodeFrame(frameBytes);
    return new P2pMessage<>(frame.getHeader(), frame.getPayload());
  }

  public ProtocolFrame decodeFrame(byte[] frameBytes) throws Exception {
    FrameCodec.Frame frame = FrameCodec.decode(frameBytes);
    P2pHeader header = decodeHeader(frame.headerBytes());
    Object payload = decodePayload(header, frame.payloadBytes());
    return new ProtocolFrame(frame.headerBytes(), frame.payloadBytes(), header, payload);
  }


  private Class<?> resolvePayloadType(MessageType type) {
    return switch (type) {
      case HANDSHAKE -> HandshakeMessage.class;
      case VERSION -> VersionMessage.class;
      case VERACK -> VerackMessage.class;
      case GETADDR -> GetAddrMessage.class;
      case ADDR -> AddrMessage.class;
      case INV -> InvMessage.class;
      case GETDATA -> GetDataMessage.class;
      case DATA -> DataMessage.class;
      case PING -> PingMessage.class;
      case PONG -> PongMessage.class;
      case ACK -> AckMessage.class;
      case NACK -> NackMessage.class;
      case AGENT_CHAT -> AgentChatMessage.class;
      case AGENT_CHAT_ACK -> AgentChatAckMessage.class;
      case AGENT_CHAT_NACK -> AgentChatNackMessage.class;
      default -> Object.class;

    };

  }
}
