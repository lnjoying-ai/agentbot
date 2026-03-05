package com.agentbot.core.browser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;

import java.util.Map;

public class BrowserControlServer {
  private final BrowserService browserService;
  private final int port;
  private final ObjectMapper mapper = new ObjectMapper();
  private HttpServer server;

  public BrowserControlServer(BrowserService browserService, int port) {
    this.browserService = browserService;
    this.port = port;
  }

  public void start() {
    if (!browserService.isEnabled()) return;
    if (server != null) return;
    try {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
    } catch (IOException e) {
      throw new RuntimeException("Failed to start browser control server", e);
    }
    browserService.startExtensionRelayIfConfigured();
    server.createContext("/", new RootHandler());

    server.createContext("/start", new StartHandler());
    server.createContext("/stop", new StopHandler());
    server.createContext("/tabs", new TabsHandler());
    server.createContext("/tabs/open", new OpenTabHandler());
    server.createContext("/tabs/focus", new FocusTabHandler());
    server.createContext("/tabs/close", new CloseTabHandler());
    server.createContext("/snapshot", new SnapshotHandler());
    server.createContext("/act", new ActHandler());
    server.createContext("/navigate", new NavigateHandler());
    server.createContext("/click", new ClickHandler());
    server.createContext("/type", new TypeHandler());
    server.createContext("/upload", new UploadHandler());
    server.createContext("/screenshot", new ScreenshotHandler());
    server.createContext("/content", new ContentHandler());

    server.setExecutor(null);
    server.start();
  }

  public void stop() {
    if (server != null) {
      server.stop(0);
      server = null;
    }
    browserService.stopExtensionRelay();
  }


