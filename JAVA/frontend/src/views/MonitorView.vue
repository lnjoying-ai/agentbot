<template>
  <section>
    <h2 class="section-title">{{ t("monitor.title") }}</h2>
    <div class="card-grid" style="margin-bottom: 20px">
      <StatCard :title="t('monitor.uptime')" :value="stats.uptime" :subtitle="t('monitor.uptimeSubtitle')" />
      <StatCard :title="t('monitor.toolCalls')" :value="stats.toolCalls" :subtitle="t('monitor.toolCallsSubtitle')" />

      <div class="card">
        <h3>{{ t("monitor.serviceStatus") }}</h3>
        <div style="display: flex; align-items: center; gap: 10px; margin-top: 8px">
          <HealthBadge :status="health" :text="healthLabel" />
          <span style="color: var(--muted); font-size: 12px">{{ serviceStatusText }}</span>
        </div>
      </div>
      <div class="card">
        <h3>{{ t("monitor.modelConfig") }}</h3>
        <div style="font-size: 14px; color: var(--muted)">{{ stats.model }}</div>

        <div style="margin-top: 12px">
          <button class="button secondary" @click="refresh">{{ t("monitor.syncHealth") }}</button>
        </div>
      </div>
    </div>

    <div class="card-grid" style="margin-bottom: 20px">
      <div class="card">
        <h3>{{ t("monitor.systemInfo") }}</h3>
        <div style="font-size: 12px; color: var(--muted)">{{ t("monitor.workspace") }}</div>
        <div style="font-size: 13px; word-break: break-all">{{ stats.workspace }}</div>
      </div>
      <div class="card">
        <h3>{{ t("monitor.heartbeatCron") }}</h3>
        <div style="display: grid; gap: 10px">
          <div>
            <div style="font-size: 12px; color: var(--muted)">{{ t("monitor.heartbeat") }}</div>
            <div>
              <strong>{{ stats.heartbeat?.enabled ? t("common.enabled") : t("common.disabled") }}</strong>
              <span style="margin-left: 8px; color: var(--muted)">{{ t("monitor.interval", { value: formatInterval(stats.heartbeat?.intervalSeconds) }) }}</span>
            </div>
          </div>
          <div>
            <div style="font-size: 12px; color: var(--muted)">{{ t("nav.cron") }}</div>
            <div>
              <strong>{{ stats.cron?.enabled ? t("common.enabled") : t("common.disabled") }}</strong>
              <span style="margin-left: 8px; color: var(--muted)">{{ t("monitor.defaultInterval", { value: formatInterval(stats.cron?.defaultIntervalSeconds) }) }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="card">
        <h3>{{ t("monitor.p2pMetrics") }}</h3>
        <div style="display: grid; gap: 6px; font-size: 12px">
          <div>{{ t("monitor.connectionsOpened", { count: stats.p2p?.connectionsOpened ?? 0 }) }}</div>
          <div>{{ t("monitor.connectionsClosed", { count: stats.p2p?.connectionsClosed ?? 0 }) }}</div>
          <div>{{ t("monitor.handshakesCompleted", { count: stats.p2p?.handshakesCompleted ?? 0 }) }}</div>
          <div>{{ t("monitor.messages", { received: stats.p2p?.messagesReceived ?? 0, sent: stats.p2p?.messagesSent ?? 0 }) }}</div>
          <div>ACK/NACK：{{ stats.p2p?.acks ?? 0 }} / {{ stats.p2p?.nacks ?? 0 }}</div>
          <div>{{ t("monitor.retries", { count: stats.p2p?.retries ?? 0 }) }}</div>
        </div>
      </div>
      <div class="card">
        <h3>{{ t("monitor.channelStatus") }}</h3>
        <div v-if="Object.keys(stats.channelStatus).length === 0" style="color: var(--muted); font-size: 12px">
          {{ t("monitor.noChannels") }}
        </div>
        <div v-else style="display: grid; gap: 10px">
          <div v-for="(status, channel) in stats.channelStatus" :key="channel">
            <strong>{{ channel }}</strong>
            <span style="margin-left: 8px; color: var(--muted)">{{ status }}</span>
          </div>
        </div>
      </div>
    </div>

    <h2 class="section-title">{{ t("monitor.opsTitle") }}</h2>
    <div class="card-grid">
      <div class="card">
        <h3>{{ t("monitor.workspaceInit") }}</h3>
        <div style="font-size: 12px; color: var(--muted)">{{ t("monitor.workspaceInitDesc") }}</div>
        <div style="margin-top: 12px; display: flex; gap: 10px; flex-wrap: wrap">
          <button class="button" @click="initWorkspace">{{ t("monitor.runInit") }}</button>
          <button class="button secondary" @click="refresh">{{ t("monitor.refreshStatus") }}</button>
        </div>
        <div v-if="lastInit" style="margin-top: 12px; font-size: 12px; color: var(--muted)">
          {{ t("monitor.lastInit", { status: lastInit.ok ? t("monitor.health.healthy") : t("monitor.health.unavailable"), count: lastInit.files?.length ?? 0 }) }}
        </div>
      </div>
      <div class="card">
        <h3>{{ t("monitor.logs") }}</h3>
        <div v-if="logEntries.length === 0" style="color: var(--muted); font-size: 12px">{{ t("monitor.noLogs") }}</div>
        <div v-else style="display: grid; gap: 10px; max-height: 240px; overflow: auto">
          <div v-for="entry in logEntries" :key="entry.timestamp + entry.type">
            <div style="font-size: 11px; color: var(--muted)">{{ formatTimestamp(entry.timestamp) }}</div>
            <div style="font-weight: 600">{{ entry.type }}</div>
            <div style="font-size: 12px; color: var(--muted); word-break: break-all">
              {{ formatPayload(entry.payload) }}
            </div>
          </div>
        </div>
        <div style="margin-top: 12px">
          <button class="button secondary" @click="() => fetchLogs()">{{ t("monitor.refreshLogs") }}</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from "vue";
import StatCard from "../components/StatCard.vue";
import HealthBadge from "../components/HealthBadge.vue";
import { useMonitorStore } from "../store/monitor";
import { useI18n } from "../i18n";

const { health, stats, logEntries, lastInit, refresh, fetchLogs, initWorkspace } = useMonitorStore();
const { t, locale } = useI18n();
fetchLogs();

const healthLabel = computed(() =>
  health.value === "ok"
    ? t("monitor.health.healthy")
    : health.value === "degraded"
      ? t("monitor.health.warning")
      : t("monitor.health.unavailable")
);

const serviceStatusText = computed(() => {
  if (stats.status === "ok") return t("topbar.health.ok");
  if (stats.status === "degraded") return t("topbar.health.degraded");
  if (stats.status === "error") return t("topbar.health.error");
  return stats.status;
});

function formatPayload(payload: Record<string, any>) {
  try {
    return JSON.stringify(payload);
  } catch (e) {
    return "";
  }
}

function formatTimestamp(value: string) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString(locale.value);
}

function formatInterval(seconds?: number) {
  if (seconds === undefined || seconds === null) return "-";
  return t("monitor.intervalSeconds", { count: seconds });
}
</script>


