package com.agentbot.core.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Agent 间消息总线.
 * 
 * 功能:
 * - 点对点消息传递
 * - 广播消息
 * - 消息优先级队列
 * - 异步处理
 * - 消息统计和监控
 */
public class AgentMessageBus {
  
  private static final Logger log = LoggerFactory.getLogger(AgentMessageBus.class);
  
  private final AgentRegistry registry;
  private final Map<String, AgentMessageHandler> handlers = new ConcurrentHashMap<>();
  private final ExecutorService executor;
  private final PriorityBlockingQueue<AgentMessage> messageQueue;
  private final ScheduledExecutorService scheduler;
  private final Map<String, Long> recentMessageIds = new ConcurrentHashMap<>();
  private final long dedupWindowMs = 60000;
  
  // Statistics
  private final AtomicLong messagesSent = new AtomicLong(0);

  private final AtomicLong messagesDelivered = new AtomicLong(0);
  private final AtomicLong messagesFailed = new AtomicLong(0);
  private final AtomicLong broadcastCount = new AtomicLong(0);
  
  private volatile boolean running = false;
  
  public AgentMessageBus(AgentRegistry registry) {
    this(registry, 10); // default 10 threads
  }
  
  public AgentMessageBus(AgentRegistry registry, int threadPoolSize) {
    this.registry = registry;
    this.messageQueue = new PriorityBlockingQueue<>(
        100,
        (m1, m2) -> Integer.compare(m2.getPriority(), m1.getPriority()) // higher priority first
    );
    
    this.executor = Executors.newFixedThreadPool(
        threadPoolSize,
        r -> {
          Thread t = new Thread(r, "AgentMessageBus-Worker");
          t.setDaemon(true);
          return t;
        }
    );
    
    this.scheduler = Executors.newSingleThreadScheduledExecutor(
        r -> {
          Thread t = new Thread(r, "AgentMessageBus-Scheduler");
          t.setDaemon(true);
          return t;
        }
    );
  }
  
  /**
   * 启动消息总线.
   */
  public void start() {
    if (running) {
      log.warn("AgentMessageBus already running");
      return;
    }
    
    running = true;
    log.info("Starting AgentMessageBus...");
    
    // Start message processing workers
    for (int i = 0; i < 3; i++) {
      executor.submit(this::processMessages);
    }
    
    // Start statistics logging
    scheduler.scheduleAtFixedRate(
        this::logStatistics,
        300, 300, TimeUnit.SECONDS
    );

    scheduler.scheduleAtFixedRate(
        this::cleanupDedup,
        60, 60, TimeUnit.SECONDS
    );


    
    log.info("AgentMessageBus started");
  }
  
  /**
   * 停止消息总线.
   */
  public void stop() {
    if (!running) {
      return;
    }
    
    log.info("Stopping AgentMessageBus...");
    running = false;
    
    executor.shutdown();
    scheduler.shutdown();
    
    try {
      if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
    
    log.info("AgentMessageBus stopped");
  }
  
  /**
   * 注册 Agent 消息处理器.
   */
  public void registerHandler(String agentId, AgentMessageHandler handler) {
    handlers.put(agentId, handler);
    log.debug("Registered message handler for agent: {}", agentId);
  }
  
  /**
   * 取消注册 Agent 消息处理器.
   */
  public void unregisterHandler(String agentId) {
    handlers.remove(agentId);
    log.debug("Unregistered message handler for agent: {}", agentId);
  }
  
  /**
   * 发送消息给指定 Agent.
   */
  public CompletableFuture<Void> sendMessage(AgentMessage message) {
    if (!running) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("AgentMessageBus not running")
      );
    }
    
    if (message.getToAgentId() == null) {
      return CompletableFuture.failedFuture(
          new IllegalArgumentException("Target agent ID is required for sendMessage")
      );
    }
    
    messagesSent.incrementAndGet();
    messageQueue.offer(message);
    
    log.debug("Message queued: {} -> {}", message.getFromAgentId(), message.getToAgentId());
    
