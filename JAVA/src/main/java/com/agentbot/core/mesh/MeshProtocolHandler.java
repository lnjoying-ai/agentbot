package com.agentbot.core.mesh;

import com.agentbot.core.p2p.P2pConnection;
import com.agentbot.core.protocol.AckMessage;
import com.agentbot.core.protocol.GetDataMessage;
import com.agentbot.core.protocol.MessageType;
import com.agentbot.core.protocol.NackMessage;
import com.agentbot.core.protocol.P2pHeader;
import com.agentbot.core.protocol.ProtocolHandler;

import com.agentbot.core.p2p.P2pAudit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeshProtocolHandler implements ProtocolHandler {
  private static final Logger log = LoggerFactory.getLogger(MeshProtocolHandler.class);

  private final RetryManager retryManager;

  public MeshProtocolHandler(RetryManager retryManager) {
    this.retryManager = retryManager;
  }

  @Override
  public void handle(P2pConnection connection, P2pHeader header, Object payload) {
    if (header == null) return;
    switch (header.getMsgType()) {
      case ACK -> handleAck(payload instanceof AckMessage ? (AckMessage) payload : null);
      case NACK -> handleNack(connection, payload instanceof NackMessage ? (NackMessage) payload : null);
      default -> {
      }
    }
  }

  private void handleAck(AckMessage ack) {
    if (ack == null) return;
    log.debug("Mesh ACK received: msgId={}", ack.getMsgId());
    retryManager.ack(ack.getMsgId());
    com.agentbot.core.p2p.P2pMetrics.recordAck();
  }

  private void handleNack(P2pConnection connection, NackMessage nack) {
    if (nack == null) return;
    log.debug("Mesh NACK received: msgId={}, reason={}, remote={}", nack.getMsgId(), nack.getReason(), connection == null ? null : connection.remoteHost());
    retryManager.nack(nack.getMsgId());
    com.agentbot.core.p2p.P2pMetrics.recordNack();
    if (retryManager.shouldRetry(nack.getMsgId())) {
      GetDataMessage retry = new GetDataMessage();
      retry.setDataIds(java.util.List.of(nack.getMsgId()));
      P2pAudit.info(log, "mesh-retry-getdata", "msgId=" + nack.getMsgId() + ", remote=" + (connection == null ? null : connection.remoteHost()));
      connection.send(MessageType.GETDATA, retry, nack.getMsgId());

      com.agentbot.core.p2p.P2pMetrics.recordRetry();

    }
  }


}

