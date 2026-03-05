package com.agentbot.core.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Channel Registry - 管理所有渠道适配器.
 */
public class ChannelRegistry {
  
  private static final Logger log = LoggerFactory.getLogger(ChannelRegistry.class);
  
  private final Map<String, ChannelAdapter> adapters = new ConcurrentHashMap<>();
  
  /**
   * 注册渠道适配器.
   */
  public void register(ChannelAdapter adapter) {
    String key = adapter.name();
    adapters.put(key, adapter);
    log.info("Registered channel adapter: {}", key);
  }
  
  /**
   * 取消注册渠道适配器.
   */
  public void unregister(String channelName) {
    ChannelAdapter adapter = adapters.remove(channelName);
    if (adapter != null) {
      adapter.stop();
      log.info("Unregistered channel adapter: {}", channelName);
    }
  }
  
  /**
   * 获取渠道适配器.
   */
  public ChannelAdapter getAdapter(String channelName) {
    return adapters.get(channelName);
  }
  
  /**
   * 获取所有适配器.
   */
  public List<ChannelAdapter> getAllAdapters() {
    return List.copyOf(adapters.values());
  }
  
  /**
   * 获取所有适配器 (Map形式).
   */
  public Map<String, ChannelAdapter> all() {
    return Map.copyOf(adapters);
  }
  
  /**
   * 启动所有适配器.
   */
  public void startAll() {
    log.info("Starting all channel adapters...");
    adapters.values().forEach(adapter -> {
      try {
        adapter.start();
      } catch (Exception e) {
        log.error("Failed to start adapter: {}", adapter.name(), e);
      }
    });
    log.info("All channel adapters started");
  }
  
  /**
   * 停止所有适配器.
   */
  public void stopAll() {
    log.info("Stopping all channel adapters...");
    adapters.values().forEach(adapter -> {
      try {
        adapter.stop();
      } catch (Exception e) {
        log.error("Failed to stop adapter: {}", adapter.name(), e);
      }
    });
    log.info("All channel adapters stopped");
  }
}
