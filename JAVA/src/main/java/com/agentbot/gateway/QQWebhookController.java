package com.agentbot.gateway;

import com.agentbot.core.channel.impl.QQChannelAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@ConditionalOnBean(QQChannelAdapter.class)
public class QQWebhookController {
  private static final Logger log = LoggerFactory.getLogger(QQWebhookController.class);
  private final QQChannelAdapter adapter;


  public QQWebhookController(QQChannelAdapter adapter) {
    this.adapter = adapter;
  }

  @PostMapping("/webhook/qq")
  public Map<String, Object> inbound(@RequestBody Map<String, Object> payload) {
    Object eventId = payload == null ? null : payload.get("id");
    log.info("qq webhook received: eventId={}", eventId);
    adapter.handleWebhook(payload);
    return Map.of("ok", true);
  }

}
