package com.agentbot.core.agent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PendingActionStore {
    private final Map<String, PendingAction> pendingActions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToRecentId = new ConcurrentHashMap<>();

    public void put(String id, String sessionKey, PendingAction action) {
        pendingActions.put(id, action);
        if (sessionKey != null) {
            sessionToRecentId.put(sessionKey, id);
        }
    }

    public PendingAction get(String id) {
        return pendingActions.get(id);
    }

    public void remove(String id) {
        PendingAction action = pendingActions.remove(id);
        if (action != null) {
            // Cleanup session mapping if this was the recent one
            sessionToRecentId.values().removeIf(val -> val.equals(id));
        }
    }

    public String getMostRecentId(String sessionKey) {
        return sessionToRecentId.get(sessionKey);
    }



    public static class PendingAction {
        public final String toolName;
        public final Map<String, Object> args;
        public final java.util.List<Map<String, Object>> messages;
        public final java.util.List<Map<String, Object>> allToolCalls;
        public final java.util.Map<String, String> completedResults; // id -> output
        public final int round;
        public final java.util.Set<String> approvedIds;

        public PendingAction(String toolName, Map<String, Object> args, 
                             java.util.List<Map<String, Object>> messages,
                             java.util.List<Map<String, Object>> allToolCalls,
                             java.util.Map<String, String> completedResults,
                             int round,
                             java.util.Set<String> approvedIds) {
            this.toolName = toolName;
            this.args = args;
            this.messages = messages;
            this.allToolCalls = allToolCalls;
            this.completedResults = completedResults;
            this.round = round;
            this.approvedIds = approvedIds != null ? approvedIds : new java.util.HashSet<>();
        }
    }
}
