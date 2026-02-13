package com.agentbot.core.agent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SubAgentManager {
  private final Map<String, CompletableFuture<String>> tasks = new ConcurrentHashMap<>();

  public String spawn(String input) {
    String taskId = UUID.randomUUID().toString();
    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "sub-agent result: " + input);
    tasks.put(taskId, future);
    return taskId;
  }

  public CompletableFuture<String> getResult(String taskId) {
    return tasks.get(taskId);
  }
}
