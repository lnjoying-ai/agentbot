<template>
  <header class="top-bar">
    <div>
      <div class="section-title">控制台</div>
      <div class="status-group">
        <HealthBadge :status="healthStatus" :text="healthLabel" />
        <span class="badge">模型: {{ monitor.stats.model }}</span>
        <span class="badge">API: {{ config.state.apiBaseUrl || '本地' }}</span>
      </div>
    </div>
    <div class="status-group">
      <span class="badge">活跃会话 {{ monitor.stats.activeSessions }}</span>
      <span class="badge">工具调用 {{ monitor.stats.toolCalls }}</span>
      <button class="button secondary" @click="monitor.refresh">刷新</button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from "vue";
import HealthBadge from "./HealthBadge.vue";
import { useConfigStore } from "../store/config";
import { useMonitorStore } from "../store/monitor";

const config = useConfigStore();
const monitor = useMonitorStore();

const healthStatus = computed(() => monitor.health.value);
const healthLabel = computed(() => {
  if (monitor.health.value === "ok") return "健康";
  if (monitor.health.value === "degraded") return "波动";
  return "异常";
});
</script>
