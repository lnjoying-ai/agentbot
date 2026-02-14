package com.agentbot.gateway;

import com.agentbot.core.agent.AgentRuntime;
import com.agentbot.core.bus.events.InboundMessage;
import com.agentbot.core.bus.events.OutboundMessage;
import com.agentbot.core.session.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController

@RequestMapping("/api/chat")
public class ChatController {
  private static final Logger log = LoggerFactory.getLogger(ChatController.class);
  private final AgentRuntime runtime;

  private final SessionService sessionService;

  public ChatController(AgentRuntime runtime, SessionService sessionService) {
    this.runtime = runtime;
    this.sessionService = sessionService;
  }

  @PostMapping("/send")
  public ChatResponse send(@RequestBody ChatRequest request) {
    log.info("Received chat request: channel={}, senderId={}, chatId={}, contentLen={}",
        request.channel(), request.senderId(), request.chatId(), 
        request.content() != null ? request.content().length() : 0);

    InboundMessage inbound = new InboundMessage(
        request.channel() == null ? "web" : request.channel(),
        request.senderId() == null ? "web-user" : request.senderId(),
        request.chatId() == null ? "default" : request.chatId(),
        request.content()
    );
    
    try {
      OutboundMessage outbound = runtime.handle(inbound);
      log.info("Agent response generated for chatId={}: contentLen={}", 
          request.chatId(), outbound != null ? outbound.getContent().length() : 0);
      
      return new ChatResponse(
          java.util.UUID.randomUUID().toString(),
          "assistant",
          outbound == null ? "No response from agent" : outbound.getContent(),
          java.time.OffsetDateTime.now().toString(),
          List.of()
      );
    } catch (Exception e) {
      log.error("Error handling chat request for chatId={}", request.chatId(), e);
      throw e;
    }
  }


  public record ChatRequest(String channel, String senderId, String chatId, String content) {}

  public record ChatResponse(String id, String role, String content, String timestamp, List<Object> toolResults) {}

}
