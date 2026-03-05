package com.agentbot.core.agent;

/**
 * Agent 消息处理器接口.
 * Agent 实现此接口来接收和处理来自其他 Agent 的消息.
 */
@FunctionalInterface
public interface AgentMessageHandler {
  
  /**
   * 处理接收到的 Agent 消息.
   * 
   * @param message 接收到的消息
   */
  void handleMessage(AgentMessage message);
}
