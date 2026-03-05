package com.agentbot.core.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AntiBotConfig {
  private final String level;
  private final String userAgent;
  private final String locale;
  private final String timezoneId;
  private final Map<String, String> headers;
  private final List<String> blockResourceTypes;
  private final List<String> blockUrlPatterns;
  private final List<String> proxies;
  private final boolean enableBehavior;
  private final boolean enableDetection;
  private final boolean enableStealth;
  private final boolean enableResourceBlock;

  public AntiBotConfig(
      String level,
      String userAgent,
      String locale,
      String timezoneId,
      Map<String, String> headers,
      List<String> blockResourceTypes,
      List<String> blockUrlPatterns,
      List<String> proxies,
      boolean enableBehavior,
      boolean enableDetection,
      boolean enableStealth,
      boolean enableResourceBlock
  ) {
    this.level = level == null ? "basic" : level;
    this.userAgent = userAgent == null ? "" : userAgent;
    this.locale = locale == null ? "zh-CN" : locale;
    this.timezoneId = timezoneId == null ? "Asia/Shanghai" : timezoneId;
    this.headers = headers == null ? new HashMap<>() : new HashMap<>(headers);
    this.blockResourceTypes = blockResourceTypes == null ? new ArrayList<>() : new ArrayList<>(blockResourceTypes);
    this.blockUrlPatterns = blockUrlPatterns == null ? new ArrayList<>() : new ArrayList<>(blockUrlPatterns);
    this.proxies = proxies == null ? new ArrayList<>() : new ArrayList<>(proxies);
    this.enableBehavior = enableBehavior;
    this.enableDetection = enableDetection;
    this.enableStealth = enableStealth;
    this.enableResourceBlock = enableResourceBlock;
  }

  public String getLevel() {
    return level;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public String getLocale() {
    return locale;
  }

  public String getTimezoneId() {
    return timezoneId;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public List<String> getBlockResourceTypes() {
    return blockResourceTypes;
  }

  public List<String> getBlockUrlPatterns() {
    return blockUrlPatterns;
  }

  public List<String> getProxies() {
    return proxies;
  }

  public boolean isEnableBehavior() {
    return enableBehavior;
  }

  public boolean isEnableDetection() {
    return enableDetection;
  }

  public boolean isEnableStealth() {
    return enableStealth;
  }

  public boolean isEnableResourceBlock() {
    return enableResourceBlock;
  }

  public int levelRank() {
    String normalized = level == null ? "" : level.trim().toLowerCase();
    return switch (normalized) {
      case "advanced" -> 3;
      case "enhanced" -> 2;
      default -> 1;
    };
  }
}
