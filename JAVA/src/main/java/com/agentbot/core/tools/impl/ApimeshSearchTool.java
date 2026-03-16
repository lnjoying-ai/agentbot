package com.agentbot.core.tools.impl;

import com.agentbot.core.tools.ToolDefinition;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolWithDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApimeshSearchTool implements ToolWithDefinition {
    private static final String API_URL = "https://api.apimesh.io/v1/web-search";
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ApimeshSearchTool(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String name() {
        return "web_search";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
            "web_search",
            "Search the web using Apimesh Search API. Returns titles, URLs, and snippets.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "query", Map.of("type", "string", "description", "The search query"),
                    "freshness", Map.of("type", "string", "description", "Time range filter (noLimit, oneDay, oneWeek, oneMonth, oneYear)", "default", "noLimit")
                ),
                "required", List.of("query")
            )
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args) {
        if (apiKey == null || apiKey.isBlank()) {
            return new ToolExecutionResult(false, "Apimesh API key is not configured.");
        }

        String query = (String) args.get("query");
        String freshness = (String) args.getOrDefault("freshness", "noLimit");

        try {
            Map<String, String> bodyMap = Map.of(
                "query", query,
                "freshness", freshness
            );
            String requestBody = objectMapper.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return new ToolExecutionResult(false, "Search failed with status code: " + response.statusCode() + ". Body: " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode webPages = root.path("data").path("webPages");
            if (webPages.isMissingNode()) {
                webPages = root.path("webPages");
            }

            JsonNode resultsList = webPages.path("value");
            if (resultsList.isMissingNode() || !resultsList.isArray()) {
                resultsList = webPages;
            }

            if (resultsList.isMissingNode() || resultsList.size() == 0) {
                return new ToolExecutionResult(true, "No results found for: " + query);
            }

            List<String> lines = new ArrayList<>();
            lines.add("Results for: " + query + "\n");

            for (int i = 0; i < resultsList.size() && i < 10; i++) {
                JsonNode item = resultsList.get(i);
                String title = item.path("name").asText();
                String itemUrl = item.path("url").asText();
                String snippet = item.path("snippet").asText();

                lines.add((i + 1) + ". " + title);
                lines.add("   " + itemUrl);
                if (!snippet.isBlank()) {
                    lines.add("   " + snippet);
                }
                lines.add("");
            }

            return new ToolExecutionResult(true, String.join("\n", lines));
        } catch (Exception e) {
            return new ToolExecutionResult(false, "Search failed: " + e.getMessage());
        }
    }
}
