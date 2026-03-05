package com.agentbot.gateway;

import com.agentbot.core.events.SystemEvent;
import com.agentbot.core.events.SystemEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/p2p/chat")
public class P2pChatEventController {
  private static final Logger log = LoggerFactory.getLogger(P2pChatEventController.class);
  private final SystemEventBus eventBus;


  public P2pChatEventController(SystemEventBus eventBus) {
    this.eventBus = eventBus;
  }

  @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter events() {
    log.info("P2P SSE stream connected");
    SseEmitter emitter = new SseEmitter(0L);
    SystemEventBus.Subscription subscription = eventBus.subscribe(event -> {
      if (!isP2pChatEvent(event)) return;
      sendEvent(emitter, event);
    });
    emitter.onCompletion(() -> {
      log.info("P2P SSE stream completed");
      subscription.unsubscribe();
    });
    emitter.onTimeout(() -> {
      log.warn("P2P SSE stream timeout");
      subscription.unsubscribe();
      emitter.complete();
    });
    return emitter;
  }


  private boolean isP2pChatEvent(SystemEvent event) {
    if (event == null) return false;
    String type = event.getType();
    return "p2p.chat.inbound".equals(type)
        || "p2p.chat.outbound".equals(type)
        || "p2p.chat.ack".equals(type)
        || "p2p.chat.nack".equals(type);
  }

  private void sendEvent(SseEmitter emitter, SystemEvent event) {
    try {
      Map<String, Object> data = new HashMap<>();
      data.put("type", event.getType());
      data.put("timestamp", event.getTimestamp().toString());
      data.put("payload", event.getPayload());
      log.debug("P2P SSE send: type={}", event.getType());
      emitter.send(data);
    } catch (IOException error) {
      log.warn("P2P SSE send failed", error);
      emitter.completeWithError(error);
    }

  }
}
