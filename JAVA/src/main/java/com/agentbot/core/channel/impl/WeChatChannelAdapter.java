package com.agentbot.core.channel.impl;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.bus.ExternalMessageBus;
import com.agentbot.core.bus.MessageEnvelope;
import com.agentbot.core.bus.events.OutboundMessage;
import com.agentbot.core.channel.ChannelAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class WeChatChannelAdapter implements ChannelAdapter {

  private static final Logger log = LoggerFactory.getLogger(WeChatChannelAdapter.class);

  private final ExternalMessageBus messageBus;
  private final AgentbotProperties.WeChat config;

  public WeChatChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
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
    log.info("wechat inbound: chatId={} senderId={} length={}", chatId, from, content == null ? 0 : content.length());
    HashMap<String, Object> metadata = new HashMap<>();
    metadata.put(MessageEnvelope.META_ACCOUNT_ID, "default");
    metadata.put(MessageEnvelope.META_PEER_KIND, "dm");
    metadata.put(MessageEnvelope.META_PEER_ID, chatId);
    MessageEnvelope inbound = MessageEnvelope.externalInbound("wechat", from, chatId, content, metadata);
    messageBus.publish(inbound);


  }

}
