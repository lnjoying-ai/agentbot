package com.agentbot.core.heartbeat;

/**
 * Represents a task from HEARTBEAT.md.
 */
public class HeartbeatTask {
    private final String title;
    private final String description;
    
    public HeartbeatTask(String title, String description) {
        this.title = title;
        this.description = description;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the full task as a prompt for the agent.
     */
    public String toPrompt() {
        return String.format("# Heartbeat Task: %s\n\n%s", title, description);
    }
    
    @Override
    public String toString() {
        return "HeartbeatTask{title='" + title + "', description='" + description + "'}";
    }
}
