package com.agentbot.core.tools.impl;

import com.agentbot.core.tools.ToolDefinition;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolWithDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BraveSearchTool implements ToolWithDefinition {
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BraveSearchTool(String apiKey) {

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
            "Search the web using Brave Search API. Returns titles, URLs, and snippets.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "query", Map.of("type", "string", "description", "The search query"),
                    "count", Map.of("type", "integer", "description", "Number of results (1-10)", "minimum", 1, "maximum", 10)
                ),
                "required", List.of("query")
            )
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args) {
        if (apiKey == null || apiKey.isBlank()) {
            return new ToolExecutionResult(false, "Brave API key is not configured.");
        }

        String query = (String) args.get("query");
        int count = args.containsKey("count") ? (int) args.get("count") : 5;
        count = Math.min(Math.max(count, 1), 10);

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://api.search.brave.com/res/v1/web/search?q=" + encodedQuery + "&count=" + count;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("X-Subscription-Token", apiKey)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return new ToolExecutionResult(false, "Search failed with status code: " + response.statusCode() + ". Body: " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.path("web").path("results");

            if (results.isMissingNode() || results.size() == 0) {
                return new ToolExecutionResult(true, "No results found for: " + query);
            }

            List<String> lines = new ArrayList<>();
            lines.add("Results for: " + query + "\n");

            for (int i = 0; i < results.size(); i++) {
                JsonNode item = results.get(i);
                String title = item.path("title").asText();
                String itemUrl = item.path("url").asText();
                String description = item.path("description").asText();

                lines.add((i + 1) + ". " + title);
                lines.add("   " + itemUrl);
                if (!description.isBlank()) {
                    lines.add("   " + description);
                }
                lines.add("");
            }

            return new ToolExecutionResult(true, String.join("\n", lines));
        } catch (Exception e) {
            return new ToolExecutionResult(false, "Search failed: " + e.getMessage());
        }
    }
}

