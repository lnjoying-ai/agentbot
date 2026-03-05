package com.agentbot.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAiCompatibleProvider implements LLMProvider {
  private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleProvider.class);
  private final String baseUrl;

  private final String apiKey;
  private final String model;
  private final double temperature;
  private final Map<String, String> extraHeaders;
  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

  public OpenAiCompatibleProvider(
      String baseUrl,
      String apiKey,
      String model,
      double temperature,
      Map<String, String> extraHeaders
  ) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.model = model;
    this.temperature = temperature;
    this.extraHeaders = extraHeaders == null ? Map.of() : extraHeaders;
  }

  @Override
  public LLMResponse chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
    log.info("LLM request: model={}, messages={}, tools={}", model, messages.size(), tools != null ? tools.size() : 0);
    try {
      Map<String, Object> payload = new HashMap<>();
      payload.put("model", model);
      payload.put("messages", messages);
      payload.put("temperature", temperature);
      if (tools != null && !tools.isEmpty()) {
        payload.put("tools", tools);
        payload.put("tool_choice", "auto");
      }

    String body = mapper.writeValueAsString(payload);
    log.debug("LLM request payload: url={}", baseUrl);

    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + apiKey)
        .POST(HttpRequest.BodyPublishers.ofString(body));


      for (Map.Entry<String, String> header : extraHeaders.entrySet()) {
        builder.header(header.getKey(), header.getValue());
      }

      long start = System.currentTimeMillis();
    HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    long duration = System.currentTimeMillis() - start;

    String formattedBody = formatJsonBody(response.body());
    log.debug("LLM response: status={}, duration={}ms, body={}", response.statusCode(), duration, formattedBody);


    if (response.statusCode() != 200) {
      log.error("LLM error: status={}, body={}", response.statusCode(), response.body());
      return new LLMResponse(buildErrorContent(response.statusCode(), response.body()), List.of());
    }


    log.info("LLM success: duration={}ms", duration);
    return parseResponse(response.body());

    } catch (Exception e) {
      log.error("LLM exception", e);
      return new LLMResponse(buildExceptionContent(e), List.of());
    }
  }


  private String formatJsonBody(String raw) {

    if (raw == null || raw.isBlank()) {
      return raw;
    }
    try {
      JsonNode json = mapper.readTree(raw);
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    } catch (Exception e) {
      return raw;
    }
  }

  private String buildErrorContent(int status, String rawBody) {
    ErrorInfo error = parseErrorInfo(rawBody);
    StringBuilder sb = new StringBuilder();
    sb.append("[LLM_ERROR]");
    sb.append(" status=").append(status);
    if (!model.isBlank()) sb.append(" model=").append(model);
    if (!baseUrl.isBlank()) sb.append(" provider=").append(baseUrl);
    if (error != null) {
      if (!error.code().isBlank()) sb.append(" code=").append(error.code());
      if (!error.type().isBlank()) sb.append(" type=").append(error.type());
      if (!error.message().isBlank()) sb.append(" message=").append(error.message());
    }
    String hint = buildHint(status, error == null ? "" : error.message());
    if (!hint.isBlank()) sb.append(" | 提示：").append(hint);
    return sb.toString();
  }

  private String buildExceptionContent(Exception e) {
    String message = e == null ? "" : (e.getMessage() == null ? "" : e.getMessage());
    StringBuilder sb = new StringBuilder();
    sb.append("[LLM_ERROR]");
    if (!model.isBlank()) sb.append(" model=").append(model);
    if (!baseUrl.isBlank()) sb.append(" provider=").append(baseUrl);
    if (!message.isBlank()) sb.append(" message=").append(message);
    String hint = buildHint(0, message);
    if (!hint.isBlank()) sb.append(" | 提示：").append(hint);
    return sb.toString();
  }

  private String buildHint(int status, String message) {
    String msg = message == null ? "" : message.toLowerCase();
    if (status == 401 || msg.contains("unauthorized") || msg.contains("token") || msg.contains("api key")) {
      return "接口令牌无效或已过期，请检查 API Key 与配置来源";
    }
    if (msg.contains("exceeds limit") || msg.contains("total message size") || msg.contains("context length")) {
      return "请求内容超出模型限制，建议缩短历史消息或开启裁剪/摘要";
    }
    if (status == 429 || msg.contains("rate") || msg.contains("too many")) {
      return "请求过于频繁，请稍后重试或降低并发";
    }
    if (status >= 500) {
      return "模型服务异常，请稍后重试或切换备用模型";
    }
    return "";
  }

  private ErrorInfo parseErrorInfo(String rawBody) {
    if (rawBody == null || rawBody.isBlank()) return null;
    try {
      JsonNode root = mapper.readTree(rawBody);
      JsonNode error = root.path("error");
      if (error.isMissingNode() || error.isNull()) return null;
      String code = error.path("code").asText("");
      String type = error.path("type").asText("");
      String message = error.path("message").asText("");
      return new ErrorInfo(code, type, message);
    } catch (Exception ignored) {
      return null;
    }
  }

  private record ErrorInfo(String code, String type, String message) {}

  private LLMResponse parseResponse(String raw) throws Exception {


    JsonNode root = mapper.readTree(raw);
    JsonNode choice = root.path("choices").path(0).path("message");
    String content = choice.path("content").asText("");
    String reasoningContent = choice.path("reasoning_content").asText(null);
    List<Map<String, Object>> toolCalls = new ArrayList<>();
    JsonNode tools = choice.path("tool_calls");
    if (tools.isArray()) {
      for (JsonNode tool : tools) {
        String id = tool.path("id").asText("");
        JsonNode fn = tool.path("function");
        String name = fn.path("name").asText("");
        String arguments = fn.path("arguments").asText("");
        toolCalls.add(Map.of("id", id, "name", name, "arguments", arguments));
      }
    }
    return new LLMResponse(content, reasoningContent, toolCalls);
  }

}
