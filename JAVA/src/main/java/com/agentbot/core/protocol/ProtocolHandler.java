package com.agentbot.core.protocol;

import com.agentbot.core.p2p.P2pConnection;

public interface ProtocolHandler {
  void handle(P2pConnection connection, P2pHeader header, Object payload);
}
