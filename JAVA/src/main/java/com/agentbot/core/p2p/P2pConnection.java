package com.agentbot.core.p2p;

import com.agentbot.core.p2p.crypto.CipherSuite;
import com.agentbot.core.p2p.crypto.Hkdf;
import com.agentbot.core.p2p.crypto.IdentitySigner;
import com.agentbot.core.p2p.crypto.KeyExchange;
import com.agentbot.core.p2p.crypto.NoopObfuscator;
import com.agentbot.core.p2p.crypto.ObfuscatorFactory;
import com.agentbot.core.p2p.crypto.PublicKeyObfuscator;
import com.agentbot.core.p2p.crypto.SecureChannel;
import com.agentbot.core.p2p.crypto.SessionKeys;
import com.agentbot.core.protocol.FlowControlConfig;
import com.agentbot.core.protocol.FrameCodec;
import com.agentbot.core.protocol.FrameDecodeException;
import com.agentbot.core.protocol.HandshakeMessage;
import com.agentbot.core.protocol.JsonMessageCodec;
import com.agentbot.core.protocol.MessageType;
import com.agentbot.core.protocol.P2pHeader;

import com.agentbot.core.protocol.PingMessage;
import com.agentbot.core.protocol.PongMessage;
import com.agentbot.core.protocol.ProtocolCodec;
import com.agentbot.core.protocol.ProtocolConstants;
import com.agentbot.core.protocol.ProtocolDispatcher;
import com.agentbot.core.protocol.VerackMessage;
import com.agentbot.core.protocol.VersionMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;



public class P2pConnection {
  private static final Logger log = LoggerFactory.getLogger(P2pConnection.class);
  public static final AttributeKey<P2pConnection> CONNECTION_KEY = AttributeKey.valueOf("p2pConnection");

  private enum State {

    HANDSHAKE,
    SECURE,
    CLOSED
  }

  private final Channel channel;
  private final ProtocolCodec protocolCodec;

  private final P2pSettings settings;
  private final AddrExchangeHandler addrExchangeService;

  private final ProtocolDispatcher dispatcher;
  private final boolean initiator;
  private final ConnectionRegistry registry;
  private final String addressKey;
  private final Object writeLock = new Object();


  private final KeyExchange keyExchange;
  private final PublicKeyObfuscator obfuscator;
  private final JsonMessageCodec handshakeCodec = new JsonMessageCodec();
  private SecureChannel secureChannel;
  private SessionKeys sessionKeys;


  private static final CipherSuite HANDSHAKE_CIPHER = CipherSuite.AES_GCM_256;
  private static final long HANDSHAKE_TIMEOUT_MS = 15000L;
  private static final long RETRY_INTERVAL_MS = 3000L;
  private static final int MAX_HANDSHAKE_RETRIES = 3;
  private static final int MAX_VERSION_RETRIES = 3;
  private static final int MAX_VERACK_RETRIES = 3;

  private static final long HEARTBEAT_INTERVAL_MS = 60000L;
  private static final long HEARTBEAT_TIMEOUT_MS = 60000L;
  private static final int HEARTBEAT_MAX_MISSED = 3;

  private static final ScheduledExecutorService RETRY_SCHEDULER = Executors.newScheduledThreadPool(1, new ThreadFactory() {
    @Override
    public Thread newThread(Runnable r) {
      Thread thread = new Thread(r, "p2p-handshake-retry");
      thread.setDaemon(true);
      return thread;
    }
  });

  private static final ScheduledExecutorService HEARTBEAT_SCHEDULER = Executors.newScheduledThreadPool(1, new ThreadFactory() {
    @Override
    public Thread newThread(Runnable r) {
      Thread thread = new Thread(r, "p2p-heartbeat");
      thread.setDaemon(true);
      return thread;
    }
  });


  private volatile boolean running;


  private volatile boolean sentHandshake;
  private volatile boolean sentVersion;
  private volatile boolean receivedVersion;
  private volatile boolean nodeRegistered;


  private volatile boolean sentVerack;
  private volatile boolean receivedVerack;
  private volatile boolean handshakeComplete;

  private volatile CipherSuite negotiatedCipher;
  private volatile CipherSuite activeCipher = HANDSHAKE_CIPHER;
  private volatile String negotiatedContentType;
  private volatile String remoteNodeId;
  private volatile String remoteIdentityPubKey;
  private volatile int negotiatedFlowWindow;

  private volatile int negotiatedMaxInFlight;

  private volatile State state = State.HANDSHAKE;
  private byte[] localHandshakeBytes;
  private byte[] remoteHandshakeBytes;
  private byte[] handshakeTranscript;
  private byte[] sharedSecret;
  private long rekeyEpoch = 0;

  private long handshakeStartMs;
  private long connectionStartMs;
  private long lastHandshakeSendMs;
  private long lastVersionSendMs;
  private long lastVerackSendMs;

  private int handshakeAttempts;
  private int versionAttempts;
  private int verackAttempts;
  private boolean lastVerackAccepted;
  private String lastVerackReason;
  private ScheduledFuture<?> retryTask;
  private ScheduledFuture<?> heartbeatTask;
  private volatile String closeReason;

  private final ConcurrentHashMap<String, Long> pendingPings = new ConcurrentHashMap<>();
  private volatile long lastPingReceivedMs;
  private volatile long lastPingSentMs;

  private volatile int missedPingCount;
  private volatile int missedPongCount;

  private volatile long lastGetAddrSentMs;
  private volatile long lastGetAddrMinIntervalMs;

