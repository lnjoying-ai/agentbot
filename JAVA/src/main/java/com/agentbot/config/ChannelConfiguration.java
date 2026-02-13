package com.agentbot.config;

import com.agentbot.core.bus.MessageBus;
import com.agentbot.core.bus.SimpleMessageBus;
import com.agentbot.core.channel.ChannelAdapter;
import com.agentbot.core.channel.ChannelManager;
import com.agentbot.core.channel.ChannelRegistry;
import com.agentbot.core.channel.impl.TelegramChannelAdapter;
import com.agentbot.core.channel.impl.WeChatChannelAdapter;
import com.agentbot.core.channel.impl.WhatsAppBridgeChannelAdapter;
import com.agentbot.core.events.SimpleSystemEventBus;
import com.agentbot.core.events.SystemEventBus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ChannelConfiguration {

  @Bean
  public SystemEventBus systemEventBus() {
    return new SimpleSystemEventBus();
  }

  @Bean
  public MessageBus messageBus(SystemEventBus eventBus) {
    SimpleMessageBus bus = new SimpleMessageBus(eventBus);
    bus.start();
    return bus;
  }


  @Bean
  @ConditionalOnProperty(prefix = "agentbot.channels.telegram", name = "enabled", havingValue = "true")
  public TelegramChannelAdapter telegramChannelAdapter(MessageBus messageBus, AgentbotProperties properties) {
    return new TelegramChannelAdapter(messageBus, properties);
  }

  @Bean
  @ConditionalOnProperty(prefix = "agentbot.channels.whatsapp", name = "enabled", havingValue = "true")
  public WhatsAppBridgeChannelAdapter whatsAppBridgeChannelAdapter(MessageBus messageBus, AgentbotProperties properties) {
    return new WhatsAppBridgeChannelAdapter(messageBus, properties);
  }

  @Bean
  @ConditionalOnProperty(prefix = "agentbot.channels.wechat", name = "enabled", havingValue = "true")
  public WeChatChannelAdapter weChatChannelAdapter(MessageBus messageBus, AgentbotProperties properties) {
    return new WeChatChannelAdapter(messageBus, properties);
  }

  @Bean
  public ChannelRegistry channelRegistry(List<ChannelAdapter> adapters) {
    ChannelRegistry registry = new ChannelRegistry();
    for (ChannelAdapter adapter : adapters) {
      registry.register(adapter);
    }
    return registry;
  }

  @Bean
  public ChannelManager channelManager(ChannelRegistry registry, MessageBus messageBus) {
    ChannelManager manager = new ChannelManager(registry, messageBus);
    manager.startAll();
    return manager;
  }
}

