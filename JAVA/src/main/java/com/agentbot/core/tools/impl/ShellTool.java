package com.agentbot.core.tools.impl;

import com.agentbot.core.tools.ToolDefinition;
import com.agentbot.core.tools.ToolExecutionResult;
import com.agentbot.core.tools.ToolWithDefinition;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            Charset outputCharset = StandardCharsets.UTF_8;

            if (os.contains("win")) {
                String powerShellScript = extractPowerShellScript(command);
                if (powerShellScript != null || containsNonAscii(command)) {
                    String script = powerShellScript != null ? powerShellScript : command;
                    pb = new ProcessBuilder(
                        "powershell",
                        "-NoProfile",
                        "-Command",
                        buildPowerShellCommand(script)
                    );
                } else {
                    String wrapped = "chcp 65001 >NUL & " + command;
                    pb = new ProcessBuilder("cmd.exe", "/c", wrapped);
                }
                outputCharset = StandardCharsets.UTF_8;
            } else if (os.contains("mac")) {

                // MacOS: Use zsh (default since Catalina) or fallback to sh
                pb = new ProcessBuilder("zsh", "-c", command);
            } else {
                // Linux/Other Unix: Use sh
                pb = new ProcessBuilder("sh", "-c", command);
            }

            
            pb.redirectErrorStream(true);
            if (os.contains("win")) {
                Map<String, String> env = pb.environment();
                env.putIfAbsent("PYTHONIOENCODING", "utf-8");
                env.putIfAbsent("PYTHONUTF8", "1");
            }
            Process process = pb.start();


            // Read output using negotiated charset
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), outputCharset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }


            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ToolExecutionResult(false, "Command timed out after 60 seconds");
            }

            return new ToolExecutionResult(true, output.toString());
        } catch (Exception e) {
            return new ToolExecutionResult(false, "Shell execution failed: " + e.getMessage());
        }
    }

    private String buildPowerShellCommand(String script) {
        String safeScript = script == null ? "" : script.trim();
        return "[Console]::OutputEncoding=[System.Text.Encoding]::UTF8; "
            + "$OutputEncoding=[System.Text.Encoding]::UTF8; "
            + safeScript;
    }

    private boolean containsNonAscii(String command) {
        if (command == null || command.isEmpty()) return false;
        for (int i = 0; i < command.length(); i++) {
            if (command.charAt(i) > 127) {
                return true;
            }
        }
        return false;
    }

    private String extractPowerShellScript(String command) {

        if (command == null) return null;
        String trimmed = command.trim();
        if (trimmed.isEmpty()) return null;
        String lowered = trimmed.toLowerCase();
        if (!lowered.startsWith("powershell") && !lowered.startsWith("pwsh")) return null;

        String rest = trimmed.replaceFirst("(?i)^(powershell|pwsh)(\\.exe)?\\s*", "").trim();
        if (rest.isEmpty()) return "";

        String loweredRest = rest.toLowerCase();
        if (loweredRest.startsWith("-noprofile")) {
            rest = rest.substring("-noprofile".length()).trim();
        }
        loweredRest = rest.toLowerCase();
        if (loweredRest.startsWith("-command")) {
            rest = rest.substring("-command".length()).trim();
        } else if (loweredRest.startsWith("-c")) {
            rest = rest.substring("-c".length()).trim();
        }

        if (rest.startsWith("\"") && rest.endsWith("\"") && rest.length() >= 2) {
            rest = rest.substring(1, rest.length() - 1);
        }
        return rest;
    }

    @Override
    public boolean requiresApproval(Map<String, Object> args) {
        String command = (String) args.get("command");
        if (command == null || command.isBlank()) {
            return false;
        }


        String cmd = command.trim().toLowerCase();
        
        // 1. Check for shell metacharacters that allow command chaining or redirection
        // These are almost always high-risk if used by an AI.
        if (cmd.contains("|") || cmd.contains("&") || cmd.contains(";") || 
            cmd.contains(">") || cmd.contains("<") || cmd.contains("`") || cmd.contains("$(")) {
            return true;
        }

        // 2. Check for high-risk keywords (with word boundaries or space to avoid false positives)
        String[] highRiskKeywords = {
            "rm ", "del ", "rd ", "rmdir ", "erase ", // Deletion
            "format ", "mkfs", "fdisk", "parted", "dd ", // Disk operations
            "sudo ", "su ", "chmod ", "chown ", "visudo", // Permission/Identity
            "reboot", "shutdown", "halt", "poweroff", "init 0", "init 6", // System state
            "kill ", "pkill ", "killall ", // Process management
            "iptables", "ufw", "firewall-cmd", "nc ", "netcat ", // Network/Firewall
            "passwd", "userdel", "groupdel", "usermod", // User management
            "curl ", "wget ", "ssh ", "scp ", "ftp ", "telnet", "nmap", // Network transfer / scanning
            "npm ", "pip ", "yum ", "apt", "brew ", "apk ", "dnf ", // Package management
            "bash ", "sh ", "zsh ", "python", "node ", "perl ", "ruby ", // Script execution
            "cp -r", "mv ", ">", ">>", // File movement/overwrite (though metachars already checked)
            "systemctl", "service ", "crontab", "at " // System services
        };

        for (String keyword : highRiskKeywords) {
            if (cmd.contains(keyword)) {
                return true;
            }
        }

        // Additional checks for specific risky combinations
        if (cmd.contains("chmod") && (cmd.contains("777") || cmd.contains("+x"))) {
            return true;
        }


        // 3. Check for specific commands that are safe but only when they are simple
        String[] safeCommands = {
            "ls", "pwd", "dir", "echo", "cat", "type", "ps", "df", "du", 
            "whoami", "date", "grep", "find", "tail", "head", "stat", "hostname", "uname"
        };

        boolean isSafeBase = false;
        for (String safe : safeCommands) {
            if (cmd.equals(safe) || cmd.startsWith(safe + " ")) {
                isSafeBase = true;
                break;
            }
        }

        // If it's a known safe command and doesn't contain risky redirection/chaining (checked in step 1),
        // we still require approval if the command is unusually long/complex.
        if (isSafeBase) {
            return command.length() > 100; // Safe commands are usually short
        }

        // 4. Default to requiring approval for all other commands
        return true;
    }
}

