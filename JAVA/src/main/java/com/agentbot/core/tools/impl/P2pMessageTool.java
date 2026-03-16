package com.agentbot.core.tools.impl;

import com.agentbot.core.bus.ExternalMessageBus;
import com.agentbot.core.bus.MessageEnvelope;
import com.agentbot.core.tools.ToolDefinition;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolWithDefinition;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class P2pMessageTool implements ToolWithDefinition {
    private static final String CHANNEL_P2P = "p2p";
    private static final String CHAT_ID_PREFIX = "p2p:";
    private static final String CHAT_ID_EMPTY = "-";

    private final ExternalMessageBus messageBus;

    public P2pMessageTool(ExternalMessageBus messageBus) {
        this.messageBus = messageBus;
    }

    @Override
    public String name() {
        return "p2p_message";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
            "p2p_message",
            "send a message to an outer node's agent.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "content", Map.of("type", "string", "description", "The message content"),
                    "toNodeId", Map.of("type", "string", "description", "Target node ID"),
                    "toAgentId", Map.of("type", "string", "description", "Target agent ID"),
                    "fromAgentId", Map.of("type", "string", "description", "Source agent ID")
                ),
                "required", List.of("content", "toNodeId", "toAgentId", "fromAgentId")
            )
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args) {
        String content = args == null ? null : String.valueOf(args.get("content"));
        String toNodeId = args == null ? null : String.valueOf(args.get("toNodeId"));
        String toAgentId = args == null ? null : String.valueOf(args.get("toAgentId"));
        String fromAgentId = args == null ? null : String.valueOf(args.get("fromAgentId"));

        if (content == null || content.isBlank()) {
            return new ToolExecutionResult(false, "Content must be provided");
        }
        if (toNodeId == null || toNodeId.isBlank()) {
            return new ToolExecutionResult(false, "NodeId must be provided");
        }
        if (toAgentId == null || toAgentId.isBlank()) {
            return new ToolExecutionResult(false, "AgentId must be provided");
        }

        String chatId = buildChatId(fromAgentId, toNodeId, toAgentId);
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("fromAgentId", fromAgentId.trim());
        metadata.put("toNodeId", toNodeId.trim());
        metadata.put("toAgentId", toAgentId.trim());
        metadata.put("msgId", UUID.randomUUID().toString());
        metadata.put("ackRequired", true);

        try {
            messageBus.publish(MessageEnvelope.externalOutbound(CHANNEL_P2P, chatId, content, metadata));
            return new ToolExecutionResult(true, "P2P message sent to " + toNodeId + ":" + toAgentId);
        } catch (Exception e) {
            return new ToolExecutionResult(false, "Failed to send P2P message: " + e.getMessage());
        }
    }

    private String buildChatId(String fromAgentId, String toNodeId, String toAgentId) {
        String safeToNode = toNodeId == null || toNodeId.isBlank() ? CHAT_ID_EMPTY : toNodeId.trim();
        String safeToAgent = toAgentId == null || toAgentId.isBlank() ? CHAT_ID_EMPTY : toAgentId.trim();
        String safeFromAgent = fromAgentId == null || fromAgentId.isBlank() ? CHAT_ID_EMPTY : fromAgentId.trim();
        return CHAT_ID_PREFIX + ":" + safeFromAgent + ":" + safeToNode + ":" + safeToAgent;
    }
}
