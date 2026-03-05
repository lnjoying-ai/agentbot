package com.agentbot.core.agent;

import com.agentbot.core.bus.ExternalMessageBus;
import com.agentbot.core.bus.MessageEnvelope;
import com.agentbot.core.bus.events.InboundMessage;
import com.agentbot.core.bus.events.OutboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AgentDispatcher {
  private static final Logger log = LoggerFactory.getLogger(AgentDispatcher.class);
  private final ExternalMessageBus messageBus;

  private final AgentRouter router;
  private final Map<String, AgentRuntime> runtimes;
  private final ExecutorService executor = Executors.newFixedThreadPool(4);
  private volatile boolean running = false;

  public AgentDispatcher(ExternalMessageBus messageBus, AgentRouter router, Map<String, AgentRuntime> runtimes) {
    this.messageBus = messageBus;
    this.router = router;
    this.runtimes = runtimes;
  }

  public void start() {
    if (running) return;
    running = true;
    log.info("AgentDispatcher started");
    messageBus.subscribe(MessageEnvelope.TOPIC_EXTERNAL_INBOUND, envelope -> {
      if (!running) return;
      executor.execute(() -> dispatch(envelope));
    });
  }


  public void stop() {
    running = false;
    executor.shutdownNow();
    log.info("AgentDispatcher stopped");
  }


  private void dispatch(MessageEnvelope envelope) {
    InboundMessage inbound = new InboundMessage(
        envelope.getChannel(),
        envelope.getSenderId(),
        envelope.getChatId(),
        envelope.getContent()
    );
    if (envelope.getMetadata() != null) {
      inbound.getMetadata().putAll(envelope.getMetadata());
    }
    putIfAbsent(inbound.getMetadata(), MessageEnvelope.META_ACCOUNT_ID, envelope.getAccountId());
    putIfAbsent(inbound.getMetadata(), MessageEnvelope.META_PEER_KIND, envelope.getPeerKind());
    putIfAbsent(inbound.getMetadata(), MessageEnvelope.META_PEER_ID, envelope.getPeerId());
    putIfAbsent(inbound.getMetadata(), MessageEnvelope.META_GUILD_ID, envelope.getGuildId());
    putIfAbsent(inbound.getMetadata(), MessageEnvelope.META_TEAM_ID, envelope.getTeamId());

    String agentId = router.resolveAgentId(inbound);
    if (agentId != null && !agentId.isBlank()) {
      inbound.getMetadata().put("agentId", agentId);
    }

    log.debug("Dispatch inbound: channel={}, chatId={}, agentId={}", inbound.getChannel(), inbound.getChatId(), agentId);

    AgentRuntime runtime = runtimes.get(agentId);
    if (runtime == null) {
      log.warn("No runtime for agentId={}", agentId);
      return;
    }
    OutboundMessage out = runtime.handle(inbound);

    if (out != null) {
      if (agentId != null && !agentId.isBlank()) {
        out.getMetadata().putIfAbsent("agentId", agentId);
      }
      propagateRouteMetadata(inbound, out);
      log.debug("Dispatch outbound: channel={}, chatId={}", out.getChannel(), out.getChatId());
      messageBus.publish(MessageEnvelope.externalOutbound(
          out.getChannel(),
          out.getChatId(),
          out.getContent(),
          out.getMetadata()
      ));
    }
  }

  private void propagateRouteMetadata(InboundMessage inbound, OutboundMessage outbound) {
    if (inbound == null || outbound == null) return;
    putIfAbsent(outbound.getMetadata(), MessageEnvelope.META_ACCOUNT_ID, inbound.getAccountId());
    putIfAbsent(outbound.getMetadata(), MessageEnvelope.META_PEER_KIND, inbound.getPeerKind());
    putIfAbsent(outbound.getMetadata(), MessageEnvelope.META_PEER_ID, inbound.getPeerId());
    putIfAbsent(outbound.getMetadata(), MessageEnvelope.META_GUILD_ID, inbound.getGuildId());
    putIfAbsent(outbound.getMetadata(), MessageEnvelope.META_TEAM_ID, inbound.getTeamId());
  }

  private void putIfAbsent(Map<String, Object> metadata, String key, String value) {
    if (metadata == null || key == null || value == null || value.isBlank()) return;
    metadata.putIfAbsent(key, value);
  }
}

