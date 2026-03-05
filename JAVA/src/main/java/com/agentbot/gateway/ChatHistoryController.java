package com.agentbot.gateway;

import com.agentbot.core.session.ChatHistoryService;
import com.agentbot.core.session.ChatUnreadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;


import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/chats")
public class ChatHistoryController {
  private static final Logger log = LoggerFactory.getLogger(ChatHistoryController.class);
  private final ChatHistoryService historyService;
  private final ChatUnreadService unreadService;


  public ChatHistoryController(ChatHistoryService historyService, ChatUnreadService unreadService) {
    this.historyService = historyService;
    this.unreadService = unreadService;
  }

  @GetMapping
  public List<ChatHistoryService.SessionInfo> listSessions(
      @RequestParam(value = "channel", defaultValue = "web") String channel,
      @RequestParam(value = "agentId", required = false) String agentId,
      @RequestParam(value = "limit", defaultValue = "50") int limit
  ) {
    log.debug("List sessions: channel={}, agentId={}, limit={}", channel, agentId, limit);
    List<ChatHistoryService.SessionInfo> sessions = historyService.listSessions(channel, agentId, limit);
    log.debug("List sessions result: count={}", sessions.size());
    return sessions;
  }

  @GetMapping("/unread")
  public UnreadResponse unread(
      @RequestParam(value = "channel", defaultValue = "web") String channel,
      @RequestParam(value = "chatId", required = false) String chatId
  ) {
    if (chatId != null && !chatId.isBlank()) {
      int count = unreadService == null ? 0 : unreadService.get(channel, chatId);
      return new UnreadResponse(List.of(new UnreadItem(chatId, channel, count)));
    }
    List<ChatUnreadService.UnreadInfo> list = unreadService == null ? List.of() : unreadService.list(channel);
    List<UnreadItem> items = list.stream()
        .map(info -> new UnreadItem(info.chatId(), info.channel(), info.unread()))
        .toList();
    return new UnreadResponse(items);
  }

  @PostMapping("/{chatId}/unread/clear")
  public UnreadItem clearUnread(
      @PathVariable("chatId") String chatId,
      @RequestParam(value = "channel", defaultValue = "web") String channel
  ) {
    if (unreadService != null) {
      unreadService.clear(channel, chatId);
    }
    return new UnreadItem(chatId, channel, 0);
  }


  @GetMapping("/{chatId}/messages")
  public ChatMessagesResponse listMessages(
      @PathVariable("chatId") String chatId,
      @RequestParam(value = "channel", defaultValue = "web") String channel,
      @RequestParam(value = "agentId", required = false) String agentId,
      @RequestParam(value = "limit", defaultValue = "50") int limit,
      @RequestParam(value = "before", required = false) String before
  ) {
    Instant beforeInstant = null;
    if (before != null && !before.isBlank()) {
      try {
        beforeInstant = Instant.parse(before);
      } catch (Exception error) {
        log.warn("Invalid before cursor: {}", before);
        beforeInstant = null;
      }
    }
    List<ChatHistoryService.MessageInfo> messages = historyService.listMessages(channel, chatId, agentId, limit, beforeInstant);
    String nextCursor = messages.isEmpty() ? null : messages.get(0).timestamp().toString();
    return new ChatMessagesResponse(chatId, channel, agentId, messages, nextCursor);


  }

  public record ChatMessagesResponse(
      String chatId,
      String channel,
      String agentId,
      List<ChatHistoryService.MessageInfo> messages,
      String nextCursor
  ) {}

  public record UnreadItem(String chatId, String channel, int unread) {}

  public record UnreadResponse(List<UnreadItem> items) {}
}
