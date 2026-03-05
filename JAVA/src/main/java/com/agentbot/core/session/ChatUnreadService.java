package com.agentbot.core.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatUnreadService {
  private final ConcurrentHashMap<String, AtomicInteger> unreadCounts = new ConcurrentHashMap<>();

  public int increment(String channel, String chatId) {
    String key = buildKey(channel, chatId);
    return unreadCounts.computeIfAbsent(key, ignored -> new AtomicInteger(0)).incrementAndGet();
  }

  public int get(String channel, String chatId) {
    AtomicInteger count = unreadCounts.get(buildKey(channel, chatId));
    return count == null ? 0 : Math.max(0, count.get());
  }

  public void clear(String channel, String chatId) {
    unreadCounts.remove(buildKey(channel, chatId));
  }

  public List<UnreadInfo> list(String channel) {
    List<UnreadInfo> result = new ArrayList<>();
    String prefix = (channel == null || channel.isBlank() ? "web" : channel) + ":";
    for (Map.Entry<String, AtomicInteger> entry : unreadCounts.entrySet()) {
      String key = entry.getKey();
      if (!key.startsWith(prefix)) continue;
      String chatId = key.substring(prefix.length());
      int count = entry.getValue() == null ? 0 : Math.max(0, entry.getValue().get());
      result.add(new UnreadInfo(chatId, channel, count));
    }
    return result;
  }

  private String buildKey(String channel, String chatId) {
    String safeChannel = channel == null || channel.isBlank() ? "web" : channel;
    String safeChatId = chatId == null || chatId.isBlank() ? "default" : chatId;
    return safeChannel + ":" + safeChatId;
  }

  public record UnreadInfo(String chatId, String channel, int unread) {}
}
