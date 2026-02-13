package com.agentbot;

import com.agentbot.config.AgentbotProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AgentbotProperties.class)
public class AgentbotApplication {
  public static final long START_TIME = System.currentTimeMillis();

  public static void main(String[] args) {

    SpringApplication.run(AgentbotApplication.class, args);
  }
}

