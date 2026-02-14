package com.agentbot.core.tools.impl;

import com.agentbot.core.agent.SubAgentManager;
import com.agentbot.core.tools.ToolDefinition;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolWithDefinition;

import java.util.List;
import java.util.Map;

public class SpawnTool implements ToolWithDefinition {
    private final SubAgentManager manager;
    private String originChannel;
    private String originChatId;

    public SpawnTool(SubAgentManager manager) {
        this.manager = manager;
    }

    public void setContext(String channel, String chatId) {
        this.originChannel = channel;
        this.originChatId = chatId;
    }

    @Override
    public String name() {
        return "spawn";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
            "spawn",
            "Spawn a sub-agent to handle a complex task in the background.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "task", Map.of("type", "string", "description", "The task for the sub-agent"),
                    "label", Map.of("type", "string", "description", "Optional label for the task")
                ),
                "required", List.of("task")
            )
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args) {
        String task = (String) args.get("task");
        String label = (String) args.get("label");

        try {
            String subAgentId = manager.spawn(task, label, originChannel, originChatId);
            return new ToolExecutionResult(true, "Sub-agent spawned with ID: " + subAgentId);
        } catch (Exception e) {
            return new ToolExecutionResult(false, "Failed to spawn sub-agent: " + e.getMessage());
        }
    }
}
