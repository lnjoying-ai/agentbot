<template>
  <section>
    <h2 class="section-title">运行监控</h2>
    <div class="card-grid" style="margin-bottom: 20px">
      <StatCard title="运行时长" :value="stats.uptime" subtitle="自上次启动" />
      <StatCard title="工具调用" :value="stats.toolCalls" subtitle="当前累计" />

      <div class="card">
        <h3>服务状态</h3>
        <div style="display: flex; align-items: center; gap: 10px; margin-top: 8px">
          <HealthBadge :status="health" :text="healthLabel" />
          <span style="color: var(--muted); font-size: 12px">{{ stats.status }}</span>

        </div>
      </div>
      <div class="card">
        <h3>模型配置</h3>
        <div style="font-size: 14px; color: var(--muted)">{{ stats.model }}</div>

        <div style="margin-top: 12px">
          <button class="button secondary" @click="refresh">同步健康状态</button>

        </div>
      </div>
    </div>

    <div class="card-grid" style="margin-bottom: 20px">
      <div class="card">
        <h3>系统信息</h3>
        <div style="font-size: 12px; color: var(--muted)">工作区</div>
        <div style="font-size: 13px; word-break: break-all">{{ stats.workspace }}</div>



      </div>
      <div class="card">
        <h3>心跳与定时任务</h3>
        <div style="display: grid; gap: 10px">
          <div>
            <div style="font-size: 12px; color: var(--muted)">心跳</div>
            <div>
              <strong>{{ stats.heartbeat?.enabled ? "已启用" : "未启用" }}</strong>
              <span style="margin-left: 8px; color: var(--muted)">周期 {{ formatInterval(stats.heartbeat?.intervalSeconds) }}</span>

            </div>
          </div>
          <div>
            <div style="font-size: 12px; color: var(--muted)">定时任务</div>
            <div>
              <strong>{{ stats.cron?.enabled ? "已启用" : "未启用" }}</strong>
              <span style="margin-left: 8px; color: var(--muted)">默认 {{ formatInterval(stats.cron?.defaultIntervalSeconds) }}</span>

            </div>
          </div>
        </div>
      </div>
      <div class="card">
        <h3>P2P 运行指标</h3>
        <div style="display: grid; gap: 6px; font-size: 12px">
          <div>连接打开：{{ stats.p2p?.connectionsOpened ?? 0 }}</div>
          <div>连接关闭：{{ stats.p2p?.connectionsClosed ?? 0 }}</div>
          <div>握手完成：{{ stats.p2p?.handshakesCompleted ?? 0 }}</div>
          <div>消息收发：{{ stats.p2p?.messagesReceived ?? 0 }} / {{ stats.p2p?.messagesSent ?? 0 }}</div>
          <div>ACK/NACK：{{ stats.p2p?.acks ?? 0 }} / {{ stats.p2p?.nacks ?? 0 }}</div>
          <div>重试次数：{{ stats.p2p?.retries ?? 0 }}</div>

        </div>
      </div>
      <div class="card">
        <h3>渠道状态</h3>
        <div v-if="Object.keys(stats.channelStatus).length === 0" style="color: var(--muted); font-size: 12px">

          暂无在线渠道
        </div>
        <div v-else style="display: grid; gap: 10px">
          <div v-for="(status, channel) in stats.channelStatus" :key="channel">

            <strong>{{ channel }}</strong>
            <span style="margin-left: 8px; color: var(--muted)">{{ status }}</span>
          </div>
        </div>
      </div>
    </div>

    <h2 class="section-title">运维操作</h2>
    <div class="card-grid">
      <div class="card">
        <h3>工作区初始化</h3>
        <div style="font-size: 12px; color: var(--muted)">用于生成默认目录与配置</div>
        <div style="margin-top: 12px; display: flex; gap: 10px; flex-wrap: wrap">
          <button class="button" @click="initWorkspace">执行初始化</button>
          <button class="button secondary" @click="refresh">刷新状态</button>

        </div>
        <div v-if="lastInit" style="margin-top: 12px; font-size: 12px; color: var(--muted)">
          最近一次：{{ lastInit.ok ? "成功" : "失败" }} · {{ lastInit.files?.length ?? 0 }} 个文件

        </div>
      </div>
      <div class="card">
        <h3>运维日志</h3>
        <div v-if="logEntries.length === 0" style="color: var(--muted); font-size: 12px">暂无日志</div>
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
          <button class="button secondary" @click="() => fetchLogs()">刷新日志</button>

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

const { health, stats, logEntries, lastInit, refresh, fetchLogs, initWorkspace } = useMonitorStore();
fetchLogs();

const healthLabel = computed(() =>
  health.value === "ok" ? "健康" : health.value === "degraded" ? "告警" : "不可用"
);


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
  return date.toLocaleString();
}

function formatInterval(seconds?: number) {
  if (seconds === undefined || seconds === null) return "-";
  return `${seconds}s`;
}
</script>

