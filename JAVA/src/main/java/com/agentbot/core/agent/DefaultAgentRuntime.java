package com.agentbot.core.agent;

import com.agentbot.core.bus.events.InboundMessage;
import com.agentbot.core.bus.events.OutboundMessage;
import com.agentbot.core.events.SystemEvent;
import com.agentbot.core.events.SystemEventBus;
import com.agentbot.core.memory.MemoryService;
import com.agentbot.core.model.LLMProvider;
import com.agentbot.core.model.LLMResponse;
import com.agentbot.core.model.ToolCallParser;
import com.agentbot.core.session.SessionService;
import com.agentbot.core.skills.Skill;
import com.agentbot.core.tools.ToolApprovalPolicy;
import com.agentbot.core.tools.ToolDefinition;
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
import java.util.stream.Collectors;

import java.util.concurrent.atomic.AtomicLong;

public class DefaultAgentRuntime implements AgentRuntime {
  private static final Logger log = LoggerFactory.getLogger(DefaultAgentRuntime.class);
  private static final AtomicLong totalToolCalls = new AtomicLong(0);
  private final LLMProvider provider;

  private final ToolRegistry tools;
  private final ToolCallParser toolCallParser;
  private final SessionService sessionService;
  private final MemoryService memoryService;
  private final PendingActionStore pendingActionStore;
  private final List<Skill> skills;

  private final int maxToolRounds;
  private final boolean parallelTools;
  private final ExecutorService toolExecutor;
  private final AgentGuidelinesService guidelinesService;
  private final SystemEventBus eventBus;

  public DefaultAgentRuntime(
      LLMProvider provider,
      ToolRegistry tools,
      ToolCallParser toolCallParser,
      SessionService sessionService,
      MemoryService memoryService,
      PendingActionStore pendingActionStore,
      List<Skill> skills,
      AgentGuidelinesService guidelinesService,
      SystemEventBus eventBus,

      int maxToolRounds,
      boolean parallelTools,
      int toolParallelism
  ) {

    this.provider = provider;
    this.tools = tools;
    this.toolCallParser = toolCallParser;
    this.sessionService = sessionService;
    this.memoryService = memoryService;
    this.pendingActionStore = pendingActionStore;
    this.skills = skills;
    this.guidelinesService = guidelinesService;
    this.eventBus = eventBus;

    this.maxToolRounds = Math.max(1, maxToolRounds);

    this.parallelTools = parallelTools;
    int parallelism = Math.max(1, toolParallelism);
    this.toolExecutor = Executors.newFixedThreadPool(parallelism);
  }


  @Override
  public OutboundMessage handle(InboundMessage message) {
    String sessionKey = message.sessionKey();
    String content = message.getContent();
    log.info("Agent handle: session={}, contentLen={}", sessionKey, content != null ? content.length() : 0);

    String confirmedToolCallId = (String) message.getMetadata().get("confirmedToolCallId");

    // Heuristic: If no ID in metadata but message content looks like a confirmation (typing "yes", "confirm", etc.)
    if (confirmedToolCallId == null && (isConfirmationMessage(content) || isDenialMessage(content))) {
        confirmedToolCallId = pendingActionStore.getMostRecentId(sessionKey);
        if (confirmedToolCallId != null) {
            log.info("Heuristic matched confirmation for session: {}. Found pending action: {}", sessionKey, confirmedToolCallId);
        }
    }

    if (confirmedToolCallId != null) {
        // Default to confirmed=true if metadata is missing or if it's a confirmation message
        boolean confirmed = true;
        Object metaConfirmed = message.getMetadata().get("confirmed");
        if (metaConfirmed instanceof Boolean) {
            confirmed = (Boolean) metaConfirmed;
        }
        
        // content-based override (higher priority for text messages)
        if (isDenialMessage(content)) {
            confirmed = false;
        } else if (isConfirmationMessage(content)) {
            confirmed = true;
        }
        
        log.info("Processing confirmation: id={}, confirmed={}", confirmedToolCallId, confirmed);
        OutboundMessage resumeResponse = handleToolConfirmation(message, sessionKey, confirmedToolCallId, confirmed);
        if (resumeResponse != null) {
            return resumeResponse;
        }
        // If resumeResponse is null, it means no pending action was found; fall back to normal flow
    }

    // 2. Normal message flow
    sessionService.appendUserMessage(sessionKey, content);
    List<Map<String, Object>> messages = constructInitialMessages(sessionKey);
    
    log.debug("Calling primary LLM for session: {}", sessionKey);
    long llmStartedAt = System.currentTimeMillis();
    LLMResponse response = provider.chat(messages, tools.definitionsForLlm());
    long llmDurationMs = System.currentTimeMillis() - llmStartedAt;
    int toolCalls = response.getToolCalls() == null ? 0 : response.getToolCalls().size();
    log.debug("LLM response received: session={}, durationMs={}, toolCalls={}", sessionKey, llmDurationMs, toolCalls);
    return processLlmResponse(message, sessionKey, messages, response, 0, false);

  }