  private abstract class BaseHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      try {
        handleInternal(exchange);
      } catch (Exception e) {
        sendJson(exchange, 500, Map.of("ok", false, "error", e.getMessage()));
      }
    }

    protected abstract void handleInternal(HttpExchange exchange) throws Exception;

    protected Map<String, Object> readJsonBody(HttpExchange exchange) throws IOException {
      try (InputStream is = exchange.getRequestBody()) {
        if (is == null) return new HashMap<>();
        return mapper.readValue(is, new TypeReference<>() {});
      }
    }

    protected Map<String, String> parseQuery(HttpExchange exchange) {
      Map<String, String> result = new HashMap<>();
      URI uri = exchange.getRequestURI();
      if (uri == null || uri.getRawQuery() == null) return result;
      String[] parts = uri.getRawQuery().split("&");
      for (String part : parts) {
        if (part.isBlank()) continue;
        String[] kv = part.split("=", 2);
        result.put(kv[0], kv.length > 1 ? kv[1] : "");
      }
      return result;
    }

    protected void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
      byte[] data = mapper.writeValueAsBytes(body);
      Headers headers = exchange.getResponseHeaders();
      headers.set("Content-Type", "application/json; charset=utf-8");
      exchange.sendResponseHeaders(status, data.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(data);
      }
    }
  }

  private class RootHandler extends BaseHandler {
    @Override
    protected void handleInternal(HttpExchange exchange) throws Exception {
      if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJson(exchange, 405, Map.of("ok", false, "error", "Method not allowed"));
        return;
      }
      sendJson(exchange, 200, browserService.status());
    }
  }

  private class StartHandler extends BaseHandler {
    @Override
    protected void handleInternal(HttpExchange exchange) throws Exception {
      if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJson(exchange, 405, Map.of("ok", false, "error", "Method not allowed"));
        return;
      }
      Map<String, String> query = parseQuery(exchange);
      String profile = query.get("profile");
      browserService.startProfile(profile);
      sendJson(exchange, 200, browserService.status());
    }
  }

  private class StopHandler extends BaseHandler {
    @Override
    protected void handleInternal(HttpExchange exchange) throws Exception {
      if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJson(exchange, 405, Map.of("ok", false, "error", "Method not allowed"));
        return;
      }
      Map<String, String> query = parseQuery(exchange);
      String profile = query.get("profile");
      browserService.stopProfile(profile);
      sendJson(exchange, 200, browserService.status());
    }
  }

  private class TabsHandler extends BaseHandler {
    @Override
    protected void handleInternal(HttpExchange exchange) throws Exception {
      if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJson(exchange, 405, Map.of("ok", false, "error", "Method not allowed"));
        return;
      }
      Map<String, String> query = parseQuery(exchange);
      String profile = query.get("profile");
      sendJson(exchange, 200, Map.of("tabs", browserService.listTabs(profile)));
    }
  }

  private class OpenTabHandler extends BaseHandler {
    @Override
    protected void handleInternal(HttpExchange exchange) throws Exception {
      if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJson(exchange, 405, Map.of("ok", false, "error", "Method not allowed"));
        return;
      }
      Map<String, Object> body = readJsonBody(exchange);
      String url = body.get("url") == null ? "" : body.get("url").toString();
      String profile = body.get("profile") == null ? null : body.get("profile").toString();
      sendJson(exchange, 200, browserService.openTab(profile, url));
    }
  }

  private class FocusTabHandler extends BaseHandler {
    @Override
    protected void handleInternal(HttpExchange exchange) throws Exception {
      if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJson(exchange, 405, Map.of("ok", false, "error", "Method not allowed"));
        return;
      }
      Map<String, Object> body = readJsonBody(exchange);
      String targetId = body.get("targetId") == null ? "" : body.get("targetId").toString();
      String profile = body.get("profile") == null ? null : body.get("profile").toString();
      browserService.focusTab(profile, targetId);
      sendJson(exchange, 200, Map.of("ok", true));
    }
  }

  private class CloseTabHandler extends BaseHandler {
    @Override
    protected void handleInternal(HttpExchange exchange) throws Exception {
      if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJson(exchange, 405, Map.of("ok", false, "error", "Method not allowed"));
        return;
      }
      Map<String, String> query = parseQuery(exchange);
      String targetId = query.get("targetId");
      String profile = query.get("profile");
      browserService.closeTab(profile, targetId);
      sendJson(exchange, 200, Map.of("ok", true));
    }
  }

  private class SnapshotHandler extends BaseHandler {
    @Override
    protected void handleInternal(HttpExchange exchange) throws Exception {
      if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJson(exchange, 405, Map.of("ok", false, "error", "Method not allowed"));
        return;
      }
      Map<String, String> query = parseQuery(exchange);
      String profile = query.get("profile");
      String targetId = query.get("targetId");
      String format = query.getOrDefault("format", query.get("snapshotFormat"));
      sendJson(exchange, 200, browserService.snapshot(profile, targetId, format));

    }
  }

  private class ActHandler extends BaseHandler {
    @Override
    protected void handleInternal(HttpExchange exchange) throws Exception {
      if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJson(exchange, 405, Map.of("ok", false, "error", "Method not allowed"));
        return;
      }
      Map<String, Object> body = readJsonBody(exchange);
      String profile = body.get("profile") == null ? null : body.get("profile").toString();
      String targetId = body.get("targetId") == null ? null : body.get("targetId").toString();
      String ref = body.get("ref") == null ? null : body.get("ref").toString();
      String kind = body.get("kind") == null ? null : body.get("kind").toString();
      String text = body.get("text") == null ? null : body.get("text").toString();
      String key = body.get("key") == null ? null : body.get("key").toString();
      sendJson(exchange, 200, browserService.act(profile, targetId, ref, kind, text, key));
    }
  }

  private class NavigateHandler extends BaseHandler {
    @Override
    protected void handleInternal(HttpExchange exchange) throws Exception {
      if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJson(exchange, 405, Map.of("ok", false, "error", "Method not allowed"));
        return;
      }
      Map<String, Object> body = readJsonBody(exchange);
      String profile = body.get("profile") == null ? null : body.get("profile").toString();
      String targetId = body.get("targetId") == null ? null : body.get("targetId").toString();
      String url = body.get("url") == null ? null : body.get("url").toString();
      sendJson(exchange, 200, browserService.navigate(profile, targetId, url));
    }
  }

  private class ClickHandler extends BaseHandler {
    @Override
    protected void handleInternal(HttpExchange exchange) throws Exception {
      if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJson(exchange, 405, Map.of("ok", false, "error", "Method not allowed"));
        return;
      }
      Map<String, Object> body = readJsonBody(exchange);
      String profile = body.get("profile") == null ? null : body.get("profile").toString();
      String targetId = body.get("targetId") == null ? null : body.get("targetId").toString();
      String selector = body.get("selector") == null ? null : body.get("selector").toString();
      sendJson(exchange, 200, browserService.click(profile, targetId, selector));
    }
  }

  private class TypeHandler extends BaseHandler {
    @Override
    protected void handleInternal(HttpExchange exchange) throws Exception {
      if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJson(exchange, 405, Map.of("ok", false, "error", "Method not allowed"));
        return;
      }
      Map<String, Object> body = readJsonBody(exchange);
      String profile = body.get("profile") == null ? null : body.get("profile").toString();
      String targetId = body.get("targetId") == null ? null : body.get("targetId").toString();
      String selector = body.get("selector") == null ? null : body.get("selector").toString();
      String text = body.get("text") == null ? null : body.get("text").toString();
      sendJson(exchange, 200, browserService.type(profile, targetId, selector, text));
    }
  }

  private class UploadHandler extends BaseHandler {
    @Override
    protected void handleInternal(HttpExchange exchange) throws Exception {
      if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJson(exchange, 405, Map.of("ok", false, "error", "Method not allowed"));
        return;
      }
      Map<String, Object> body = readJsonBody(exchange);
      String profile = body.get("profile") == null ? null : body.get("profile").toString();
      String targetId = body.get("targetId") == null ? null : body.get("targetId").toString();
      String selector = body.get("selector") == null ? null : body.get("selector").toString();
      String filePath = body.get("filePath") == null ? null : body.get("filePath").toString();
      String ref = body.get("ref") == null ? null : body.get("ref").toString();
      sendJson(exchange, 200, browserService.upload(profile, targetId, selector, filePath, ref));
    }
  }

  private class ScreenshotHandler extends BaseHandler {
    @Override
    protected void handleInternal(HttpExchange exchange) throws Exception {
      if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJson(exchange, 405, Map.of("ok", false, "error", "Method not allowed"));
        return;
      }
      Map<String, Object> body = readJsonBody(exchange);
      String profile = body.get("profile") == null ? null : body.get("profile").toString();
      String targetId = body.get("targetId") == null ? null : body.get("targetId").toString();
      sendJson(exchange, 200, browserService.screenshot(profile, targetId));
    }
  }


  private class ContentHandler extends BaseHandler {
    @Override
    protected void handleInternal(HttpExchange exchange) throws Exception {
      if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJson(exchange, 405, Map.of("ok", false, "error", "Method not allowed"));
        return;
      }
      Map<String, String> query = parseQuery(exchange);
      String profile = query.get("profile");
      String targetId = query.get("targetId");
      sendJson(exchange, 200, browserService.content(profile, targetId));
    }
  }
}
