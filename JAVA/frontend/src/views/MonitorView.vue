<template>
  <section>
    <h2 class="section-title">运行监控</h2>
    <div class="card-grid" style="margin-bottom: 20px">
      <StatCard title="运行时长" :value="monitor.stats.uptime" subtitle="自上次启动" />
      <StatCard title="活跃会话" :value="monitor.stats.activeSessions" subtitle="最近 15 分钟" />
      <StatCard title="工具调用" :value="monitor.stats.toolCalls" subtitle="今日累计" />
      <StatCard title="队列深度" :value="monitor.stats.queueDepth" subtitle="等待任务" />
    </div>

    <div class="card-grid">
      <div class="card">
        <h3>延迟趋势</h3>
        <div class="sparkline"></div>
        <div style="margin-top: 10px; color: var(--muted); font-size: 12px">
          P50 {{ monitor.stats.latencyP50 }}ms · P95 {{ monitor.stats.latencyP95 }}ms
        </div>
      </div>
      <div class="card">
        <h3>错误率</h3>
        <div style="font-size: 26px; font-weight: 600">{{ monitor.stats.errorRate }}%</div>
        <div style="margin-top: 10px; color: var(--muted); font-size: 12px">近 24 小时</div>
      </div>
      <div class="card">
        <h3>渠道状态</h3>
        <div style="display: grid; gap: 10px">
          <div v-for="(status, channel) in monitor.stats.channelStatus" :key="channel">
            <strong>{{ channel }}</strong>
            <span style="margin-left: 8px; color: var(--muted)">{{ status }}</span>
          </div>
        </div>
      </div>
      <div class="card">
        <h3>模型配置</h3>
        <div style="font-size: 14px; color: var(--muted)">{{ monitor.stats.model }}</div>
        <div style="margin-top: 12px">
          <button class="button secondary" @click="monitor.refresh">同步健康状态</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import StatCard from "../components/StatCard.vue";
import { useMonitorStore } from "../store/monitor";

const monitor = useMonitorStore();
</script>
