package com.agentbot.core.agent;

import com.agentbot.core.bus.events.InboundMessage;
import com.agentbot.core.bus.events.OutboundMessage;
import com.agentbot.core.memory.MemoryService;
import com.agentbot.core.model.LLMProvider;
import com.agentbot.core.model.LLMResponse;
import com.agentbot.core.model.ToolCallParser;
import com.agentbot.core.session.SessionService;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultAgentRuntime implements AgentRuntime {
  private static final Logger log = LoggerFactory.getLogger(DefaultAgentRuntime.class);
  private static long totalToolCalls = 0;
  private final LLMProvider provider;


  private final ToolRegistry tools;
  private final ToolCallParser toolCallParser;
  private final SessionService sessionService;
  private final MemoryService memoryService;
  private final int maxToolRounds;
  private final boolean parallelTools;
  private final ExecutorService toolExecutor;

  public DefaultAgentRuntime(
      LLMProvider provider,
      ToolRegistry tools,
      ToolCallParser toolCallParser,
      SessionService sessionService,
      MemoryService memoryService,
      int maxToolRounds,
      boolean parallelTools,
      int toolParallelism
  ) {
    this.provider = provider;
    this.tools = tools;
    this.toolCallParser = toolCallParser;
    this.sessionService = sessionService;
    this.memoryService = memoryService;
    this.maxToolRounds = Math.max(1, maxToolRounds);
    this.parallelTools = parallelTools;
    int parallelism = Math.max(1, toolParallelism);
    this.toolExecutor = Executors.newFixedThreadPool(parallelism);
  }


  @Override
  public OutboundMessage handle(InboundMessage message) {
    String sessionKey = message.sessionKey();
    log.info("Agent handle: session={}, contentLen={}", sessionKey, message.getContent().length());
    sessionService.appendUserMessage(sessionKey, message.getContent());

    List<Map<String, Object>> messages = new ArrayList<>();
    List<String> memory = memoryService.loadContext();
    if (!memory.isEmpty()) {
      messages.add(Map.of("role", "system", "content", String.join("\n", memory)));
    }
    sessionService.getRecent(sessionKey, 20).forEach(entry -> {
        String role = entry.getRole();
        String content = entry.getContent();
        
        // Kimi/OpenAI rule: assistant message content cannot be null. 
        // If it's empty and we don't have tool_calls (which aren't stored in basic session service), 
        // it's safer to skip or provide a placeholder to avoid 400 error.
        if ("assistant".equals(role) && (content == null || content.isBlank())) {
            return;
        }
        
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content == null ? "" : content);
        messages.add(msg);
    });

    // Ensure the first message after system is a user message
    while (messages.size() > (memory.isEmpty() ? 0 : 1) && !"user".equals(messages.get(memory.isEmpty() ? 0 : 1).get("role"))) {
        messages.remove(memory.isEmpty() ? 0 : 1);
    }


    log.debug("Calling primary LLM for session: {}", sessionKey);
    LLMResponse response = provider.chat(messages, tools.definitionsForLlm());

    for (int round = 0; round < maxToolRounds; round++) {
      if (response.getToolCalls() == null || response.getToolCalls().isEmpty()) {
        break;
      }
      log.info("Round {}: agent requested {} tool calls", round + 1, response.getToolCalls().size());
      
      // Add assistant message with tool calls to history
      Map<String, Object> assistantMessage = new HashMap<>();
      assistantMessage.put("role", "assistant");
      assistantMessage.put("content", response.getContent() == null ? "" : response.getContent());
      
      // Kimi-k2.5 and other thinking models require reasoning_content to be present in history, 
      // even if it's an empty string, when tool_calls are present and thinking is enabled.
      assistantMessage.put("reasoning_content", response.getReasoningContent() == null ? "" : response.getReasoningContent());



      
      List<Map<String, Object>> toolCallsForLlm = new ArrayList<>();
      for (Map<String, Object> tc : response.getToolCalls()) {
        Map<String, Object> toolCall = new HashMap<>();
        toolCall.put("id", tc.get("id"));
        toolCall.put("type", "function");
        toolCall.put("function", Map.of(
            "name", tc.get("name"),
            "arguments", tc.get("arguments")
        ));
        toolCallsForLlm.add(toolCall);
      }
      assistantMessage.put("tool_calls", toolCallsForLlm);
      messages.add(assistantMessage);



      List<Map<String, Object>> toolCalls = response.getToolCalls();
      List<CompletableFuture<ToolExecutionResult>> futures = new ArrayList<>();

      if (parallelTools) {
        for (Map<String, Object> toolCall : toolCalls) {
          totalToolCalls++;
          String toolName = String.valueOf(toolCall.get("name"));

          String rawArgs = String.valueOf(toolCall.get("arguments"));
          Map<String, Object> args = toolCallParser.parseArguments(rawArgs);
          futures.add(CompletableFuture.supplyAsync(() -> tools.execute(toolName, args), toolExecutor));
        }
      }

      for (int i = 0; i < toolCalls.size(); i++) {
        Map<String, Object> toolCall = toolCalls.get(i);
        String toolName = String.valueOf(toolCall.get("name"));
        String toolCallId = String.valueOf(toolCall.getOrDefault("id", ""));

        ToolExecutionResult result;
        if (parallelTools) {
          result = futures.get(i).join();
        } else {
          totalToolCalls++;
          String rawArgs = String.valueOf(toolCall.get("arguments"));

          Map<String, Object> args = toolCallParser.parseArguments(rawArgs);
          result = tools.execute(toolName, args);
        }

        Map<String, Object> toolMessage = new HashMap<>();
        toolMessage.put("role", "tool");
        toolMessage.put("name", toolName);
        toolMessage.put("content", result.getOutput());
        if (!toolCallId.isBlank()) {
          toolMessage.put("tool_call_id", toolCallId);
        }
        messages.add(toolMessage);
      }

      response = provider.chat(messages, tools.definitionsForLlm());
    }

    String content = response.getContent();


    sessionService.appendAssistantMessage(sessionKey, content);
    return new OutboundMessage(message.getChannel(), message.getChatId(), content);
  }

  public static long getTotalToolCalls() {
      return totalToolCalls;
  }
}

