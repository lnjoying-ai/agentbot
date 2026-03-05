package com.agentbot.core.p2p;

import com.agentbot.core.agent.AgentRegistry;
import com.agentbot.core.bus.ExternalMessageBus;
import com.agentbot.core.bus.MessageEnvelope;
import com.agentbot.core.events.SystemEvent;
import com.agentbot.core.events.SystemEventBus;
import com.agentbot.core.identity.NodeIdentityService;
import com.agentbot.core.mesh.MsgIdWindow;
import com.agentbot.core.p2p.crypto.EcdhKeyAgreement;
import com.agentbot.core.p2p.crypto.Hkdf;
import com.agentbot.core.protocol.AgentChatAckMessage;
import com.agentbot.core.protocol.AgentChatMessage;
import com.agentbot.core.protocol.AgentChatNackMessage;
import com.agentbot.core.protocol.MessageType;
import com.agentbot.core.protocol.P2pHeader;
import com.agentbot.core.protocol.ProtocolHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class P2pChatHandler implements ProtocolHandler {
  private static final Logger log = LoggerFactory.getLogger(P2pChatHandler.class);
  private static final String CHANNEL_P2P = "p2p";
  private static final String CHAT_ID_PREFIX = "p2p:";
  private static final String CHAT_ID_EMPTY = "-";
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String CHAT_CIPHER = "CHACHA20_POLY1305";
  private static final byte[] HKDF_INFO_KEY = "agentbot-chat-key".getBytes(StandardCharsets.UTF_8);
  private static final byte[] HKDF_INFO_NONCE = "agentbot-chat-nonce".getBytes(StandardCharsets.UTF_8);

  private final P2pSettings settings;

  private final ConnectionRegistry registry;
  private final ExternalMessageBus messageBus;
  private final AgentRegistry agentRegistry;
  private final SystemEventBus eventBus;
  private final MsgIdWindow chatDedup;

  private final MsgIdWindow ackDedup;
  private final QpsLimiter rateLimiter;
  private final Map<String, PendingChat> pending = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "p2p-chat-retry");
    t.setDaemon(true);
    return t;
  });

  public P2pChatHandler(P2pSettings settings,
                        ConnectionRegistry registry,
                        ExternalMessageBus messageBus,
                        AgentRegistry agentRegistry,
                        SystemEventBus eventBus) {
    this.settings = settings;
    this.registry = registry;
    this.messageBus = messageBus;
    this.agentRegistry = agentRegistry;
    this.eventBus = eventBus;
    this.chatDedup = new MsgIdWindow(settings == null ? 0 : settings.getChatDedupWindowMs());
    this.ackDedup = new MsgIdWindow(settings == null ? 0 : settings.getChatDedupWindowMs());
    this.rateLimiter = new QpsLimiter(settings == null ? 0 : settings.getChatRateLimitQps());
    scheduleRetries();
  }


  public void start() {
    if (messageBus == null) return;
    messageBus.subscribe(MessageEnvelope.topicExternalOutbound(CHANNEL_P2P), this::handleOutbound);
  }

  @Override
  public void handle(P2pConnection connection, P2pHeader header, Object payload) {
    if (header == null) return;
    switch (header.getMsgType()) {
      case AGENT_CHAT -> handleChat(connection, payload instanceof AgentChatMessage ? (AgentChatMessage) payload : null);
      case AGENT_CHAT_ACK -> handleAck(connection, payload instanceof AgentChatAckMessage ? (AgentChatAckMessage) payload : null);
      case AGENT_CHAT_NACK -> handleNack(connection, payload instanceof AgentChatNackMessage ? (AgentChatNackMessage) payload : null);
      default -> {
      }
    }
  }

  private void handleOutbound(MessageEnvelope envelope) {
    if (envelope == null) return;
    String chatId = envelope.getChatId();
    Map<String, Object> metadata = envelope.getMetadata() == null ? new HashMap<>() : envelope.getMetadata();
    Target target = resolveTarget(chatId, metadata);
    String msgId = stringValue(metadata, "msgId");
    if (msgId == null || msgId.isBlank()) {
      msgId = UUID.randomUUID().toString();
      metadata.put("msgId", msgId);
    }
    String traceId = stringValue(metadata, "traceId");
    String fromAgentId = stringValue(metadata, "fromAgentId");
    if (fromAgentId == null) {
      fromAgentId = stringValue(metadata, "agentId");
      if (fromAgentId != null) {
        metadata.put("fromAgentId", fromAgentId);
      }
    }

    AgentChatMessage message = new AgentChatMessage();
    message.setMsgId(msgId);
    message.setTraceId(traceId);
    message.setFromNodeId(settings == null ? null : settings.getNodeId());
    message.setFromAgentId(fromAgentId);
    message.setToNodeId(target == null ? null : target.nodeId);
    message.setToAgentId(target == null ? null : target.agentId);
    message.setRegionId(settings == null ? null : settings.getRegionId());
    message.setTimestamp(System.currentTimeMillis());
    message.setTtl(settings == null ? 0 : settings.getChatTtlDefault());
    message.setHopCount(0);
    message.setPrevHopNodeId(settings == null ? null : settings.getNodeId());
    message.setCipher(CHAT_CIPHER);
    message.setPayload(envelope.getContent());
    String outboundContent = message.getPayload();
    message.setSenderPubKey(settings == null ? null : settings.getIdentityPublicKeyHex());
    message.setAckRequired(booleanValue(metadata, "ackRequired", true));


    if (!validateRequiredFields(message)) {

      P2pAudit.warn(log, "chat-outbound-missing-required", "msgId=" + message.getMsgId());
      return;
    }

    if (!validatePayload(message)) {
      P2pAudit.warn(log, "chat-outbound-invalid", "msgId=" + message.getMsgId());
      return;
    }

    String receiverPubKey = resolveReceiverPubKey(message, metadata);
    if (!encryptOutbound(message, receiverPubKey)) {
      P2pAudit.warn(log, "chat-outbound-encrypt-failed", "msgId=" + message.getMsgId());
      return;
    }

    boolean sent = sendChatToPeers(null, message, true);

    publishOutboundEvent(message, sent, outboundContent);

    if (sent && message.isAckRequired() && settings != null && settings.isChatAckEnabled()) {
      registerPending(message);
    }
  }


  private void handleChat(P2pConnection connection, AgentChatMessage message) {
    if (message == null) return;
    normalizeChatSource(connection, message);

    if (!validateRequiredFields(message)) {
      P2pAudit.warn(log, "chat-missing-required", "msgId=" + message.getMsgId());
      maybeSendNack(connection, message, "missing-required-fields");
      return;
    }

    if (!rateLimiter.allow()) {
      P2pAudit.warn(log, "chat-rate-limited", "msgId=" + message.getMsgId());
      maybeSendNack(connection, message, "rate-limited");
      return;
    }


    if (!validatePayload(message)) {
      P2pAudit.warn(log, "chat-payload-invalid", "msgId=" + message.getMsgId());
      maybeSendNack(connection, message, "payload-invalid");
      return;
    }

    if (!validateNodeBinding(message)) {
      P2pAudit.warn(log, "chat-nodeid-mismatch", "msgId=" + message.getMsgId());
      maybeSendNack(connection, message, "nodeid-mismatch");
      return;
    }

    int ttl = effectiveTtl(message.getTtl());

    if (ttl <= 0) {
      P2pAudit.warn(log, "chat-ttl-expired", "msgId=" + message.getMsgId());
      maybeSendNack(connection, message, "ttl-expired");
      return;
    }
    message.setTtl(ttl);

    if (!chatDedup.markIfNew(dedupKey("chat", message.getMsgId(), message.getFromNodeId()))) {
      maybeSendAck(connection, message);
      return;
    }

    if (shouldDeliverLocal(message)) {
      DecryptedPayload decrypted = decryptInbound(message);
      if (decrypted == null) {
        P2pAudit.warn(log, "chat-decrypt-failed", "msgId=" + message.getMsgId());
        maybeSendNack(connection, message, "decrypt-failed");
        return;
      }
      message.setPayload(decrypted.payload);
      if (message.getToAgentId() != null && !message.getToAgentId().isBlank()
          && (agentRegistry == null || !agentRegistry.hasAgent(message.getToAgentId()))) {
        P2pAudit.warn(log, "chat-agent-not-found", "msgId=" + message.getMsgId() + ", agentId=" + message.getToAgentId());
        maybeSendNack(connection, message, "agent-not-found");
        return;
      }
      deliverToLocal(message);
      maybeSendAck(connection, message);
    }



    if (shouldForward(message)) {
      AgentChatMessage forward = copyChat(message);
      updateForwardState(forward);
      sendChatToPeers(connection, forward, false);
    }
  }

  private void handleAck(P2pConnection connection, AgentChatAckMessage ack) {
    if (ack == null || ack.getMsgId() == null || ack.getMsgId().isBlank()) return;
    normalizeAckSource(connection, ack);

    if (!ackDedup.markIfNew(dedupKey("ack", ack.getMsgId(), ack.getFromNodeId()))) {
      return;
    }

    if (isTargetLocal(ack.getToNodeId())) {
      PendingChat pendingChat = pending.get(ack.getMsgId());
      publishAckEvent(ack, pendingChat);
      pending.remove(ack.getMsgId());
      com.agentbot.core.p2p.P2pMetrics.recordAck();
      return;
    }


    if (shouldForwardAck(ack)) {
      AgentChatAckMessage forward = copyAck(ack);
      updateForwardState(forward);
      sendAckToPeers(connection, forward);
    }
  }

  private void handleNack(P2pConnection connection, AgentChatNackMessage nack) {
    if (nack == null || nack.getMsgId() == null || nack.getMsgId().isBlank()) return;
    normalizeNackSource(connection, nack);

    if (!ackDedup.markIfNew(dedupKey("nack", nack.getMsgId(), nack.getFromNodeId()))) {
      return;
    }

    if (isTargetLocal(nack.getToNodeId())) {
      com.agentbot.core.p2p.P2pMetrics.recordNack();
      PendingChat pendingChat = pending.get(nack.getMsgId());
      publishNackEvent(nack, pendingChat);
      if (pendingChat != null && pendingChat.shouldRetry(settings)) {
        resendPending(pendingChat);
      } else {
        pending.remove(nack.getMsgId());
      }
      return;
    }


    if (shouldForwardAck(nack)) {
      AgentChatNackMessage forward = copyNack(nack);
      updateForwardState(forward);
      sendNackToPeers(connection, forward);
    }
  }

  private boolean shouldDeliverLocal(AgentChatMessage message) {
    if (message == null) return false;
    if (message.getToNodeId() == null || message.getToNodeId().isBlank()) return true;
    return settings != null && message.getToNodeId().equals(settings.getNodeId());
  }

  private boolean shouldForward(AgentChatMessage message) {
    if (message == null) return false;
    int ttl = effectiveTtl(message.getTtl());
    if (ttl <= 1) return false;
    if (message.getToNodeId() == null || message.getToNodeId().isBlank()) return true;
    return settings == null || !message.getToNodeId().equals(settings.getNodeId());
  }

  private boolean shouldForwardAck(AgentChatAckMessage ack) {
    if (ack == null) return false;
    return effectiveTtl(ack.getTtl()) > 1;
  }

  private boolean shouldForwardAck(AgentChatNackMessage nack) {
    if (nack == null) return false;
    return effectiveTtl(nack.getTtl()) > 1;
  }

  private void deliverToLocal(AgentChatMessage message) {
    if (messageBus == null) return;
    String sender = message.getFromAgentId();
    if (sender == null || sender.isBlank()) {
      sender = message.getFromNodeId();
    }
    String chatId = buildChatId(message.getFromNodeId(), message.getFromAgentId());

    Map<String, Object> metadata = new java.util.HashMap<>();
    metadata.put("agentId", resolveLocalAgentId(message.getToAgentId()));
    metadata.put("p2pFromNodeId", message.getFromNodeId());
    metadata.put("p2pFromAgentId", message.getFromAgentId());
    metadata.put("p2pToNodeId", message.getToNodeId());
    metadata.put("p2pToAgentId", message.getToAgentId());
    metadata.put("msgId", message.getMsgId());
    metadata.put("traceId", message.getTraceId());
    metadata.put("regionId", message.getRegionId());
    metadata.put("ttl", message.getTtl());
    metadata.put("hopCount", message.getHopCount());
    metadata.put("prevHopNodeId", message.getPrevHopNodeId());

    MessageEnvelope inbound = MessageEnvelope.externalInbound(CHANNEL_P2P, sender, chatId, message.getPayload(), metadata);
    messageBus.publish(inbound);
    publishInboundEvent(message);
  }


  private boolean sendChatToPeers(P2pConnection source, AgentChatMessage message, boolean origin) {
    if (message == null || registry == null) return false;
    int ttl = effectiveTtl(message.getTtl());
    if (!origin) {
      ttl -= 1;
    }
    if (ttl <= 0) return false;

    message.setTtl(ttl);
    List<P2pConnection> targets = selectTargets(source, message.getToNodeId(), message.getPrevHopNodeId());
    if (targets.isEmpty()) return false;

    int fanout = settings == null ? targets.size() : settings.getChatFanout();
    if (fanout <= 0 || fanout >= targets.size()) {
      for (P2pConnection conn : targets) {
        sendToConnection(conn, MessageType.AGENT_CHAT, message, message.getMsgId());
      }
      return true;
    }

    Collections.shuffle(targets);
    for (int i = 0; i < fanout; i++) {
      sendToConnection(targets.get(i), MessageType.AGENT_CHAT, message, message.getMsgId());
    }
    return true;
  }

  private void sendAckToPeers(P2pConnection source, AgentChatAckMessage ack) {
    if (ack == null || registry == null) return;
    int ttl = effectiveTtl(ack.getTtl()) - 1;
    if (ttl <= 0) return;
    ack.setTtl(ttl);
    List<P2pConnection> targets = selectTargets(source, ack.getToNodeId(), ack.getPrevHopNodeId());
    if (targets.isEmpty()) return;

    int fanout = settings == null ? targets.size() : settings.getChatFanout();
    if (fanout <= 0 || fanout >= targets.size()) {
      for (P2pConnection conn : targets) {
        sendToConnection(conn, MessageType.AGENT_CHAT_ACK, ack, ack.getMsgId());
      }
      return;
    }

    Collections.shuffle(targets);
    for (int i = 0; i < fanout; i++) {
      sendToConnection(targets.get(i), MessageType.AGENT_CHAT_ACK, ack, ack.getMsgId());
    }
  }

  private void sendNackToPeers(P2pConnection source, AgentChatNackMessage nack) {
    if (nack == null || registry == null) return;
    int ttl = effectiveTtl(nack.getTtl()) - 1;
    if (ttl <= 0) return;
    nack.setTtl(ttl);
    List<P2pConnection> targets = selectTargets(source, nack.getToNodeId(), nack.getPrevHopNodeId());
    if (targets.isEmpty()) return;

    int fanout = settings == null ? targets.size() : settings.getChatFanout();
    if (fanout <= 0 || fanout >= targets.size()) {
      for (P2pConnection conn : targets) {
        sendToConnection(conn, MessageType.AGENT_CHAT_NACK, nack, nack.getMsgId());
      }
      return;
    }

    Collections.shuffle(targets);
    for (int i = 0; i < fanout; i++) {
      sendToConnection(targets.get(i), MessageType.AGENT_CHAT_NACK, nack, nack.getMsgId());
    }
  }

  private void maybeSendAck(P2pConnection connection, AgentChatMessage message) {
    if (connection == null || message == null || !message.isAckRequired()) return;
    if (settings == null || !settings.isChatAckEnabled()) return;
    AgentChatAckMessage ack = new AgentChatAckMessage();
    ack.setMsgId(message.getMsgId());
    ack.setTraceId(message.getTraceId());
    ack.setFromNodeId(settings.getNodeId());
    ack.setToNodeId(message.getFromNodeId());
    ack.setTimestamp(System.currentTimeMillis());
    ack.setTtl(settings.getChatTtlDefault());
    ack.setHopCount(0);
    ack.setPrevHopNodeId(settings.getNodeId());
    sendToConnection(connection, MessageType.AGENT_CHAT_ACK, ack, ack.getMsgId());
  }

  private void maybeSendNack(P2pConnection connection, AgentChatMessage message, String reason) {
    if (connection == null || message == null) return;
    if (settings == null || !settings.isChatNackEnabled()) return;
    AgentChatNackMessage nack = new AgentChatNackMessage();
    nack.setMsgId(message.getMsgId());
    nack.setTraceId(message.getTraceId());
    nack.setFromNodeId(settings.getNodeId());
    nack.setToNodeId(message.getFromNodeId());
    nack.setReason(reason);
    nack.setTimestamp(System.currentTimeMillis());
    nack.setTtl(settings.getChatTtlDefault());
    nack.setHopCount(0);
    nack.setPrevHopNodeId(settings.getNodeId());
    sendToConnection(connection, MessageType.AGENT_CHAT_NACK, nack, nack.getMsgId());
  }

  private void sendToConnection(P2pConnection connection, MessageType type, Object payload, String msgId) {
    if (connection == null) return;
    if (!connection.isHandshakeComplete()) return;
    try {
      connection.send(type, payload, msgId);
    } catch (Exception ex) {
      log.debug("P2P chat send failed: type={}, remote={}", type, connection.remoteHost(), ex);
    }
  }

  private void updateForwardState(AgentChatMessage message) {
    if (message == null) return;
    message.setHopCount(message.getHopCount() + 1);
    message.setPrevHopNodeId(settings == null ? null : settings.getNodeId());
  }

  private void updateForwardState(AgentChatAckMessage message) {
    if (message == null) return;
    message.setHopCount(message.getHopCount() + 1);
    message.setPrevHopNodeId(settings == null ? null : settings.getNodeId());
  }

  private void updateForwardState(AgentChatNackMessage message) {
    if (message == null) return;
    message.setHopCount(message.getHopCount() + 1);
    message.setPrevHopNodeId(settings == null ? null : settings.getNodeId());
  }

  private List<P2pConnection> selectTargets(P2pConnection source, String targetNodeId, String prevHopNodeId) {
    if (registry == null) return List.of();
    if (targetNodeId != null && !targetNodeId.isBlank()) {
      P2pConnection direct = registry.findByNodeId(targetNodeId);
      if (direct != null && direct != source && direct.isHandshakeComplete()) {
        return List.of(direct);
      }
    }

    List<P2pConnection> candidates = new ArrayList<>();
    for (P2pConnection conn : registry.listConnections()) {
      if (conn == null || conn == source) continue;
      if (!conn.isHandshakeComplete()) continue;
      String remoteNodeId = conn.getRemoteNodeId();
      if (prevHopNodeId != null && !prevHopNodeId.isBlank() && prevHopNodeId.equals(remoteNodeId)) {
        continue;
      }
      candidates.add(conn);
    }
    return candidates;
  }

  private void normalizeChatSource(P2pConnection connection, AgentChatMessage message) {
    if (message == null || connection == null) return;
    if (message.getFromNodeId() == null || message.getFromNodeId().isBlank()) {
      message.setFromNodeId(connection.getRemoteNodeId());
    }
    if (message.getPrevHopNodeId() == null || message.getPrevHopNodeId().isBlank()) {
      message.setPrevHopNodeId(connection.getRemoteNodeId());
    }
  }

  private void normalizeAckSource(P2pConnection connection, AgentChatAckMessage message) {
    if (message == null || connection == null) return;
    if (message.getFromNodeId() == null || message.getFromNodeId().isBlank()) {
      message.setFromNodeId(connection.getRemoteNodeId());
    }
    if (message.getPrevHopNodeId() == null || message.getPrevHopNodeId().isBlank()) {
      message.setPrevHopNodeId(connection.getRemoteNodeId());
    }
  }

  private void normalizeNackSource(P2pConnection connection, AgentChatNackMessage message) {
    if (message == null || connection == null) return;
    if (message.getFromNodeId() == null || message.getFromNodeId().isBlank()) {
      message.setFromNodeId(connection.getRemoteNodeId());
    }
    if (message.getPrevHopNodeId() == null || message.getPrevHopNodeId().isBlank()) {
      message.setPrevHopNodeId(connection.getRemoteNodeId());
    }
  }

  private void publishOutboundEvent(AgentChatMessage message, boolean sent, String content) {
    if (message == null) return;
    Map<String, Object> payload = new HashMap<>();
    payload.put("direction", "outbound");
    payload.put("channel", CHANNEL_P2P);
    payload.put("chatId", buildChatId(message.getToNodeId(), message.getToAgentId()));
    payload.put("content", content);
    payload.put("timestamp", message.getTimestamp());

    payload.put("msgId", message.getMsgId());
    payload.put("traceId", message.getTraceId());
    payload.put("fromNodeId", message.getFromNodeId());
    payload.put("fromAgentId", message.getFromAgentId());
    payload.put("toNodeId", message.getToNodeId());
    payload.put("toAgentId", message.getToAgentId());
    payload.put("status", sent ? "SENT" : "FAILED");
    publishChatEvent("p2p.chat.outbound", payload);
  }

  private void publishInboundEvent(AgentChatMessage message) {
    if (message == null) return;
    Map<String, Object> payload = new HashMap<>();
    payload.put("direction", "inbound");
    payload.put("channel", CHANNEL_P2P);
    payload.put("chatId", buildChatId(message.getFromNodeId(), message.getFromAgentId()));
    payload.put("content", message.getPayload());
    payload.put("timestamp", message.getTimestamp());
    payload.put("msgId", message.getMsgId());
    payload.put("traceId", message.getTraceId());
    payload.put("fromNodeId", message.getFromNodeId());
    payload.put("fromAgentId", message.getFromAgentId());
    payload.put("toNodeId", message.getToNodeId());
    payload.put("toAgentId", message.getToAgentId());
    payload.put("status", "RECEIVED");
    publishChatEvent("p2p.chat.inbound", payload);
  }

  private void publishAckEvent(AgentChatAckMessage ack, PendingChat pendingChat) {
    if (ack == null) return;
    Map<String, Object> payload = new HashMap<>();
    payload.put("msgId", ack.getMsgId());
    payload.put("traceId", ack.getTraceId());
    payload.put("fromNodeId", ack.getFromNodeId());
    payload.put("toNodeId", ack.getToNodeId());
    payload.put("timestamp", ack.getTimestamp());
    payload.put("status", "ACKED");
    if (pendingChat != null && pendingChat.message != null) {
      payload.put("chatId", buildChatId(pendingChat.message.getToNodeId(), pendingChat.message.getToAgentId()));
      payload.put("toAgentId", pendingChat.message.getToAgentId());
      payload.put("fromAgentId", pendingChat.message.getFromAgentId());
    }
    publishChatEvent("p2p.chat.ack", payload);
  }

  private void publishNackEvent(AgentChatNackMessage nack, PendingChat pendingChat) {
    if (nack == null) return;
    Map<String, Object> payload = new HashMap<>();
    payload.put("msgId", nack.getMsgId());
    payload.put("traceId", nack.getTraceId());
    payload.put("fromNodeId", nack.getFromNodeId());
    payload.put("toNodeId", nack.getToNodeId());
    payload.put("timestamp", nack.getTimestamp());
    payload.put("status", "NACKED");
    payload.put("reason", nack.getReason());
    if (pendingChat != null && pendingChat.message != null) {
      payload.put("chatId", buildChatId(pendingChat.message.getToNodeId(), pendingChat.message.getToAgentId()));
      payload.put("toAgentId", pendingChat.message.getToAgentId());
      payload.put("fromAgentId", pendingChat.message.getFromAgentId());
    }
    publishChatEvent("p2p.chat.nack", payload);
  }

  private void publishChatEvent(String type, Map<String, Object> payload) {
    if (eventBus == null) return;
    eventBus.publish(new SystemEvent(type, payload));
  }

  private boolean validatePayload(AgentChatMessage message) {

    if (message == null) return false;
    if (message.getMsgId() == null || message.getMsgId().isBlank()) return false;
    if (message.getPayload() == null) return false;
    if (settings == null || settings.getChatMaxPayloadBytes() <= 0) return true;
    int size = message.getPayload().getBytes(StandardCharsets.UTF_8).length;
    return size <= settings.getChatMaxPayloadBytes();
  }

  private boolean validateRequiredFields(AgentChatMessage message) {
    if (message == null) return false;
    if (message.getFromNodeId() == null || message.getFromNodeId().isBlank()) return false;
    if (message.getToNodeId() == null || message.getToNodeId().isBlank()) return false;
    if (message.getToAgentId() == null || message.getToAgentId().isBlank()) return false;
    if (message.getTimestamp() <= 0) return false;
    if (message.getTtl() <= 0) return false;
    if (message.getSenderPubKey() == null || message.getSenderPubKey().isBlank()) return false;
    if (message.getCipher() == null || message.getCipher().isBlank()) return false;
    return message.getHopCount() >= 0;
  }


  private boolean isTargetLocal(String nodeId) {
    if (nodeId == null || nodeId.isBlank()) return true;
    return settings != null && nodeId.equals(settings.getNodeId());
  }

  private AgentChatMessage copyChat(AgentChatMessage message) {
    AgentChatMessage copy = new AgentChatMessage();
    copy.setMsgId(message.getMsgId());
    copy.setTraceId(message.getTraceId());
    copy.setFromNodeId(message.getFromNodeId());
    copy.setToNodeId(message.getToNodeId());
    copy.setFromAgentId(message.getFromAgentId());
    copy.setToAgentId(message.getToAgentId());
    copy.setRegionId(message.getRegionId());
    copy.setTimestamp(message.getTimestamp());
    copy.setTtl(message.getTtl());
    copy.setHopCount(message.getHopCount());
    copy.setPrevHopNodeId(message.getPrevHopNodeId());
    copy.setCipher(message.getCipher());
    copy.setPayload(message.getPayload());
    copy.setSenderPubKey(message.getSenderPubKey());
    copy.setAckRequired(message.isAckRequired());
    return copy;
  }


  private AgentChatAckMessage copyAck(AgentChatAckMessage ack) {
    AgentChatAckMessage copy = new AgentChatAckMessage();
    copy.setMsgId(ack.getMsgId());
    copy.setTraceId(ack.getTraceId());
    copy.setFromNodeId(ack.getFromNodeId());
    copy.setToNodeId(ack.getToNodeId());
    copy.setStatus(ack.getStatus());
    copy.setTimestamp(ack.getTimestamp());

    copy.setTtl(ack.getTtl());
    copy.setHopCount(ack.getHopCount());
    copy.setPrevHopNodeId(ack.getPrevHopNodeId());
    return copy;
  }

  private AgentChatNackMessage copyNack(AgentChatNackMessage nack) {
    AgentChatNackMessage copy = new AgentChatNackMessage();
    copy.setMsgId(nack.getMsgId());
    copy.setTraceId(nack.getTraceId());
    copy.setFromNodeId(nack.getFromNodeId());
    copy.setToNodeId(nack.getToNodeId());
    copy.setReason(nack.getReason());
    copy.setStatus(nack.getStatus());
    copy.setTimestamp(nack.getTimestamp());

    copy.setTtl(nack.getTtl());
    copy.setHopCount(nack.getHopCount());
    copy.setPrevHopNodeId(nack.getPrevHopNodeId());
    return copy;
  }

  private void registerPending(AgentChatMessage message) {
    if (message == null || message.getMsgId() == null || message.getMsgId().isBlank()) return;
    pending.put(message.getMsgId(), new PendingChat(message));
  }

  private void resendPending(PendingChat pendingChat) {
    if (pendingChat == null) return;
    pendingChat.markAttempt();
    sendChatToPeers(null, pendingChat.message, true);
  }

  private void scheduleRetries() {
    if (settings == null) return;
    if (settings.getChatAckTimeoutMs() <= 0 || settings.getChatRetryMax() <= 0) return;
    scheduler.scheduleAtFixedRate(this::checkRetries, settings.getChatAckTimeoutMs(), settings.getChatAckTimeoutMs(), TimeUnit.MILLISECONDS);
  }

  private void checkRetries() {
    long now = System.currentTimeMillis();
    for (PendingChat pendingChat : pending.values()) {
      if (pendingChat == null) continue;
      if (!pendingChat.shouldRetry(settings)) {
        pending.remove(pendingChat.message.getMsgId());
        continue;
      }
      if (now - pendingChat.lastSentAt >= settings.getChatAckTimeoutMs()) {
        resendPending(pendingChat);
      }
    }
  }

  private int effectiveTtl(int ttl) {
    if (ttl > 0) return ttl;
    return settings == null ? 0 : settings.getChatTtlDefault();
  }

  private String resolveLocalAgentId(String targetAgentId) {
    if (targetAgentId == null || targetAgentId.isBlank()) return null;
    if (agentRegistry == null || !agentRegistry.hasAgent(targetAgentId)) return null;
    return targetAgentId;
  }

  private Target resolveTarget(String chatId, Map<String, Object> metadata) {
    String nodeId = stringValue(metadata, "toNodeId");
    String agentId = stringValue(metadata, "toAgentId");
    if ((nodeId == null || nodeId.isBlank()) && chatId != null && chatId.startsWith(CHAT_ID_PREFIX)) {
      String raw = chatId.substring(CHAT_ID_PREFIX.length());
      String[] parts = raw.split(":", 2);
      if (parts.length > 0 && !parts[0].isBlank() && !CHAT_ID_EMPTY.equals(parts[0])) {
        nodeId = parts[0];
      }
      if (parts.length == 2 && !parts[1].isBlank() && !CHAT_ID_EMPTY.equals(parts[1])) {
        agentId = parts[1];
      }
    }
    if ((nodeId == null || nodeId.isBlank()) && (agentId == null || agentId.isBlank())) {
      return null;
    }
    return new Target(nodeId, agentId);
  }

  private String buildChatId(String nodeId, String agentId) {
    String safeNode = nodeId == null || nodeId.isBlank() ? CHAT_ID_EMPTY : nodeId;
    String safeAgent = agentId == null || agentId.isBlank() ? CHAT_ID_EMPTY : agentId;
    return CHAT_ID_PREFIX + safeNode + ":" + safeAgent;
  }

  private String stringValue(Map<String, Object> metadata, String key) {
    if (metadata == null || key == null) return null;
    Object value = metadata.get(key);
    if (value == null) return null;
    String str = String.valueOf(value);
    return str.isBlank() ? null : str;
  }

  private boolean booleanValue(Map<String, Object> metadata, String key, boolean fallback) {
    if (metadata == null || key == null) return fallback;
    Object value = metadata.get(key);
    if (value instanceof Boolean bool) return bool;
    if (value == null) return fallback;
    String text = String.valueOf(value).trim();
    if (text.isBlank()) return fallback;
    return "true".equalsIgnoreCase(text) || "1".equals(text);
  }

  private String dedupKey(String prefix, String msgId, String fromNodeId) {
    String safeMsgId = msgId == null ? "" : msgId;
    String safeFrom = fromNodeId == null ? "" : fromNodeId;
    return prefix + ":" + safeMsgId + ":" + safeFrom;
  }

  private boolean encryptOutbound(AgentChatMessage message, String receiverPubKey) {
    if (message == null) return false;
    if (receiverPubKey == null || receiverPubKey.isBlank()) {
      log.debug("P2P chat encrypt skipped: missing receiver pubKey, msgId={}, toNodeId={}", message.getMsgId(), message.getToNodeId());
      return false;
    }
    if (settings == null || settings.getIdentityPrivateKeyHex() == null || settings.getIdentityPrivateKeyHex().isBlank()) {
      log.warn("P2P chat encrypt skipped: missing identity private key, msgId={}", message.getMsgId());
      return false;
    }
    try {
      String payload = message.getPayload();
      String hash = computeHash(message, payload);
      EncryptedPayload encrypted = new EncryptedPayload(payload, hash);
      byte[] plaintext = MAPPER.writeValueAsBytes(encrypted);

      byte[] shared = EcdhKeyAgreement.computeSharedSecret(settings.getIdentityPrivateKeyHex(), receiverPubKey);
      byte[] salt = hkdfSalt(message.getMsgId(), message.getTimestamp());
      byte[] key = Hkdf.deriveKey(shared, salt, HKDF_INFO_KEY, 32);
      byte[] nonce = Hkdf.deriveKey(shared, salt, HKDF_INFO_NONCE, 12);
      byte[] aad = buildAad(message);
      byte[] ciphertext = aeadEncrypt(plaintext, key, nonce, aad);

      message.setPayload(Base64.getEncoder().encodeToString(ciphertext));
      message.setCipher(CHAT_CIPHER);
      return true;
    } catch (Exception ex) {
      log.warn("P2P chat encrypt failed: msgId={}, toNodeId={}, toAgentId={}", message.getMsgId(), message.getToNodeId(), message.getToAgentId(), ex);
      return false;
    }
  }

  private DecryptedPayload decryptInbound(AgentChatMessage message) {
    if (message == null) return null;
    if (settings == null || settings.getIdentityPrivateKeyHex() == null || settings.getIdentityPrivateKeyHex().isBlank()) {
      log.debug("P2P chat decrypt skipped: missing identity private key, msgId={}", message.getMsgId());
      return null;
    }
    if (message.getSenderPubKey() == null || message.getSenderPubKey().isBlank()) {
      log.debug("P2P chat decrypt skipped: missing sender pubKey, msgId={}", message.getMsgId());
      return null;
    }
    try {
      byte[] shared = EcdhKeyAgreement.computeSharedSecret(settings.getIdentityPrivateKeyHex(), message.getSenderPubKey());
      byte[] salt = hkdfSalt(message.getMsgId(), message.getTimestamp());
      byte[] key = Hkdf.deriveKey(shared, salt, HKDF_INFO_KEY, 32);
      byte[] nonce = Hkdf.deriveKey(shared, salt, HKDF_INFO_NONCE, 12);
      byte[] aad = buildAad(message);
      byte[] ciphertext = Base64.getDecoder().decode(message.getPayload());
      byte[] plaintext = aeadDecrypt(ciphertext, key, nonce, aad);
      EncryptedPayload decoded = MAPPER.readValue(plaintext, EncryptedPayload.class);
      if (decoded == null || decoded.payload == null || decoded.hash == null) {
        log.debug("P2P chat decrypt failed: invalid payload, msgId={}", message.getMsgId());
        return null;
      }
      String expected = computeHash(message, decoded.payload);
      if (!expected.equalsIgnoreCase(decoded.hash)) {
        log.debug("P2P chat decrypt failed: hash mismatch, msgId={}", message.getMsgId());
        return null;
      }
      return new DecryptedPayload(decoded.payload, decoded.hash);
    } catch (Exception ex) {
      log.debug("P2P chat decrypt failed: msgId={}", message.getMsgId(), ex);
      return null;
    }
  }

  private boolean validateNodeBinding(AgentChatMessage message) {
    if (message == null) return false;
    String pubKey = message.getSenderPubKey();
    String nodeId = message.getFromNodeId();
    if (pubKey == null || pubKey.isBlank() || nodeId == null || nodeId.isBlank()) return false;
    String derived = NodeIdentityService.nodeIdFromPublicKeyHex(pubKey);
    boolean match = derived != null && derived.equals(nodeId);
    if (!match) {
      log.debug("P2P chat nodeId binding mismatch: msgId={}, fromNodeId={}", message.getMsgId(), nodeId);
    }
    return match;
  }

  private String resolveReceiverPubKey(AgentChatMessage message, Map<String, Object> metadata) {
    String fromMeta = stringValue(metadata, "toPubKey");
    if (fromMeta != null && !fromMeta.isBlank()) return fromMeta;
    if (message == null || registry == null) return null;
    if (message.getToNodeId() == null || message.getToNodeId().isBlank()) return null;
    P2pConnection direct = registry.findByNodeId(message.getToNodeId());
    if (direct == null) {
      log.debug("P2P chat receiver pubKey not found: msgId={}, toNodeId={}", message.getMsgId(), message.getToNodeId());
      return null;
    }
    String pubKey = direct.getRemoteIdentityPubKey();
    if (pubKey == null || pubKey.isBlank()) {
      log.debug("P2P chat receiver pubKey empty: msgId={}, toNodeId={}", message.getMsgId(), message.getToNodeId());
    }
    return pubKey;
  }

  private byte[] buildAad(AgentChatMessage message) {
    String aad = safe(message.getFromNodeId()) + "|" + safe(message.getFromAgentId()) + "|"
        + safe(message.getToNodeId()) + "|" + safe(message.getToAgentId()) + "|"
        + safe(message.getMsgId()) + "|" + message.getTimestamp();
    return aad.getBytes(StandardCharsets.UTF_8);
  }

  private String computeHash(AgentChatMessage message, String payload) {
    String input = safe(message.getFromNodeId()) + "|" + safe(message.getFromAgentId()) + "|"
        + safe(message.getToNodeId()) + "|" + safe(message.getToAgentId()) + "|"
        + safe(message.getMsgId()) + "|" + message.getTimestamp() + "|"
        + safe(payload);

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] out = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(out);
    } catch (Exception ex) {
      return "";
    }
  }

  private byte[] hkdfSalt(String msgId, long timestamp) {
    String salt = safe(msgId) + ":" + timestamp;
    return salt.getBytes(StandardCharsets.UTF_8);
  }

  private byte[] aeadEncrypt(byte[] plaintext, byte[] key, byte[] nonce, byte[] aad) throws Exception {
    Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "ChaCha20"), new IvParameterSpec(nonce));
    if (aad != null && aad.length > 0) {
      cipher.updateAAD(aad);
    }
    return cipher.doFinal(plaintext);
  }

  private byte[] aeadDecrypt(byte[] ciphertext, byte[] key, byte[] nonce, byte[] aad) throws Exception {
    Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "ChaCha20"), new IvParameterSpec(nonce));
    if (aad != null && aad.length > 0) {
      cipher.updateAAD(aad);
    }
    return cipher.doFinal(ciphertext);
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }

  private static class EncryptedPayload {
    public String payload;
    public String hash;

    public EncryptedPayload() {}

    public EncryptedPayload(String payload, String hash) {
      this.payload = payload;
      this.hash = hash;
    }
  }

  private static class DecryptedPayload {
    private final String payload;
    private final String hash;

    private DecryptedPayload(String payload, String hash) {
      this.payload = payload;
      this.hash = hash;
    }
  }

  private static class PendingChat {

    private final AgentChatMessage message;
    private int attempts;
    private long lastSentAt;

    private PendingChat(AgentChatMessage message) {
      this.message = message;
      this.attempts = 0;
      this.lastSentAt = System.currentTimeMillis();
    }

    private void markAttempt() {
      attempts++;
      lastSentAt = System.currentTimeMillis();
    }

    private boolean shouldRetry(P2pSettings settings) {
      if (settings == null) return false;
      return attempts < settings.getChatRetryMax();
    }
  }

  private static class Target {
    private final String nodeId;
    private final String agentId;

    private Target(String nodeId, String agentId) {
      this.nodeId = nodeId;
      this.agentId = agentId;
    }
  }

  private static class QpsLimiter {
    private final int qps;
    private long windowStartMs = System.currentTimeMillis();
    private int count = 0;

    private QpsLimiter(int qps) {
      this.qps = qps;
    }

    private synchronized boolean allow() {
      if (qps <= 0) return true;
      long now = System.currentTimeMillis();
      if (now - windowStartMs >= 1000) {
        windowStartMs = now;
        count = 0;
      }
      if (count >= qps) {
        return false;
      }
      count++;
      return true;
    }
  }
}