  private boolean isConfirmationMessage(String content) {
    if (content == null || content.isBlank()) return false;
    String c = content.trim().toLowerCase();
    return c.matches("^(yes|ok|y|confirm|agree|proceed|继续|确认|同意|是|可以|执行|好了|没问题|干吧|开始).*");
  }

  private boolean isDenialMessage(String content) {
    if (content == null || content.isBlank()) return false;
    String c = content.trim().toLowerCase();
    return c.matches("^(no|n|stop|cancel|deny|reject|停止|取消|拒绝|不|不行|算了|别执行|终止).*");
  }
  

  private OutboundMessage handleToolConfirmation(InboundMessage message, String sessionKey, String confirmedToolCallId, boolean confirmed) {
    PendingActionStore.PendingAction pending = pendingActionStore.get(confirmedToolCallId);
    if (pending == null) {
        log.warn("Pending action not found for ID: {}", confirmedToolCallId);
        return null;
    }

    if (!confirmed) {
        log.info("Tool execution denied by user: id={}, tool={}", confirmedToolCallId, pending.toolName);
        pendingActionStore.remove(confirmedToolCallId);
        String deniedMessage = buildToolDeniedMessage(pending.toolName, pending.args, "用户拒绝执行");
        sessionService.appendAssistantMessage(sessionKey, deniedMessage);
        publishToolApprovalResolved(message, confirmedToolCallId, pending.toolName, pending.args, "deny");
        return new OutboundMessage(message.getChannel(), message.getChatId(), deniedMessage);
    }

    log.info("Tool confirmed by user: id={}, tool={}", confirmedToolCallId, pending.toolName);
    pending.approvedIds.add(confirmedToolCallId);
    publishToolApprovalResolved(message, confirmedToolCallId, pending.toolName, pending.args, "allow");

    // Check if there are more tools needing approval in this batch
    for (Map<String, Object> tc : pending.allToolCalls) {
        String id = String.valueOf(tc.get("id"));
        if (pending.approvedIds.contains(id)) continue;

        String name = String.valueOf(tc.get("name"));
        Map<String, Object> args = parseArguments(tc.get("arguments"));
        ToolApprovalPolicy.Decision decision = tools.approvalDecision(name, args, message.getChannel());
        if (decision == ToolApprovalPolicy.Decision.DENY) {
            log.info("Tool execution denied by policy: id={}, tool={}", id, name);
            pendingActionStore.remove(confirmedToolCallId);
            String deniedMessage = buildToolDeniedMessage(name, args, "审批策略拒绝执行");
            sessionService.appendAssistantMessage(sessionKey, deniedMessage);
            publishToolApprovalResolved(message, id, name, args, "deny");
            return new OutboundMessage(message.getChannel(), message.getChatId(), deniedMessage);
        }
        if (decision == ToolApprovalPolicy.Decision.ASK) {
            log.info("Requesting next approval in batch: id={}, tool={}", id, name);
            pendingActionStore.put(id, sessionKey, new PendingActionStore.PendingAction(
                name, args, pending.messages, pending.allToolCalls, pending.completedResults, pending.round, pending.approvedIds));
            pendingActionStore.remove(confirmedToolCallId);

            String approvalText = buildApprovalMessage(name, args);
            OutboundMessage nextPendingMsg = new OutboundMessage(message.getChannel(), message.getChatId(), approvalText);
            
            // Standardize metadata for frontend card rendering
            Map<String, Object> metadata = nextPendingMsg.getMetadata();
            metadata.put("status", "PENDING_APPROVAL");
            metadata.put("toolName", name);
            metadata.put("toolArgs", tc.get("arguments")); // Use original String JSON for frontend compatibility
            metadata.put("confirmedToolCallId", id);
            metadata.put("toolCallId", id);
            metadata.put("id", id);
            
            enrichApprovalMetadata(metadata, name, args);

            // Add tool_calls structure to metadata for consistent card rendering in batch approvals
            List<Map<String, Object>> toolCallsForLlm = new ArrayList<>();
            for (Map<String, Object> toolCall : pending.allToolCalls) {
                toolCallsForLlm.add(Map.of(
                    "id", toolCall.get("id"),
                    "type", "function",
                    "function", Map.of("name", toolCall.get("name"), "arguments", toolCall.get("arguments"))
                ));
            }
            metadata.put("tool_calls", toolCallsForLlm);
            publishToolApprovalRequested(message, id, name, args);
            return nextPendingMsg;
        }
        pending.approvedIds.add(id);
    }

    // All tools in this batch are approved (or safe). Now execute them all.
    log.info("All tools in batch approved. Executing batch...");
    pendingActionStore.remove(confirmedToolCallId);
    
    List<Map<String, Object>> messages = new ArrayList<>(pending.messages);
    sessionService.appendAssistantMessage(sessionKey, buildToolStatusMessage(pending.allToolCalls, false));
    ToolBatchResult batchResult = executeToolBatch(pending.allToolCalls, message.getChannel());
    Map<String, String> results = batchResult.results();

    for (Map<String, Object> tc : pending.allToolCalls) {
        String id = String.valueOf(tc.get("id"));
        Map<String, Object> toolMsg = new HashMap<>();
        toolMsg.put("role", "tool");
        toolMsg.put("name", tc.get("name"));
        toolMsg.put("content", results.get(id));
        toolMsg.put("tool_call_id", id);
        messages.add(toolMsg);
    }
    sessionService.appendAssistantMessage(sessionKey, buildToolResultMessage(pending.allToolCalls, results, batchResult.durationMs()));

    LLMResponse response = provider.chat(messages, tools.definitionsForLlm());

    // Use skipFirstPreCheck = true to break potential approval loops
    return processLlmResponse(message, sessionKey, messages, response, pending.round + 1, true);
  }



