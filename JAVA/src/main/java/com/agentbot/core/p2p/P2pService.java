package com.agentbot.core.p2p;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.agent.AgentRegistry;
import com.agentbot.core.bus.ExternalMessageBus;
import com.agentbot.core.events.SystemEventBus;
import com.agentbot.core.identity.NodeIdentity;

import com.agentbot.core.identity.NodeIdentityService;
import com.agentbot.core.mesh.DataExchangeHandler;
import com.agentbot.core.mesh.InvStore;
import com.agentbot.core.mesh.MeshProtocolHandler;
import com.agentbot.core.mesh.RetryManager;

import com.agentbot.core.protocol.MessageCodecRegistry;
import com.agentbot.core.protocol.MessageType;
import com.agentbot.core.protocol.ProtocolCodec;
import com.agentbot.core.protocol.ProtocolDispatcher;
import com.agentbot.core.skills.SkillStoreService;




import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;




@Component
public class P2pService implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(P2pService.class);
  private final AgentbotProperties properties;
  private final ApplicationArguments args;
  private final ExternalMessageBus messageBus;
  private final AgentRegistry agentRegistry;
  private final SystemEventBus eventBus;
  private final SkillStoreService skillStoreService;


  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

  private final ConnectionRegistry registry = new ConnectionRegistry();
  private long getaddrWindowStartMs;
  private int getaddrWindowCount;

  private P2pServer server;



  public P2pService(AgentbotProperties properties, ApplicationArguments args, ExternalMessageBus messageBus, AgentRegistry agentRegistry, SystemEventBus eventBus, SkillStoreService skillStoreService) {
    this.properties = properties;
    this.args = args;
    this.messageBus = messageBus;
    this.agentRegistry = agentRegistry;
    this.eventBus = eventBus;
    this.skillStoreService = skillStoreService;
  }



  @Override
  public void run(ApplicationArguments arguments) {
    AgentbotProperties.P2p p2p = properties.getP2p();
    if (!p2p.isEnabled()) return;

    int port = p2p.getPort();
    if (port <= 0 || port > 65535) {
      log.error("P2P disabled due to invalid port: {}", port);
      return;
    }

    Path configDir = resolveConfigDir();

    Path peersFile = configDir.resolve(p2p.getPeersFile());
    PeerAddressBook addressBook = new PeerAddressBook(new YAMLMapper(), peersFile);

    NodeIdentity identity = resolveLocalIdentity(configDir);
    P2pSettings settings = P2pSettingsFactory.fromProperties(p2p, identity);

    MessageCodecRegistry codecRegistry = P2pSettingsFactory.defaultCodecRegistry();
    ProtocolCodec protocolCodec = new ProtocolCodec(codecRegistry);

    AddrExchangeHandler addrService = new AddrExchangeHandler(addressBook, p2p.getGetaddrLimit(), port);

    long getaddrBackoffMaxMs = TimeUnit.SECONDS.toMillis(Math.max(0, p2p.getGetaddrBackoffMaxSeconds()));

    ProtocolDispatcher dispatcher = new ProtocolDispatcher();
    dispatcher.register(MessageType.GETADDR, addrService);
    dispatcher.register(MessageType.ADDR, addrService);

    InvStore invStore = new InvStore();
    RetryManager retryManager = new RetryManager(3);
    SkillExchangeService skillExchangeService = new SkillExchangeService(p2p, settings, skillStoreService);
    DataExchangeHandler dataHandler = new DataExchangeHandler(invStore, retryManager, settings, skillExchangeService);
    MeshProtocolHandler meshHandler = new MeshProtocolHandler(retryManager);
    dispatcher.register(MessageType.INV, dataHandler);
    dispatcher.register(MessageType.GETDATA, dataHandler);
    dispatcher.register(MessageType.DATA, dataHandler);
    dispatcher.register(MessageType.ACK, meshHandler);
    dispatcher.register(MessageType.NACK, meshHandler);

    P2pChatHandler chatHandler = new P2pChatHandler(settings, registry, messageBus, agentRegistry, eventBus);

    chatHandler.start();
    dispatcher.register(MessageType.AGENT_CHAT, chatHandler);
    dispatcher.register(MessageType.AGENT_CHAT_ACK, chatHandler);
    dispatcher.register(MessageType.AGENT_CHAT_NACK, chatHandler);

    PeerDiscoveryManager discovery = new PeerDiscoveryManager(addressBook, new SeedResolver(), new PeerSelector(), addrService, protocolCodec, dispatcher, settings, port, registry, getaddrBackoffMaxMs);

    List<PeerAddress> manual = parseManualPeers(port);
    List<PeerAddress> connectOnly = parseConnectPeers(port);
    discovery.setConnectOnly(connectOnly);
    discovery.bootstrap(manual, p2p.getSeeds(), p2p.getMaxNeighbors());

    server = new P2pServer(port, channel -> {

      java.net.SocketAddress remote = channel.remoteAddress();
      String host = "unknown";
      int remotePort = -1;
      if (remote instanceof java.net.InetSocketAddress inet) {
        host = inet.getAddress() != null ? inet.getAddress().getHostAddress() : inet.getHostString();
        remotePort = inet.getPort();
      }
      String addressKey = ConnectionRegistry.addressKey(host, remotePort);

      P2pConnection connection = new P2pConnection(channel, false, protocolCodec, settings, addrService, dispatcher, registry, addressKey);
      channel.attr(P2pConnection.CONNECTION_KEY).set(connection);
      connection.setGetaddrBackoffMaxMs(getaddrBackoffMaxMs);
      connection.start();
    });


    server.start();

    scheduler.scheduleAtFixedRate(addressBook::save, p2p.getPersistSeconds(), p2p.getPersistSeconds(), TimeUnit.SECONDS);
    scheduler.scheduleAtFixedRate(() -> {
      discovery.rotateConnections(p2p.getMaxNeighbors());
      discovery.connectToCandidates(p2p.getMaxNeighbors());
    }, p2p.getRefreshSeconds(), p2p.getRefreshSeconds(), TimeUnit.SECONDS);

    int getaddrIntervalSeconds = p2p.getGetaddrIntervalSeconds();
    if (getaddrIntervalSeconds > 0) {
      int effectiveIntervalSeconds = getaddrIntervalSeconds;
      if (p2p.getRefreshSeconds() > 0 && effectiveIntervalSeconds < p2p.getRefreshSeconds()) {
        log.info("P2P getaddr interval raised to refreshSeconds: getaddrIntervalSeconds={}, refreshSeconds={}", getaddrIntervalSeconds, p2p.getRefreshSeconds());
        effectiveIntervalSeconds = p2p.getRefreshSeconds();
      }
      long minIntervalMs = TimeUnit.SECONDS.toMillis(effectiveIntervalSeconds);
      double sampleRatio = p2p.getGetaddrSampleRatio();
      int maxPerMinute = p2p.getGetaddrMaxPerMinute();
      scheduler.scheduleAtFixedRate(() -> {
        List<P2pConnection> connections = registry.listConnections();
        if (connections.isEmpty()) return;
        List<P2pConnection> eligible = connections.stream()
            .filter(P2pConnection::isHandshakeComplete)
            .toList();
        if (eligible.isEmpty()) return;
        List<P2pConnection> sorted = new ArrayList<>(eligible);
        sorted.sort(Comparator.comparingLong(P2pConnection::getLastGetAddrSentMs));
        int sampleCount = computeSampleCount(sorted.size(), sampleRatio);
        if (sampleCount <= 0) return;
        List<P2pConnection> sample = new ArrayList<>(sorted.subList(0, sampleCount));
        Collections.shuffle(sample);

        long now = System.currentTimeMillis();
        int remainingBudget = remainingGetaddrBudget(now, maxPerMinute);
        for (P2pConnection connection : sample) {
          if (remainingBudget <= 0) {
            P2pMetrics.recordGetAddrSkipped();
            break;
          }
          boolean sent = connection.requestGetAddr(minIntervalMs, "periodic");
          if (sent) {
            remainingBudget -= 1;
            incrementGetaddrWindowCount(1, now, maxPerMinute);
          }
        }
      }, effectiveIntervalSeconds, effectiveIntervalSeconds, TimeUnit.SECONDS);
    }

    int skillInvIntervalSeconds = p2p.getSkillInvIntervalSeconds();
    if (p2p.isSkillExchangeEnabled() && skillInvIntervalSeconds > 0) {
      scheduler.scheduleAtFixedRate(() -> {
        List<P2pConnection> connections = registry.listConnections();
        if (connections.isEmpty()) return;
        int sampleCount = skillExchangeService.sampleConnectionCount(connections.size());
        List<P2pConnection> sample = skillExchangeService.pickRandomConnections(connections, sampleCount);
        if (sample.isEmpty()) return;
        skillExchangeService.broadcastInv(sample);
      }, skillInvIntervalSeconds, skillInvIntervalSeconds, TimeUnit.SECONDS);
    }

  }

  private Path resolveConfigDir() {
    return com.agentbot.core.util.ConfigPathResolver.resolveConfigDir();
  }


  private List<PeerAddress> parseManualPeers(int defaultPort) {
    List<String> addNodes = args.getOptionValues("addnode");
    return parsePeers(addNodes, defaultPort, "manual");
  }

  private List<PeerAddress> parseConnectPeers(int defaultPort) {
    List<String> connectNodes = args.getOptionValues("connect");
    return parsePeers(connectNodes, defaultPort, "manual");
  }

  private List<PeerAddress> parsePeers(List<String> entries, int defaultPort, String source) {
    if (entries == null) return List.of();
    List<PeerAddress> peers = new ArrayList<>();
    for (String entry : entries) {
      if (entry == null || entry.isBlank()) continue;
      String[] parts = entry.trim().split(":", 2);
      String host = parts[0];
      int port = defaultPort;
      if (parts.length == 2) {
        try {
          port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ignored) {
          port = defaultPort;
        }
      }
      PeerAddress peer = new PeerAddress(host, port);
      peer.setSource(source);
      peers.add(peer);
    }
    return peers;
  }

  private int computeSampleCount(int size, double ratio) {
    if (size <= 0) return 0;
    if (ratio <= 0) return 0;
    if (ratio >= 1) return size;
    int count = (int) Math.ceil(size * ratio);
    return Math.max(1, Math.min(size, count));
  }

  private int remainingGetaddrBudget(long now, int maxPerMinute) {
    if (maxPerMinute <= 0) return Integer.MAX_VALUE;
    if (getaddrWindowStartMs <= 0 || now - getaddrWindowStartMs >= 60_000L) {
      getaddrWindowStartMs = now;
      getaddrWindowCount = 0;
    }
    return Math.max(0, maxPerMinute - getaddrWindowCount);
  }

  private void incrementGetaddrWindowCount(int delta, long now, int maxPerMinute) {
    if (maxPerMinute <= 0) return;
    if (getaddrWindowStartMs <= 0 || now - getaddrWindowStartMs >= 60_000L) {
      getaddrWindowStartMs = now;
      getaddrWindowCount = 0;
    }
    getaddrWindowCount += delta;
  }

  private NodeIdentity resolveLocalIdentity(Path configDir) {

    try {
      Path nodeFile = configDir.resolve("node.yml");
      NodeIdentityService service = new NodeIdentityService(new YAMLMapper(), nodeFile);

      return service.loadOrCreate();
    } catch (Exception ignored) {
      return null;
    }
  }

}

