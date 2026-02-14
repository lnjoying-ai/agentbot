package com.agentbot.core.tools.impl;

import com.agentbot.core.bus.MessageBus;
import com.agentbot.core.bus.events.OutboundMessage;
import com.agentbot.core.tools.ToolDefinition;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolWithDefinition;

import java.util.List;
import java.util.Map;

public class MessageTool implements ToolWithDefinition {
    private final MessageBus messageBus;
    private String defaultChannel;
    private String defaultChatId;

    public MessageTool(MessageBus messageBus) {
        this.messageBus = messageBus;
    }

    public void setContext(String channel, String chatId) {
        this.defaultChannel = channel;
        this.defaultChatId = chatId;
    }

    @Override
    public String name() {
        return "message";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
            "message",
            "Send a message to a user on a specific channel.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "content", Map.of("type", "string", "description", "The message content"),
                    "channel", Map.of("type", "string", "description", "Optional target channel"),
                    "chatId", Map.of("type", "string", "description", "Optional target chat ID")
                ),
                "required", List.of("content")
            )
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args) {
        String content = (String) args.get("content");
        String channel = (String) args.getOrDefault("channel", defaultChannel);
        String chatId = (String) args.getOrDefault("chatId", defaultChatId);

        if (channel == null || chatId == null) {
            return new ToolExecutionResult(false, "Channel and ChatId must be specified or available in context");
        }

        try {
            messageBus.publishOutbound(new OutboundMessage(channel, chatId, content));
            return new ToolExecutionResult(true, "Message sent to " + channel + ":" + chatId);
        } catch (Exception e) {
            return new ToolExecutionResult(false, "Failed to send message: " + e.getMessage());
        }
    }
}
