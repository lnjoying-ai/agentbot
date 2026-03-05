package com.agentbot.core.browser;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class BrowserProxyClient {
  private final String baseUrl;
  private final HttpClient client = HttpClient.newHttpClient();
  private final ObjectMapper mapper = new ObjectMapper();

  public BrowserProxyClient(String baseUrl) {
    this.baseUrl = normalizeBaseUrl(baseUrl);
  }

  public String status() {
    return send(HttpRequest.newBuilder(uri("/", Map.of()))
        .GET()
        .build());
  }

  public String start(String profile) {
    return send(HttpRequest.newBuilder(uri("/start", Map.of("profile", safe(profile))))
        .POST(HttpRequest.BodyPublishers.noBody())
        .build());
  }

  public String stop(String profile) {
    return send(HttpRequest.newBuilder(uri("/stop", Map.of("profile", safe(profile))))
        .POST(HttpRequest.BodyPublishers.noBody())
        .build());
  }

  public String tabs(String profile) {
    return send(HttpRequest.newBuilder(uri("/tabs", Map.of("profile", safe(profile))))
        .GET()
        .build());
  }

  public String open(String profile, String url) {
    return sendJson("/tabs/open", Map.of("profile", safe(profile), "url", safe(url)));
  }

  public String focus(String profile, String targetId) {
    return sendJson("/tabs/focus", Map.of("profile", safe(profile), "targetId", safe(targetId)));
  }

  public String close(String profile, String targetId) {
    return send(HttpRequest.newBuilder(uri("/tabs/close", Map.of("profile", safe(profile), "targetId", safe(targetId))))
        .DELETE()
        .build());
  }

  public String snapshot(String profile, String targetId, String format) {
    Map<String, String> query = new HashMap<>();
    query.put("profile", safe(profile));
    query.put("targetId", safe(targetId));
    query.put("format", safe(format));
    return send(HttpRequest.newBuilder(uri("/snapshot", query))
        .GET()
        .build());
  }

  public String act(String profile, String targetId, String ref, String kind, String text, String key) {
    return sendJson("/act", Map.of(
        "profile", safe(profile),
        "targetId", safe(targetId),
        "ref", safe(ref),
        "kind", safe(kind),
        "text", safe(text),
        "key", safe(key)
    ));
  }

  public String navigate(String profile, String targetId, String url) {
    return sendJson("/navigate", Map.of("profile", safe(profile), "targetId", safe(targetId), "url", safe(url)));
  }

  public String click(String profile, String targetId, String selector) {
    return sendJson("/click", Map.of("profile", safe(profile), "targetId", safe(targetId), "selector", safe(selector)));
  }

  public String type(String profile, String targetId, String selector, String text) {
    return sendJson("/type", Map.of("profile", safe(profile), "targetId", safe(targetId), "selector", safe(selector), "text", safe(text)));
  }

  public String upload(String profile, String targetId, String selector, String filePath, String ref) {
    return sendJson("/upload", Map.of(
        "profile", safe(profile),
        "targetId", safe(targetId),
        "selector", safe(selector),
        "filePath", safe(filePath),
        "ref", safe(ref)
    ));
  }

  public String screenshot(String profile, String targetId) {
    return sendJson("/screenshot", Map.of("profile", safe(profile), "targetId", safe(targetId)));
  }

  public String content(String profile, String targetId) {
    return send(HttpRequest.newBuilder(uri("/content", Map.of("profile", safe(profile), "targetId", safe(targetId))))
        .GET()
        .build());
  }

  private String sendJson(String path, Map<String, Object> body) {
    try {
      String json = mapper.writeValueAsString(body);
      HttpRequest request = HttpRequest.newBuilder(uri(path, Map.of()))
          .POST(HttpRequest.BodyPublishers.ofString(json))
          .header("Content-Type", "application/json")
          .build();
      return send(request);
    } catch (Exception e) {
      throw new RuntimeException("Failed to send request: " + e.getMessage(), e);
    }
  }

  private String send(HttpRequest request) {
    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return response.body();
    } catch (Exception e) {
      throw new RuntimeException("Browser proxy request failed: " + e.getMessage(), e);
    }
  }

  private URI uri(String path, Map<String, String> query) {
    StringBuilder sb = new StringBuilder();
    sb.append(baseUrl);
    if (!path.startsWith("/")) sb.append("/");
    sb.append(path);
    if (query != null && !query.isEmpty()) {
      boolean first = true;
      for (Map.Entry<String, String> entry : query.entrySet()) {
        if (entry.getValue() == null || entry.getValue().isBlank()) continue;
        sb.append(first ? "?" : "&");
        first = false;
        sb.append(encode(entry.getKey())).append("=").append(encode(entry.getValue()));
      }
    }
    return URI.create(sb.toString());
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }

  private String normalizeBaseUrl(String url) {
    String trimmed = url == null ? "" : url.trim();
    if (trimmed.isBlank()) {
      throw new RuntimeException("baseUrl required for proxy target");
    }
    if (trimmed.endsWith("/")) {
      return trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }
}