    return CompletableFuture.completedFuture(null);
  }
  
  /**
   * 广播消息给所有 Agent (除了发送者).
   */
  public CompletableFuture<Void> broadcast(AgentMessage message) {
    if (!running) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("AgentMessageBus not running")
      );
    }
    
    broadcastCount.incrementAndGet();
    
    Map<String, AgentInstance> agents = registry.getAllAgents();
    
    CompletableFuture<?>[] futures = agents.keySet().stream()
        .filter(agentId -> !agentId.equals(message.getFromAgentId()))
        .map(agentId -> {
          AgentMessage targetMessage = AgentMessage.builder()
              .id(message.getId() + "-" + agentId)
              .type(AgentMessage.MessageType.BROADCAST)
              .from(message.getFromAgentId())
              .to(agentId)
              .content(message.getContent())
              .metadata(message.getMetadata())
              .priority(message.getPriority())
              .build();
          
          messagesSent.incrementAndGet();
          messageQueue.offer(targetMessage);
          return CompletableFuture.completedFuture(null);
        })
        .toArray(CompletableFuture[]::new);
    
    log.debug("Broadcast message from {} to {} agents", 
        message.getFromAgentId(), futures.length);
    
    return CompletableFuture.allOf(futures);
  }
  
  /**
   * 发送请求并等待响应.
   */
  public CompletableFuture<AgentMessage> sendRequest(
      String fromAgentId,
      String toAgentId,
      String content,
      long timeoutMs
  ) {
    CompletableFuture<AgentMessage> responseFuture = new CompletableFuture<>();
    
    AgentMessage request = AgentMessage.builder()
        .type(AgentMessage.MessageType.REQUEST)
        .from(fromAgentId)
        .to(toAgentId)
        .content(content)
        .priority(7) // high priority for requests
        .build();
    
    // Register temporary response handler
    String correlationId = request.getId();
    AgentMessageHandler responseHandler = (msg) -> {
      if (correlationId.equals(msg.getCorrelationId()) && 
          msg.getType() == AgentMessage.MessageType.RESPONSE) {
        responseFuture.complete(msg);
        unregisterHandler(fromAgentId + "-response-" + correlationId);
      }
    };
    
    registerHandler(fromAgentId + "-response-" + correlationId, responseHandler);
    
    // Send request
    sendMessage(request);
    
    // Setup timeout
    scheduler.schedule(() -> {
      if (!responseFuture.isDone()) {
        responseFuture.completeExceptionally(
            new TimeoutException("Request timeout: " + correlationId)
        );
        unregisterHandler(fromAgentId + "-response-" + correlationId);
      }
    }, timeoutMs, TimeUnit.MILLISECONDS);
    
    return responseFuture;
  }
  
  /**
   * 发送响应消息.
   */
  public CompletableFuture<Void> sendResponse(
      String fromAgentId,
      String toAgentId,
      String correlationId,
      String content
  ) {
    AgentMessage response = AgentMessage.builder()
        .type(AgentMessage.MessageType.RESPONSE)
        .from(fromAgentId)
        .to(toAgentId)
        .correlationId(correlationId)
        .content(content)
        .priority(8) // very high priority for responses
        .build();
    
    return sendMessage(response);
  }
  
  /**
   * 消息处理工作线程.
   */
  private void processMessages() {
    while (running) {
      try {
        AgentMessage message = messageQueue.poll(1, TimeUnit.SECONDS);
        if (message == null) {
          continue;
        }
        
        deliverMessage(message);
        
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        log.error("Error processing message", e);
      }
    }
  }
  
  /**
   * 投递消息到目标 Agent.
   */
  private void deliverMessage(AgentMessage message) {
    if (isDuplicate(message.getId())) {
      return;
    }
    if (isExpired(message)) {
      return;
    }
    String targetAgentId = message.getToAgentId();
    
    AgentMessageHandler handler = handlers.get(targetAgentId);


    if (handler == null) {
      log.warn("No handler registered for agent: {}", targetAgentId);
      messagesFailed.incrementAndGet();
      return;
    }
    
    try {
      handler.handleMessage(message);
      messagesDelivered.incrementAndGet();
      
      log.debug("Message delivered: {} -> {}", 
          message.getFromAgentId(), targetAgentId);
      
    } catch (Exception e) {
      log.error("Error delivering message to agent: {}", targetAgentId, e);
      messagesFailed.incrementAndGet();
    }
  }
  
  /**
   * 记录统计信息.
   */
  private void logStatistics() {
    long sent = messagesSent.get();
    long delivered = messagesDelivered.get();
    long failed = messagesFailed.get();
    long broadcast = broadcastCount.get();
    int queueSize = messageQueue.size();

    if (sent == 0 && delivered == 0 && failed == 0 && broadcast == 0 && queueSize == 0) {
      return;
    }

    log.info("AgentMessageBus Statistics: sent={}, delivered={}, failed={}, broadcast={}, queue={}",
        sent, delivered, failed, broadcast, queueSize
    );
  }

  private boolean isDuplicate(String messageId) {
    if (messageId == null || messageId.isBlank()) {
      return false;
    }
    long now = System.currentTimeMillis();
    Long existing = recentMessageIds.putIfAbsent(messageId, now);
    if (existing != null) {
      return true;
    }
    recentMessageIds.put(messageId, now);
    return false;
  }

  private void cleanupDedup() {
    long now = System.currentTimeMillis();
    recentMessageIds.entrySet().removeIf(entry -> now - entry.getValue() > dedupWindowMs);
  }

  private boolean isExpired(AgentMessage message) {
    if (message == null || message.getMetadata() == null) {
      return false;
    }
    Object ttlObj = message.getMetadata().get("ttl");
    if (ttlObj instanceof Number ttlNumber) {
      long ttl = ttlNumber.longValue();
      return message.getTimestamp().toEpochMilli() + ttl < System.currentTimeMillis();
    }
    Object expireAt = message.getMetadata().get("expireAt");
    if (expireAt instanceof Number expireNumber) {
      return expireNumber.longValue() < System.currentTimeMillis();
    }
    return false;
  }

  
  /**
   * 获取统计信息.
   */

  public Map<String, Object> getStatistics() {

    return Map.of(
        "messagesSent", messagesSent.get(),
        "messagesDelivered", messagesDelivered.get(),
        "messagesFailed", messagesFailed.get(),
        "broadcastCount", broadcastCount.get(),
        "queueSize", messageQueue.size(),
        "registeredHandlers", handlers.size()
    );
  }
}
