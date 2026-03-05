package com.agentbot.core.bus;

import java.util.function.Consumer;

public interface UnifiedMessageBus {
  void publish(MessageEnvelope envelope);

  default void publishInv(MessageEnvelope envelope) {
    publish(envelope);
  }

  default void publishGetData(MessageEnvelope envelope) {
    publish(envelope);
  }

  default void publishData(MessageEnvelope envelope) {
    publish(envelope);
  }

  void subscribe(String topic, Consumer<MessageEnvelope> handler);


  void start();

  void stop();
}
