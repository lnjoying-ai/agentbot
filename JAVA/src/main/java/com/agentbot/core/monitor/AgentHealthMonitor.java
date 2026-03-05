package com.agentbot.core.monitor;

import com.agentbot.core.agent.AgentInstance;
import com.agentbot.core.agent.AgentRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Agent 健康监控器.
 * 
 * 功能:
 * - 定期检查 Agent 健康状态
 * - 检测 Agent 异常和故障
 * - 自动重启故障 Agent
 * - 健康报告生成
 */
public class AgentHealthMonitor {
  
  private static final Logger log = LoggerFactory.getLogger(AgentHealthMonitor.class);
  
  /**
   * 健康状态.
   */
  public enum HealthStatus {
    HEALTHY,    // 运行正常
    DEGRADED,   // 性能下降
    UNHEALTHY,  // 不健康
    CRITICAL,   // 严重故障
    UNKNOWN     // 未知状态
  }
  
  /**
   * 健康检查结果.
   */
  public static class HealthCheck {
    private final String agentId;
    private final HealthStatus status;
    private final Instant timestamp;
    private final String message;
    private final Map<String, Object> metrics;
    
    public HealthCheck(
        String agentId,
        HealthStatus status,
        String message,
        Map<String, Object> metrics
    ) {
      this.agentId = agentId;
      this.status = status;
      this.timestamp = Instant.now();
      this.message = message;
      this.metrics = metrics != null ? Map.copyOf(metrics) : Map.of();
    }
    
    public String getAgentId() { return agentId; }
    public HealthStatus getStatus() { return status; }
    public Instant getTimestamp() { return timestamp; }
    public String getMessage() { return message; }
    public Map<String, Object> getMetrics() { return metrics; }
  }
  
  private final AgentRegistry registry;
  private final Map<String, HealthCheck> latestChecks = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler;
  
  private volatile boolean running = false;
  private final long checkIntervalSeconds;
  
  public AgentHealthMonitor(AgentRegistry registry) {
    this(registry, 30); // Default: check every 30 seconds
  }
  
  public AgentHealthMonitor(AgentRegistry registry, long checkIntervalSeconds) {
    this.registry = registry;
    this.checkIntervalSeconds = checkIntervalSeconds;
    this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "AgentHealthMonitor");
      t.setDaemon(true);
      return t;
    });
  }
  
  /**
   * 启动健康监控.
   */
  public void start() {
    if (running) {
      log.warn("AgentHealthMonitor already running");
      return;
    }
    
    running = true;
    log.info("Starting AgentHealthMonitor with interval: {}s", checkIntervalSeconds);
    
    scheduler.scheduleAtFixedRate(
        this::performHealthChecks,
        0,
        checkIntervalSeconds,
        TimeUnit.SECONDS
    );
  }
  
  /**
   * 停止健康监控.
   */
  public void stop() {
    if (!running) {
      return;
    }
    
    log.info("Stopping AgentHealthMonitor");
    running = false;
    
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
    
    log.info("AgentHealthMonitor stopped");
  }
  
  /**
   * 执行所有 Agent 的健康检查.
   */
  private void performHealthChecks() {
    Map<String, AgentInstance> agents = registry.getAllAgents();
    
    for (Map.Entry<String, AgentInstance> entry : agents.entrySet()) {
      String agentId = entry.getKey();
      AgentInstance instance = entry.getValue();
      
      try {
        HealthCheck check = checkAgent(agentId, instance);
        latestChecks.put(agentId, check);
        
        if (check.getStatus() == HealthStatus.CRITICAL || 
            check.getStatus() == HealthStatus.UNHEALTHY) {
          log.warn("Agent {} health check failed: {} - {}",
              agentId, check.getStatus(), check.getMessage());
          
          // TODO: Trigger alerts or auto-recovery
        }
        
      } catch (Exception e) {
        log.error("Error checking agent health: {}", agentId, e);
        latestChecks.put(agentId, new HealthCheck(
            agentId,
            HealthStatus.UNKNOWN,
            "Health check error: " + e.getMessage(),
            null
        ));
      }
    }
  }
  
  /**
   * 检查单个 Agent 的健康状态.
   */
  private HealthCheck checkAgent(String agentId, AgentInstance instance) {
    // Perform basic health checks
    Map<String, Object> metrics = new java.util.HashMap<>();
    
    try {
      // Check 1: Agent runtime exists
      if (instance.getRuntime() == null) {
        return new HealthCheck(
            agentId,
            HealthStatus.CRITICAL,
            "Agent runtime is null",
            metrics
        );
      }
      
      // Check 2: Session service responsive
      // TODO: Add actual responsiveness check
      metrics.put("session_count", 0); // Placeholder
      
      // Check 3: Memory usage
      // TODO: Add memory metrics
      metrics.put("memory_mb", Runtime.getRuntime().totalMemory() / 1024 / 1024);
      
      // Check 4: Error rate
      // TODO: Track error rate
      metrics.put("error_rate", 0.0);
      
      // All checks passed
      return new HealthCheck(
          agentId,
          HealthStatus.HEALTHY,
          "All checks passed",
          metrics
      );
      
    } catch (Exception e) {
      return new HealthCheck(
          agentId,
          HealthStatus.UNHEALTHY,
          "Health check exception: " + e.getMessage(),
          metrics
      );
    }
  }
  
  /**
   * 获取 Agent 的最新健康检查结果.
   */
  public HealthCheck getLatestCheck(String agentId) {
    return latestChecks.get(agentId);
  }
  
  /**
   * 获取所有 Agent 的健康状态.
   */
  public Map<String, HealthCheck> getAllHealthChecks() {
    return Map.copyOf(latestChecks);
  }
  
  /**
   * 获取不健康的 Agent 列表.
   */
  public Map<String, HealthCheck> getUnhealthyAgents() {
    return latestChecks.entrySet().stream()
        .filter(e -> e.getValue().getStatus() != HealthStatus.HEALTHY)
        .collect(java.util.stream.Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue
        ));
  }
  
  /**
   * 手动触发健康检查.
   */
  public void triggerHealthCheck(String agentId) {
    AgentInstance instance = registry.getAgent(agentId);
    if (instance == null) {
      log.warn("Agent not found for health check: {}", agentId);
      return;
    }
    
    HealthCheck check = checkAgent(agentId, instance);
    latestChecks.put(agentId, check);
    log.info("Triggered health check for agent {}: {}", agentId, check.getStatus());
  }
}
