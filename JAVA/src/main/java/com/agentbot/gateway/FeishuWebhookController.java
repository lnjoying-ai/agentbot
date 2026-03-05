package com.agentbot.gateway;

import com.agentbot.core.channel.impl.FeishuChannelAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@ConditionalOnBean(FeishuChannelAdapter.class)
public class FeishuWebhookController {
  private static final Logger log = LoggerFactory.getLogger(FeishuWebhookController.class);
  private final FeishuChannelAdapter adapter;
  private final ObjectMapper mapper = new ObjectMapper();


  public FeishuWebhookController(FeishuChannelAdapter adapter) {
    this.adapter = adapter;
  }

  @PostMapping("/webhook/feishu")
  public ResponseEntity<Map<String, Object>> inbound(@RequestBody String body) {
    Map<String, Object> payload;
    try {
      payload = mapper.readValue(body, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

    } catch (Exception e) {
      log.warn("feishu webhook parse failed", e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("ok", false));
    }
    Map<String, Object> decoded = adapter.decodeWebhookPayload(payload);
    if (!adapter.verifyToken(decoded)) {
      log.warn("feishu webhook token verify failed");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ok", false));
    }
    Object challenge = adapter.getChallenge(decoded);
    if (challenge != null) {
      log.info("feishu webhook challenge received");
      return ResponseEntity.ok(Map.of("challenge", challenge));
    }
    log.info("feishu webhook received");
    adapter.handleWebhook(decoded);
    return ResponseEntity.ok(Map.of("ok", true));
  }

}
