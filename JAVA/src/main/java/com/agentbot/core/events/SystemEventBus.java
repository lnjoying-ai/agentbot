package com.agentbot.core.events;

import java.util.function.Consumer;

public interface SystemEventBus {
  Subscription subscribe(Consumer<SystemEvent> handler);

  void publish(SystemEvent event);

  interface Subscription {
    void unsubscribe();
  }
}
