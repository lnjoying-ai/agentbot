package com.agentbot.core.p2p;

import com.agentbot.core.protocol.AddrMessage;
import com.agentbot.core.protocol.GetAddrMessage;
import com.agentbot.core.protocol.MessageType;
import com.agentbot.core.protocol.P2pHeader;
import com.agentbot.core.protocol.ProtocolHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import java.util.stream.Collectors;



public class AddrExchangeHandler implements ProtocolHandler {
  private static final Logger log = LoggerFactory.getLogger(AddrExchangeHandler.class);

  private final PeerAddressBook addressBook;

  private final int getaddrLimit;
  private final int listenPort;

  public AddrExchangeHandler(PeerAddressBook addressBook, int getaddrLimit, int listenPort) {
    this.addressBook = addressBook;
    this.getaddrLimit = getaddrLimit;
    this.listenPort = listenPort;
  }


  @Override
  public void handle(P2pConnection connection, P2pHeader header, Object payload) {
    if (header == null) return;
    MessageType type = header.getMsgType();
    if (type == MessageType.GETADDR) {
      handleGetAddr(connection, payload instanceof GetAddrMessage ? (GetAddrMessage) payload : null);
      return;
    }
    if (type == MessageType.ADDR) {
      handleAddr(connection, payload instanceof AddrMessage ? (AddrMessage) payload : null);
    }


  }


  public void sendGetAddr(P2pConnection connection) {
    GetAddrMessage payload = new GetAddrMessage();
    payload.setLimit(getaddrLimit);
    connection.send(MessageType.GETADDR, payload);
  }

  public void recordPeerSuccess(String host, int port, String nodeId, long latencyMs) {
    if (nodeId != null && !nodeId.isBlank()) {
      addressBook.markSuccessByNodeId(nodeId, latencyMs);
      return;
    }
    if (host == null || host.isBlank() || port <= 0) return;
    PeerAddress peer = new PeerAddress(host, port);
    peer.setSource("peers");
    addressBook.markSuccess(peer, latencyMs);
  }

  public void recordPeerFailure(String host, int port, String nodeId) {
    if (nodeId != null && !nodeId.isBlank()) {
      addressBook.markFailureByNodeId(nodeId);
      return;
    }
    if (host == null || host.isBlank() || port <= 0) return;
    PeerAddress peer = new PeerAddress(host, port);
    peer.setSource("peers");
    addressBook.markFailure(peer);
  }


  private void handleGetAddr(P2pConnection connection, GetAddrMessage request) {
    List<PeerAddress> peers = addressBook.list();
    int limit = request != null && request.getLimit() > 0 ? request.getLimit() : getaddrLimit;
    limit = Math.min(limit, peers.size());
    List<PeerAddress> subset = peers.subList(0, limit);

    AddrMessage response = new AddrMessage();
    response.setNodes(subset.stream().map(peer -> {
      AddrMessage.NodeInfo info = new AddrMessage.NodeInfo();
      info.setNodeId(peer.getNodeId());
      info.setRegionId(peer.getRegionId());
      info.setEndpoint(peer.getHost() + ":" + peer.getPort());
      info.setLastSeen(peer.getLastSeen());
      return info;
    }).collect(Collectors.toList()));

    connection.send(MessageType.ADDR, response);
  }

  private void handleAddr(P2pConnection connection, AddrMessage message) {
    if (message == null || message.getNodes() == null) return;
    int total = message.getNodes().size();
    int accepted = 0;
    int invalid = 0;
    for (AddrMessage.NodeInfo node : message.getNodes()) {
      String endpoint = node.getEndpoint();
      if (endpoint == null || endpoint.isBlank()) {
        invalid += 1;
        continue;
      }
      String[] parts = endpoint.split(":", 2);
      String host = parts[0];
      boolean isIp = HostValidator.isValidIpLiteral(host);
      boolean isHost = !isIp && HostValidator.isValidHostname(host);
      if (!isIp && !isHost) {
        invalid += 1;
        P2pAudit.warn(log, "addr-invalid-host", "endpoint=" + endpoint + ", nodeId=" + node.getNodeId());
        continue;
      }
      boolean publicOk = isIp ? HostValidator.isPublicIp(host) : HostValidator.isPublicHostname(host);
      if (!publicOk) {
        invalid += 1;
        P2pAudit.warn(log, "addr-non-public-host", "endpoint=" + endpoint + ", nodeId=" + node.getNodeId());
        continue;
      }


      int port = listenPort;

      if (parts.length == 2) {
        try {
          port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ignored) {
          port = listenPort;
        }
      }
      PeerAddress peer = new PeerAddress(host, port);
      peer.setNodeId(node.getNodeId());
      peer.setRegionId(node.getRegionId());
      peer.setLastSeen(node.getLastSeen());
      peer.setSource("addr");
      addressBook.upsert(peer);
      accepted += 1;
    }

    if (total > 0) {
      P2pMetrics.recordAddrReceived(total);
      P2pMetrics.recordAddrAccepted(accepted);
      P2pMetrics.recordAddrInvalid(invalid);
    } else {
      P2pMetrics.recordAddrEmpty();
    }

    if (connection != null) {
      connection.recordAddrResponse(total, accepted, invalid);
    }
  }



}
