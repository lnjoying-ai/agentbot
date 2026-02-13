package com.agentbot.core.channel.impl;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.bus.MessageBus;
import com.agentbot.core.bus.events.InboundMessage;
import com.agentbot.core.bus.events.OutboundMessage;
import com.agentbot.core.channel.ChannelAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeChatChannelAdapter implements ChannelAdapter {
  private static final Logger log = LoggerFactory.getLogger(WeChatChannelAdapter.class);

  private final MessageBus messageBus;
  private final AgentbotProperties.WeChat config;

  public WeChatChannelAdapter(MessageBus messageBus, AgentbotProperties properties) {
    this.messageBus = messageBus;
    this.config = properties.getChannels().getWechat();
  }

  @Override
  public String name() {
    return "wechat";
  }

  @Override
  public void start() {
    if (!config.isEnabled()) {
      log.info("wechat channel disabled");
      return;
    }
    log.info("wechat channel started (webhook mode)");
  }

  @Override
  public void stop() {
    log.info("wechat channel stopped");
  }

  @Override
  public void send(OutboundMessage message) {
    if (!config.isEnabled()) return;
    log.info("wechat send placeholder: chatId={} content={}", message.getChatId(), message.getContent());
  }

  public void handleInbound(String from, String chatId, String content) {
    if (!config.isEnabled()) return;
    InboundMessage inbound = new InboundMessage("wechat", from, chatId, content);
    messageBus.publishInbound(inbound);
  }
}
