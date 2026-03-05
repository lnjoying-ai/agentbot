package com.agentbot.core.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;

public class CdpSessionManager {
  public record CdpSession(Browser browser, BrowserContext context) {}

  public CdpSession connect(BrowserType browserType, String cdpUrl, int timeoutMs) {
    if (browserType == null) {
      throw new RuntimeException("BrowserType not initialized");
    }
    if (cdpUrl == null || cdpUrl.isBlank()) {
      throw new RuntimeException("cdpUrl required");
    }
    Browser browser;
    try {
      BrowserType.ConnectOverCDPOptions options = new BrowserType.ConnectOverCDPOptions();
      if (timeoutMs > 0) {
        options.setTimeout(timeoutMs);
      }
      browser = browserType.connectOverCDP(cdpUrl, options);
    } catch (NoSuchMethodError | NoClassDefFoundError e) {
      browser = browserType.connectOverCDP(cdpUrl);
    }
    BrowserContext context = browser.contexts().isEmpty() ? browser.newContext() : browser.contexts().get(0);
    return new CdpSession(browser, context);
  }
}
