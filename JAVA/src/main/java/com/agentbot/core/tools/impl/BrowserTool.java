package com.agentbot.core.tools.impl;

import com.agentbot.core.browser.BrowserProxyClient;
import com.agentbot.core.browser.BrowserService;
import com.agentbot.core.tools.ToolDefinition;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolWithDefinition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Browser Control Tool backed by BrowserService.
 * Supports profile-aware tabs, snapshot + act, and basic navigation.
 */
public class BrowserTool implements ToolWithDefinition {
  private final BrowserService browserService;
  private final ObjectMapper mapper = new ObjectMapper();

  public BrowserTool(BrowserService browserService) {
    this.browserService = browserService;
  }

  @Override
  public String name() {
    return "browser_control";
  }

  @Override
  public ToolDefinition definition() {
    return new ToolDefinition(
        "browser_control",
        "Control the browser via the local browser control service (status/start/stop/tabs/open/snapshot/act/navigate/screenshot/content/upload).",
        Map.of(
            "type", "object",
            "properties", Map.ofEntries(
                Map.entry("action", Map.of(
                    "type", "string",
                    "enum", List.of(
                        "status", "start", "stop", "profiles", "tabs", "open", "focus", "close",
                        "snapshot", "act", "navigate", "goto", "click", "type", "screenshot", "content", "upload"
                    ),
                    "description", "The action to perform"
                )),
                Map.entry("profile", Map.of("type", "string", "description", "Browser profile name")),
                Map.entry("target", Map.of("type", "string", "description", "Target location: host|sandbox|node")),
                Map.entry("targetId", Map.of("type", "string", "description", "Tab id")),
                Map.entry("snapshotFormat", Map.of("type", "string", "description", "Snapshot format (ai/aria/role)")),

                Map.entry("url", Map.of("type", "string", "description", "Navigation URL")),

                Map.entry("targetUrl", Map.of("type", "string", "description", "Navigation URL")),
                Map.entry("selector", Map.of("type", "string", "description", "CSS selector")),
                Map.entry("text", Map.of("type", "string", "description", "Input text")),
                Map.entry("filePath", Map.of("type", "string", "description", "Local file path to upload")),
                Map.entry("ref", Map.of("type", "string", "description", "Snapshot ref for act/upload")),
                Map.entry("kind", Map.of("type", "string", "description", "Act kind (click/type/hover/press)")),
                Map.entry("key", Map.of("type", "string", "description", "Key name for press" ))
            ),
            "required", List.of("action")
        )

    );
  }

  @Override
  public ToolExecutionResult execute(Map<String, Object> args) {
    String action = asString(args.get("action"));
    String profile = asString(args.get("profile"));
    String target = normalizeTarget(asString(args.get("target")));
    String targetId = asString(args.get("targetId"));

    try {
      if (!"host".equals(target)) {
        return executeViaProxy(target, action, profile, targetId, args);
      }

      switch (action) {
        case "status":
        case "profiles":
          return new ToolExecutionResult(true, toJson(browserService.status()));
        case "start":
          browserService.startProfile(profile);
          return new ToolExecutionResult(true, toJson(browserService.status()));
        case "stop":
          browserService.stopProfile(profile);
          return new ToolExecutionResult(true, toJson(browserService.status()));
        case "tabs":
          return new ToolExecutionResult(true, toJson(Map.of("tabs", browserService.listTabs(profile))));
        case "open": {
          String url = firstNonBlank(asString(args.get("targetUrl")), asString(args.get("url")));
          return new ToolExecutionResult(true, toJson(browserService.openTab(profile, url)));
        }
        case "focus": {
          if (isBlank(targetId)) return new ToolExecutionResult(false, "targetId required");
          browserService.focusTab(profile, targetId);
          return new ToolExecutionResult(true, "Focused tab: " + targetId);
        }
        case "close": {
          if (isBlank(targetId)) return new ToolExecutionResult(false, "targetId required");
          browserService.closeTab(profile, targetId);
          return new ToolExecutionResult(true, "Closed tab: " + targetId);
        }
        case "snapshot": {
          String snapshotFormat = asString(args.get("snapshotFormat"));
          var snapshot = browserService.snapshot(profile, targetId, snapshotFormat);
          return new ToolExecutionResult(true, snapshot.snapshot());
        }

        case "act": {
          String ref = asString(args.get("ref"));
          String kind = asString(args.get("kind"));
          String text = asString(args.get("text"));
          String key = asString(args.get("key"));
          var result = browserService.act(profile, targetId, ref, kind, text, key);
          return new ToolExecutionResult(result.ok(), result.message());
        }
        case "navigate":
        case "goto": {
          String url = firstNonBlank(asString(args.get("targetUrl")), asString(args.get("url")));
          var result = browserService.navigate(profile, targetId, url);
          return new ToolExecutionResult(result.ok(), result.message());
        }
        case "click": {
          String selector = asString(args.get("selector"));
          var result = browserService.click(profile, targetId, selector);
          return new ToolExecutionResult(result.ok(), result.message());
        }
        case "type": {
          String selector = asString(args.get("selector"));
          String text = asString(args.get("text"));
          var result = browserService.type(profile, targetId, selector, text);
          return new ToolExecutionResult(result.ok(), result.message());
        }
        case "screenshot": {
          var result = browserService.screenshot(profile, targetId);
          return new ToolExecutionResult(result.ok(), result.message());
        }
        case "content": {
          var result = browserService.content(profile, targetId);
          return new ToolExecutionResult(result.ok(), result.message());
        }
        case "upload": {
          String selector = asString(args.get("selector"));
          String filePath = asString(args.get("filePath"));
          String ref = asString(args.get("ref"));
          var result = browserService.upload(profile, targetId, selector, filePath, ref);
          return new ToolExecutionResult(result.ok(), result.message());
        }
        default:
          return new ToolExecutionResult(false, "Unsupported action: " + action);
      }
    } catch (Exception e) {
      return new ToolExecutionResult(false, "Browser error (" + action + "): " + e.getMessage());
    }
  }