  private List<Map<String, Object>> constructInitialMessages(String sessionKey) {
    List<Map<String, Object>> messages = new ArrayList<>();
    StringBuilder systemPrompt = new StringBuilder();
    
    // 1. Load agent guidelines from AGENTS.md and TOOLS.md
    if (guidelinesService != null && guidelinesService.hasGuidelines()) {
        String guidelines = guidelinesService.buildSystemPrompt();
        systemPrompt.append(guidelines).append("\n\n");
        log.debug("Loaded agent guidelines: {} chars", guidelines.length());
    }
    
    // 2. Load context from memory service
    List<String> memory = memoryService.loadContext();
    if (!memory.isEmpty()) {
      systemPrompt.append("## Context from Memory\n")
                  .append(String.join("\n", memory))
                  .append("\n\n");
    }
    
    // 3. Load active skills
    if (!skills.isEmpty()) {
      systemPrompt.append("## Available Skills and Instructions\n")
                  .append(skills.stream().map(Skill::toString).collect(Collectors.joining("\n")))
                  .append("\n");
    }
    
    if (systemPrompt.length() > 0) {
      messages.add(Map.of("role", "system", "content", systemPrompt.toString()));
    }
    
    sessionService.getRecent(sessionKey, 20).forEach(entry -> {
        String role = entry.getRole();
        String content = entry.getContent();
        if ("assistant".equals(role) && (content == null || content.isBlank())) return;
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content == null ? "" : content);
        messages.add(msg);
    });
    
    while (messages.size() > (memory.isEmpty() ? 0 : 1) && !"user".equals(messages.get(memory.isEmpty() ? 0 : 1).get("role"))) {
        messages.remove(memory.isEmpty() ? 0 : 1);
    }
    return messages;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseArguments(Object arguments) {
    if (arguments instanceof Map) {
      return (Map<String, Object>) arguments;
    }
    return toolCallParser.parseArguments(String.valueOf(arguments));
  }


