import { reactive, ref } from "vue";
import type { MonitorStats } from "../types";
import { getApiBaseUrl } from "./config";

const health = ref<"ok" | "degraded" | "down">("ok");

const stats = reactive<MonitorStats>({
  uptime: "2d 04:12",
  activeSessions: 12,
  toolCalls: 286,
  queueDepth: 4,
  errorRate: 0.7,
  latencyP50: 380,
  latencyP95: 920,
  channelStatus: {
    Telegram: "online",
    WhatsApp: "degraded",
    WeChat: "online"
  },
  model: "openai / gpt-4o-mini"
});

async function refresh() {
  const baseUrl = getApiBaseUrl();
  if (!baseUrl) return;

  try {
    // 1. Check health
    const healthRes = await fetch(`${baseUrl}/health`);
    if (healthRes.ok) {
      const hData = await healthRes.json();
      health.value = hData.status === "ok" ? "ok" : "degraded";
    } else {
      health.value = "degraded";
    }

    // 2. Fetch Ops Status
    const statusRes = await fetch(`${baseUrl}/api/ops/status`);
    if (statusRes.ok) {
      const sData = await statusRes.json();
      // sData format: { status, workspace, configFile, channels: [], heartbeat: {}, cron: {}, llm: {} }
      stats.channelStatus = {};
      if (Array.isArray(sData.channels)) {
        sData.channels.forEach((name: string) => {
          stats.channelStatus[name] = "online";
        });
      }
      stats.model = `${sData.llm?.provider || "unknown"} / ${sData.llm?.model || "unknown"}`;
      stats.toolCalls = sData.toolCalls || 0;
      
      if (sData.uptimeMillis) {

        const seconds = Math.floor(sData.uptimeMillis / 1000);
        const mins = Math.floor(seconds / 60);
        const hours = Math.floor(mins / 60);
        const days = Math.floor(hours / 24);
        stats.uptime = `${days}d ${hours % 24}h ${mins % 60}m`;
      }

    }
  } catch (error) {
    console.error("Monitor refresh failed", error);
    health.value = "down";
  }
}

// Subscribe to SSE events if supported
function subscribeEvents() {
  const baseUrl = getApiBaseUrl();
  if (!baseUrl) return;
  
  const eventSource = new EventSource(`${baseUrl}/api/monitor/events`);
  eventSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      // Update stats based on real-time events if needed
      console.log("Monitor event:", data);
    } catch (e) {}
  };
  eventSource.onerror = () => {
    eventSource.close();
  };
}

// Initial refresh
refresh();

export function useMonitorStore() {
  return { health, stats, refresh, subscribeEvents };
}

