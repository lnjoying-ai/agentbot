package com.agentbot.gateway;

import com.agentbot.core.channel.impl.WeChatChannelAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@ConditionalOnBean(WeChatChannelAdapter.class)
public class WeChatWebhookController {
  private final WeChatChannelAdapter adapter;

  public WeChatWebhookController(WeChatChannelAdapter adapter) {
    this.adapter = adapter;
  }

  @PostMapping("/webhook/wechat")
  public Map<String, Object> inbound(@RequestBody Map<String, Object> payload) {
    String from = String.valueOf(payload.getOrDefault("from", ""));
    String chatId = String.valueOf(payload.getOrDefault("chatId", from));
    String content = String.valueOf(payload.getOrDefault("content", ""));
    if (!content.isBlank()) {
      adapter.handleInbound(from, chatId, content);
    }
    return Map.of("ok", true);
  }
}