  private OutboundMessage processLlmResponse(InboundMessage message, String sessionKey, List<Map<String, Object>> messages, LLMResponse response, int startRound, boolean skipFirstPreCheck) {

    for (int round = startRound; round < maxToolRounds; round++) {
      if (response.getToolCalls() == null || response.getToolCalls().isEmpty()) {
        break;
      }
      log.info("Round {}: agent requested {} tool calls", round + 1, response.getToolCalls().size());
      
      // Store assistant message
      Map<String, Object> assistantMessage = new HashMap<>();
      assistantMessage.put("role", "assistant");
      assistantMessage.put("content", response.getContent() == null ? "" : response.getContent());
      assistantMessage.put("reasoning_content", response.getReasoningContent() == null ? "" : response.getReasoningContent());
      
      List<Map<String, Object>> toolCallsForLlm = new ArrayList<>();
      for (Map<String, Object> tc : response.getToolCalls()) {
        toolCallsForLlm.add(Map.of(
            "id", tc.get("id"),
            "type", "function",
            "function", Map.of("name", tc.get("name"), "arguments", tc.get("arguments"))
        ));
      }
      assistantMessage.put("tool_calls", toolCallsForLlm);
      messages.add(assistantMessage);

      List<Map<String, Object>> toolCalls = response.getToolCalls();
      // PERSIST assistant message with tool calls
      String assistantContent = response.getContent();
      boolean allowApprovalCheck = !(skipFirstPreCheck && round == startRound);
      boolean needsApproval = allowApprovalCheck && hasApprovalToolCalls(toolCalls, message.getChannel());
      String displayContent = resolveDisplayContent(response, toolCalls, needsApproval, "（模型未返回可展示内容）");
      if (toolCalls != null && !toolCalls.isEmpty()) {
          log.info("Tool calls received: count={}, names={}, contentLen={}, reasoningLen={} ",
              toolCalls.size(), summarizeToolCalls(toolCalls),
              assistantContent == null ? 0 : assistantContent.length(),
              response.getReasoningContent() == null ? 0 : response.getReasoningContent().length());
      }
      sessionService.appendAssistantMessage(sessionKey, displayContent);


      // 1. PRE-CHECK for any tools requiring approval
      Map<String, Object> firstPendingToolCall = null;
      String deniedMessage = null;
      String deniedToolId = null;
      String deniedToolName = null;
      Map<String, Object> deniedArgs = null;
      // skipFirstPreCheck only applies to the very first iteration of this loop
      if (!(skipFirstPreCheck && round == startRound)) {
          for (Map<String, Object> tc : toolCalls) {
              String name = String.valueOf(tc.get("name"));
              Map<String, Object> args = parseArguments(tc.get("arguments"));
              ToolApprovalPolicy.Decision decision = tools.approvalDecision(name, args, message.getChannel());
              if (decision == ToolApprovalPolicy.Decision.DENY) {
                  deniedToolId = String.valueOf(tc.get("id"));
                  deniedToolName = name;
                  deniedArgs = args;
                  deniedMessage = buildToolDeniedMessage(name, args, "审批策略拒绝执行");
                  break;
              }
              if (decision == ToolApprovalPolicy.Decision.ASK) {
                  firstPendingToolCall = tc;
                  break;
              }
          }
      } else {
          log.info("Skipping first pre-check for round {} as requested (prevents approval loop)", round + 1);
      }

      if (deniedMessage != null) {
          sessionService.appendAssistantMessage(sessionKey, deniedMessage);
          publishToolApprovalResolved(message, deniedToolId, deniedToolName, deniedArgs, "deny");
          return new OutboundMessage(message.getChannel(), message.getChatId(), deniedMessage);
      }

      if (firstPendingToolCall != null) {
          String id = String.valueOf(firstPendingToolCall.get("id"));
          String name = String.valueOf(firstPendingToolCall.get("name"));
          Map<String, Object> args = parseArguments(firstPendingToolCall.get("arguments"));
          
          log.info("Batch execution suspended for approval: id={}, tool={}", id, name);
          pendingActionStore.put(id, sessionKey, new PendingActionStore.PendingAction(
              name, args, new ArrayList<>(messages), toolCalls, new HashMap<>(), round, new java.util.HashSet<>()));
          
          String approvalText = buildApprovalMessage(name, args);
          if (response.getContent() != null && !response.getContent().isBlank()) {
              approvalText = response.getContent() + "\n\n" + approvalText;
          }

          OutboundMessage pendingMsg = new OutboundMessage(message.getChannel(), message.getChatId(), approvalText);
          
          // Standardize metadata for frontend card rendering
          Map<String, Object> metadata = pendingMsg.getMetadata();
          metadata.put("status", "PENDING_APPROVAL");
          metadata.put("toolName", name);
          metadata.put("toolArgs", firstPendingToolCall.get("arguments")); // Use original String JSON
          metadata.put("confirmedToolCallId", id);
          metadata.put("toolCallId", id);
          metadata.put("id", id);
          
          enrichApprovalMetadata(metadata, name, args);

          // Add tool_calls structure to metadata as some frontends require it for card rendering
          metadata.put("tool_calls", toolCallsForLlm);
          publishToolApprovalRequested(message, id, name, args);
          return pendingMsg;
      }



      // 2. All tools are safe, execute them as a batch
      ToolBatchResult batchResult = executeToolBatch(toolCalls, message.getChannel());

      Map<String, String> results = batchResult.results();

      for (Map<String, Object> tc : toolCalls) {
          String id = String.valueOf(tc.get("id"));
          Map<String, Object> toolMsg = new HashMap<>();
          toolMsg.put("role", "tool");
          toolMsg.put("name", tc.get("name"));
          toolMsg.put("content", results.get(id));
          toolMsg.put("tool_call_id", id);
          messages.add(toolMsg);
      }
      sessionService.appendAssistantMessage(sessionKey, buildToolResultMessage(toolCalls, results, batchResult.durationMs()));

      response = provider.chat(messages, tools.definitionsForLlm());

    }

    String content = response.getContent();
    sessionService.appendAssistantMessage(sessionKey, content);
    return new OutboundMessage(message.getChannel(), message.getChatId(), content);
  }

