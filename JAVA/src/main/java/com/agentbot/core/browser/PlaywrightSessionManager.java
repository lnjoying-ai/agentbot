package com.agentbot.core.browser;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;

import java.nio.file.Path;

public class PlaywrightSessionManager {
  public BrowserContext launchPersistent(BrowserType browserType, Path dataDir, BrowserType.LaunchPersistentContextOptions options) {
    if (browserType == null) {
      throw new RuntimeException("BrowserType not initialized");
    }
    if (dataDir == null) {
      throw new RuntimeException("dataDir required");
    }
    return browserType.launchPersistentContext(dataDir, options);
  }
}
