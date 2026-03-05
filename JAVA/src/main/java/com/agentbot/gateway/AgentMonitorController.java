package com.agentbot.gateway;

import com.agentbot.core.monitor.AgentHealthMonitor;
import com.agentbot.core.monitor.AgentMetricsCollector;
import com.agentbot.core.security.AgentAccessControl;
import com.agentbot.core.security.AgentRateLimiter;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Agent 监控管理 REST API.
 * 
 * Endpoints:
 * - GET /api/monitor/health - 健康状态
 * - GET /api/monitor/metrics - 性能指标
 * - GET /api/monitor/system - 系统统计
 * - POST /api/monitor/health/{agentId}/check - 触发健康检查
 */
@RestController
@RequestMapping("/api/monitor")
public class AgentMonitorController {
  
  private final AgentHealthMonitor healthMonitor;
  private final AgentMetricsCollector metricsCollector;
  private final AgentRateLimiter rateLimiter;
  private final AgentAccessControl accessControl;
  
  public AgentMonitorController(
      AgentHealthMonitor healthMonitor,
      AgentMetricsCollector metricsCollector,
      AgentRateLimiter rateLimiter,
      AgentAccessControl accessControl
  ) {
    this.healthMonitor = healthMonitor;
    this.metricsCollector = metricsCollector;
    this.rateLimiter = rateLimiter;
    this.accessControl = accessControl;
  }
  
  /**
   * 获取所有 Agent 健康状态.
   */
  @GetMapping("/health")
  public Map<String, Object> getHealthStatus() {
    Map<String, AgentHealthMonitor.HealthCheck> allChecks = healthMonitor.getAllHealthChecks();
    Map<String, AgentHealthMonitor.HealthCheck> unhealthy = healthMonitor.getUnhealthyAgents();
    
    return Map.of(
        "totalAgents", allChecks.size(),
        "unhealthyCount", unhealthy.size(),
        "checks", allChecks,
        "unhealthy", unhealthy
    );
  }
  
  /**
   * 获取指定 Agent 健康状态.
   */
  @GetMapping("/health/{agentId}")
  public Object getAgentHealth(@PathVariable("agentId") String agentId) {
    AgentHealthMonitor.HealthCheck check = healthMonitor.getLatestCheck(agentId);
    if (check == null) {
      return Map.of("error", "Agent not found or no health check available");
    }
    
    return Map.of(
        "agentId", check.getAgentId(),
        "status", check.getStatus(),
        "timestamp", check.getTimestamp(),
        "message", check.getMessage(),
        "metrics", check.getMetrics()
    );
  }
  
  /**
   * 触发 Agent 健康检查.
   */
  @PostMapping("/health/{agentId}/check")
  public Map<String, Object> triggerHealthCheck(@PathVariable("agentId") String agentId) {
    healthMonitor.triggerHealthCheck(agentId);
    
    AgentHealthMonitor.HealthCheck check = healthMonitor.getLatestCheck(agentId);
    if (check == null) {
      return Map.of("error", "Health check failed");
    }
    
    return Map.of(
        "success", true,
        "agentId", agentId,
        "status", check.getStatus(),
        "message", check.getMessage()
    );
  }
  
  /**
   * 获取所有 Agent 性能指标.
   */
  @GetMapping("/metrics")
  public Map<String, Object> getAllMetrics() {
    Map<String, AgentMetricsCollector.AgentMetrics> allMetrics = metricsCollector.getAllMetrics();
    
    Map<String, Object> metricsData = new java.util.HashMap<>();
    allMetrics.forEach((agentId, metrics) -> {
      metricsData.put(agentId, metrics.toMap());
    });
    
    return Map.of(
        "agents", metricsData,
        "system", metricsCollector.getSystemStats()
    );
  }
  
  /**
   * 获取指定 Agent 性能指标.
   */
  @GetMapping("/metrics/{agentId}")
  public Map<String, Object> getAgentMetrics(@PathVariable("agentId") String agentId) {
    AgentMetricsCollector.AgentMetrics metrics = metricsCollector.getMetrics(agentId);
    return metrics.toMap();
  }
  
  /**
   * 重置 Agent 指标.
   */
  @PostMapping("/metrics/{agentId}/reset")
  public Map<String, Object> resetMetrics(@PathVariable("agentId") String agentId) {
    metricsCollector.resetMetrics(agentId);
    return Map.of(
        "success", true,
        "message", "Metrics reset for agent: " + agentId
    );
  }
  
  /**
   * 获取系统级统计.
   */
  @GetMapping("/system")
  public Map<String, Object> getSystemStats() {
    return Map.of(
        "health", healthMonitor.getAllHealthChecks().size(),
        "metrics", metricsCollector.getSystemStats(),
        "rateLimiter", rateLimiter.getStatistics()
    );
  }
  
  /**
   * 获取速率限制统计.
   */
  @GetMapping("/rate-limits")
  public Map<String, Object> getRateLimitStats() {
    return rateLimiter.getStatistics();
  }
  
  /**
   * 获取用户配额.
   */
  @GetMapping("/rate-limits/{userId}/{agentId}")
  public Map<String, Object> getUserQuota(
      @PathVariable("userId") String userId,
      @PathVariable("agentId") String agentId
  ) {
    int current = rateLimiter.getCurrentCount(userId, agentId);
    int remaining = rateLimiter.getRemainingQuota(userId, agentId);
    
    return Map.of(
        "userId", userId,
        "agentId", agentId,
        "currentCount", current,
        "remainingQuota", remaining
    );
  }
  
  /**
   * 获取仪表板数据 (综合).
   */
  @GetMapping("/dashboard")
  public Map<String, Object> getDashboard() {
    Map<String, AgentHealthMonitor.HealthCheck> allChecks = healthMonitor.getAllHealthChecks();
    Map<String, AgentHealthMonitor.HealthCheck> unhealthy = healthMonitor.getUnhealthyAgents();
    Map<String, Object> systemStats = metricsCollector.getSystemStats();
    
    // Calculate health percentage
    long healthyCount = allChecks.values().stream()
        .filter(c -> c.getStatus() == AgentHealthMonitor.HealthStatus.HEALTHY)
        .count();
    double healthPercentage = allChecks.size() > 0 
        ? (double) healthyCount / allChecks.size() * 100 
        : 0;
    
    return Map.of(
        "health", Map.of(
            "totalAgents", allChecks.size(),
            "healthyCount", healthyCount,
            "unhealthyCount", unhealthy.size(),
            "healthPercentage", healthPercentage
        ),
        "performance", systemStats,
        "rateLimits", rateLimiter.getStatistics()
    );
  }
}