  private String buildApprovalMessage(String toolName, Map<String, Object> args) {
    ToolDefinition definition = tools == null ? null : tools.getDefinition(toolName);
    String description = definition == null ? "" : safeText(definition.getDescription());
    RiskInfo risk = assessRisk(toolName, args);
    String preview = buildPreview(toolName, args);
    String impact = buildImpact(toolName);

    StringBuilder sb = new StringBuilder();
    sb.append("工具执行需要你的批准：\n");
    sb.append("- 工具：").append(toolName == null ? "" : toolName).append("\n");
    if (!description.isBlank()) {
      sb.append("- 目的：").append(description).append("\n");
    }
    sb.append("- 风险等级：").append(risk.level).append("（").append(risk.reason).append("）\n");
    if (!impact.isBlank()) {
      sb.append("- 影响范围：").append(impact).append("\n");
    }
    if (!preview.isBlank()) {
      sb.append("- 关键参数：").append(preview).append("\n");
    }
    sb.append("- 建议：如不确定，可回复“取消”，或让我先解释/调整参数。");
    return sb.toString();
  }

  private void enrichApprovalMetadata(Map<String, Object> metadata, String toolName, Map<String, Object> args) {
    if (metadata == null) return;
    ToolDefinition definition = tools == null ? null : tools.getDefinition(toolName);
    RiskInfo risk = assessRisk(toolName, args);
    metadata.put("toolDescription", definition == null ? "" : definition.getDescription());
    metadata.put("toolRisk", risk.level);
    metadata.put("toolRiskReason", risk.reason);
    metadata.put("toolImpact", buildImpact(toolName));
    metadata.put("toolPreview", buildPreview(toolName, args));
  }

  private RiskInfo assessRisk(String toolName, Map<String, Object> args) {
    String name = toolName == null ? "" : toolName.toLowerCase();
    if (name.contains("shell")) {
      return new RiskInfo("高", "将直接执行系统命令");
    }
    if (name.contains("write_file")) {
      return new RiskInfo("高", "会写入或覆盖文件");
    }
    if (name.contains("spawn")) {
      return new RiskInfo("中", "会启动后台子任务");
    }
    if (name.contains("browser_control")) {
      return new RiskInfo("中", "会访问网页并可能交互/上传");
    }
    if (name.contains("web_search")) {
      return new RiskInfo("低", "仅发起外部搜索请求");
    }
    if (name.contains("read_file")) {
      return new RiskInfo("低", "仅读取文件内容");
    }
    if (name.contains("memory")) {
      return new RiskInfo("低", "仅访问本地记忆数据");
    }
    if (name.contains("message") || name.contains("echo") || name.contains("time")) {
      return new RiskInfo("低", "无敏感系统操作");
    }
    return new RiskInfo("中", "可能影响系统或外部资源");
  }

