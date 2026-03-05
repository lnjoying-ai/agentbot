package com.agentbot.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Agent 速率限制器.
 * 
 * 功能:
 * - 限制用户对 Agent 的请求频率
 * - 防止滥用和 DoS 攻击
 * - 支持多种限流策略
 * - 自动清理过期数据
 */
public class AgentRateLimiter {
  
  private static final Logger log = LoggerFactory.getLogger(AgentRateLimiter.class);
  
  /**
   * 限流策略.
   */
  public static class RateLimit {
    private final int maxRequests;
    private final Duration window;
    
    public RateLimit(int maxRequests, Duration window) {
      this.maxRequests = maxRequests;
      this.window = window;
    }
    
    public int getMaxRequests() {
      return maxRequests;
    }
    
    public Duration getWindow() {
      return window;
    }
  }
  
  /**
   * 请求记录.
   */
  private static class RequestRecord {
    private int count;
    private Instant windowStart;
    
    public RequestRecord() {
      this.count = 0;
      this.windowStart = Instant.now();
    }
    
    public void reset() {
      this.count = 0;
      this.windowStart = Instant.now();
    }
    
    public void increment() {
      this.count++;
    }
    
    public int getCount() {
      return count;
    }
    
    public Instant getWindowStart() {
      return windowStart;
    }
  }
  
  // Default rate limits
  private static final RateLimit DEFAULT_LIMIT = new RateLimit(60, Duration.ofMinutes(1)); // 60 req/min
  private static final RateLimit STRICT_LIMIT = new RateLimit(10, Duration.ofMinutes(1));  // 10 req/min
  
  // User rate limits: userId -> agentId -> record
  private final Map<String, Map<String, RequestRecord>> userRecords = new ConcurrentHashMap<>();
  
  // Agent-specific rate limits: agentId -> rateLimit
  private final Map<String, RateLimit> agentLimits = new ConcurrentHashMap<>();
  
  private final ScheduledExecutorService cleanupExecutor;
  
  public AgentRateLimiter() {
    this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "RateLimiter-Cleanup");
      t.setDaemon(true);
      return t;
    });
    
    // Schedule cleanup every 5 minutes
    cleanupExecutor.scheduleAtFixedRate(
        this::cleanup,
        5, 5, TimeUnit.MINUTES
    );
  }
  
  /**
   * 检查请求是否被限流.
   * 
   * @return true if allowed, false if rate limited
   */
  public boolean checkLimit(String userId, String agentId) {
    RateLimit limit = agentLimits.getOrDefault(agentId, DEFAULT_LIMIT);
    
    RequestRecord record = userRecords
        .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(agentId, k -> new RequestRecord());
    
    synchronized (record) {
      Instant now = Instant.now();
      Duration elapsed = Duration.between(record.getWindowStart(), now);
      
      // Reset window if expired
      if (elapsed.compareTo(limit.getWindow()) >= 0) {
        record.reset();
      }
      
      // Check if limit exceeded
      if (record.getCount() >= limit.getMaxRequests()) {
        log.warn("Rate limit exceeded for user {} on agent {}: {}/{} in {}",
            userId, agentId, record.getCount(), limit.getMaxRequests(), limit.getWindow());
        return false;
      }
      
      // Increment and allow
      record.increment();
      return true;
    }
  }
  
  /**
   * 设置 Agent 的速率限制.
   */
  public void setAgentLimit(String agentId, RateLimit limit) {
    agentLimits.put(agentId, limit);
    log.info("Set rate limit for agent {}: {} requests per {}",
        agentId, limit.getMaxRequests(), limit.getWindow());
  }
  
  /**
   * 设置 Agent 的速率限制 (简化版).
   */
  public void setAgentLimit(String agentId, int maxRequests, Duration window) {
    setAgentLimit(agentId, new RateLimit(maxRequests, window));
  }
  
  /**
   * 移除 Agent 的速率限制 (使用默认).
   */
  public void removeAgentLimit(String agentId) {
    agentLimits.remove(agentId);
  }
  
  /**
   * 获取用户当前请求数.
   */
  public int getCurrentCount(String userId, String agentId) {
    Map<String, RequestRecord> agentRecords = userRecords.get(userId);
    if (agentRecords == null) {
      return 0;
    }
    
    RequestRecord record = agentRecords.get(agentId);
    return record != null ? record.getCount() : 0;
  }
  
  /**
   * 获取用户剩余配额.
   */
  public int getRemainingQuota(String userId, String agentId) {
    RateLimit limit = agentLimits.getOrDefault(agentId, DEFAULT_LIMIT);
    int current = getCurrentCount(userId, agentId);
    return Math.max(0, limit.getMaxRequests() - current);
  }
  
  /**
   * 重置用户的速率限制.
   */
  public void resetUser(String userId, String agentId) {
    Map<String, RequestRecord> agentRecords = userRecords.get(userId);
    if (agentRecords != null) {
      agentRecords.remove(agentId);
    }
  }
  
  /**
   * 清理过期记录.
   */
  private void cleanup() {
    Instant now = Instant.now();
    final int[] cleanedCount = {0};
    
    for (Map.Entry<String, Map<String, RequestRecord>> userEntry : userRecords.entrySet()) {
      Map<String, RequestRecord> agentRecords = userEntry.getValue();
      
      agentRecords.entrySet().removeIf(entry -> {
        String agentId = entry.getKey();
        RequestRecord record = entry.getValue();
        RateLimit limit = agentLimits.getOrDefault(agentId, DEFAULT_LIMIT);
        
        Duration elapsed = Duration.between(record.getWindowStart(), now);
        boolean expired = elapsed.compareTo(limit.getWindow().multipliedBy(2)) >= 0;
        
        if (expired) {
          cleanedCount[0]++;
        }
        
        return expired;
      });
      
      // Remove user entry if no agent records
      if (agentRecords.isEmpty()) {
        userRecords.remove(userEntry.getKey());
      }
    }
    
    if (cleanedCount[0] > 0) {
      log.debug("Cleaned {} expired rate limit records", cleanedCount[0]);
    }
  }
  
  /**
   * 停止清理任务.
   */
  public void shutdown() {
    cleanupExecutor.shutdown();
    try {
      if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        cleanupExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      cleanupExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
  
  /**
   * 获取统计信息.
   */
  public Map<String, Object> getStatistics() {
    int totalUsers = userRecords.size();
    int totalRecords = userRecords.values().stream()
        .mapToInt(Map::size)
        .sum();
    
    return Map.of(
        "totalUsers", totalUsers,
        "totalRecords", totalRecords,
        "agentLimits", agentLimits.size()
    );
  }
}