  private volatile long getaddrBackoffUntilMs;
  private volatile long getaddrBackoffMaxMs;
  private volatile int consecutiveEmptyAddr;



  public P2pConnection(Channel channel,

                       boolean initiator,
                       ProtocolCodec protocolCodec,
                       P2pSettings settings,
                       AddrExchangeHandler addrExchangeService,

                       ProtocolDispatcher dispatcher,
                       ConnectionRegistry registry,
                       String addressKey) {
    this.channel = channel;
    this.initiator = initiator;
    this.protocolCodec = protocolCodec;
    this.settings = settings;
    this.addrExchangeService = addrExchangeService;
    this.dispatcher = dispatcher;
    this.registry = registry;
    this.addressKey = addressKey;
    this.obfuscator = ObfuscatorFactory.resolve(settings.getObfuscationAlgo(), settings.isObfuscationEnabled());


    try {
      this.keyExchange = new KeyExchange();
    } catch (Exception ex) {
      throw new RuntimeException("Failed to initialize key exchange", ex);
    }
  }

  public void start() {
    if (running) {
      log.debug("P2P connection already running: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator);
      return;
    }
    running = true;

    if (channel != null) {
      channel.attr(CONNECTION_KEY).set(this);
    }

    connectionStartMs = System.currentTimeMillis();
    handshakeStartMs = connectionStartMs;
    if (registry != null) {
      if (addressKey != null && !addressKey.isBlank() && !registry.registerAddress(addressKey, this)) {
        log.warn("P2P duplicate address, closing: addressKey={}, remote={}:{}, initiator={}", addressKey, remoteHost(), remotePort(), initiator);
        closeWithReason("duplicate-address");
        return;
      }
      registry.registerConnection(this);
    }
    log.info("P2P connection start: initiator={}, remote={}:{}, addressKey={}", initiator, remoteHost(), remotePort(), addressKey);
    P2pMetrics.recordOpen();

    if (initiator) {
      sendHandshake();
    }
    scheduleRetries();
    startHeartbeat();
  }


  public void send(MessageType type, Object payload) {
    P2pHeader header = P2pHeader.builder()
        .msgType(type)
        .regionId(settings.getRegionId())
        .contentType(negotiatedContentType != null ? negotiatedContentType : settings.getPreferredContentType())
        .features(settings.getFeatures())
        .build();
    sendInternal(header, payload, state == State.SECURE);
  }

  public void send(MessageType type, Object payload, String msgId) {
    P2pHeader.Builder builder = P2pHeader.builder()
        .msgType(type)
        .regionId(settings.getRegionId())
        .contentType(negotiatedContentType != null ? negotiatedContentType : settings.getPreferredContentType())
        .features(settings.getFeatures());
    if (msgId != null && !msgId.isBlank()) {
      builder.msgId(msgId);
    }
    sendInternal(builder.build(), payload, state == State.SECURE);
  }


  public String remoteHost() {
    java.net.SocketAddress address = channel == null ? null : channel.remoteAddress();
    if (address instanceof java.net.InetSocketAddress inet) {
      if (inet.getAddress() != null) return inet.getAddress().getHostAddress();
      return inet.getHostString();
    }
    return "unknown";
  }

  public int remotePort() {
    java.net.SocketAddress address = channel == null ? null : channel.remoteAddress();
    if (address instanceof java.net.InetSocketAddress inet) {
      return inet.getPort();
    }
    return -1;
  }

  public String getRemoteNodeId() {

    return remoteNodeId;
  }

  public String getRemoteIdentityPubKey() {
    return remoteIdentityPubKey;
  }

  public boolean isHandshakeComplete() {

    return handshakeComplete;
  }

  public long getLastGetAddrSentMs() {
    return lastGetAddrSentMs;
  }

  public void setGetaddrBackoffMaxMs(long getaddrBackoffMaxMs) {
    this.getaddrBackoffMaxMs = Math.max(0, getaddrBackoffMaxMs);
  }

  public boolean requestGetAddr(long minIntervalMs, String reason) {
    if (!running || !handshakeComplete) return false;
    if (addrExchangeService == null) return false;
    long now = System.currentTimeMillis();
    if (minIntervalMs > 0 && now - lastGetAddrSentMs < minIntervalMs) {
      P2pMetrics.recordGetAddrSkipped();
      return false;
    }
    if (getaddrBackoffUntilMs > 0 && now < getaddrBackoffUntilMs) {
      P2pMetrics.recordGetAddrSkipped();
      return false;
    }
    if (minIntervalMs > 0) {
      lastGetAddrMinIntervalMs = minIntervalMs;
    }
    lastGetAddrSentMs = now;
    P2pMetrics.recordGetAddrSent();
    log.debug("P2P send getaddr: reason={}, remoteNodeId={}, remote={}:{}, initiator={}", reason, remoteNodeId, remoteHost(), remotePort(), initiator);
    addrExchangeService.sendGetAddr(this);
    return true;
  }

  public void recordAddrResponse(int total, int accepted, int invalid) {
    if (total <= 0) {
      consecutiveEmptyAddr += 1;
      applyGetaddrBackoff();
      return;
    }

    if (accepted <= 0) {
      consecutiveEmptyAddr += 1;
      applyGetaddrBackoff();
      return;
    }
    consecutiveEmptyAddr = 0;
    getaddrBackoffUntilMs = 0;
  }

  private void applyGetaddrBackoff() {
    long base = lastGetAddrMinIntervalMs > 0 ? lastGetAddrMinIntervalMs : TimeUnit.SECONDS.toMillis(60);
    long backoff = base * (1L << Math.min(consecutiveEmptyAddr, 5));
    if (getaddrBackoffMaxMs > 0) {
      backoff = Math.min(backoff, getaddrBackoffMaxMs);
    }
    if (backoff > 0) {
      getaddrBackoffUntilMs = System.currentTimeMillis() + backoff;
    }
  }


  public long getConnectionAgeMs() {
    if (connectionStartMs <= 0) return 0L;
    return System.currentTimeMillis() - connectionStartMs;
  }

  private void onPacket(byte[] packet) {
    if (!running) return;
    int length = packet == null ? 0 : packet.length;
    if (length <= 0 || length > settings.getMaxPayload() * 4L) {
      log.warn("P2P invalid frame length={}, closing: remote={}:{}, initiator={}", length, remoteHost(), remotePort(), initiator);
      closeWithReason("invalid-frame-length");
      return;
    }

    FrameCodec.Frame frame;
    try {
      frame = FrameCodec.decode(packet);
    } catch (FrameDecodeException ex) {
      log.warn("P2P frame decode failed, closing: reason={}, remote={}:{}, initiator={}", ex.getReason(), remoteHost(), remotePort(), initiator, ex);
      closeWithReason("frame-decode-" + ex.getReason());
      return;
    }
    byte[] headerBytes = frame.headerBytes();
    byte[] payloadBytes = frame.payloadBytes();

    if (headerBytes == null || headerBytes.length == 0) {
      handleRawHandshake(payloadBytes);
      return;
    }
    P2pHeader header;
    try {
      header = protocolCodec.decodeHeader(headerBytes);
    } catch (Exception ex) {
      log.warn("P2P header decode failed, closing: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator, ex);
      closeWithReason("header-decode-error");
      return;
    }
    if (header.getMagic() != ProtocolConstants.MAGIC) {
      log.warn("P2P invalid magic, closing: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator);
      closeWithReason("invalid-magic");
      return;
    }

    try {
      if (state == State.SECURE && secureChannel != null && payloadBytes != null && payloadBytes.length > 0) {
        payloadBytes = secureChannel.decrypt(payloadBytes, headerBytes);
        maybeRekey();
      }
      Object payload = protocolCodec.decodePayload(header, payloadBytes);
      P2pMetrics.recordReceive();
      handleMessage(header, payload, payloadBytes);
    } catch (Exception ex) {
      log.warn("P2P payload decode failed, closing: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator, ex);
      closeWithReason("payload-decode-error");
    }

  }

  public static class NettyPacketHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      if (!(msg instanceof ByteBuf buf)) {
        ctx.fireChannelRead(msg);
        return;
      }
      try {
        byte[] packet = new byte[buf.readableBytes()];
        buf.readBytes(packet);
        P2pConnection connection = ctx.channel().attr(CONNECTION_KEY).get();
        if (connection != null) {
          connection.onPacket(packet);
        }
      } finally {
        ReferenceCountUtil.release(msg);
      }
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
      P2pConnection connection = ctx.channel().attr(CONNECTION_KEY).get();
      if (connection != null) {
        connection.closeWithReason("channel-inactive");
      } else {
        ctx.close();
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      P2pConnection connection = ctx.channel().attr(CONNECTION_KEY).get();
      if (connection != null) {
        connection.closeWithReason("netty-error");
      } else {
        ctx.close();
      }
    }
  }


  private void handleMessage(P2pHeader header, Object payload, byte[] payloadBytes) {
    if (header == null) return;
    MessageType type = header.getMsgType();

    if (type == MessageType.HANDSHAKE) {
      if (state != State.HANDSHAKE) {
        log.warn("P2P unexpected handshake in state={}, closing: remote={}:{}, initiator={}", state, remoteHost(), remotePort(), initiator);
        closeWithReason("unexpected-handshake");
        return;
      }
      handleHandshake((HandshakeMessage) payload, payloadBytes);
      return;
    }


    if (type == MessageType.VERSION) {
      handleVersion((VersionMessage) payload);
      return;
    }

    if (type == MessageType.VERACK) {
      handleVerack((VerackMessage) payload);
      return;
    }

    if (type == MessageType.PING) {
      handlePing((PingMessage) payload);
      return;
    }

    if (type == MessageType.PONG) {
      handlePong((PongMessage) payload);
      return;
    }

    if (dispatcher != null) {
      dispatcher.dispatch(this, header, payload);
    }

  }

  private void handlePing(PingMessage ping) {
    if (ping == null || ping.getNonce() == null || ping.getNonce().isBlank()) {
      log.warn("P2P invalid ping payload, ignoring: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator);
      return;
    }
    lastPingReceivedMs = System.currentTimeMillis();
    missedPingCount = 0;
    PongMessage pong = new PongMessage();
    pong.setNonce(ping.getNonce());
    pong.setSentAt(ping.getSentAt());
    send(MessageType.PONG, pong);
  }

  private void handlePong(PongMessage pong) {
    if (pong == null || pong.getNonce() == null || pong.getNonce().isBlank()) {
      log.warn("P2P invalid pong payload, ignoring: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator);
      return;
    }
    Long sentAt = pendingPings.remove(pong.getNonce());
    if (sentAt == null || sentAt <= 0) {
      log.warn("P2P pong without pending ping, ignoring: nonce={}, remote={}:{}, initiator={}", pong.getNonce(), remoteHost(), remotePort(), initiator);
      return;
    }
    long now = System.currentTimeMillis();
    long rtt = Math.max(0L, now - sentAt);
    missedPongCount = 0;

    if (addrExchangeService != null) {
      addrExchangeService.recordPeerSuccess(remoteHost(), remotePort(), remoteNodeId, rtt);
    }
  }


  private void sendHandshake() {
    if (handshakeAttempts >= MAX_HANDSHAKE_RETRIES) {
      log.warn("P2P handshake retries exceeded, closing: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator);
      closeWithReason("handshake-retry-exceeded");
      return;
    }

    HandshakeMessage handshake = new HandshakeMessage();
    int padding = ThreadLocalRandom.current().nextInt(1 << 16);

    handshake.setVersionWithPadding(1, padding);
    try {
      byte[] encoded = obfuscator.encode(keyExchange.getPublicKeyEncoded());
      handshake.setKeyExchangePub(Base64.getEncoder().encodeToString(encoded));
    } catch (Exception ex) {
      handshake.setKeyExchangePub(Base64.getEncoder().encodeToString(keyExchange.getPublicKeyEncoded()));
    }

    sentHandshake = true;
    handshakeAttempts += 1;
    lastHandshakeSendMs = System.currentTimeMillis();
    log.debug("P2P send handshake: attempt={}, remote={}:{}, initiator={}", handshakeAttempts, remoteHost(), remotePort(), initiator);


    try {
      localHandshakeBytes = handshakeCodec.encode(handshake);
      sendRawFrame(localHandshakeBytes);
    } catch (Exception ex) {
      log.warn("P2P send handshake failed, closing: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator, ex);
      closeWithReason("handshake-send-failed");
    }
  }


  private void sendVersion() {
    if (secureChannel == null) {
      return;
    }
    if (versionAttempts >= MAX_VERSION_RETRIES) {
      log.warn("P2P version retries exceeded, closing: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator);
      closeWithReason("version-retry-exceeded");
      return;
    }

    VersionMessage version = new VersionMessage();

    version.setNodeId(settings.getNodeId());
    version.setRegionId(settings.getRegionId());
    version.setServices(0L);
    version.setMaxPayload(settings.getMaxPayload());
    version.setCompression(settings.getSupportedCompression());
    version.setCipherSuites(settings.getSupportedCipherSuites().stream().map(Enum::name).toList());
    version.setContentTypes(settings.getSupportedContentTypes());
    version.setFeatures(settings.getFeatures());

    FlowControlConfig flow = new FlowControlConfig();
    flow.setWindowSize(settings.getFlowWindow());
    flow.setMaxInFlight(settings.getMaxInFlight());
    version.setFlowControl(flow);

    sentVersion = true;
    versionAttempts += 1;
    lastVersionSendMs = System.currentTimeMillis();
    log.debug("P2P send version: attempt={}, remote={}:{}, initiator={}", versionAttempts, remoteHost(), remotePort(), initiator);


    P2pHeader header = P2pHeader.builder()
        .msgType(MessageType.VERSION)
        .contentType(ProtocolConstants.CONTENT_JSON)
        .build();

    sendInternal(header, version, true);
  }


  private void handleRawHandshake(byte[] payloadBytes) {
    if (state != State.HANDSHAKE) {
      log.warn("P2P raw handshake in state={}, closing: remote={}:{}, initiator={}", state, remoteHost(), remotePort(), initiator);
      closeWithReason("raw-handshake-wrong-state");
      return;
    }
    if (payloadBytes == null || payloadBytes.length == 0) {
      log.warn("P2P empty handshake payload, closing: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator);
      closeWithReason("empty-handshake-payload");
      return;
    }

    try {
      HandshakeMessage handshake = handshakeCodec.decode(payloadBytes, HandshakeMessage.class);
      handleHandshake(handshake, payloadBytes);
    } catch (Exception ex) {
      log.warn("P2P decode handshake failed, closing: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator, ex);
      closeWithReason("handshake-decode-failed");
    }
  }

  private void handleHandshake(HandshakeMessage handshake, byte[] payloadBytes) {
    if (handshake == null || handshake.getKeyExchangePub() == null || handshake.getVersion() != 2) {
      log.warn("P2P invalid handshake, closing: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator);
      closeWithReason("invalid-handshake");
      return;
    }

    if (!sentHandshake) {
      sendHandshake();
    }

    remoteHandshakeBytes = payloadBytes == null ? new byte[0] : payloadBytes;
    buildHandshakeTranscript();

    try {
      byte[] peerEncoded = Base64.getDecoder().decode(handshake.getKeyExchangePub());
      byte[] peerPub = decodePeerPublicKey(peerEncoded);
      sharedSecret = keyExchange.computeSharedSecret(peerPub);
      sessionKeys = deriveSessionKeys(sharedSecret, initiator, handshakeTranscript);
    } catch (Exception ex) {
      log.warn("P2P derive session keys failed, closing: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator, ex);
      closeWithReason("session-keys-failed");
      return;
    }

    if (secureChannel == null) {
      secureChannel = new SecureChannel(HANDSHAKE_CIPHER, sessionKeys.getSendKey(), sessionKeys.getRecvKey());
      activeCipher = HANDSHAKE_CIPHER;
    }
    state = State.SECURE;
    log.info("P2P secure channel established: cipher={}, remote={}:{}, initiator={}", activeCipher, remoteHost(), remotePort(), initiator);


    if (initiator && !sentVersion) {
      sendVersion();
    }
  }


  private void handleVersion(VersionMessage version) {
    if (state != State.SECURE || secureChannel == null || sessionKeys == null) {
      log.warn("P2P version in invalid state, closing: state={}, remote={}:{}, initiator={}", state, remoteHost(), remotePort(), initiator);
      closeWithReason("version-invalid-state");
      return;
    }

    if (version == null) {
      log.warn("P2P null version, closing: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator);
      closeWithReason("version-null");
      return;
    }

    receivedVersion = true;
    log.debug("P2P received version: remoteNodeId={}, initiator={}", version.getNodeId(), initiator);
    if (initiator && !sentVersion) {
      sendVersion();
    }

    remoteNodeId = version.getNodeId();
    if (settings.getNodeId() != null && remoteNodeId != null && settings.getNodeId().equals(remoteNodeId)) {
      log.warn("P2P self-connection detected, closing: nodeId={}", remoteNodeId);
      closeWithReason("self-connection");
      return;
    }

    if (!nodeRegistered && registry != null && remoteNodeId != null && !remoteNodeId.isBlank()) {
      if (!registry.registerNode(remoteNodeId, this)) {
        log.warn("P2P duplicate nodeId detected, closing: nodeId={}", remoteNodeId);
        closeWithReason("duplicate-node");
        return;
      }
      nodeRegistered = true;
    }

    CipherSuite candidateCipher = selectCipher(version.getCipherSuites());
    String candidateContentType = selectContentType(version.getContentTypes());

    if (initiator) {
      negotiatedCipher = candidateCipher;
      negotiatedContentType = candidateContentType;
    }
    negotiatedFlowWindow = selectFlowWindow(version.getFlowControl());
    negotiatedMaxInFlight = selectMaxInFlight(version.getFlowControl());


    if (candidateCipher == null && settings.isRequireEncryption()) {
      log.warn("P2P no common cipher, rejecting: remoteNodeId={}", remoteNodeId);
      sendVerack(false, "no common cipher");
      return;
    }

    if (settings.getIdempotentWindow() > 0
        && (version.getFeatures() == null || !version.getFeatures().containsKey("idempotentWindow"))) {
      log.warn("P2P missing features, rejecting: remoteNodeId={}", remoteNodeId);
      sendVerack(false, "missing features");
      return;
    }

    sendVerack(true, null);
    if (!initiator && !sentVersion) {
      sendVersion();
    }

  }

  private void sendVerack(boolean accepted, String reason) {
    if (secureChannel == null) {
      log.warn("P2P verack without secure channel, closing: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator);
      closeWithReason("verack-no-secure-channel");
      return;
    }
    if (verackAttempts >= MAX_VERACK_RETRIES) {
      log.warn("P2P verack retries exceeded, closing: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator);
      closeWithReason("verack-retry-exceeded");
      return;
    }

    VerackMessage verack = new VerackMessage();

    verack.setNodeId(settings.getNodeId());
    verack.setAccepted(accepted);
    verack.setReason(reason);
    if (initiator) {
      verack.setSelectedCipher(negotiatedCipher == null ? null : negotiatedCipher.name());
      verack.setSelectedContentType(negotiatedContentType);
    }

    verack.setSelectedCompression("none");

    verack.setFlowWindow(negotiatedFlowWindow);
    verack.setMaxInFlight(negotiatedMaxInFlight);

    boolean shouldAuth = settings.isAuthRequired()
        || (settings.getIdentityPrivateKeyHex() != null && !settings.getIdentityPrivateKeyHex().isBlank());
    if (shouldAuth) {
      if (settings.getIdentityPrivateKeyHex() == null || settings.getIdentityPrivateKeyHex().isBlank()) {
        verack.setAccepted(false);
        verack.setReason("identity required");
      } else if (sessionKeys != null) {
        try {
          byte[] transcript = buildAuthTranscript(sessionKeys.getSessionId(), settings.getNodeId(), remoteNodeId);
          verack.setIdentityPubKey(settings.getIdentityPublicKeyHex());
          verack.setIdentitySignature(IdentitySigner.sign(transcript, settings.getIdentityPrivateKeyHex()));
        } catch (Exception ex) {
          verack.setAccepted(false);
          verack.setReason("identity sign failed");
        }
      }
    }

    sentVerack = true;
    verackAttempts += 1;
    lastVerackSendMs = System.currentTimeMillis();
    lastVerackAccepted = accepted;
    lastVerackReason = reason;
    log.debug("P2P send verack: accepted={}, reason={}, attempt={}, remoteNodeId={}", accepted, reason, verackAttempts, remoteNodeId);

    sendInternal(P2pHeader.builder()

        .msgType(MessageType.VERACK)
        .contentType(ProtocolConstants.CONTENT_JSON)
        .build(), verack, true);
  }


  private void handleVerack(VerackMessage verack) {
    receivedVerack = true;
    log.debug("P2P received verack: accepted={}, remoteNodeId={}", verack == null ? null : verack.isAccepted(), verack == null ? null : verack.getNodeId());
    if (verack != null && !verack.isAccepted()) {

      log.warn("P2P verack rejected, closing: reason={}, remoteNodeId={}", verack.getReason(), verack.getNodeId());
      closeWithReason("verack-rejected");
      return;
    }

    if (verack != null && verack.getSelectedCipher() != null) {
      CipherSuite selected = CipherSuite.from(verack.getSelectedCipher());
      if (initiator) {
        if (negotiatedCipher != null && selected != negotiatedCipher) {
          log.warn("P2P verack cipher mismatch, closing: expected={}, got={}", negotiatedCipher, selected);
          closeWithReason("verack-cipher-mismatch");
          return;
        }
      } else {
        if (!settings.getSupportedCipherSuites().contains(selected)) {
          log.warn("P2P verack cipher unsupported, closing: selected={}", selected);
          closeWithReason("verack-cipher-unsupported");
          return;
        }
        negotiatedCipher = selected;
      }
    } else if (!initiator) {
      log.warn("P2P verack missing cipher, closing: remoteNodeId={}", verack == null ? null : verack.getNodeId());
      closeWithReason("verack-cipher-missing");
      return;
    }


    if (verack != null && verack.getSelectedContentType() != null) {
      String selected = verack.getSelectedContentType();
      if (initiator) {
        if (negotiatedContentType != null && !negotiatedContentType.equalsIgnoreCase(selected)) {
          log.warn("P2P verack contentType mismatch, closing: expected={}, got={}", negotiatedContentType, selected);
          closeWithReason("verack-content-type-mismatch");
          return;
        }
      } else {
        if (!settings.getSupportedContentTypes().stream().anyMatch(type -> type.equalsIgnoreCase(selected))) {
          log.warn("P2P verack contentType unsupported, closing: selected={}", selected);
          closeWithReason("verack-content-type-unsupported");
          return;
        }
        negotiatedContentType = selected;
      }
    } else if (!initiator) {
      log.warn("P2P verack missing contentType, closing: remoteNodeId={}", verack == null ? null : verack.getNodeId());
      closeWithReason("verack-content-type-missing");
      return;
    }


    negotiatedFlowWindow = verack == null ? negotiatedFlowWindow : verack.getFlowWindow();
    negotiatedMaxInFlight = verack == null ? negotiatedMaxInFlight : verack.getMaxInFlight();

    if (!verifyIdentity(verack)) {
      log.warn("P2P verack identity verification failed, closing: remoteNodeId={}", verack == null ? null : verack.getNodeId());
      closeWithReason("verack-identity-failed");
      return;
    }

    if (verack != null && verack.getIdentityPubKey() != null && !verack.getIdentityPubKey().isBlank()) {
      remoteIdentityPubKey = verack.getIdentityPubKey();
    }

    maybeCompleteHandshake();

  }

  private void maybeCompleteHandshake() {
    if (handshakeComplete || !sentVerack || !receivedVerack) return;
    if (sessionKeys != null && negotiatedCipher != null && secureChannel != null && negotiatedCipher != activeCipher) {
      secureChannel = new SecureChannel(negotiatedCipher, sessionKeys.getSendKey(), sessionKeys.getRecvKey());
      activeCipher = negotiatedCipher;
    }
    handshakeComplete = true;
    if (retryTask != null) {
      retryTask.cancel(false);
    }
    log.info("P2P handshake complete: remoteNodeId={}, cipher={}, contentType={}", remoteNodeId, negotiatedCipher, negotiatedContentType);
    P2pMetrics.recordHandshake();
    requestGetAddr(0, "handshake");
    startHeartbeat();
  }


  private void sendInternal(P2pHeader header, Object payload, boolean encrypt) {
    try {
      byte[] headerBytes = protocolCodec.encodeHeader(header);
      byte[] payloadBytes = protocolCodec.encodePayload(header, payload);
      if (encrypt && secureChannel != null) {
        if (secureChannel.shouldRekeySend()) {
          rekey();
        }
        payloadBytes = secureChannel.encrypt(payloadBytes, headerBytes);
      }
      byte[] packet = FrameCodec.encode(headerBytes, payloadBytes);
      writePacket(packet);
      P2pMetrics.recordSend();
    } catch (Exception ex) {
      log.warn("P2P send failed, closing: msgType={}, remote={}:{}, initiator={}", header == null ? null : header.getMsgType(), remoteHost(), remotePort(), initiator, ex);
      if (addrExchangeService != null) {
        addrExchangeService.recordPeerFailure(remoteHost(), remotePort(), remoteNodeId);
      }
      closeWithReason("send-failed");
    }
  }


  private void scheduleRetries() {
    if (retryTask != null) return;
    retryTask = RETRY_SCHEDULER.scheduleAtFixedRate(this::retryIfNeeded, RETRY_INTERVAL_MS, RETRY_INTERVAL_MS, TimeUnit.MILLISECONDS);
  }

  private void startHeartbeat() {
    if (heartbeatTask != null) return;
    long now = System.currentTimeMillis();
    lastPingReceivedMs = now;
    heartbeatTask = HEARTBEAT_SCHEDULER.scheduleAtFixedRate(this::heartbeatTick, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);

  }

  private void heartbeatTick() {
    if (!running || !handshakeComplete) return;
    long now = System.currentTimeMillis();

    if (now - lastPingSentMs >= HEARTBEAT_INTERVAL_MS) {
      sendPing();
    }

    for (var entry : pendingPings.entrySet()) {
      if (now - entry.getValue() > HEARTBEAT_TIMEOUT_MS) {
        pendingPings.remove(entry.getKey());
        missedPongCount += 1;
      }
    }

    if (missedPongCount >= HEARTBEAT_MAX_MISSED) {
      log.warn("P2P heartbeat pong timeout: missed={}, remote={}:{}, initiator={}", missedPongCount, remoteHost(), remotePort(), initiator);
      if (addrExchangeService != null) {
        addrExchangeService.recordPeerFailure(remoteHost(), remotePort(), remoteNodeId);
      }
      missedPongCount = 0;
    }

    if (lastPingReceivedMs > 0 && now - lastPingReceivedMs > HEARTBEAT_TIMEOUT_MS) {
      missedPingCount += 1;
      if (missedPingCount >= HEARTBEAT_MAX_MISSED) {
        log.warn("P2P heartbeat ping missing: missed={}, remote={}:{}, initiator={}", missedPingCount, remoteHost(), remotePort(), initiator);
        if (addrExchangeService != null) {
          addrExchangeService.recordPeerFailure(remoteHost(), remotePort(), remoteNodeId);
        }
        missedPingCount = 0;
      }
    }
  }

  private void sendPing() {
    long now = System.currentTimeMillis();
    String nonce = Long.toHexString(ThreadLocalRandom.current().nextLong());
    PingMessage ping = new PingMessage();
    ping.setNonce(nonce);
    ping.setSentAt(now);
    pendingPings.put(nonce, now);
    lastPingSentMs = now;
    try {
      send(MessageType.PING, ping);
    } catch (Exception ex) {
      log.warn("P2P send ping failed: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator, ex);
    }
  }


  private void retryIfNeeded() {
    if (!running) return;
    long now = System.currentTimeMillis();
    if (handshakeComplete) return;

    if (now - handshakeStartMs > HANDSHAKE_TIMEOUT_MS) {
      log.warn("P2P handshake timeout, closing: remote={}:{}, initiator={}", remoteHost(), remotePort(), initiator);
      closeWithReason("handshake-timeout");
      return;
    }


    if (state == State.HANDSHAKE) {
      if (initiator) {
        if (now - lastHandshakeSendMs >= RETRY_INTERVAL_MS && handshakeAttempts < MAX_HANDSHAKE_RETRIES) {
          log.debug("P2P retry handshake: attempt={}, remote={}:{}, initiator={}", handshakeAttempts + 1, remoteHost(), remotePort(), initiator);
          sendHandshake();
        }

      }
      return;
    }

    if (!receivedVersion && sentVersion) {
      if (now - lastVersionSendMs >= RETRY_INTERVAL_MS && versionAttempts < MAX_VERSION_RETRIES) {
        log.debug("P2P retry version: attempt={}, remote={}:{}, initiator={}", versionAttempts + 1, remoteHost(), remotePort(), initiator);
        sendVersion();
      }
    }

    if (sentVerack && !receivedVerack) {
      if (now - lastVerackSendMs >= RETRY_INTERVAL_MS && verackAttempts < MAX_VERACK_RETRIES) {
        log.debug("P2P retry verack: attempt={}, remote={}:{}, initiator={}", verackAttempts + 1, remoteHost(), remotePort(), initiator);
        sendVerack(lastVerackAccepted, lastVerackReason);
      }
    }

  }


  private void sendRawFrame(byte[] payloadBytes) throws Exception {
    byte[] packet = FrameCodec.encode(new byte[0], payloadBytes == null ? new byte[0] : payloadBytes);
    writePacket(packet);
    P2pMetrics.recordSend();
  }

  private void writePacket(byte[] packet) throws Exception {
    synchronized (writeLock) {
      if (channel == null || !channel.isActive()) {
        throw new IllegalStateException("channel not active");
      }
      channel.writeAndFlush(Unpooled.wrappedBuffer(packet)).addListener(future -> {
        if (!future.isSuccess()) {
          closeWithReason("write-failed");
        }
      });

    }
  }


  private CipherSuite selectCipher(List<String> peerSuites) {
    if (peerSuites == null || peerSuites.isEmpty()) {
      return settings.isRequireEncryption() ? null : settings.getPreferredCipherSuite();
    }
    for (CipherSuite suite : settings.getSupportedCipherSuites()) {
      for (String peer : peerSuites) {
        if (suite.name().equalsIgnoreCase(peer)) {
          return suite;
        }
      }
    }
    return settings.isRequireEncryption() ? null : settings.getPreferredCipherSuite();
  }

  private String selectContentType(List<String> peerTypes) {
    if (peerTypes == null || peerTypes.isEmpty()) {
      return settings.getPreferredContentType();
    }
    for (String type : settings.getSupportedContentTypes()) {
      for (String peer : peerTypes) {
        if (type.equalsIgnoreCase(peer)) {
          return type;
        }
      }
    }
    return settings.getPreferredContentType();
  }

  private int selectFlowWindow(FlowControlConfig peerFlow) {
    if (peerFlow == null || peerFlow.getWindowSize() <= 0) {
      return settings.getFlowWindow();
    }
    return Math.min(settings.getFlowWindow(), peerFlow.getWindowSize());
  }

  private int selectMaxInFlight(FlowControlConfig peerFlow) {
    if (peerFlow == null || peerFlow.getMaxInFlight() <= 0) {
      return settings.getMaxInFlight();
    }
    return Math.min(settings.getMaxInFlight(), peerFlow.getMaxInFlight());
  }

  private byte[] decodePeerPublicKey(byte[] encoded) throws Exception {
    try {
      return obfuscator.decode(encoded);
    } catch (Exception ex) {
      PublicKeyObfuscator fallback = new NoopObfuscator();
      return fallback.decode(encoded);
    }
  }

  private void buildHandshakeTranscript() {
    if (localHandshakeBytes == null || remoteHandshakeBytes == null) return;
    if (handshakeTranscript != null) return;
    if (initiator) {
      handshakeTranscript = concat(localHandshakeBytes, remoteHandshakeBytes);
    } else {
      handshakeTranscript = concat(remoteHandshakeBytes, localHandshakeBytes);
    }
  }


  private SessionKeys deriveSessionKeys(byte[] sharedSecret, boolean initiator, byte[] transcript) throws Exception {
    byte[] salt = "agentbot-p2p-v1".getBytes(StandardCharsets.UTF_8);
    byte[] binding = transcript == null ? new byte[0] : transcript;
    byte[] sessionId = Hkdf.deriveKey(sharedSecret, salt, concat("session".getBytes(StandardCharsets.UTF_8), binding), 32);
    byte[] initiatorKey = Hkdf.deriveKey(sharedSecret, salt, concat("initiator".getBytes(StandardCharsets.UTF_8), binding), 32);
    byte[] responderKey = Hkdf.deriveKey(sharedSecret, salt, concat("responder".getBytes(StandardCharsets.UTF_8), binding), 32);
    if (initiator) {
      return new SessionKeys(initiatorKey, responderKey, sessionId);
    }
    return new SessionKeys(responderKey, initiatorKey, sessionId);
  }

  private void maybeRekey() {
    if (secureChannel == null || sharedSecret == null) return;
    if (!secureChannel.shouldRekeyRecv() && !secureChannel.shouldRekeySend()) return;
    rekey();
  }

  private void rekey() {
    try {
      rekeyEpoch += 1;
      byte[] salt = sessionKeys == null ? new byte[0] : sessionKeys.getSessionId();
      byte[] info = ("rekey-" + rekeyEpoch).getBytes(StandardCharsets.UTF_8);
      byte[] newInitiatorKey = Hkdf.deriveKey(sharedSecret, salt, concat("initiator".getBytes(StandardCharsets.UTF_8), info), 32);
      byte[] newResponderKey = Hkdf.deriveKey(sharedSecret, salt, concat("responder".getBytes(StandardCharsets.UTF_8), info), 32);
      if (initiator) {
        secureChannel.updateKeys(newInitiatorKey, newResponderKey);
      } else {
        secureChannel.updateKeys(newResponderKey, newInitiatorKey);
      }
    } catch (Exception ex) {
      close();
    }
  }

  private boolean verifyIdentity(VerackMessage verack) {
    if (verack == null) {
      boolean ok = !settings.isAuthRequired();
      if (!ok) {
        log.debug("P2P identity verify failed: missing verack while auth required");
      }
      return ok;
    }
    String signature = verack.getIdentitySignature();
    String pubKey = verack.getIdentityPubKey();
    if ((signature == null || signature.isBlank()) && settings.isAuthRequired()) {
      log.debug("P2P identity verify failed: missing signature, nodeId={}", verack.getNodeId());
      return false;
    }
    if (signature == null || signature.isBlank()) {
      return true;
    }
    if (sessionKeys == null) {
      log.debug("P2P identity verify failed: missing session keys, nodeId={}", verack.getNodeId());
      return false;
    }
    try {
      byte[] transcript = buildAuthTranscript(sessionKeys.getSessionId(), verack.getNodeId(), settings.getNodeId());
      boolean ok = IdentitySigner.verify(transcript, pubKey, signature);
      if (!ok) {
        log.debug("P2P identity verify failed: signature mismatch, nodeId={}", verack.getNodeId());
      }
      return ok;
    } catch (Exception ex) {
      log.debug("P2P identity verify failed: nodeId={}", verack.getNodeId(), ex);
      return false;
    }
  }

  private byte[] buildAuthTranscript(byte[] sessionId, String signerNodeId, String peerNodeId) {
    String signer = signerNodeId == null ? "" : signerNodeId;
    String peer = peerNodeId == null ? "" : peerNodeId;
    byte[] signerBytes = signer.getBytes(StandardCharsets.UTF_8);
    byte[] peerBytes = peer.getBytes(StandardCharsets.UTF_8);
    byte[] output = new byte[sessionId.length + signerBytes.length + peerBytes.length];
    System.arraycopy(sessionId, 0, output, 0, sessionId.length);
    System.arraycopy(signerBytes, 0, output, sessionId.length, signerBytes.length);
    System.arraycopy(peerBytes, 0, output, sessionId.length + signerBytes.length, peerBytes.length);
    return output;
  }

  private byte[] concat(byte[] left, byte[] right) {
    byte[] out = new byte[left.length + right.length];
    System.arraycopy(left, 0, out, 0, left.length);
    System.arraycopy(right, 0, out, left.length, right.length);
    return out;
  }

  private void closeWithReason(String reason) {
    closeReason = reason;
    log.warn("P2P closing connection: reason={}, remote={}:{}, initiator={}", reason, remoteHost(), remotePort(), initiator);
    close();
  }


  public void close() {
    if (!running && state == State.CLOSED) return;
    running = false;

    state = State.CLOSED;
    log.info("P2P connection closed: reason={}, remote={}:{}, initiator={}, handshakeComplete={}, remoteNodeId={}", closeReason, remoteHost(), remotePort(), initiator, handshakeComplete, remoteNodeId);
    if (retryTask != null) {
      retryTask.cancel(false);
    }
    if (heartbeatTask != null) {
      heartbeatTask.cancel(false);
    }
    pendingPings.clear();

    if (registry != null) {

      if (nodeRegistered) {
        registry.unregisterNode(remoteNodeId, this);
      }
      registry.unregisterAddress(addressKey, this);
      registry.unregisterConnection(this);
    }
    try {
      if (channel != null) {
        channel.close();
      }
    } catch (Exception ignored) {
      // ignore
    }
    P2pMetrics.recordClose();
  }

}
