import { reactive, ref } from "vue";
import { getApiBaseUrl } from "./config";
const health = ref("ok");
const logEntries = ref([]);
const lastInit = ref(null);
const stats = reactive({
    uptime: "0d 0h 0m",
    activeSessions: 0,
    toolCalls: 0,
    queueDepth: 0,
    errorRate: 0,
    latencyP50: 0,
    latencyP95: 0,
    channelStatus: {},
    model: "unknown",
    status: "unknown",
    workspace: "-",
    heartbeat: { enabled: false, intervalSeconds: 0 },
    cron: { enabled: false, defaultIntervalSeconds: 0 },
    p2p: {
        connectionsOpened: 0,
        connectionsClosed: 0,
        handshakesCompleted: 0,
        messagesReceived: 0,
        messagesSent: 0,
        acks: 0,
        nacks: 0,
        retries: 0
    }
});
async function refresh() {
    const baseUrl = getApiBaseUrl();
    if (!baseUrl)
        return;
    try {
        // 1. Check health
        const healthRes = await fetch(`${baseUrl}/health`);
        if (healthRes.ok) {
            const hData = await healthRes.json();
            health.value = hData.status === "ok" ? "ok" : "degraded";
        }
        else {
            health.value = "degraded";
        }
        // 2. Fetch Ops Status
        const statusRes = await fetch(`${baseUrl}/api/ops/status`);
        if (statusRes.ok) {
            const sData = await statusRes.json();
            // sData format: { status, workspace, channels: [], heartbeat: {}, cron: {}, llm: {}, p2p: {} }
            stats.status = sData.status || "unknown";
            stats.workspace = sData.workspace || "-";
            stats.heartbeat = sData.heartbeat || stats.heartbeat;
            stats.cron = sData.cron || stats.cron;
            stats.p2p = sData.p2p || stats.p2p;
            stats.channelStatus = {};
            if (Array.isArray(sData.channels)) {
                sData.channels.forEach((name) => {
                    stats.channelStatus[name] = "online";
                });
            }
            const provider = sData.llm?.currentProvider || sData.llm?.provider || "unknown";
            const model = sData.llm?.currentModel || sData.llm?.model || "unknown";
            let modelLabel = `${provider} / ${model}`;
            const fallbackTotal = sData.llm?.totalFallbacks || 0;
            const lastReason = sData.llm?.lastFallback?.reason || "";
            if (fallbackTotal > 0) {
                modelLabel += ` (回退 ${fallbackTotal}${lastReason ? `，最近：${lastReason}` : ""})`;
            }
            stats.model = modelLabel;
            stats.toolCalls = sData.toolCalls || 0;
            if (sData.uptimeMillis) {
                const seconds = Math.floor(sData.uptimeMillis / 1000);
                const mins = Math.floor(seconds / 60);
                const hours = Math.floor(mins / 60);
                const days = Math.floor(hours / 24);
                stats.uptime = `${days}d ${hours % 24}h ${mins % 60}m`;
            }
        }
    }
    catch (error) {
        console.error("Monitor refresh failed", error);
        health.value = "down";
    }
}
// Subscribe to SSE events if supported
function subscribeEvents() {
    const baseUrl = getApiBaseUrl();
    if (!baseUrl)
        return;
    const eventSource = new EventSource(`${baseUrl}/api/monitor/events`);
    eventSource.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);
            // Update stats based on real-time events if needed
            console.log("Monitor event:", data);
        }
        catch (e) { }
    };
    eventSource.onerror = () => {
        eventSource.close();
    };
}
async function fetchLogs(limit = 200) {
    const baseUrl = getApiBaseUrl();
    if (!baseUrl)
        return;
    try {
        const res = await fetch(`${baseUrl}/api/ops/logs?limit=${limit}`);
        if (res.ok) {
            logEntries.value = await res.json();
        }
    }
    catch (error) {
        console.error("Fetch ops logs failed", error);
    }
}
async function initWorkspace() {
    const baseUrl = getApiBaseUrl();
    if (!baseUrl)
        return;
    try {
        const res = await fetch(`${baseUrl}/api/ops/init`, { method: "POST" });
        if (res.ok) {
            lastInit.value = await res.json();
        }
    }
    catch (error) {
        console.error("Init workspace failed", error);
    }
}
// Initial refresh
refresh();
export function useMonitorStore() {
    return { health, stats, logEntries, lastInit, refresh, fetchLogs, initWorkspace, subscribeEvents };
}