class ConnectionRegistry {
  private final ConcurrentHashMap<String, P2pConnection> byNodeId = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, P2pConnection> byAddress = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<P2pConnection, Boolean> connections = new ConcurrentHashMap<>();

  static String addressKey(String host, int port) {
    String safeHost = host == null ? "" : host;
    return safeHost + ":" + port;
  }

  boolean registerAddress(String key, P2pConnection connection) {
    if (key == null || key.isBlank()) return true;
    return byAddress.putIfAbsent(key, connection) == null;
  }

  void unregisterAddress(String key, P2pConnection connection) {
    if (key == null || key.isBlank()) return;
    byAddress.remove(key, connection);
  }

  boolean registerNode(String nodeId, P2pConnection connection) {
    if (nodeId == null || nodeId.isBlank()) return true;
    P2pConnection existing = byNodeId.putIfAbsent(nodeId, connection);
    return existing == null || existing == connection;
  }

  P2pConnection findByNodeId(String nodeId) {
    if (nodeId == null || nodeId.isBlank()) return null;
    return byNodeId.get(nodeId);
  }

  void unregisterNode(String nodeId, P2pConnection connection) {
    if (nodeId == null || nodeId.isBlank()) return;
    byNodeId.remove(nodeId, connection);
  }


  void registerConnection(P2pConnection connection) {
    if (connection == null) return;
    connections.put(connection, Boolean.TRUE);
  }

  void unregisterConnection(P2pConnection connection) {
    if (connection == null) return;
    connections.remove(connection);
  }

  boolean isAddressActive(String host, int port) {
    return byAddress.containsKey(addressKey(host, port));
  }

  int activeConnectionCount() {
    return connections.size();
  }

  List<P2pConnection> listConnections() {
    return new ArrayList<>(connections.keySet());
  }
}

