package com.agentbot.core.agent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 间通信消息协议.
 * 
 * Message Types:
 * - REQUEST: 请求另一个 Agent 执行任务
 * - RESPONSE: 响应其他 Agent 的请求
 * - NOTIFICATION: 单向通知消息
 * - BROADCAST: 广播消息给所有 Agent
 */
public class AgentMessage {
  
  public enum MessageType {
    REQUEST,      // 需要响应的请求
    RESPONSE,     // 对 REQUEST 的响应
    NOTIFICATION, // 单向通知
    BROADCAST     // 广播消息
  }
  
  private final String id;
  private final MessageType type;
  private final String fromAgentId;
  private final String toAgentId;  // null for broadcast
  private final String correlationId; // 关联请求ID (用于响应)
  private final String content;
  private final Map<String, Object> metadata;
  private final Instant timestamp;
  private final int priority; // 0-9, higher is more urgent
  
  private AgentMessage(Builder builder) {
    this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
    this.type = builder.type;
    this.fromAgentId = builder.fromAgentId;
    this.toAgentId = builder.toAgentId;
    this.correlationId = builder.correlationId;
    this.content = builder.content;
    this.metadata = builder.metadata != null ? Map.copyOf(builder.metadata) : Map.of();
    this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
    this.priority = builder.priority;
  }
  
  public String getId() {
    return id;
  }
  
  public MessageType getType() {
    return type;
  }
  
  public String getFromAgentId() {
    return fromAgentId;
  }
  
  public String getToAgentId() {
    return toAgentId;
  }
  
  public String getCorrelationId() {
    return correlationId;
  }
  
  public String getContent() {
    return content;
  }
  
  public Map<String, Object> getMetadata() {
    return metadata;
  }
  
  public Instant getTimestamp() {
    return timestamp;
  }
  
  public int getPriority() {
    return priority;
  }
  
  public boolean isBroadcast() {
    return type == MessageType.BROADCAST || toAgentId == null;
  }
  
  public static Builder builder() {
    return new Builder();
  }
  
  public static class Builder {
    private String id;
    private MessageType type = MessageType.REQUEST;
    private String fromAgentId;
    private String toAgentId;
    private String correlationId;
    private String content;
    private Map<String, Object> metadata;
    private Instant timestamp;
    private int priority = 5; // default medium priority
    
    public Builder id(String id) {
      this.id = id;
      return this;
    }
    
    public Builder type(MessageType type) {
      this.type = type;
      return this;
    }
    
    public Builder from(String fromAgentId) {
      this.fromAgentId = fromAgentId;
      return this;
    }
    
    public Builder to(String toAgentId) {
      this.toAgentId = toAgentId;
      return this;
    }
    
    public Builder correlationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }
    
    public Builder content(String content) {
      this.content = content;
      return this;
    }
    
    public Builder metadata(Map<String, Object> metadata) {
      this.metadata = metadata;
      return this;
    }
    
    public Builder timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }
    
    public Builder priority(int priority) {
      this.priority = Math.max(0, Math.min(9, priority));
      return this;
    }
    
    public AgentMessage build() {
      if (fromAgentId == null) {
        throw new IllegalArgumentException("fromAgentId is required");
      }
      if (content == null) {
        throw new IllegalArgumentException("content is required");
      }
      return new AgentMessage(this);
    }
  }
  
  @Override
  public String toString() {
    return String.format("AgentMessage{id='%s', type=%s, from='%s', to='%s', priority=%d}",
        id, type, fromAgentId, toAgentId, priority);
  }
}
