package com.agentbot.gateway;

import com.agentbot.core.agent.AgentInstance;
import com.agentbot.core.agent.AgentRegistry;
import com.agentbot.core.agent.AgentRuntime;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.bus.events.InboundMessage;
import com.agentbot.core.bus.events.OutboundMessage;

import com.agentbot.core.events.SystemEvent;
import com.agentbot.core.events.SystemEventBus;
import com.agentbot.core.session.ChatUnreadService;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController

@RequestMapping("/api/chat")
public class ChatController {
  private static final Logger log = LoggerFactory.getLogger(ChatController.class);
  private final AgentRuntime defaultRuntime;
  private final AgentRegistry agentRegistry;
  private final SystemEventBus eventBus;
  private final AgentbotProperties properties;
  private final ChatUnreadService unreadService;

  public ChatController(
      AgentRuntime defaultRuntime,
      AgentRegistry agentRegistry,
      SystemEventBus eventBus,
      AgentbotProperties properties,
      ChatUnreadService unreadService
  ) {
    this.defaultRuntime = defaultRuntime;
    this.agentRegistry = agentRegistry;
    this.eventBus = eventBus;
    this.properties = properties;
    this.unreadService = unreadService;
  }



  @PostMapping("/send")
  public ChatResponse send(@RequestBody ChatRequest request) {
    log.info("Received chat request: channel={}, senderId={}, chatId={}, contentLen={}",
        request.channel(), request.senderId(), request.chatId(), 
        request.content() != null ? request.content().length() : 0);

    String channel = request.channel() == null ? "web" : request.channel();
    String senderId = request.senderId() == null ? "web-user" : request.senderId();
    String chatId = request.chatId() == null ? "default" : request.chatId();

    InboundMessage inbound = new InboundMessage(
        channel,
        senderId,
        chatId,
        request.content()
    );

    if (request.metadata() != null) {
      inbound.getMetadata().putAll(request.metadata());
    }

    String agentId = resolveAgentId(request, chatId);
    log.debug("Resolved agentId: chatId={}, agentId={}", chatId, agentId);
    if (agentId != null && !agentId.isBlank()) {
      inbound.getMetadata().putIfAbsent("agentId", agentId);
    }
    
    publishChatEvent("inbound.message", inbound.getChannel(), inbound.getChatId(), inbound.getSenderId(), inbound.getContent(), inbound.getTimestamp().toString());

    try {
      AgentRuntime runtime = resolveRuntime(agentId);
      long startedAt = System.currentTimeMillis();
      OutboundMessage outbound = runtime.handle(inbound);
      long durationMs = System.currentTimeMillis() - startedAt;
      log.info("Agent response generated for chatId={}: contentLen={}, durationMs={}",
          request.chatId(), outbound != null ? outbound.getContent().length() : 0, durationMs);

      boolean sseMode = isSseMode();
      if (outbound != null) {
        outbound.getMetadata().putIfAbsent("agentId", agentId);
        if (sseMode) {
          publishChatEvent("outbound.message", outbound.getChannel(), outbound.getChatId(), null, outbound.getContent(), outbound.getTimestamp().toString());
        }
      }

      if (!sseMode && outbound != null && unreadService != null) {
        int unread = unreadService.increment(outbound.getChannel(), outbound.getChatId());
        outbound.getMetadata().put("unread", unread);
      }


      String responseContent;
      if (sseMode) {
        responseContent = "SSE模式已启用，回复将通过SSE推送。";
      } else {
        responseContent = outbound == null ? "No response from agent" : outbound.getContent();
      }

      return new ChatResponse(
          java.util.UUID.randomUUID().toString(),
          "assistant",
          responseContent,
          java.time.OffsetDateTime.now().toString(),
          List.of(),
          outbound == null ? null : outbound.getMetadata()
      );
    } catch (Exception e) {
      log.error("Error handling chat request for chatId={}", request.chatId(), e);
      throw e;
    }
  }


  private String resolveAgentId(ChatRequest request, String chatId) {
    Object metaAgent = request.metadata() == null ? null : request.metadata().get("agentId");
    String agentId = metaAgent == null ? null : String.valueOf(metaAgent).trim();
    if (agentId != null && !agentId.isBlank() && agentRegistry.hasAgent(agentId)) {
      return agentId;
    }
    if (chatId != null && agentRegistry.hasAgent(chatId)) {
      return chatId;
    }
    return "default";
  }

  private AgentRuntime resolveRuntime(String agentId) {
    if (agentId == null || agentId.isBlank()) {
      log.debug("Resolve runtime: agentId is blank, using default runtime");
      return defaultRuntime;
    }
    AgentInstance instance = agentRegistry.getAgent(agentId);
    if (instance == null) {
      log.debug("Resolve runtime: agentId={}, runtime not found, using default", agentId);
      return defaultRuntime;
    }
    log.debug("Resolve runtime: agentId={}, runtime found", agentId);
    return instance.getRuntime();

  }

  private boolean isSseMode() {
    String mode = properties == null || properties.getOps() == null ? null : properties.getOps().getChatMode();
    return mode != null && mode.trim().equalsIgnoreCase("sse");
  }

  private void publishChatEvent(String type, String channel, String chatId, String senderId, String content, String timestamp) {
    if (eventBus == null) return;
    Map<String, Object> payload = new java.util.HashMap<>();
    payload.put("channel", channel);
    payload.put("chatId", chatId);
    if (senderId != null) payload.put("senderId", senderId);
    payload.put("content", content);
    payload.put("timestamp", timestamp);
    log.debug("Publish chat event: type={}, channel={}, chatId={}", type, channel, chatId);
    eventBus.publish(new SystemEvent(type, payload));
  }



  public record ChatRequest(String channel, String senderId, String chatId, String content, java.util.Map<String, Object> metadata) {}


  public record ChatResponse(String id, String role, String content, String timestamp, List<Object> toolResults, java.util.Map<String, Object> metadata) {}


}