  private String buildImpact(String toolName) {
    String name = toolName == null ? "" : toolName.toLowerCase();
    if (name.contains("shell")) return "本机命令执行（文件/进程/网络）";
    if (name.contains("write_file")) return "本机文件系统（写入/覆盖）";
    if (name.contains("read_file")) return "本机文件系统（读取）";
    if (name.contains("spawn")) return "后台子任务与资源占用";
    if (name.contains("browser_control")) return "外部网站访问与可能下载/上传";
    if (name.contains("web_search")) return "外部搜索服务请求";
    if (name.contains("memory")) return "本地记忆/上下文数据";
    if (name.contains("message")) return "消息通道";
    if (name.contains("time") || name.contains("echo")) return "无外部影响";
    return "影响范围未知";
  }

  private String buildPreview(String toolName, Map<String, Object> args) {
    if (args == null) return "";
    String name = toolName == null ? "" : toolName.toLowerCase();
    if (name.contains("shell")) {
      return "command=" + safeArg(args.get("command"), 200);
    }
    if (name.contains("write_file")) {
      String path = safeArg(args.get("path"), 200);
      Object content = args.get("content");
      int len = content == null ? 0 : String.valueOf(content).length();
      return "path=" + path + ", contentLength=" + len;
    }
    if (name.contains("read_file")) {
      return "path=" + safeArg(args.get("path"), 200);
    }
    if (name.contains("browser_control")) {
      String action = safeArg(args.get("action"), 50);
      String url = safeArg(args.get("url"), 200);
      String selector = safeArg(args.get("selector"), 200);
      String filePath = safeArg(args.get("filePath"), 200);
      StringBuilder sb = new StringBuilder();
      if (!action.isBlank()) sb.append("action=").append(action);
      if (!url.isBlank()) sb.append(", url=").append(url);
      if (!selector.isBlank()) sb.append(", selector=").append(selector);
      if (!filePath.isBlank()) sb.append(", filePath=").append(filePath);
      return sb.toString();
    }
    if (name.contains("web_search")) {
      String query = safeArg(args.get("query"), 200);
      String count = safeArg(args.get("count"), 20);
      String freshness = safeArg(args.get("freshness"), 20);
      StringBuilder sb = new StringBuilder();
      if (!query.isBlank()) sb.append("query=").append(query);
      if (!count.isBlank()) sb.append(", count=").append(count);
      if (!freshness.isBlank()) sb.append(", freshness=").append(freshness);
      return sb.toString();
    }
    if (name.contains("spawn")) {
      String task = safeArg(args.get("task"), 200);
      String label = safeArg(args.get("label"), 100);
      return "task=" + task + (label.isBlank() ? "" : ", label=" + label);
    }
    if (name.contains("memory_search")) {
      String query = safeArg(args.get("query"), 200);
      String limit = safeArg(args.get("limit"), 20);
      return "query=" + query + (limit.isBlank() ? "" : ", limit=" + limit);
    }
    if (name.contains("memory_get")) {
      return "读取长期记忆";
    }
    if (name.contains("message")) {
      String channel = safeArg(args.get("channel"), 50);
      String chatId = safeArg(args.get("chatId"), 80);
      Object content = args.get("content");
      int len = content == null ? 0 : String.valueOf(content).length();
      StringBuilder sb = new StringBuilder();
      if (!channel.isBlank()) sb.append("channel=").append(channel);
      if (!chatId.isBlank()) sb.append(", chatId=").append(chatId);
      sb.append(", contentLength=").append(len);
      return sb.toString();
    }
    return args.isEmpty() ? "" : "args=" + abbreviate(args.toString(), 200);
  }

  private boolean hasApprovalToolCalls(List<Map<String, Object>> toolCalls, String channel) {
    if (toolCalls == null || toolCalls.isEmpty()) return false;
    for (Map<String, Object> tc : toolCalls) {
      String name = String.valueOf(tc.get("name"));
      Map<String, Object> args = parseArguments(tc.get("arguments"));
      if (tools.approvalDecision(name, args, channel) == ToolApprovalPolicy.Decision.ASK) {
        return true;
      }
    }
    return false;
  }