  @Override
  public boolean requiresApproval(Map<String, Object> args) {
    String action = asString(args == null ? null : args.get("action")).toLowerCase();
    String target = normalizeTarget(asString(args == null ? null : args.get("target")));
    if ("sandbox".equals(target) || "node".equals(target)) {
      return true;
    }
    return action.equals("upload") || action.equals("screenshot") || action.equals("content");
  }

  private ToolExecutionResult executeViaProxy(String target, String action, String profile, String targetId, Map<String, Object> args) {
    BrowserProxyClient client = new BrowserProxyClient(resolveProxyBaseUrl(target));
    switch (action) {
      case "status":
      case "profiles":
        return new ToolExecutionResult(true, client.status());
      case "start":
        return new ToolExecutionResult(true, client.start(profile));
      case "stop":
        return new ToolExecutionResult(true, client.stop(profile));
      case "tabs":
        return new ToolExecutionResult(true, client.tabs(profile));
      case "open": {
        String url = firstNonBlank(asString(args.get("targetUrl")), asString(args.get("url")));
        return new ToolExecutionResult(true, client.open(profile, url));
      }
      case "focus": {
        if (isBlank(targetId)) return new ToolExecutionResult(false, "targetId required");
        return proxyResult(client.focus(profile, targetId));
      }
      case "close": {
        if (isBlank(targetId)) return new ToolExecutionResult(false, "targetId required");
        return proxyResult(client.close(profile, targetId));
      }
      case "snapshot": {
        String snapshotFormat = asString(args.get("snapshotFormat"));
        String body = client.snapshot(profile, targetId, snapshotFormat);
        String snapshot = extractSnapshot(body);
        return new ToolExecutionResult(true, snapshot.isBlank() ? body : snapshot);
      }
      case "act": {
        String ref = asString(args.get("ref"));
        String kind = asString(args.get("kind"));
        String text = asString(args.get("text"));
        String key = asString(args.get("key"));
        return proxyResult(client.act(profile, targetId, ref, kind, text, key));
      }
      case "navigate":
      case "goto": {
        String url = firstNonBlank(asString(args.get("targetUrl")), asString(args.get("url")));
        return proxyResult(client.navigate(profile, targetId, url));
      }
      case "click": {
        String selector = asString(args.get("selector"));
        return proxyResult(client.click(profile, targetId, selector));
      }
      case "type": {
        String selector = asString(args.get("selector"));
        String text = asString(args.get("text"));
        return proxyResult(client.type(profile, targetId, selector, text));
      }
      case "screenshot": {
        return proxyResult(client.screenshot(profile, targetId));
      }
      case "content": {
        return proxyResult(client.content(profile, targetId));
      }
      case "upload": {
        String selector = asString(args.get("selector"));
        String filePath = asString(args.get("filePath"));
        String ref = asString(args.get("ref"));
        return proxyResult(client.upload(profile, targetId, selector, filePath, ref));
      }
      default:
        return new ToolExecutionResult(false, "Unsupported action: " + action);
    }
  }

  private ToolExecutionResult proxyResult(String body) {
    try {
      Map<?, ?> map = mapper.readValue(body, Map.class);
      Object ok = map.get("ok");
      Object message = map.get("message");
      if (ok instanceof Boolean bool) {
        return new ToolExecutionResult(bool, message == null ? body : String.valueOf(message));
      }
    } catch (Exception ignored) {
    }
    return new ToolExecutionResult(true, body);
  }

  private String extractSnapshot(String body) {
    try {
      Map<?, ?> map = mapper.readValue(body, Map.class);
      Object snapshot = map.get("snapshot");
      return snapshot == null ? "" : String.valueOf(snapshot);
    } catch (Exception ignored) {
      return "";
    }
  }

  private String normalizeTarget(String target) {
    String normalized = asString(target).trim().toLowerCase();
    if (normalized.isBlank()) return "host";
    if (normalized.equals("sandbox") || normalized.equals("node") || normalized.equals("host")) return normalized;
    return "host";
  }

  private String resolveProxyBaseUrl(String target) {
    if ("sandbox".equals(target)) {
      String base = browserService.getSandboxBridgeUrl();
      if (isBlank(base)) throw new RuntimeException("sandboxBridgeUrl is not configured");
      return base;
    }
    if ("node".equals(target)) {
      String base = browserService.getNodeBridgeUrl();
      if (isBlank(base)) throw new RuntimeException("nodeBridgeUrl is not configured");
      return base;
    }
    throw new RuntimeException("Unsupported target: " + target);
  }

  private String asString(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  private boolean isBlank(String value) {

    return value == null || value.trim().isEmpty();
  }

  private String firstNonBlank(String a, String b) {
    if (!isBlank(a)) return a;
    if (!isBlank(b)) return b;
    return "";
  }

  private String toJson(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      return String.valueOf(value);
    }
  }
}
