package com.agentbot.core.tools.impl;

import com.agentbot.core.tools.ToolDefinition;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolWithDefinition;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ListDirTool implements ToolWithDefinition {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final List<Path> searchRoots;

    public ListDirTool() {
        this(null);
    }

    public ListDirTool(Path workspaceDir) {
        List<Path> roots = new ArrayList<>();
        if (workspaceDir != null) {
            Path ws = workspaceDir.toAbsolutePath().normalize();
            roots.add(ws);
            roots.add(ws.resolve("skills"));
            roots.add(ws.resolve("system").resolve("skills"));
            roots.add(ws.resolve("agents"));
        }
        this.searchRoots = List.copyOf(roots);
    }

    @Override
    public String name() {
        return "list_dir";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
            "list_dir",
            "List contents of a directory.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "path", Map.of(
                        "type", "string",
                        "description", "The absolute or relative path to the directory"
                    )
                ),
                "required", List.of("path")
            )
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args) {
        String pathStr = (String) args.get("path");
        if (pathStr == null || pathStr.isBlank()) {
            return new ToolExecutionResult(false, "Directory path is empty");
        }
        try {
            Path resolved = resolveDirectory(pathStr);
            if (resolved == null) {
                return new ToolExecutionResult(false, "Directory not found: " + pathStr);
            }
            if (!Files.isDirectory(resolved)) {
                return new ToolExecutionResult(false, "Not a directory: " + resolved);
            }

            List<Path> entries;
            try (var stream = Files.list(resolved)) {
                entries = stream.sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .collect(Collectors.toList());
            }

            StringBuilder sb = new StringBuilder();
            sb.append("name\ttype\tsize\tmodified\n");
            for (Path entry : entries) {
                BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class);
                String type = attrs.isDirectory() ? "dir" : "file";
                long size = attrs.isDirectory() ? 0L : attrs.size();
                String modified = TIME_FORMATTER.format(attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()));
                sb.append(entry.getFileName())
                    .append('\t')
                    .append(type)
                    .append('\t')
                    .append(size)
                    .append('\t')
                    .append(modified)
                    .append('\n');
            }
            return new ToolExecutionResult(true, sb.toString().trim());
        } catch (Exception e) {
            return new ToolExecutionResult(false, "Failed to list directory: " + e.getMessage());
        }
    }

    private Path resolveDirectory(String pathStr) {
        Path path = Path.of(pathStr);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        if (Files.exists(path) && Files.isDirectory(path)) {
            return path.normalize();
        }
        for (Path root : searchRoots) {
            Path candidate = root.resolve(pathStr).normalize();
            if (Files.exists(candidate) && Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
