package com.agentbot.core.monitor;

import com.agentbot.core.agent.AgentRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Agent 性能指标收集器.
 * 
 * 功能:
 * - 收集 Agent 性能指标
 * - 统计消息处理速率
 * - 跟踪响应时间
 * - 监控资源使用
 */
public class AgentMetricsCollector {
  
  private static final Logger log = LoggerFactory.getLogger(AgentMetricsCollector.class);
  
  /**
   * Agent 性能指标.
   */
  public static class AgentMetrics {
    private final String agentId;
    private final AtomicLong messagesProcessed = new AtomicLong(0);
    private final AtomicLong messagesSuccess = new AtomicLong(0);
    private final AtomicLong messagesFailed = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0); // milliseconds
    private final AtomicLong maxResponseTime = new AtomicLong(0);
    private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
    private Instant startTime = Instant.now();
    private Instant lastMessageTime;
    
    public AgentMetrics(String agentId) {
      this.agentId = agentId;
    }
    
    public void recordMessage(boolean success, long responseTimeMs) {
      messagesProcessed.incrementAndGet();
      if (success) {
        messagesSuccess.incrementAndGet();
      } else {
        messagesFailed.incrementAndGet();
      }
      
      totalResponseTime.addAndGet(responseTimeMs);
      maxResponseTime.updateAndGet(max -> Math.max(max, responseTimeMs));
      minResponseTime.updateAndGet(min -> Math.min(min, responseTimeMs));
      lastMessageTime = Instant.now();
    }
    
    public void reset() {
      messagesProcessed.set(0);
      messagesSuccess.set(0);
      messagesFailed.set(0);
      totalResponseTime.set(0);
      maxResponseTime.set(0);
      minResponseTime.set(Long.MAX_VALUE);
      startTime = Instant.now();
      lastMessageTime = null;
    }
    
    public long getMessagesProcessed() {
      return messagesProcessed.get();
    }
    
    public long getMessagesSuccess() {
      return messagesSuccess.get();
    }
    
    public long getMessagesFailed() {
      return messagesFailed.get();
    }
    
    public double getSuccessRate() {
      long total = messagesProcessed.get();
      return total > 0 ? (double) messagesSuccess.get() / total : 0.0;
    }
    
    public double getAverageResponseTime() {
      long total = messagesProcessed.get();
      return total > 0 ? (double) totalResponseTime.get() / total : 0.0;
    }
    
    public long getMaxResponseTime() {
      long max = maxResponseTime.get();
      return max > 0 ? max : 0;
    }
    
    public long getMinResponseTime() {
      long min = minResponseTime.get();
      return min < Long.MAX_VALUE ? min : 0;
    }
    
    public double getMessagesPerSecond() {
      Duration uptime = Duration.between(startTime, Instant.now());
      long seconds = uptime.getSeconds();
      return seconds > 0 ? (double) messagesProcessed.get() / seconds : 0.0;
    }
    
    public Duration getUptime() {
      return Duration.between(startTime, Instant.now());
    }
    
    public Instant getLastMessageTime() {
      return lastMessageTime;
    }
    
    public Map<String, Object> toMap() {
      Map<String, Object> result = new java.util.HashMap<>();
      result.put("agentId", agentId);
      result.put("messagesProcessed", messagesProcessed.get());
      result.put("messagesSuccess", messagesSuccess.get());
      result.put("messagesFailed", messagesFailed.get());
      result.put("successRate", getSuccessRate());
      result.put("averageResponseTime", getAverageResponseTime());
      result.put("maxResponseTime", getMaxResponseTime());
      result.put("minResponseTime", getMinResponseTime());
      result.put("messagesPerSecond", getMessagesPerSecond());
      result.put("uptimeSeconds", getUptime().getSeconds());
      result.put("lastMessageTime", lastMessageTime != null ? lastMessageTime.toString() : null);
      return result;
    }
  }
  
  private final Map<String, AgentMetrics> metricsMap = new ConcurrentHashMap<>();
  private final AgentRegistry registry;
  
  public AgentMetricsCollector(AgentRegistry registry) {
    this.registry = registry;
  }
  
  /**
   * 获取 Agent 指标.
   */
  public AgentMetrics getMetrics(String agentId) {
    return metricsMap.computeIfAbsent(agentId, AgentMetrics::new);
  }
  
  /**
   * 记录消息处理.
   */
  public void recordMessage(String agentId, boolean success, long responseTimeMs) {
    getMetrics(agentId).recordMessage(success, responseTimeMs);
  }
  
  /**
   * 重置 Agent 指标.
   */
  public void resetMetrics(String agentId) {
    AgentMetrics metrics = metricsMap.get(agentId);
    if (metrics != null) {
      metrics.reset();
    }
  }
  
  /**
   * 获取所有 Agent 指标.
   */
  public Map<String, AgentMetrics> getAllMetrics() {
    return Map.copyOf(metricsMap);
  }
  
  /**
   * 获取系统级统计.
   */
  public Map<String, Object> getSystemStats() {
    long totalMessages = 0;
    long totalSuccess = 0;
    long totalFailed = 0;
    double totalResponseTime = 0;
    
    for (AgentMetrics metrics : metricsMap.values()) {
      totalMessages += metrics.getMessagesProcessed();
      totalSuccess += metrics.getMessagesSuccess();
      totalFailed += metrics.getMessagesFailed();
      totalResponseTime += metrics.getAverageResponseTime() * metrics.getMessagesProcessed();
    }
    
    double avgResponseTime = totalMessages > 0 ? totalResponseTime / totalMessages : 0;
    double successRate = totalMessages > 0 ? (double) totalSuccess / totalMessages : 0;
    
    return Map.of(
        "totalAgents", metricsMap.size(),
        "totalMessages", totalMessages,
        "totalSuccess", totalSuccess,
        "totalFailed", totalFailed,
        "systemSuccessRate", successRate,
        "systemAvgResponseTime", avgResponseTime
    );
  }
  
  /**
   * 记录消息开始时间 (用于计算响应时间).
   */
  public Instant startTimer() {
    return Instant.now();
  }
  
  /**
   * 计算并记录响应时间.
   */
  public void recordResponse(String agentId, Instant startTime, boolean success) {
    long responseTimeMs = Duration.between(startTime, Instant.now()).toMillis();
    recordMessage(agentId, success, responseTimeMs);
  }
  
  /**
   * 输出指标日志.
   */
  public void logMetrics() {
    log.info("=== Agent Metrics Summary ===");
    metricsMap.forEach((agentId, metrics) -> {
      log.info("Agent {}: {} messages, {:.1f}% success, {:.1f}ms avg response",
          agentId,
          metrics.getMessagesProcessed(),
          metrics.getSuccessRate() * 100,
          metrics.getAverageResponseTime()
      );
    });
    
    Map<String, Object> systemStats = getSystemStats();
    log.info("System: {} agents, {} total messages, {:.1f}% success rate",
        systemStats.get("totalAgents"),
        systemStats.get("totalMessages"),
        (double) systemStats.get("systemSuccessRate") * 100
    );
  }
}
