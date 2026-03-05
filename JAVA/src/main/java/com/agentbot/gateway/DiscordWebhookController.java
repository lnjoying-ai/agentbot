package com.agentbot.gateway;

import com.agentbot.core.channel.impl.DiscordChannelAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@ConditionalOnBean(DiscordChannelAdapter.class)
public class DiscordWebhookController {
  private static final Logger log = LoggerFactory.getLogger(DiscordWebhookController.class);
  private final DiscordChannelAdapter adapter;


  public DiscordWebhookController(DiscordChannelAdapter adapter) {
    this.adapter = adapter;
  }

  @PostMapping("/webhook/discord")
  public Map<String, Object> inbound(@RequestBody Map<String, Object> payload) {
    Object eventId = payload == null ? null : payload.get("id");
    log.info("discord webhook received: eventId={}", eventId);
    adapter.handleWebhook(payload);
    return Map.of("ok", true);
  }

}
