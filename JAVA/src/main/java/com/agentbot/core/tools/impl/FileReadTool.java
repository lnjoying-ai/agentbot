package com.agentbot.core.tools.impl;

import com.agentbot.core.tools.ToolDefinition;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolWithDefinition;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileReadTool implements ToolWithDefinition {
    private final List<Path> searchRoots;
    private final Path workspaceDir;

    public FileReadTool() {
        this(null);
    }

    public FileReadTool(Path workspaceDir) {
        this.workspaceDir = workspaceDir == null ? null : workspaceDir.toAbsolutePath().normalize();
        List<Path> roots = new ArrayList<>();
        if (this.workspaceDir != null) {
            Path ws = this.workspaceDir;
            roots.add(ws);
            roots.add(ws.resolve("tmp"));
            roots.add(ws.resolve("skills"));
            roots.add(ws.resolve("system").resolve("skills"));
            roots.add(ws.resolve("agents"));
        }
        this.searchRoots = List.copyOf(roots);
    }



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
            if (!path.isAbsolute()) {
                if (this.workspaceDir != null && (pathStr.startsWith("workspace/") || pathStr.startsWith("workspace\\"))) {
                    String relative = pathStr.substring("workspace".length());
                    while (relative.startsWith("/") || relative.startsWith("\\")) {
                        relative = relative.substring(1);
                    }
                    Path candidate = this.workspaceDir.resolve(relative).normalize();
                    if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                        String content = Files.readString(candidate);
                        return new ToolExecutionResult(true, content);
                    }
                    return new ToolExecutionResult(false, "File not found: " + pathStr);
                }

                if (Files.exists(path) && Files.isRegularFile(path)) {
                    String content = Files.readString(path);
                    return new ToolExecutionResult(true, content);
                }
                for (Path root : searchRoots) {
                    Path candidate = root.resolve(pathStr).normalize();
                    if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                        String content = Files.readString(candidate);
                        return new ToolExecutionResult(true, content);
                    }
                }
                return new ToolExecutionResult(false, "File not found: " + pathStr);
            }


            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return new ToolExecutionResult(false, "File not found: " + pathStr);
            }
            String content = Files.readString(path);
            return new ToolExecutionResult(true, content);
        } catch (Exception e) {
            return new ToolExecutionResult(false, "Failed to read file: " + e.getMessage());
        }
    }
}

