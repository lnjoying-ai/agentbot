package com.agentbot.gateway;

import com.agentbot.core.channel.impl.LineChannelAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@ConditionalOnBean(LineChannelAdapter.class)
public class LineWebhookController {
  private static final Logger log = LoggerFactory.getLogger(LineWebhookController.class);
  private final LineChannelAdapter adapter;
  private final ObjectMapper mapper = new ObjectMapper();


  public LineWebhookController(LineChannelAdapter adapter) {
    this.adapter = adapter;
  }

  @PostMapping("/webhook/line")
  public ResponseEntity<Map<String, Object>> inbound(
      @RequestBody String body,
      @RequestHeader(value = "X-Line-Signature", required = false) String signature
  ) {
    if (!adapter.verifySignature(body, signature)) {
      log.warn("line webhook signature verify failed");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ok", false));
    }
    try {
      Map<String, Object> payload = mapper.readValue(body, Map.class);
      log.info("line webhook received");
      adapter.handleWebhook(payload);
    } catch (Exception e) {
      log.warn("line webhook parse failed", e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("ok", false));
    }
    return ResponseEntity.ok(Map.of("ok", true));
  }

}
