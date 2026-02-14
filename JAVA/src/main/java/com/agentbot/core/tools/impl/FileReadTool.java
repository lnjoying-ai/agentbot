package com.agentbot.core.tools.impl;

import com.agentbot.core.tools.ToolDefinition;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolWithDefinition;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class FileReadTool implements ToolWithDefinition {
    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
            "read_file",
            "Read the content of a file from the filesystem.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "path", Map.of(
                        "type", "string",
                        "description", "The absolute or relative path to the file"
                    )
                ),
                "required", List.of("path")
            )
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args) {
        String pathStr = (String) args.get("path");
        try {
            Path path = Path.of(pathStr);
            if (!Files.exists(path)) {
                return new ToolExecutionResult(false, "File not found: " + pathStr);
            }
            String content = Files.readString(path);
            return new ToolExecutionResult(true, content);
        } catch (Exception e) {
            return new ToolExecutionResult(false, "Failed to read file: " + e.getMessage());
        }
    }
}
