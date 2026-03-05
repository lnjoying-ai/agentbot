package com.agentbot.gateway;

import com.agentbot.core.channel.impl.DingTalkChannelAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@ConditionalOnBean(DingTalkChannelAdapter.class)
public class DingTalkWebhookController {
  private static final Logger log = LoggerFactory.getLogger(DingTalkWebhookController.class);
  private final DingTalkChannelAdapter adapter;


  public DingTalkWebhookController(DingTalkChannelAdapter adapter) {
    this.adapter = adapter;
  }

  @PostMapping("/webhook/dingtalk")
  public Map<String, Object> inbound(@RequestBody Map<String, Object> payload) {
    Object msgId = payload == null ? null : payload.get("msgId");
    log.info("dingtalk webhook received: msgId={}", msgId);
    adapter.handleWebhook(payload);
    return Map.of("ok", true);
  }

}
