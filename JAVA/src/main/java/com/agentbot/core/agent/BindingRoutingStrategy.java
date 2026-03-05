package com.agentbot.core.agent;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.bus.events.InboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Route inbound messages using configured bindings.
 */
public class BindingRoutingStrategy implements AgentRoutingStrategy {
  private static final Logger log = LoggerFactory.getLogger(BindingRoutingStrategy.class);

  private final AgentbotProperties properties;

  public BindingRoutingStrategy(AgentbotProperties properties) {
    this.properties = properties;
  }

  @Override
  public String route(InboundMessage message, AgentRegistry registry) {
    if (properties == null || message == null) return null;
    List<AgentbotProperties.Binding> bindings = properties.getBindings();
    if (bindings == null || bindings.isEmpty()) return null;

    String channel = normalizeToken(message.getChannel());
    if (channel.isBlank()) return null;

    String accountId = normalizeAccountId(message.getAccountId());
    String peerKind = normalizeToken(message.getPeerKind());
    String peerId = normalizeId(message.getPeerId());
    String guildId = normalizeId(message.getGuildId());
    String teamId = normalizeId(message.getTeamId());

    AgentbotProperties.Binding match = findPeerMatch(bindings, channel, accountId, peerKind, peerId);
    if (match == null && !guildId.isBlank()) {
      match = findGuildMatch(bindings, channel, accountId, guildId);
    }
    if (match == null && !teamId.isBlank()) {
      match = findTeamMatch(bindings, channel, accountId, teamId);
    }
    if (match == null) {
      match = findAccountMatch(bindings, channel, accountId);
    }
    if (match == null) {
      match = findWildcardAccountMatch(bindings, channel);
    }
    if (match == null) {
      match = findChannelMatch(bindings, channel);
    }

    if (match == null) return null;
    String agentId = match.getAgentId();
    if (agentId == null || agentId.isBlank()) return null;
    if (!registry.hasAgent(agentId)) {
      log.warn("Binding matched agentId but not found: {}", agentId);
      return null;
    }
    log.debug("Binding routing: channel={} accountId={} peerKind={} peerId={} -> {}", channel, accountId, peerKind, peerId, agentId);
    return agentId;
  }

  private AgentbotProperties.Binding findPeerMatch(List<AgentbotProperties.Binding> bindings, String channel, String accountId, String peerKind, String peerId) {
    if (peerKind.isBlank() || peerId.isBlank()) return null;
    for (AgentbotProperties.Binding binding : bindings) {
      AgentbotProperties.Match match = binding == null ? null : binding.getMatch();
      if (!matchesChannel(match, channel)) continue;
      if (!matchesAccountId(match == null ? null : match.getAccountId(), accountId)) continue;
      AgentbotProperties.Peer peer = match == null ? null : match.getPeer();
      if (peer == null) continue;
      if (!normalizeToken(peer.getKind()).equals(peerKind)) continue;
      if (!normalizeId(peer.getId()).equals(peerId)) continue;
      return binding;
    }
    return null;
  }

  private AgentbotProperties.Binding findGuildMatch(List<AgentbotProperties.Binding> bindings, String channel, String accountId, String guildId) {
    for (AgentbotProperties.Binding binding : bindings) {
      AgentbotProperties.Match match = binding == null ? null : binding.getMatch();
      if (!matchesChannel(match, channel)) continue;
      if (!matchesAccountId(match == null ? null : match.getAccountId(), accountId)) continue;
      if (!normalizeId(match == null ? null : match.getGuildId()).equals(guildId)) continue;
      if (match != null && match.getPeer() != null) continue;
      return binding;
    }
    return null;
  }

  private AgentbotProperties.Binding findTeamMatch(List<AgentbotProperties.Binding> bindings, String channel, String accountId, String teamId) {
    for (AgentbotProperties.Binding binding : bindings) {
      AgentbotProperties.Match match = binding == null ? null : binding.getMatch();
      if (!matchesChannel(match, channel)) continue;
      if (!matchesAccountId(match == null ? null : match.getAccountId(), accountId)) continue;
      if (!normalizeId(match == null ? null : match.getTeamId()).equals(teamId)) continue;
      if (match != null && match.getPeer() != null) continue;
      return binding;
    }
    return null;
  }

  private AgentbotProperties.Binding findAccountMatch(List<AgentbotProperties.Binding> bindings, String channel, String accountId) {
    for (AgentbotProperties.Binding binding : bindings) {
      AgentbotProperties.Match match = binding == null ? null : binding.getMatch();
      if (!matchesChannel(match, channel)) continue;
      if (match == null) continue;
      if (isBlank(match.getAccountId()) || "*".equals(match.getAccountId().trim())) continue;
      if (!matchesAccountId(match.getAccountId(), accountId)) continue;
      if (match.getPeer() != null || !isBlank(match.getGuildId()) || !isBlank(match.getTeamId())) continue;
      return binding;
    }
    return null;
  }

  private AgentbotProperties.Binding findWildcardAccountMatch(List<AgentbotProperties.Binding> bindings, String channel) {
    for (AgentbotProperties.Binding binding : bindings) {
      AgentbotProperties.Match match = binding == null ? null : binding.getMatch();
      if (!matchesChannel(match, channel)) continue;
      if (match == null) continue;
      if (!"*".equals(match.getAccountId() == null ? "" : match.getAccountId().trim())) continue;
      if (match.getPeer() != null || !isBlank(match.getGuildId()) || !isBlank(match.getTeamId())) continue;
      return binding;
    }
    return null;
  }

  private AgentbotProperties.Binding findChannelMatch(List<AgentbotProperties.Binding> bindings, String channel) {
    for (AgentbotProperties.Binding binding : bindings) {
      AgentbotProperties.Match match = binding == null ? null : binding.getMatch();
      if (!matchesChannel(match, channel)) continue;
      if (match == null) continue;
      if (!isBlank(match.getAccountId())) continue;
      if (match.getPeer() != null || !isBlank(match.getGuildId()) || !isBlank(match.getTeamId())) continue;
      return binding;
    }
    return null;
  }

  private boolean matchesChannel(AgentbotProperties.Match match, String channel) {
    if (match == null) return false;
    String matchChannel = normalizeToken(match.getChannel());
    return !matchChannel.isBlank() && matchChannel.equals(channel);
  }

  private boolean matchesAccountId(String matchAccountId, String actualAccountId) {
    String trimmed = matchAccountId == null ? "" : matchAccountId.trim();
    if (trimmed.isBlank()) {
      return "default".equals(actualAccountId);
    }
    if ("*".equals(trimmed)) return true;
    return normalizeAccountId(trimmed).equals(actualAccountId);
  }

  private String normalizeAccountId(String value) {
    String trimmed = value == null ? "" : value.trim();
    return trimmed.isBlank() ? "default" : trimmed.toLowerCase();
  }

  private String normalizeToken(String value) {
    return value == null ? "" : value.trim().toLowerCase();
  }

  private String normalizeId(String value) {
    return value == null ? "" : value.trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isBlank();
  }
}
