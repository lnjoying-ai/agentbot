package com.agentbot.config;

import com.agentbot.core.bus.ExternalMessageBus;
import com.agentbot.core.bus.SimpleMessageBus;
import com.agentbot.core.channel.ChannelAdapter;
import com.agentbot.core.channel.ChannelManager;
import com.agentbot.core.channel.ChannelRegistry;
import com.agentbot.core.channel.impl.TelegramChannelAdapter;
import com.agentbot.core.channel.impl.WeChatChannelAdapter;
import com.agentbot.core.channel.impl.WhatsAppBridgeChannelAdapter;
import com.agentbot.core.channel.impl.FeishuChannelAdapter;
import com.agentbot.core.channel.impl.DiscordChannelAdapter;
import com.agentbot.core.channel.impl.MaixCamChannelAdapter;
import com.agentbot.core.channel.impl.QQChannelAdapter;
import com.agentbot.core.channel.impl.DingTalkChannelAdapter;
import com.agentbot.core.channel.impl.SlackChannelAdapter;
import com.agentbot.core.channel.impl.LineChannelAdapter;
import com.agentbot.core.channel.impl.OneBotChannelAdapter;

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
  public ExternalMessageBus externalMessageBus(SystemEventBus eventBus) {
    SimpleMessageBus bus = new SimpleMessageBus(eventBus);
    ExternalMessageBus external = new ExternalMessageBus(bus);
    external.start();
    return external;
  }



  @Bean
  @ConditionalOnProperty(prefix = "agentbot.channels.telegram", name = "enabled", havingValue = "true")
  public TelegramChannelAdapter telegramChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    return new TelegramChannelAdapter(messageBus, properties);
  }

  @Bean
  @ConditionalOnProperty(prefix = "agentbot.channels.whatsapp", name = "enabled", havingValue = "true")
  public WhatsAppBridgeChannelAdapter whatsAppBridgeChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    return new WhatsAppBridgeChannelAdapter(messageBus, properties);
  }

  @Bean
  @ConditionalOnProperty(prefix = "agentbot.channels.wechat", name = "enabled", havingValue = "true")
  public WeChatChannelAdapter weChatChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    return new WeChatChannelAdapter(messageBus, properties);
  }

  @Bean
  @ConditionalOnProperty(prefix = "agentbot.channels.feishu", name = "enabled", havingValue = "true")
  public FeishuChannelAdapter feishuChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    return new FeishuChannelAdapter(messageBus, properties);
  }

  @Bean
  @ConditionalOnProperty(prefix = "agentbot.channels.discord", name = "enabled", havingValue = "true")
  public DiscordChannelAdapter discordChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    return new DiscordChannelAdapter(messageBus, properties);
  }

  @Bean
  @ConditionalOnProperty(prefix = "agentbot.channels.maixcam", name = "enabled", havingValue = "true")
  public MaixCamChannelAdapter maixCamChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    return new MaixCamChannelAdapter(messageBus, properties);
  }

  @Bean
  @ConditionalOnProperty(prefix = "agentbot.channels.qq", name = "enabled", havingValue = "true")
  public QQChannelAdapter qqChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    return new QQChannelAdapter(messageBus, properties);
  }

  @Bean
  @ConditionalOnProperty(prefix = "agentbot.channels.dingtalk", name = "enabled", havingValue = "true")
  public DingTalkChannelAdapter dingTalkChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    return new DingTalkChannelAdapter(messageBus, properties);
  }

  @Bean
  @ConditionalOnProperty(prefix = "agentbot.channels.slack", name = "enabled", havingValue = "true")
  public SlackChannelAdapter slackChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    return new SlackChannelAdapter(messageBus, properties);
  }

  @Bean
  @ConditionalOnProperty(prefix = "agentbot.channels.line", name = "enabled", havingValue = "true")
  public LineChannelAdapter lineChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    return new LineChannelAdapter(messageBus, properties);
  }

  @Bean
  @ConditionalOnProperty(prefix = "agentbot.channels.onebot", name = "enabled", havingValue = "true")
  public OneBotChannelAdapter oneBotChannelAdapter(ExternalMessageBus messageBus, AgentbotProperties properties) {
    return new OneBotChannelAdapter(messageBus, properties);
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
  public ChannelManager channelManager(ChannelRegistry registry, ExternalMessageBus messageBus) {
    ChannelManager manager = new ChannelManager(registry, messageBus);
    manager.startAll();
    return manager;
  }

}