  private String buildToolStatusMessage(List<Map<String, Object>> toolCalls, boolean needsApproval) {
    StringBuilder sb = new StringBuilder();
    sb.append(needsApproval ? "工具执行需要确认" : "工具执行中...").append("\n");
    sb.append("共 ").append(toolCalls == null ? 0 : toolCalls.size()).append(" 个工具\n");
    if (toolCalls == null) return sb.toString().trim();
    for (Map<String, Object> tc : toolCalls) {
      String name = String.valueOf(tc.get("name"));
      Map<String, Object> args = parseArguments(tc.get("arguments"));
      RiskInfo risk = assessRisk(name, args);
      String preview = buildPreview(name, args);
      String impact = buildImpact(name);
      sb.append("- 工具：").append(name).append("\n");
      if (!preview.isBlank()) sb.append("  - 关键参数：").append(preview).append("\n");
      sb.append("  - 风险等级：").append(risk.level).append("（").append(risk.reason).append("）\n");
      if (!impact.isBlank()) sb.append("  - 影响范围：").append(impact).append("\n");
    }
    if (needsApproval) {
      sb.append("请确认后继续执行。");
    }
    return sb.toString().trim();
  }

  private String buildToolResultMessage(List<Map<String, Object>> toolCalls, Map<String, String> results, long durationMs) {
    StringBuilder sb = new StringBuilder();
    sb.append("工具执行完成\n");
    sb.append("- 耗时：").append(durationMs).append("ms\n");
    if (toolCalls == null) return sb.toString().trim();
    for (Map<String, Object> tc : toolCalls) {
      String id = String.valueOf(tc.get("id"));
      String name = String.valueOf(tc.get("name"));
      String output = results == null ? null : results.get(id);
      int len = output == null ? 0 : output.length();
      String summary = output == null ? "" : abbreviate(output, 400);
      sb.append("- 工具：").append(name).append("\n");
      sb.append("  - 输出长度：").append(len).append("\n");
      if (!summary.isBlank()) sb.append("  - 输出摘要：").append(summary).append("\n");
    }
    return sb.toString().trim();
  }

  private String resolveDisplayContent(LLMResponse response, List<Map<String, Object>> toolCalls, boolean needsApproval, String fallback) {
    if (response == null) return fallback;
    String content = response.getContent();
    if (content != null && !content.isBlank()) return content;
    String reasoning = response.getReasoningContent();
    if (reasoning != null && !reasoning.isBlank()) return reasoning;
    if (toolCalls != null && !toolCalls.isEmpty()) {
      return buildToolStatusMessage(toolCalls, needsApproval);
    }
    return fallback;
  }

  private String summarizeToolCalls(List<Map<String, Object>> toolCalls) {
    if (toolCalls == null || toolCalls.isEmpty()) return "";
    StringBuilder sb = new StringBuilder();
    for (Map<String, Object> tc : toolCalls) {
      if (sb.length() > 0) sb.append(", ");
      sb.append(String.valueOf(tc.get("name"))).append("#").append(String.valueOf(tc.get("id")));
    }
    return sb.toString();
  }


  private String buildToolDeniedMessage(String toolName, Map<String, Object> args, String reason) {
    RiskInfo risk = assessRisk(toolName, args);
    String preview = buildPreview(toolName, args);
    String impact = buildImpact(toolName);
    StringBuilder sb = new StringBuilder();
    sb.append("工具执行已拒绝\n");
    sb.append("- 工具：").append(toolName == null ? "" : toolName).append("\n");
    if (reason != null && !reason.isBlank()) {
      sb.append("- 原因：").append(reason).append("\n");
    }
    sb.append("- 风险等级：").append(risk.level).append("（").append(risk.reason).append("）\n");
    if (!impact.isBlank()) {
      sb.append("- 影响范围：").append(impact).append("\n");
    }
    if (!preview.isBlank()) {
      sb.append("- 关键参数：").append(preview).append("\n");
    }
    return sb.toString().trim();
  }

