package com.agentbot.core.identity;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import org.springframework.boot.ApplicationArguments;

import org.springframework.boot.ApplicationRunner;

import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class NodeIdentityInitializer implements ApplicationRunner {
  public NodeIdentityInitializer() {}



  @Override
  public void run(ApplicationArguments args) {
    Path configDir = resolveConfigDir();
    Path nodeFile = configDir.resolve("node.yml");
    NodeIdentityService service = new NodeIdentityService(new YAMLMapper(), nodeFile);

    try {
      service.loadOrCreate();
    } catch (Exception ex) {
      System.err.println("[agentbot] node.yml validation failed: " + ex.getMessage());

      System.exit(1);
    }
  }


  private Path resolveConfigDir() {
    return com.agentbot.core.util.ConfigPathResolver.resolveConfigDir();
  }

}

