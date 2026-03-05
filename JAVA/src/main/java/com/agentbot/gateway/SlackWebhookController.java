package com.agentbot.gateway;

import com.agentbot.core.channel.impl.SlackChannelAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@ConditionalOnBean(SlackChannelAdapter.class)
public class SlackWebhookController {
  private static final Logger log = LoggerFactory.getLogger(SlackWebhookController.class);
  private final SlackChannelAdapter adapter;


  public SlackWebhookController(SlackChannelAdapter adapter) {
    this.adapter = adapter;
  }

  @PostMapping("/webhook/slack")
  public Map<String, Object> inbound(@RequestBody Map<String, Object> payload) {
    if (payload != null && "url_verification".equals(String.valueOf(payload.get("type")))) {
      log.info("slack url_verification received");
      return Map.of("challenge", payload.get("challenge"));
    }
    Object eventId = payload == null ? null : payload.get("event_id");
    log.info("slack webhook received: eventId={}", eventId);
    adapter.handleWebhook(payload);
    return Map.of("ok", true);
  }

}
