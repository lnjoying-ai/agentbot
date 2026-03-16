package com.agentbot.gateway;

import com.agentbot.core.agent.AgentInstance;
import com.agentbot.core.agent.AgentRegistry;
import com.agentbot.core.agent.AgentRuntime;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.bus.ExternalMessageBus;
import com.agentbot.core.bus.MessageEnvelope;
import com.agentbot.core.bus.events.InboundMessage;
import com.agentbot.core.bus.events.OutboundMessage;

import com.agentbot.core.events.SystemEvent;
import com.agentbot.core.events.SystemEventBus;
import com.agentbot.core.session.ChatUnreadService;


import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;



@RestController

@RequestMapping("/api/chat")
public class ChatController {
  private static final Logger log = LoggerFactory.getLogger(ChatController.class);
  private final AgentRuntime defaultRuntime;
  private final AgentRegistry agentRegistry;
  private final SystemEventBus eventBus;
  private final AgentbotProperties properties;
  private final ChatUnreadService unreadService;
  private final ExternalMessageBus messageBus;


  public ChatController(
      AgentRuntime defaultRuntime,
      AgentRegistry agentRegistry,
      SystemEventBus eventBus,
      AgentbotProperties properties,
      ChatUnreadService unreadService,
      ExternalMessageBus messageBus
  ) {
    this.defaultRuntime = defaultRuntime;
    this.agentRegistry = agentRegistry;
    this.eventBus = eventBus;
    this.properties = properties;
    this.unreadService = unreadService;
    this.messageBus = messageBus;
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
      AgentInstance instance = resolveAgentInstance(agentId);
      long startedAt = System.currentTimeMillis();
      OutboundMessage outbound = instance != null ? instance.handle(inbound) : defaultRuntime.handle(inbound);
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

  @PostMapping("/p2psend")
  public ChatResponse p2pSend(@RequestBody P2pSendRequest request) {
    if (messageBus == null) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "message bus is not available");
    }
    String fromAgentId = trimToNull(request.fromAgentId());
    String toNodeId = trimToNull(request.toNodeId());
    String toAgentId = trimToNull(request.toAgentId());
    String content = request.content();

    if (content == null || content.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content is required");
    }
    if (fromAgentId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromAgentId is required");
    }
    if (toNodeId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "toNodeId is required");
    }
    if (toAgentId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "toAgentId is required");
    }

    String chatId = buildP2pChatId(fromAgentId, toNodeId, toAgentId);

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("fromAgentId", fromAgentId);
    metadata.put("agentId", fromAgentId);
    metadata.put("toNodeId", toNodeId);
    metadata.put("toAgentId", toAgentId);
    String msgId = trimToNull(request.msgId());
    if (msgId == null) {
      msgId = UUID.randomUUID().toString();
    }
    metadata.put("msgId", msgId);
    String traceId = trimToNull(request.traceId());
    if (traceId != null) {
      metadata.put("traceId", traceId);
    }
    boolean ackRequired = request.ackRequired() == null || request.ackRequired();
    metadata.put("ackRequired", ackRequired);

    messageBus.publish(MessageEnvelope.externalOutbound("p2p", chatId, content, metadata));

    return new ChatResponse(
        UUID.randomUUID().toString(),
        "assistant",
        "P2P message sent.",
        java.time.OffsetDateTime.now().toString(),
        List.of(),
        metadata
    );
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

  private AgentInstance resolveAgentInstance(String agentId) {
    if (agentId == null || agentId.isBlank()) {
      log.debug("Resolve agent instance: agentId is blank, using default runtime");
      return null;
    }
    AgentInstance instance = agentRegistry.getAgent(agentId);
    if (instance == null) {
      log.debug("Resolve agent instance: agentId={}, runtime not found, using default", agentId);
      return null;
    }
    log.debug("Resolve agent instance: agentId={}, runtime found", agentId);
    return instance;

  }


  private String buildP2pChatId(String fromAgentId, String toNodeId, String toAgentId) {
    String safeFrom = fromAgentId == null || fromAgentId.isBlank() ? "-" : fromAgentId.trim();
    String safeNode = toNodeId == null || toNodeId.isBlank() ? "-" : toNodeId.trim();
    String safeAgent = toAgentId == null || toAgentId.isBlank() ? "-" : toAgentId.trim();
    return "p2p:" + safeFrom + ":" + safeNode + ":" + safeAgent;
  }


  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isBlank() ? null : trimmed;
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


  public record P2pSendRequest(
      String fromAgentId,
      String toNodeId,
      String toAgentId,
      String content,
      String msgId,
      String traceId,
      Boolean ackRequired
  ) {}

  public record ChatRequest(String channel, String senderId, String chatId, String content, java.util.Map<String, Object> metadata) {}


  public record ChatResponse(String id, String role, String content, String timestamp, List<Object> toolResults, java.util.Map<String, Object> metadata) {}



}
