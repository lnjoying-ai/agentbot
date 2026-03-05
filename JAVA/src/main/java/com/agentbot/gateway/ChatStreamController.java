package com.agentbot.gateway;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.events.SystemEvent;
import com.agentbot.core.events.SystemEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/chat")
public class ChatStreamController {
  private static final Logger log = LoggerFactory.getLogger(ChatStreamController.class);
  private final SystemEventBus eventBus;
  private final int bufferSize;
  private final boolean sseEnabled;
  private final Map<String, Deque<Map<String, Object>>> backlog = new ConcurrentHashMap<>();
  private final Map<String, CopyOnWriteArrayList<SseEmitter>> streams = new ConcurrentHashMap<>();


  public ChatStreamController(SystemEventBus eventBus, AgentbotProperties properties) {
    this.eventBus = eventBus;
    this.bufferSize = Math.max(0, properties.getOps().getChatStreamBufferSize());
    this.sseEnabled = "sse".equalsIgnoreCase(properties.getOps().getChatMode());
    if (this.sseEnabled) {
      this.eventBus.subscribe(this::handleEvent);
    }
  }

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(
      @RequestParam("chatId") String chatId,
      @RequestParam(value = "channel", defaultValue = "web") String channel
  ) {
    SseEmitter emitter = new SseEmitter(0L);
    if (!sseEnabled) {
      log.info("SSE stream ignored (mode disabled): channel={}, chatId={}", channel, chatId);
      emitter.complete();
      return emitter;
    }
    String key = streamKey(channel, chatId);
    log.info("SSE stream connected: channel={}, chatId={}", channel, chatId);
    streams.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

    List<Map<String, Object>> cached = drainBacklog(key);
    for (Map<String, Object> data : cached) {
      if (!sendEvent(emitter, data)) {
        streams.getOrDefault(key, new CopyOnWriteArrayList<>()).remove(emitter);
        return emitter;
      }
    }

    emitter.onCompletion(() -> removeEmitter(key, emitter, "completed"));
    emitter.onTimeout(() -> {
      log.info("SSE stream timeout: channel={}, chatId={}", channel, chatId);
      removeEmitter(key, emitter, "timeout");
      emitter.complete();
    });
    return emitter;
  }

  private void handleEvent(SystemEvent event) {
    if (!sseEnabled) return;
    if (!isChatEvent(event)) return;
    Map<String, Object> payload = event.getPayload();
    String channel = payload.get("channel") == null ? null : String.valueOf(payload.get("channel"));
    String chatId = payload.get("chatId") == null ? null : String.valueOf(payload.get("chatId"));
    if (channel == null || channel.isBlank() || chatId == null || chatId.isBlank()) return;


    String key = streamKey(channel, chatId);
    Map<String, Object> data = buildEventData(event);

    CopyOnWriteArrayList<SseEmitter> emitters = streams.get(key);
    if (emitters == null || emitters.isEmpty()) {
      cacheEvent(key, data);
      return;
    }

    for (SseEmitter emitter : emitters) {
      if (!sendEvent(emitter, data)) {
        emitters.remove(emitter);
      }
    }
  }

  private void cacheEvent(String key, Map<String, Object> data) {
    if (bufferSize <= 0 || data == null) return;
    Deque<Map<String, Object>> deque = backlog.computeIfAbsent(key, ignored -> new ArrayDeque<>());
    synchronized (deque) {
      while (deque.size() >= bufferSize) {
        deque.pollFirst();
      }
      deque.addLast(data);
    }
  }

  private List<Map<String, Object>> drainBacklog(String key) {
    Deque<Map<String, Object>> deque = backlog.get(key);
    if (deque == null) return List.of();
    synchronized (deque) {
      if (deque.isEmpty()) return List.of();
      List<Map<String, Object>> snapshot = new ArrayList<>(deque);
      deque.clear();
      return snapshot;
    }
  }

  private void removeEmitter(String key, SseEmitter emitter, String reason) {
    CopyOnWriteArrayList<SseEmitter> list = streams.get(key);
    if (list == null) return;
    list.remove(emitter);
    if (list.isEmpty()) {
      streams.remove(key);
    }
    log.info("SSE stream {}: key={}", reason, key);
  }

  private String streamKey(String channel, String chatId) {
    return (channel == null ? "" : channel) + ":" + (chatId == null ? "" : chatId);
  }

  private boolean isChatEvent(SystemEvent event) {
    if (event == null) return false;
    String type = event.getType();
    if (!"inbound.message".equals(type) && !"outbound.message".equals(type)) return false;
    Map<String, Object> payload = event.getPayload();
    if (payload == null) return false;
    return payload.get("channel") != null && payload.get("chatId") != null;
  }

  private Map<String, Object> buildEventData(SystemEvent event) {
    Map<String, Object> payload = event.getPayload() == null ? new HashMap<>() : new HashMap<>(event.getPayload());
    String type = event.getType();
    payload.put("role", "outbound.message".equals(type) ? "assistant" : "user");
    Map<String, Object> data = new HashMap<>();
    data.put("type", type);
    data.put("timestamp", event.getTimestamp().toString());
    data.put("payload", payload);
    return data;
  }

  private boolean sendEvent(SseEmitter emitter, Map<String, Object> data) {
    try {
      Map<String, Object> payload = data == null ? Map.of() : data;
      Object payloadObj = payload.get("payload");
      Map<String, Object> payloadMap = new HashMap<>();
      if (payloadObj instanceof Map<?, ?> raw) {
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
          if (entry.getKey() != null) {
            payloadMap.put(String.valueOf(entry.getKey()), entry.getValue());
          }
        }
      }
      log.info("SSE send: type={}, chatId={}, channel={}", payload.get("type"),
          payloadMap.get("chatId"),
          payloadMap.get("channel"));

      emitter.send(payload);
      return true;
    } catch (Exception error) {
      String reason = isClientAbort(error) ? "client-abort" : "send-failed";
      log.info("SSE send failed: reason={}, message={}", reason, rootCauseMessage(error));
      emitter.completeWithError(error);
      return false;
    }
  }

  private boolean isClientAbort(Throwable error) {
    if (error == null) return false;
    Throwable cur = error;
    while (cur != null) {
      String name = cur.getClass().getName();
      if (name.contains("ClientAbortException") || name.contains("AsyncRequestNotUsableException")) {
        return true;
      }
      cur = cur.getCause();
    }
    return false;
  }

  private String rootCauseMessage(Throwable error) {
    if (error == null) return "";
    Throwable cur = error;
    Throwable next = cur.getCause();
    while (next != null && next != cur) {
      cur = next;
      next = cur.getCause();
    }
    return cur.getMessage() == null ? "" : cur.getMessage();
  }

}

