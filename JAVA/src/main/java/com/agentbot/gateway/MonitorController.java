package com.agentbot.gateway;

import com.agentbot.core.events.SystemEvent;
import com.agentbot.core.events.SystemEventBus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {
  private final SystemEventBus eventBus;

  public MonitorController(SystemEventBus eventBus) {
    this.eventBus = eventBus;
  }

  @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter events() {
    SseEmitter emitter = new SseEmitter(0L);
    SystemEventBus.Subscription subscription = eventBus.subscribe(event -> sendEvent(emitter, event));
    emitter.onCompletion(subscription::unsubscribe);
    emitter.onTimeout(() -> {
      subscription.unsubscribe();
      emitter.complete();
    });
    return emitter;
  }

  private void sendEvent(SseEmitter emitter, SystemEvent event) {
    try {
      emitter.send(SseEmitter.event().name(event.getType()).data(event));
    } catch (IOException error) {
      emitter.completeWithError(error);
    }
  }
}
