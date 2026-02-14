package com.agentbot.core.tools.impl;

import com.agentbot.core.tools.ToolDefinition;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolWithDefinition;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ShellTool implements ToolWithDefinition {
    @Override
    public String name() {
        return "shell";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
            "shell",
            "Execute a shell command on the host system. Use with caution.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "command", Map.of(
                        "type", "string",
                        "description", "The shell command to execute"
                    )
                ),
                "required", List.of("command")
            )
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args) {
        String command = (String) args.get("command");
        if (command == null || command.isBlank()) {
            return new ToolExecutionResult(false, "Command is required");
        }

        StringBuilder output = new StringBuilder();
        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            ProcessBuilder pb = isWindows 
                ? new ProcessBuilder("cmd.exe", "/c", command)
                : new ProcessBuilder("sh", "-c", command);
            
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ToolExecutionResult(false, "Command timed out after 30 seconds");
            }

            return new ToolExecutionResult(true, output.toString());
        } catch (Exception e) {
            return new ToolExecutionResult(false, "Shell execution failed: " + e.getMessage());
        }
    }
}
