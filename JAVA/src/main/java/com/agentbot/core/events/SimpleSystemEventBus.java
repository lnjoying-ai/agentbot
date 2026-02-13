package com.agentbot.core.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class SimpleSystemEventBus implements SystemEventBus {
  private final List<Consumer<SystemEvent>> subscribers = new CopyOnWriteArrayList<>();

  @Override
  public Subscription subscribe(Consumer<SystemEvent> handler) {
    subscribers.add(handler);
    return () -> subscribers.remove(handler);
  }

  @Override
  public void publish(SystemEvent event) {
    for (Consumer<SystemEvent> subscriber : subscribers) {
      try {
        subscriber.accept(event);
      } catch (Exception ignored) {
        // ignore faulty subscribers
      }
    }
  }
}