  private void publishToolApprovalRequested(InboundMessage message, String toolCallId, String toolName, Map<String, Object> args) {
    if (eventBus == null || message == null) return;
    Map<String, Object> payload = new HashMap<>();
    payload.put("channel", message.getChannel());
    payload.put("chatId", message.getChatId());
    payload.put("toolCallId", toolCallId);
    payload.put("toolName", toolName);
    payload.put("toolArgs", args);
    payload.put("decision", "ask");
    payload.put("timestamp", java.time.OffsetDateTime.now().toString());
    eventBus.publish(new SystemEvent("tool.approval.requested", payload));
  }

  private void publishToolApprovalResolved(InboundMessage message, String toolCallId, String toolName, Map<String, Object> args, String decision) {
    if (eventBus == null || message == null) return;
    Map<String, Object> payload = new HashMap<>();
    payload.put("channel", message.getChannel());
    payload.put("chatId", message.getChatId());
    payload.put("toolCallId", toolCallId);
    payload.put("toolName", toolName);
    payload.put("toolArgs", args);
    payload.put("decision", decision == null ? "" : decision);
    payload.put("timestamp", java.time.OffsetDateTime.now().toString());
    eventBus.publish(new SystemEvent("tool.approval.resolved", payload));
  }

  private String safeArg(Object value, int maxLen) {


    if (value == null) return "";
    return abbreviate(String.valueOf(value), maxLen);
  }

  private String abbreviate(String text, int maxLen) {
    if (text == null) return "";
    String trimmed = text.trim();
    if (trimmed.length() <= maxLen) return trimmed;
    return trimmed.substring(0, Math.max(0, maxLen - 3)) + "...";
  }

  private String safeText(String text) {
    return text == null ? "" : text.trim();
  }

  private static class RiskInfo {
    private final String level;
    private final String reason;

    private RiskInfo(String level, String reason) {
      this.level = level;
      this.reason = reason;
    }
  }

  private ToolBatchResult executeToolBatch(List<Map<String, Object>> toolCalls, String channel) {
    long startedAt = System.currentTimeMillis();
    List<CompletableFuture<ToolExecutionResult>> futures = new ArrayList<>();
    List<String> futureIds = new ArrayList<>();
    Map<String, String> results = new HashMap<>();


    if (parallelTools) {

      for (Map<String, Object> tc : toolCalls) {
        String id = String.valueOf(tc.get("id"));
        String name = String.valueOf(tc.get("name"));
        Map<String, Object> args = new HashMap<>(parseArguments(tc.get("arguments")));
        ToolApprovalPolicy.Decision decision = tools.approvalDecision(name, args, channel);
        if (decision == ToolApprovalPolicy.Decision.DENY) {
          results.put(id, "Denied by policy: " + name);
          continue;
        }
        totalToolCalls.incrementAndGet();
        args.put("confirmed", true); // Force execute since it's pre-approved or safe
        futures.add(CompletableFuture.supplyAsync(() -> {
            try {
                return tools.execute(name, args);
            } catch (Exception e) {
                log.error("Parallel tool execution failed", e);
                return new ToolExecutionResult(false, "System error: " + e.getMessage());
            }
        }, toolExecutor));
        futureIds.add(id);
      }

      for (int i = 0; i < futureIds.size(); i++) {
        String id = futureIds.get(i);
        try {
          results.put(id, futures.get(i).join().getOutput());
        } catch (Exception e) {
          results.put(id, "Execution error: " + e.getMessage());
        }
      }
    } else {
      for (Map<String, Object> tc : toolCalls) {
        String id = String.valueOf(tc.get("id"));
        String name = String.valueOf(tc.get("name"));
        Map<String, Object> args = new HashMap<>(parseArguments(tc.get("arguments")));
        ToolApprovalPolicy.Decision decision = tools.approvalDecision(name, args, channel);
        if (decision == ToolApprovalPolicy.Decision.DENY) {
          results.put(id, "Denied by policy: " + name);
          continue;
        }
        totalToolCalls.incrementAndGet();
        args.put("confirmed", true);
        results.put(id, tools.execute(name, args).getOutput());
      }
    }
    long durationMs = System.currentTimeMillis() - startedAt;
    log.debug("Tool batch executed: count={}, parallel={}, durationMs={}", toolCalls == null ? 0 : toolCalls.size(), parallelTools, durationMs);
    return new ToolBatchResult(results, durationMs);
  }


  private record ToolBatchResult(Map<String, String> results, long durationMs) {}



  public static long getTotalToolCalls() {
      return totalToolCalls.get();
  }
}

