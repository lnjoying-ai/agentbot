package com.agentbot.core.tools.impl;

import com.agentbot.core.tools.ToolDefinition;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolWithDefinition;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class FileWriteTool implements ToolWithDefinition {
    private final Path workspaceDir;

    public FileWriteTool() {
        this(null);
    }

    public FileWriteTool(Path workspaceDir) {
        this.workspaceDir = workspaceDir == null ? null : workspaceDir.toAbsolutePath().normalize();
    }

    @Override
    public String name() {
        return "write_file";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
            "write_file",
            "Write or overwrite content to a file on the filesystem.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "path", Map.of(
                        "type", "string",
                        "description", "The path to the file (relative paths default to workspace/tmp)"
                    ),
                    "content", Map.of(
                        "type", "string",
                        "description", "The content to write"
                    )
                ),
                "required", List.of("path", "content")
            )
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args) {
        String pathStr = (String) args.get("path");
        String content = (String) args.get("content");
        if (pathStr == null || pathStr.isBlank()) {
            return new ToolExecutionResult(false, "Failed to write file: path is empty");
        }
        try {
            Path path = Path.of(pathStr);
            if (!path.isAbsolute() && workspaceDir != null) {
                Path tmpDir = workspaceDir.resolve("tmp");
                Files.createDirectories(tmpDir);
                path = tmpDir.resolve(pathStr).normalize();
            }

            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content == null ? "" : content);
            return new ToolExecutionResult(true, "File written successfully to " + path);
        } catch (Exception e) {
            return new ToolExecutionResult(false, "Failed to write file: " + e.getMessage());
        }
    }
}
