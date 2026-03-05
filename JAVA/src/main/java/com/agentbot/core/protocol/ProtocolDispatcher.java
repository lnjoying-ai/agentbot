package com.agentbot.core.protocol;

import com.agentbot.core.p2p.P2pConnection;

import java.util.EnumMap;
import java.util.Map;

public class ProtocolDispatcher {
  private final Map<MessageType, ProtocolHandler> handlers = new EnumMap<>(MessageType.class);

  public void register(MessageType type, ProtocolHandler handler) {
    if (type == null || handler == null) return;
    handlers.put(type, handler);
  }

  public void dispatch(P2pConnection connection, P2pHeader header, Object payload) {
    if (header == null) return;
    ProtocolHandler handler = handlers.get(header.getMsgType());
    if (handler != null) {
      handler.handle(connection, header, payload);
    }
  }
}
