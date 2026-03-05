package com.agentbot.core.bus;

import com.agentbot.core.bus.events.InboundMessage;
import com.agentbot.core.bus.events.OutboundMessage;
import com.agentbot.core.events.SystemEvent;
import com.agentbot.core.events.SystemEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class SimpleMessageBus implements MessageBus {
  private static final Logger log = LoggerFactory.getLogger(SimpleMessageBus.class);
  private final BlockingQueue<InboundMessage> inbound = new LinkedBlockingQueue<>();

  private final BlockingQueue<OutboundMessage> outbound = new LinkedBlockingQueue<>();
  private final Map<String, List<Consumer<OutboundMessage>>> outboundHandlers = new ConcurrentHashMap<>();
  private final ExecutorService dispatcher = Executors.newSingleThreadExecutor();
  private final SystemEventBus eventBus;
  private volatile boolean running = false;

  public SimpleMessageBus(SystemEventBus eventBus) {
    this.eventBus = eventBus;
  }

  public BlockingQueue<InboundMessage> inboundQueue() {
    return inbound;
  }


  @Override
  public void publishInbound(InboundMessage message) {
    if (message != null) {
      inbound.offer(message);
      log.debug("Inbound queued: channel={}, chatId={}", message.getChannel(), message.getChatId());
      publishEvent("inbound.message", Map.of(
          "channel", message.getChannel(),
          "chatId", message.getChatId(),
          "senderId", message.getSenderId(),
          "content", message.getContent(),
          "timestamp", message.getTimestamp().toString()
      ));
    }
  }


  @Override
  public void publishOutbound(OutboundMessage message) {
    if (message != null) {
      outbound.offer(message);
      log.debug("Outbound queued: channel={}, chatId={}", message.getChannel(), message.getChatId());
      publishEvent("outbound.message", Map.of(
          "channel", message.getChannel(),
          "chatId", message.getChatId(),
          "content", message.getContent(),
          "timestamp", message.getTimestamp().toString()
      ));
    }
  }



  @Override
  public void subscribeOutbound(String channel, Consumer<OutboundMessage> handler) {
    outboundHandlers.computeIfAbsent(channel, key -> new ArrayList<>()).add(handler);
  }

  @Override
  public void start() {
    if (running) return;
    running = true;
    log.info("SimpleMessageBus started");
    dispatcher.execute(() -> {
      while (running) {
        try {
          OutboundMessage message = outbound.take();
          List<Consumer<OutboundMessage>> handlers = outboundHandlers.getOrDefault(message.getChannel(), List.of());
          for (Consumer<OutboundMessage> handler : handlers) {
            try {
              handler.accept(message);
            } catch (Exception error) {
              log.warn("Outbound handler failed: channel={}", message.getChannel(), error);
            }
          }
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    });
  }


  @Override
  public void stop() {
    running = false;
    dispatcher.shutdownNow();
    log.info("SimpleMessageBus stopped");
  }


  private void publishEvent(String type, Map<String, Object> payload) {
    if (eventBus == null) return;
    eventBus.publish(new SystemEvent(type, payload));
  }
}

