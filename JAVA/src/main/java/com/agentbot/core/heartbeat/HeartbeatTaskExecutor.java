package com.agentbot.core.heartbeat;

import com.agentbot.core.agent.AgentRuntime;
import com.agentbot.core.bus.events.InboundMessage;
import com.agentbot.core.bus.events.OutboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Executes heartbeat tasks by sending them to the agent runtime.
 */
@Component
public class HeartbeatTaskExecutor {
    private static final Logger log = LoggerFactory.getLogger(HeartbeatTaskExecutor.class);
    
    private final AgentRuntime agentRuntime;
    private static final String HEARTBEAT_CHANNEL = "system";
    private static final String HEARTBEAT_CHAT_ID = "heartbeat";
    
    public HeartbeatTaskExecutor(AgentRuntime agentRuntime) {
        this.agentRuntime = agentRuntime;
    }
    
    /**
     * Execute a heartbeat task by sending it to the agent as a system message.
     * 
     * @param task The task to execute
     * @throws Exception if execution fails
     */
    public void executeTask(HeartbeatTask task) throws Exception {
        log.info("Executing heartbeat task: {}", task.getTitle());
        
        // Create a system-level message for the task
        InboundMessage message = new InboundMessage(
            HEARTBEAT_CHANNEL,
            "system",
            HEARTBEAT_CHAT_ID,
            task.toPrompt()
        );
        
        // Execute via agent runtime
        try {
            OutboundMessage response = agentRuntime.handle(message);
            
            if (response != null) {
                log.info("Heartbeat task completed: {} - Response: {}", 
                    task.getTitle(), 
                    response.getContent() != null ? 
                        response.getContent().substring(0, Math.min(100, response.getContent().length())) : 
                        "no content"
                );
            } else {
                log.warn("Heartbeat task returned null response: {}", task.getTitle());
            }
            
        } catch (Exception e) {
            log.error("Failed to execute heartbeat task: {}", task.getTitle(), e);
            throw e;
        }
    }
}
