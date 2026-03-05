package com.agentbot.core.protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageCodecRegistry {
  private final Map<String, MessageCodec> codecs = new ConcurrentHashMap<>();
  private final MessageCodec defaultCodec;

  public MessageCodecRegistry(MessageCodec defaultCodec) {
    this.defaultCodec = defaultCodec;
    register(defaultCodec);
  }

  public void register(MessageCodec codec) {
    if (codec == null || codec.getName() == null) return;
    codecs.put(codec.getName(), codec);
  }

  public MessageCodec resolve(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      return defaultCodec;
    }
    return codecs.getOrDefault(contentType, defaultCodec);
  }

  public MessageCodec getDefaultCodec() {
    return defaultCodec;
  }
}
