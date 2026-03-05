package com.agentbot.core.tools;

import com.agentbot.config.AgentbotProperties;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class ToolApprovalPolicy {
  public enum Decision {
    ALLOW,
    ASK,
    DENY
  }

  private final AgentbotProperties.ToolApprovals config;

  public ToolApprovalPolicy(AgentbotProperties.ToolApprovals config) {
    this.config = config;
  }

  public Decision decide(String toolName, Map<String, Object> args, boolean toolRisky, String channel) {
    if (toolName == null || toolName.isBlank()) {
      return Decision.DENY;
    }

    String security = normalize(config == null ? null : config.getSecurity(), "allowlist");
    String ask = normalize(config == null ? null : config.getAsk(), "on-miss");
    String askFallback = normalize(config == null ? null : config.getAskFallback(), "deny");

    boolean uiAvailable = isUiAvailable(channel);
    boolean allowlistSatisfied = isAllowlistSatisfied(toolName, args);

    Decision decision;
    if ("deny".equals(security)) {
      decision = Decision.DENY;
    } else if ("full".equals(security)) {
      decision = "always".equals(ask) ? Decision.ASK : Decision.ALLOW;
    } else {
      // allowlist strategy
      if ("always".equals(ask)) {
        decision = Decision.ASK;
      } else if (!toolRisky) {
        decision = Decision.ALLOW;
      } else if (allowlistSatisfied) {
        decision = Decision.ALLOW;
      } else if ("off".equals(ask)) {
        decision = Decision.DENY;
      } else {
        decision = Decision.ASK;
      }
    }

    if (decision == Decision.ASK && !uiAvailable) {
      return "allow".equals(askFallback) ? Decision.ALLOW : Decision.DENY;
    }
    return decision;
  }

  public boolean isUiAvailable(String channel) {
    if (channel == null || channel.isBlank()) return false;
    List<String> uiChannels = config == null ? List.of() : config.getUiChannels();
    if (uiChannels == null || uiChannels.isEmpty()) return "web".equalsIgnoreCase(channel);
    for (String ui : uiChannels) {
      if (ui != null && ui.equalsIgnoreCase(channel)) return true;
    }
    return false;
  }

  public boolean isAllowlistSatisfied(String toolName, Map<String, Object> args) {
    if (config == null) return false;
    List<AgentbotProperties.AllowlistRule> allowlist = config.getAllowlist();
    if (allowlist == null || allowlist.isEmpty()) return false;

    for (AgentbotProperties.AllowlistRule rule : allowlist) {
      if (rule == null) continue;
      String ruleTool = rule.getTool() == null ? "" : rule.getTool().trim();
      if (!matchesPattern(toolName, ruleTool)) {
        continue;
      }
      Map<String, String> match = rule.getMatch();
      if (match == null || match.isEmpty()) {
        return true;
      }
      if (args == null || args.isEmpty()) {
        continue;
      }
      boolean allMatch = true;
      for (Map.Entry<String, String> entry : match.entrySet()) {
        String key = entry.getKey();
        String expected = entry.getValue();
        Object actual = args.get(key);
        if (actual == null) {
          allMatch = false;
          break;
        }
        String actualStr = String.valueOf(actual);
        if (!matchesPattern(actualStr, expected)) {
          allMatch = false;
          break;
        }
      }
      if (allMatch) return true;
    }
    return false;
  }

  private boolean matchesPattern(String value, String pattern) {
    if (pattern == null || pattern.isBlank()) return true;
    if (value == null) return false;
    String trimmed = pattern.trim();
    if (trimmed.startsWith("/") && trimmed.endsWith("/") && trimmed.length() > 1) {
      String regex = trimmed.substring(1, trimmed.length() - 1);
      return Pattern.compile(regex).matcher(value).find();
    }
    StringBuilder regex = new StringBuilder();
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      if (c == '*') {
        regex.append(".*");
      } else if (c == '?') {
        regex.append('.');
      } else if (".\\+*?[^]$(){}=!<>|:-".indexOf(c) >= 0) {
        regex.append('\\').append(c);
      } else {
        regex.append(c);
      }
    }
    return Pattern.compile("^" + regex + "$").matcher(value).find();
  }


  private String normalize(String value, String fallback) {
    if (value == null || value.isBlank()) return fallback;
    return value.trim().toLowerCase();
  }
}
