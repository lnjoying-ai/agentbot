package com.agentbot.core.p2p;

import com.agentbot.core.protocol.ProtocolCodec;
import com.agentbot.core.protocol.ProtocolDispatcher;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.net.InetAddress;
import java.util.ArrayList;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;



public class PeerDiscoveryManager {
  private final PeerAddressBook addressBook;
  private final SeedResolver seedResolver;
  private final PeerSelector selector;
  private final AddrExchangeHandler addrExchangeService;

  private final ProtocolCodec protocolCodec;
  private final ProtocolDispatcher dispatcher;
  private static final long MIN_CONNECTION_AGE_MS = 30_000L;
  private static final long ROTATE_COOLDOWN_MS = 30_000L;

  private final P2pSettings settings;
  private final int defaultPort;
  private final ConnectionRegistry registry;
  private final long getaddrBackoffMaxMs;
  private static final int MAX_FRAME_SIZE = 16 * 1024 * 1024;
  private final EventLoopGroup clientGroup = new NioEventLoopGroup(Math.max(2, Runtime.getRuntime().availableProcessors()));


  private List<PeerAddress> connectOnly = List.of();

  private volatile long lastRotateMs;

  public PeerDiscoveryManager(PeerAddressBook addressBook,
                              SeedResolver seedResolver,
                              PeerSelector selector,
                              AddrExchangeHandler addrExchangeService,

                              ProtocolCodec protocolCodec,
                              ProtocolDispatcher dispatcher,
                              P2pSettings settings,
                              int defaultPort,
                              ConnectionRegistry registry,
                              long getaddrBackoffMaxMs) {

    this.addressBook = addressBook;
    this.seedResolver = seedResolver;
    this.selector = selector;
    this.addrExchangeService = addrExchangeService;
    this.protocolCodec = protocolCodec;
    this.dispatcher = dispatcher;
    this.settings = settings;
    this.defaultPort = defaultPort;
    this.registry = registry;
    this.getaddrBackoffMaxMs = getaddrBackoffMaxMs;

  }





  public void setConnectOnly(List<PeerAddress> connectOnly) {
    this.connectOnly = connectOnly == null ? List.of() : connectOnly;
  }

  public void bootstrap(List<PeerAddress> manual, List<String> seeds, int maxNeighbors) {
    addressBook.load();
    manual.forEach(addressBook::upsert);
    if (!connectOnly.isEmpty()) {
      connectOnly.forEach(addressBook::upsert);
    } else if (addressBook.list().isEmpty()) {
      seedResolver.resolve(seeds, defaultPort).forEach(addressBook::upsert);
    }
    connectToCandidates(maxNeighbors);
  }

  public void connectToCandidates(int maxNeighbors) {
    List<PeerAddress> candidates = connectOnly.isEmpty() ? addressBook.list() : new ArrayList<>(connectOnly);
    List<PeerAddress> selected = selector.select(candidates, maxNeighbors);
    for (PeerAddress peer : selected) {
      if (shouldSkip(peer)) continue;
      connect(peer);
    }
  }


  public void connect(PeerAddress peer) {
    if (peer == null || peer.getHost() == null || peer.getHost().isBlank() || peer.getPort() <= 0) return;
    if (shouldSkip(peer)) return;
    long start = System.currentTimeMillis();
    try {
      Bootstrap bootstrap = new Bootstrap()
          .group(clientGroup)
          .channel(NioSocketChannel.class)
          .option(ChannelOption.TCP_NODELAY, true)
          .option(ChannelOption.SO_KEEPALIVE, true)
          .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
              ChannelPipeline pipeline = ch.pipeline();
              pipeline.addLast(new LengthFieldBasedFrameDecoder(MAX_FRAME_SIZE, 0, 4, 0, 4));
              pipeline.addLast(new LengthFieldPrepender(4));
              pipeline.addLast(new P2pConnection.NettyPacketHandler());
            }
          });

      ChannelFuture future = bootstrap.connect(peer.getHost(), peer.getPort());
      future.awaitUninterruptibly();
      if (!future.isSuccess()) {
        addressBook.markFailure(peer);
        return;
      }

      Channel channel = future.channel();
      java.net.SocketAddress remote = channel.remoteAddress();
      String resolvedHost = peer.getHost();
      int resolvedPort = peer.getPort();
      if (remote instanceof java.net.InetSocketAddress inet) {
        resolvedHost = inet.getAddress() != null ? inet.getAddress().getHostAddress() : inet.getHostString();
        resolvedPort = inet.getPort();
      }
      String addressKey = ConnectionRegistry.addressKey(resolvedHost, resolvedPort);
      P2pConnection connection = new P2pConnection(channel, true, protocolCodec, settings, addrExchangeService, dispatcher, registry, addressKey);
      channel.attr(P2pConnection.CONNECTION_KEY).set(connection);
      connection.setGetaddrBackoffMaxMs(getaddrBackoffMaxMs);
      connection.start();

      addressBook.markSuccess(peer, System.currentTimeMillis() - start);

    } catch (Exception ex) {
      addressBook.markFailure(peer);
    }
  }



  public void rotateConnections(int maxNeighbors) {
    if (registry == null) return;
    long now = System.currentTimeMillis();
    if (now - lastRotateMs < ROTATE_COOLDOWN_MS) return;
    if (registry.activeConnectionCount() < maxNeighbors) return;

    List<PeerAddress> candidates = connectOnly.isEmpty() ? addressBook.list() : new ArrayList<>(connectOnly);
    candidates = candidates.stream().filter(peer -> !shouldSkip(peer)).toList();
    if (candidates.isEmpty()) return;

    List<P2pConnection> connections = registry.listConnections();
    List<P2pConnection> eligible = connections.stream()
        .filter(P2pConnection::isHandshakeComplete)
        .filter(conn -> conn.getConnectionAgeMs() >= MIN_CONNECTION_AGE_MS)
        .toList();
    if (eligible.isEmpty()) return;

    P2pConnection drop = eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));
    PeerAddress target = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

    lastRotateMs = now;
    drop.close();
    connect(target);
  }

  private boolean shouldSkip(PeerAddress peer) {
    if (peer == null) return true;
    if (settings.getNodeId() != null && peer.getNodeId() != null && settings.getNodeId().equals(peer.getNodeId())) {
      return true;
    }
    if (registry == null) return false;
    if (registry.isAddressActive(peer.getHost(), peer.getPort())) return true;
    String resolvedHost = resolveHost(peer.getHost());
    return resolvedHost != null && registry.isAddressActive(resolvedHost, peer.getPort());
  }

  private String resolveHost(String host) {
    if (host == null || host.isBlank()) return null;
    try {
      InetAddress address = InetAddress.getByName(host);
      return address.getHostAddress();
    } catch (Exception ex) {
      return null;
    }
  }


}


