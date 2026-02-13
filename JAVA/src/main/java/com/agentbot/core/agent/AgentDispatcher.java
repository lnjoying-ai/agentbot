package com.agentbot.core.agent;

import com.agentbot.core.bus.MessageBus;
import com.agentbot.core.bus.SimpleMessageBus;
import com.agentbot.core.bus.events.InboundMessage;
import com.agentbot.core.bus.events.OutboundMessage;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AgentDispatcher {
  private final MessageBus messageBus;
  private final AgentRouter router;
  private final Map<String, AgentRuntime> runtimes;
  private final ExecutorService executor = Executors.newFixedThreadPool(4);
  private volatile boolean running = false;

  public AgentDispatcher(MessageBus messageBus, AgentRouter router, Map<String, AgentRuntime> runtimes) {
    this.messageBus = messageBus;
    this.router = router;
    this.runtimes = runtimes;
  }

  public void start() {
    if (running) return;
    running = true;
    if (messageBus instanceof SimpleMessageBus bus) {
      executor.execute(() -> {
        while (running) {
          try {
            InboundMessage inbound = bus.inboundQueue().take();
            executor.execute(() -> dispatch(inbound));
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return;
          }
        }
      });
    }
  }

  public void stop() {
    running = false;
    executor.shutdownNow();
  }

  private void dispatch(InboundMessage inbound) {
    String agentId = router.resolveAgentId(inbound);
    AgentRuntime runtime = runtimes.get(agentId);
    if (runtime == null) return;
    OutboundMessage out = runtime.handle(inbound);
    if (out != null) {
      messageBus.publishOutbound(out);
    }
  }
}
