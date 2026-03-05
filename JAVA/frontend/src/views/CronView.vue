<template>
  <section>
    <h2 class="section-title">定时任务</h2>

    <div class="card" style="margin-bottom: 20px">
      <h3>任务列表</h3>
      <div v-if="loading" style="color: var(--muted); font-size: 12px">加载中...</div>
      <div v-else-if="jobs.length === 0" style="color: var(--muted); font-size: 12px">暂无任务</div>
      <div v-else class="job-list">
        <div class="job-item" v-for="job in jobs" :key="job.id">
          <div class="job-header">
            <div>
              <div class="job-title">{{ job.name }}</div>
              <div class="job-meta">
                <span>{{ job.scheduleType }}</span>
                <span v-if="job.scheduleType === 'every'">每 {{ job.everySeconds }} 秒</span>
                <span v-if="job.scheduleType === 'cron'">{{ job.cronExpr }}</span>
                <span v-if="job.scheduleType === 'at'">{{ job.runAt }}</span>
              </div>
            </div>
            <div class="job-actions">
              <button class="button secondary" @click="toggleJob(job)">
                {{ job.enabled ? "禁用" : "启用" }}
              </button>
              <button class="button danger" @click="removeJob(job)">删除</button>
            </div>
          </div>
          <div class="job-body">
            <div class="job-field"><strong>Prompt:</strong> {{ job.prompt }}</div>
            <div class="job-field"><strong>Session:</strong> {{ job.sessionKey }}</div>
            <div class="job-field"><strong>Deliver:</strong> {{ job.deliver ? "是" : "否" }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="card">
      <h3>新增任务</h3>
      <div class="form-grid">
        <div class="form-field">
          <label>名称</label>
          <input v-model="draft.name" placeholder="cron-job" />
        </div>
        <div class="form-field">
          <label>Session Key</label>
          <input v-model="draft.sessionKey" placeholder="cron" />
        </div>
        <div class="form-field">
          <label>类型</label>
          <select v-model="draft.scheduleType">
            <option value="every">每隔</option>
            <option value="cron">Cron 表达式</option>
            <option value="at">指定时间</option>
          </select>
        </div>
        <div class="form-field" v-if="draft.scheduleType === 'every'">
          <label>间隔（秒）</label>
          <input v-model.number="draft.intervalSeconds" type="number" min="5" />
        </div>
        <div class="form-field" v-if="draft.scheduleType === 'cron'">
          <label>Cron 表达式</label>
          <input v-model="draft.cronExpr" placeholder="0 */1 * * *" />
        </div>
        <div class="form-field" v-if="draft.scheduleType === 'at'">
          <label>执行时间（ISO-8601）</label>
          <input v-model="draft.runAt" placeholder="2026-02-23T10:30:00Z" />
        </div>
        <div class="form-field" style="grid-column: 1 / -1">
          <label>Prompt</label>
          <input v-model="draft.prompt" placeholder="请输入任务内容" />
        </div>
        <div class="form-field">
          <label>Deliver</label>
          <select v-model="draft.deliver">
            <option :value="true">是</option>
            <option :value="false">否</option>
          </select>
        </div>
        <div class="form-field">
          <label>To</label>
          <input v-model="draft.to" placeholder="可选" />
        </div>
        <div class="form-field">
          <label>Channel</label>
          <input v-model="draft.channel" placeholder="可选" />
        </div>
      </div>
      <div style="margin-top: 12px; display: flex; gap: 10px">
        <button class="button" @click="createJob">创建任务</button>
        <button class="button secondary" @click="resetDraft">重置</button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from "vue";
import { getApiBaseUrl } from "../store/config";

interface CronJobItem {
  id: string;
  name: string;
  scheduleType: string;
  everySeconds?: number | null;
  cronExpr?: string | null;
  runAt?: string | null;
  prompt: string;
  sessionKey: string;
  deliver: boolean;
  to?: string | null;
  channel?: string | null;
  enabled: boolean;
}

const loading = ref(false);
const jobs = ref<CronJobItem[]>([]);

const draft = reactive({
  name: "cron-job",
  sessionKey: "cron",
  scheduleType: "every",
  intervalSeconds: 3600,
  cronExpr: "",
  runAt: "",
  prompt: "",
  deliver: false,
  to: "",
  channel: ""
});

async function fetchJobs() {
  const baseUrl = getApiBaseUrl();
  if (!baseUrl) return;
  loading.value = true;
  try {
    const res = await fetch(`${baseUrl}/api/cron/jobs`);
    if (res.ok) {
      jobs.value = await res.json();
    }
  } finally {
    loading.value = false;
  }
}

async function createJob() {
  const baseUrl = getApiBaseUrl();
  if (!baseUrl) return;
  if (!draft.prompt.trim()) {
    alert("Prompt 不能为空");
    return;
  }

  const payload: any = {
    intervalSeconds: Math.max(5, Number(draft.intervalSeconds || 0)),
    prompt: draft.prompt,
    sessionKey: draft.sessionKey,
    name: draft.name,
    cronExpr: draft.scheduleType === "cron" ? draft.cronExpr : "",
    runAt: draft.scheduleType === "at" ? draft.runAt : "",
    deliver: draft.deliver,
    to: draft.to,
    channel: draft.channel
  };

  const res = await fetch(`${baseUrl}/api/cron/schedule`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  if (res.ok) {
    resetDraft();
    await fetchJobs();
  } else {
    alert("创建失败，请检查参数");
  }
}

async function toggleJob(job: CronJobItem) {
  const baseUrl = getApiBaseUrl();
  if (!baseUrl) return;
  const res = await fetch(`${baseUrl}/api/cron/enable`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ id: job.id, enabled: !job.enabled })
  });
  if (res.ok) {
    await fetchJobs();
  }
}

async function removeJob(job: CronJobItem) {
  const baseUrl = getApiBaseUrl();
  if (!baseUrl) return;
  const res = await fetch(`${baseUrl}/api/cron/${job.id}`, { method: "DELETE" });
  if (res.ok) {
    await fetchJobs();
  }
}

function resetDraft() {
  draft.name = "cron-job";
  draft.sessionKey = "cron";
  draft.scheduleType = "every";
  draft.intervalSeconds = 3600;
  draft.cronExpr = "";
  draft.runAt = "";
  draft.prompt = "";
  draft.deliver = false;
  draft.to = "";
  draft.channel = "";
}

onMounted(fetchJobs);
</script>

<style scoped>
.job-list {
  display: grid;
  gap: 12px;
}

.job-item {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.02);
}

.job-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.job-title {
  font-weight: 600;
}

.job-meta {
  font-size: 12px;
  color: var(--muted);
  display: flex;
  gap: 10px;
}

.job-actions {
  display: flex;
  gap: 8px;
}

.job-body {
  margin-top: 8px;
  display: grid;
  gap: 6px;
  font-size: 12px;
  color: var(--muted);
}

.button.danger {
  background: rgba(255, 99, 71, 0.2);
  border: 1px solid rgba(255, 99, 71, 0.4);
  color: #ffb3a6;
}
</style>
