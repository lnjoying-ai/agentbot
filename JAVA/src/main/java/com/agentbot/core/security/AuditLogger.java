package com.agentbot.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 审计日志记录器.
 * 
 * 功能:
 * - 记录所有 Agent 操作
 * - 异步写入日志
 * - 结构化日志格式
 * - 日志轮转
 */
public class AuditLogger {
  
  private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);
  
  /**
   * 审计事件类型.
   */
  public enum EventType {
    AGENT_CREATED,
    AGENT_UPDATED,
    AGENT_DELETED,
    AGENT_STARTED,
    AGENT_STOPPED,
    MESSAGE_SENT,
    MESSAGE_RECEIVED,
    PERMISSION_GRANTED,
    PERMISSION_REVOKED,
    ACCESS_DENIED,
    RATE_LIMIT_EXCEEDED,
    ERROR
  }
  
  /**
   * 审计事件.
   */
  public static class AuditEvent {
    private final Instant timestamp;
    private final EventType type;
    private final String userId;
    private final String agentId;
    private final String action;
    private final Map<String, Object> metadata;
    private final boolean success;
    private final String errorMessage;
    
    public AuditEvent(
        EventType type,
        String userId,
        String agentId,
        String action,
        Map<String, Object> metadata,
        boolean success,
        String errorMessage
    ) {
      this.timestamp = Instant.now();
      this.type = type;
      this.userId = userId;
      this.agentId = agentId;
      this.action = action;
      this.metadata = metadata;
      this.success = success;
      this.errorMessage = errorMessage;
    }
    
    public String toLogLine() {
      return String.format(
          "[%s] %s | user=%s agent=%s action=%s success=%s%s",
          DateTimeFormatter.ISO_INSTANT.format(timestamp),
          type,
          userId != null ? userId : "system",
          agentId != null ? agentId : "N/A",
          action,
          success,
          errorMessage != null ? " error=" + errorMessage : ""
      );
    }
  }
  
  private final Path auditLogPath;
  private final BlockingQueue<AuditEvent> eventQueue;
  private final Thread writerThread;
  private volatile boolean running = false;
  
  public AuditLogger(Path auditLogPath) {
    this.auditLogPath = auditLogPath;
    this.eventQueue = new LinkedBlockingQueue<>(10000);
    this.writerThread = new Thread(this::processEvents, "AuditLogger-Writer");
    this.writerThread.setDaemon(true);
  }
  
  /**
   * 启动审计日志记录器.
   */
  public void start() {
    if (running) {
      return;
    }
    
    try {
      Files.createDirectories(auditLogPath.getParent());
      if (!Files.exists(auditLogPath)) {
        Files.createFile(auditLogPath);
      }
    } catch (IOException e) {
      log.error("Failed to create audit log file", e);
      throw new RuntimeException(e);
    }
    
    running = true;
    writerThread.start();
    log.info("AuditLogger started, logging to: {}", auditLogPath);
  }
  
  /**
   * 停止审计日志记录器.
   */
  public void stop() {
    if (!running) {
      return;
    }
    
    running = false;
    writerThread.interrupt();
    
    try {
      writerThread.join(5000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    
    // Flush remaining events
    flushRemainingEvents();
    
    log.info("AuditLogger stopped");
  }
  
  /**
   * 记录审计事件.
   */
  public void log(AuditEvent event) {
    if (!running) {
      return;
    }
    
    if (!eventQueue.offer(event)) {
      log.warn("Audit event queue full, dropping event: {}", event.toLogLine());
    }
  }
  
  /**
   * 记录审计事件 (简化版).
   */
  public void log(
      EventType type,
      String userId,
      String agentId,
      String action,
      boolean success
  ) {
    log(new AuditEvent(type, userId, agentId, action, null, success, null));
  }
  
  /**
   * 记录审计事件 (带元数据).
   */
  public void log(
      EventType type,
      String userId,
      String agentId,
      String action,
      Map<String, Object> metadata,
      boolean success
  ) {
    log(new AuditEvent(type, userId, agentId, action, metadata, success, null));
  }
  
  /**
   * 记录错误事件.
   */
  public void logError(
      EventType type,
      String userId,
      String agentId,
      String action,
      String errorMessage
  ) {
    log(new AuditEvent(type, userId, agentId, action, null, false, errorMessage));
  }
  
  /**
   * 处理事件队列.
   */
  private void processEvents() {
    while (running || !eventQueue.isEmpty()) {
      try {
        AuditEvent event = eventQueue.poll(1, java.util.concurrent.TimeUnit.SECONDS);
        if (event != null) {
          writeEvent(event);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        log.error("Error processing audit event", e);
      }
    }
  }
  
  /**
   * 写入事件到日志文件.
   */
  private void writeEvent(AuditEvent event) {
    try {
      String logLine = event.toLogLine() + System.lineSeparator();
      Files.writeString(
          auditLogPath,
          logLine,
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND
      );
    } catch (IOException e) {
      log.error("Failed to write audit event", e);
    }
  }
  
  /**
   * 刷新剩余事件.
   */
  private void flushRemainingEvents() {
    AuditEvent event;
    while ((event = eventQueue.poll()) != null) {
      writeEvent(event);
    }
  }
}
