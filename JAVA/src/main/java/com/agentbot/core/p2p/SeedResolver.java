package com.agentbot.core.p2p;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class SeedResolver {
  public List<PeerAddress> resolve(List<String> seeds, int defaultPort) {
    List<PeerAddress> result = new ArrayList<>();
    if (seeds == null) return result;
    for (String seed : seeds) {
      if (seed == null || seed.isBlank()) continue;
      try {
        InetAddress[] addresses = InetAddress.getAllByName(seed.trim());
        for (InetAddress address : addresses) {
          PeerAddress peer = new PeerAddress(address.getHostAddress(), defaultPort);
          peer.setSource("seed");
          result.add(peer);
        }
      } catch (Exception ignored) {
        // ignore DNS failures
      }
    }
    return result;
  }
}
