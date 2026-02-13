package com.agentbot.cli;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CliRunner implements ApplicationRunner {
  private final CliService cliService;

  public CliRunner(CliService cliService) {
    this.cliService = cliService;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (args.containsOption("help") || args.containsOption("h")) {
      cliService.printHelp();
      System.exit(0);
    }
    if (!args.containsOption("cli")) {
      return;
    }
    String command = args.getOptionValues("cli") == null || args.getOptionValues("cli").isEmpty()
        ? "status" : args.getOptionValues("cli").get(0);
    boolean keepRunning = cliService.execute(command, args);
    if (!keepRunning) {
      System.exit(0);
    }
  }
}


