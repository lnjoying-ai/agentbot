package com.agentbot.core.mesh;

import com.agentbot.core.p2p.P2pConnection;
import com.agentbot.core.p2p.P2pSettings;
import com.agentbot.core.p2p.SkillExchangeService;
import com.agentbot.core.protocol.AckMessage;
import com.agentbot.core.protocol.DataMessage;
import com.agentbot.core.protocol.GetDataMessage;
import com.agentbot.core.protocol.InvItem;
import com.agentbot.core.protocol.InvMessage;
import com.agentbot.core.protocol.MessageType;
import com.agentbot.core.protocol.NackMessage;
import com.agentbot.core.protocol.P2pHeader;
import com.agentbot.core.protocol.ProtocolHandler;
import com.agentbot.core.p2p.P2pAudit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DataExchangeHandler implements ProtocolHandler {
  private static final Logger log = LoggerFactory.getLogger(DataExchangeHandler.class);

  private final InvStore invStore;
  private final RetryManager retryManager;
  private final FlowControl flowControl;
  private final MsgIdWindow msgIdWindow;
  private final SkillExchangeService skillExchangeService;

  public DataExchangeHandler(InvStore invStore, RetryManager retryManager, P2pSettings settings, SkillExchangeService skillExchangeService) {
    this.invStore = invStore;
    this.retryManager = retryManager;
    this.flowControl = new FlowControl(settings.getMaxInFlight());
    this.msgIdWindow = new MsgIdWindow(settings.getIdempotentWindow());
    this.skillExchangeService = skillExchangeService;
  }

  @Override
  public void handle(P2pConnection connection, P2pHeader header, Object payload) {
    if (header == null) return;
    switch (header.getMsgType()) {
      case INV -> handleInv(connection, payload instanceof InvMessage ? (InvMessage) payload : null);
      case GETDATA -> handleGetData(connection, payload instanceof GetDataMessage ? (GetDataMessage) payload : null);
      case DATA -> handleData(connection, payload instanceof DataMessage ? (DataMessage) payload : null);
      default -> {
      }
    }
  }

  private void handleInv(P2pConnection connection, InvMessage message) {
    if (message == null || message.getItems() == null || message.getItems().isEmpty()) return;
    log.debug("Mesh INV received: items={}, remote={}", message.getItems().size(), connection == null ? null : connection.remoteHost());
    List<String> toFetch = invStore.recordInv(message.getItems(), item -> skillExchangeService == null || skillExchangeService.shouldFetch(item));
    if (!toFetch.isEmpty()) {
      GetDataMessage getData = new GetDataMessage();
      int maxPull = flowControl.limit(toFetch.size());
      if (maxPull <= 0) return;
      if (skillExchangeService != null) {
        int budget = skillExchangeService.remainingGetdataBudget(System.currentTimeMillis());
        if (budget <= 0) return;
        maxPull = Math.min(maxPull, budget);
      }
      getData.setDataIds(toFetch.subList(0, maxPull));

      for (String id : getData.getDataIds()) {
        retryManager.register(id);
      }
      log.debug("Mesh GETDATA send: count={}, remote={}", getData.getDataIds().size(), connection == null ? null : connection.remoteHost());
      connection.send(MessageType.GETDATA, getData);
      if (skillExchangeService != null) {
        skillExchangeService.incrementGetdataWindowCount(getData.getDataIds().size(), System.currentTimeMillis());
      }
    }
  }

  private void handleGetData(P2pConnection connection, GetDataMessage request) {
    if (request == null || request.getDataIds() == null) return;
    log.debug("Mesh GETDATA received: count={}, remote={}", request.getDataIds().size(), connection == null ? null : connection.remoteHost());
    for (String dataId : request.getDataIds()) {
      String payload = null;
      InvItem inv = invStore.getInv(dataId);
      boolean skillInv = skillExchangeService != null && skillExchangeService.isSkillItem(inv, dataId);
      if (skillInv) {
        payload = skillExchangeService.buildDataPayload(dataId);
      }
      if (payload == null) {
        payload = invStore.getData(dataId);
      }
      if (payload == null) {
        P2pAudit.warn(log, "mesh-missing-data", "dataId=" + dataId + ", remote=" + (connection == null ? null : connection.remoteHost()));
        NackMessage nack = new NackMessage();

        nack.setMsgId(dataId);
        nack.setReason("missing");
        connection.send(MessageType.NACK, nack, dataId);

        continue;
      }
      DataMessage data = new DataMessage();
      data.setDataId(dataId);
      data.setPayload(payload);
      connection.send(MessageType.DATA, data, data.getDataId());

    }
  }

  private void handleData(P2pConnection connection, DataMessage data) {
    if (data == null || data.getDataId() == null) return;
    log.debug("Mesh DATA received: dataId={}, remote={}", data.getDataId(), connection == null ? null : connection.remoteHost());
    if (!msgIdWindow.markIfNew(data.getDataId())) {
      P2pAudit.info(log, "mesh-data-duplicate", "dataId=" + data.getDataId() + ", remote=" + (connection == null ? null : connection.remoteHost()));
      AckMessage ack = new AckMessage();
      ack.setMsgId(data.getDataId());
      connection.send(MessageType.ACK, ack, data.getDataId());
      return;
    }
    boolean handled = false;
    if (skillExchangeService != null) {
      InvItem inv = invStore.getInv(data.getDataId());
      if (skillExchangeService.isSkillItem(inv, data.getDataId())) {
        handled = skillExchangeService.handleIncomingData(data.getDataId(), data.getPayload());
      }
    }
    if (!handled) {
      invStore.storeData(data.getDataId(), data.getPayload());
    }
    AckMessage ack = new AckMessage();
    ack.setMsgId(data.getDataId());
    connection.send(MessageType.ACK, ack, data.getDataId());
    retryManager.ack(data.getDataId());
    com.agentbot.core.p2p.P2pMetrics.recordAck();
  }
}
