<template>
  <section>
    <h2 class="section-title">{{ t("cron.title") }}</h2>

    <div class="card" style="margin-bottom: 20px">
      <h3>{{ t("cron.listTitle") }}</h3>
      <div v-if="loading" style="color: var(--muted); font-size: 12px">{{ t("common.loading") }}</div>
      <div v-else-if="jobs.length === 0" style="color: var(--muted); font-size: 12px">{{ t("cron.empty") }}</div>
      <div v-else class="job-list">
        <div class="job-item" v-for="job in jobs" :key="job.id">
          <div class="job-header">
            <div>
              <div class="job-title">{{ job.name }}</div>
              <div class="job-meta">
                <span>{{ scheduleTypeLabel(job.scheduleType) }}</span>
                <span v-if="job.scheduleType === 'every'">{{ t("cron.everySeconds", { count: job.everySeconds ?? 0 }) }}</span>
                <span v-if="job.scheduleType === 'cron'">{{ job.cronExpr }}</span>
                <span v-if="job.scheduleType === 'at'">{{ job.runAt }}</span>
              </div>
            </div>
            <div class="job-actions">
              <button class="button secondary" @click="toggleJob(job)">
                {{ job.enabled ? t("common.disable") : t("common.enable") }}
              </button>
              <button class="button danger" @click="removeJob(job)">{{ t("common.delete") }}</button>
            </div>
          </div>
          <div class="job-body">
            <div class="job-field"><strong>{{ t("cron.promptLabel") }}</strong> {{ job.prompt }}</div>
            <div class="job-field"><strong>{{ t("cron.sessionLabel") }}</strong> {{ job.sessionKey }}</div>
            <div class="job-field"><strong>{{ t("cron.deliverLabel") }}</strong> {{ job.deliver ? t("common.yes") : t("common.no") }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="card">
      <h3>{{ t("cron.newTitle") }}</h3>
      <div class="form-grid">
        <div class="form-field">
          <label>{{ t("cron.name") }}</label>
          <input v-model="draft.name" placeholder="cron-job" />
        </div>
        <div class="form-field">
          <label>{{ t("cron.sessionKey") }}</label>
          <input v-model="draft.sessionKey" placeholder="cron" />
        </div>
        <div class="form-field">
          <label>{{ t("cron.type") }}</label>
          <select v-model="draft.scheduleType">
            <option value="every">{{ t("cron.type.every") }}</option>
            <option value="cron">{{ t("cron.type.cron") }}</option>
            <option value="at">{{ t("cron.type.at") }}</option>
          </select>
        </div>
        <div class="form-field" v-if="draft.scheduleType === 'every'">
          <label>{{ t("cron.intervalSeconds") }}</label>
          <input v-model.number="draft.intervalSeconds" type="number" min="5" />
        </div>
        <div class="form-field" v-if="draft.scheduleType === 'cron'">
          <label>{{ t("cron.cronExpr") }}</label>
          <input v-model="draft.cronExpr" placeholder="0 */1 * * *" />
        </div>
        <div class="form-field" v-if="draft.scheduleType === 'at'">
          <label>{{ t("cron.runAt") }}</label>
          <input v-model="draft.runAt" placeholder="2026-02-23T10:30:00Z" />
        </div>
        <div class="form-field" style="grid-column: 1 / -1">
          <label>{{ t("cron.prompt") }}</label>
          <input v-model="draft.prompt" :placeholder="t('cron.promptPlaceholder')" />
        </div>
        <div class="form-field">
          <label>{{ t("cron.deliver") }}</label>
          <select v-model="draft.deliver">
            <option :value="true">{{ t("common.yes") }}</option>
            <option :value="false">{{ t("common.no") }}</option>
          </select>
        </div>
        <div class="form-field">
          <label>{{ t("cron.to") }}</label>
          <input v-model="draft.to" :placeholder="t('common.optional')" />
        </div>
        <div class="form-field">
          <label>{{ t("cron.channel") }}</label>
          <input v-model="draft.channel" :placeholder="t('common.optional')" />
        </div>
      </div>
      <div style="margin-top: 12px; display: flex; gap: 10px">
        <button class="button" @click="createJob">{{ t("cron.create") }}</button>
        <button class="button secondary" @click="resetDraft">{{ t("common.reset") }}</button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from "vue";
import { getApiBaseUrl } from "../store/config";
import { useI18n } from "../i18n";

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

const { t } = useI18n();
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
    alert(t("cron.promptRequired"));
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
    alert(t("cron.createFailed"));
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

function scheduleTypeLabel(value: string) {
  if (value === "every") return t("cron.type.every");
  if (value === "cron") return t("cron.type.cron");
  if (value === "at") return t("cron.type.at");
  return value;
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
