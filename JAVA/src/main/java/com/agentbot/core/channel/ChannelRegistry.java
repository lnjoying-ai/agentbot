package com.agentbot.core.channel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChannelRegistry {
  private final Map<String, ChannelAdapter> channels = new HashMap<>();

  public void register(ChannelAdapter adapter) {
    if (adapter == null) return;
    channels.put(adapter.name(), adapter);
  }

  public Map<String, ChannelAdapter> all() {
    return Collections.unmodifiableMap(channels);
  }
}
