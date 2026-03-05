package com.agentbot.core.heartbeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HeartbeatService periodically checks HEARTBEAT.md for tasks to execute.
 * 
 * Inspired by nanobot's heartbeat mechanism, this service:
 * - Reads workspace/HEARTBEAT.md every 30 minutes (configurable)
 * - Extracts tasks from "Active Tasks" section
 * - Executes each task via the agent runtime
 * - Updates the file with completion status
 */
@Service
public class HeartbeatService {
    private static final Logger log = LoggerFactory.getLogger(HeartbeatService.class);
    
    private final Path workspacePath;
    private final HeartbeatTaskExecutor taskExecutor;
    private final int intervalSeconds;
    private boolean enabled;
    
    public HeartbeatService(Path workspacePath, HeartbeatTaskExecutor taskExecutor, boolean enabled, int intervalSeconds) {
        this.workspacePath = workspacePath;
        this.taskExecutor = taskExecutor;
        this.enabled = enabled;
        this.intervalSeconds = Math.max(1, intervalSeconds);
    }
    
    /**
     * Scheduled heartbeat check (intervalSeconds by default).
     * Can be configured via application.properties:
     * agentbot.heartbeat.intervalSeconds=60 (seconds)
     */
    @Scheduled(fixedDelayString = "${agentbot.heartbeat.intervalSeconds:60}000")
    public void performHeartbeat() {
        if (!enabled) {
            log.debug("Heartbeat is disabled, skipping");
            return;
        }
        
        Path heartbeatFile = workspacePath.resolve("HEARTBEAT.md");
        
        if (!Files.exists(heartbeatFile)) {
            log.debug("HEARTBEAT.md not found, skipping heartbeat");
            return;
        }
        
        try {
            String content = Files.readString(heartbeatFile);
            List<HeartbeatTask> tasks = parseActiveTasks(content);
            
            if (tasks.isEmpty()) {
                updateLastCheckedTime(heartbeatFile, content);
                return;
            }
            
            log.info("Heartbeat: found {} active tasks", tasks.size());

            
            // Execute tasks
            for (HeartbeatTask task : tasks) {
                try {
                    log.info("Executing heartbeat task: {}", task.getTitle());
                    taskExecutor.executeTask(task);
                    markTaskCompleted(heartbeatFile, task);
                } catch (Exception e) {
                    log.error("Failed to execute heartbeat task: {}", task.getTitle(), e);
                }
            }
            
            updateLastCheckedTime(heartbeatFile, Files.readString(heartbeatFile));
            
        } catch (IOException e) {
            log.error("Failed to read HEARTBEAT.md", e);
        }
    }
    
    /**
     * Parse active tasks from HEARTBEAT.md content.
     * 
     * Format:
     * ## Active Tasks
     * ### Task Title
     * - Task description line 1
     * - Task description line 2
     */
    private List<HeartbeatTask> parseActiveTasks(String content) {
        List<HeartbeatTask> tasks = new ArrayList<>();
        
        // Find "Active Tasks" section
        Pattern sectionPattern = Pattern.compile(
            "## Active Tasks\\s*\\n.*?\\n(.*?)(?=\\n##|$)", 
            Pattern.DOTALL
        );
        Matcher sectionMatcher = sectionPattern.matcher(content);
        
        if (!sectionMatcher.find()) {
            return tasks;
        }
        
        String activeSection = sectionMatcher.group(1);
        
        // Remove HTML comments
        activeSection = activeSection.replaceAll("<!--.*?-->", "");
        
        // Parse individual tasks (### Task Title)
        Pattern taskPattern = Pattern.compile("### (.+?)\\n([^#]*?)(?=###|$)", Pattern.DOTALL);
        Matcher taskMatcher = taskPattern.matcher(activeSection);
        
        while (taskMatcher.find()) {
            String title = taskMatcher.group(1).trim();
            String description = taskMatcher.group(2).trim();
            
            if (!title.isEmpty() && !description.isEmpty()) {
                tasks.add(new HeartbeatTask(title, description));
            }
        }
        
        return tasks;
    }
    
    /**
     * Mark a task as completed by moving it to the Completed section.
     */
    private void markTaskCompleted(Path heartbeatFile, HeartbeatTask task) {
        try {
            String content = Files.readString(heartbeatFile);
            
            // Find and remove the task from Active Tasks
            String taskPattern = "### " + Pattern.quote(task.getTitle()) + "\\n.*?(?=\\n###|\\n##|$)";
            String taskContent = extractTaskContent(content, task.getTitle());
            
            if (taskContent == null) {
                log.warn("Could not find task content for: {}", task.getTitle());
                return;
            }
            
            // Remove from active section
            content = content.replaceFirst(taskPattern, "");
            
            // Add to completed section with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String completedEntry = String.format(
                "### ✅ %s (%s)%n%s%n%n",
                task.getTitle(),
                timestamp,
                taskContent
            );
            
            // Insert into Completed section
            if (content.contains("## Completed")) {
                content = content.replaceFirst(
                    "(## Completed\\s*\\n[^#]*?)(###|$)",
                    "$1" + completedEntry + "$2"
                );
            } else {
                content += "\n\n## Completed\n\n" + completedEntry;
            }
            
            Files.writeString(heartbeatFile, content);
            log.info("Marked task as completed: {}", task.getTitle());
            
        } catch (IOException e) {
            log.error("Failed to mark task as completed: {}", task.getTitle(), e);
        }
    }
    
    /**
     * Extract the full content of a task (all lines under the ### heading).
     */
    private String extractTaskContent(String content, String title) {
        Pattern pattern = Pattern.compile(
            "### " + Pattern.quote(title) + "\\n(.*?)(?=\\n###|\\n##|$)",
            Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
    
    /**
     * Update the "Last checked" timestamp at the bottom of HEARTBEAT.md.
     */
    private void updateLastCheckedTime(Path heartbeatFile, String content) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            content = content.replaceFirst(
                "\\*Last checked:.*",
                "*Last checked: " + timestamp + "*"
            );
            
            // Update next check time (intervalSeconds from now)
            LocalDateTime nextCheck = LocalDateTime.now().plusSeconds(intervalSeconds);
            String nextTimestamp = nextCheck.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            content = content.replaceFirst(
                "\\*Next check:.*",
                "*Next check: " + nextTimestamp + "*"
            );
            
            Files.writeString(heartbeatFile, content);
            
        } catch (IOException e) {
            log.error("Failed to update last checked time", e);
        }
    }
    
    /**
     * Manually trigger a heartbeat check (useful for testing).
     */
    public void triggerHeartbeat() {
        log.info("Manually triggered heartbeat");
        performHeartbeat();
    }
    
    /**
     * Enable or disable the heartbeat service.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        log.info("Heartbeat service {}", enabled ? "enabled" : "disabled");
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
