package com.agentbot.core.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;


public class SimpleSystemEventBus implements SystemEventBus {
  private static final Logger log = LoggerFactory.getLogger(SimpleSystemEventBus.class);
  private final List<Consumer<SystemEvent>> subscribers = new CopyOnWriteArrayList<>();


  @Override
  public Subscription subscribe(Consumer<SystemEvent> handler) {
    subscribers.add(handler);
    return () -> subscribers.remove(handler);
  }

  @Override
  public void publish(SystemEvent event) {
    if (event == null) return;
    log.debug("Publish system event: type={}, subscribers={}", event.getType(), subscribers.size());
    for (Consumer<SystemEvent> subscriber : subscribers) {
      try {
        subscriber.accept(event);
      } catch (Exception error) {
        log.warn("SystemEvent subscriber failed: type={}", event.getType(), error);
      }
    }
  }

}
