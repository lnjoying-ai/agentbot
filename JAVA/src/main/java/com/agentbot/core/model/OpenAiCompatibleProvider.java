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
      HttpRequest.Builder builder = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/chat/completions"))
          .header("Content-Type", "application/json")
          .header("Authorization", "Bearer " + apiKey)
          .POST(HttpRequest.BodyPublishers.ofString(body));

      for (Map.Entry<String, String> header : extraHeaders.entrySet()) {
        builder.header(header.getKey(), header.getValue());
      }

      long start = System.currentTimeMillis();
      HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      long duration = System.currentTimeMillis() - start;
      
      if (response.statusCode() != 200) {
        log.error("LLM error: status={}, body={}", response.statusCode(), response.body());
        return new LLMResponse("[LLM_ERROR] status " + response.statusCode(), List.of());
      }
      
      log.info("LLM success: duration={}ms", duration);
      return parseResponse(response.body());
    } catch (Exception e) {
      log.error("LLM exception", e);
      return new LLMResponse("[LLM_ERROR] " + e.getMessage(), List.of());
    }
  }


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
