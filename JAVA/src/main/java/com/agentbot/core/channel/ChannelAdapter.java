package com.agentbot.core.channel;

import com.agentbot.core.bus.events.OutboundMessage;

/**
 * Channel Adapter 接口.
 * 
 * 所有渠道适配器必须实现此接口，提供特定平台的消息收发逻辑.
 * 
 * 支持的渠道类型:
 * - Telegram
 * - WhatsApp
 * - 微信 (WeChat)
 * - 飞书 (Lark/Feishu)
 * - Discord
 * - Slack
 * - REST API
 * - WebSocket
 */
public interface ChannelAdapter {
  
  /**
   * 获取渠道名称.
   */
  String name();
  
  /**
   * 启动适配器.
   */
  void start();
  
  /**
   * 停止适配器.
   */
  void stop();
  
  /**
   * 发送消息到渠道.
   */
  void send(OutboundMessage message);
}
