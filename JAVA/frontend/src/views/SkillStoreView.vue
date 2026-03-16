<template>
  <section class="skills-view">
    <div class="header">
      <div>
        <h2 class="section-title">{{ t("skillStore.title") }}</h2>
        <p class="subtitle">{{ t("skillStore.subtitle") }}</p>
      </div>
      <div class="header-actions">
        <input v-model="search" class="search" :placeholder="t('skillStore.searchPlaceholder')" />
        <select v-model="statusFilter" class="select">
          <option value="">{{ t("skillStore.allStatus") }}</option>
          <option value="available">{{ t("skillStore.status.available") }}</option>
          <option value="installed">{{ t("skillStore.status.installed") }}</option>
          <option value="update_available">{{ t("skillStore.status.update_available") }}</option>
          <option value="conflict">{{ t("skillStore.status.conflict") }}</option>
          <option value="invalid">{{ t("skillStore.status.invalid") }}</option>
        </select>
        <select v-model="originFilter" class="select">
          <option value="">{{ t("skillStore.allOrigins") }}</option>
          <option v-for="origin in originOptions" :key="origin" :value="origin">{{ origin }}</option>
        </select>
        <button class="btn" @click="refresh" :disabled="loading">{{ t("common.refresh") }}</button>
      </div>
    </div>

    <div class="content">
      <div class="summary">
        <div class="pill pill-primary">{{ t("skillStore.totalCount", { count: items.length }) }}</div>
        <div v-if="availableCount" class="pill pill-success">{{ t("skillStore.availableCount", { count: availableCount }) }}</div>
        <div v-if="updateCount" class="pill pill-warning">{{ t("skillStore.updateCount", { count: updateCount }) }}</div>
        <div v-if="invalidCount" class="pill pill-warning">{{ t("skillStore.invalidCount", { count: invalidCount }) }}</div>
      </div>

      <div v-if="loading" class="muted center">{{ t("common.loading") }}</div>
      <div v-else-if="!filteredItems.length" class="muted center">{{ t("skillStore.empty") }}</div>
      <div v-else class="grid">
        <div v-for="item in filteredItems" :key="item.id" class="card">
          <div class="card-header">
            <div class="title-row">
              <div class="skill-name">{{ item.name }}</div>
              <span class="pill" :class="statusPillClass(item.status)">{{ statusLabel(item.status) }}</span>
            </div>
            <div class="meta">{{ t("skills.sourceLabel", { value: item.origin || t("common.unknown") }) }}</div>
          </div>
          <div class="desc">{{ item.description || t("common.noDescription") }}</div>
          <div class="meta-row">
            <span>{{ t("common.version") }}：{{ item.version || t("common.unknown") }}</span>
            <span>{{ t("skillStore.updatedAt", { time: formatTime(item.updatedAt) }) }}</span>
          </div>

          <div class="actions">
            <button class="btn small" @click="openDetail(item.id)" :disabled="loading">{{ t("common.viewDetails") }}</button>
            <button class="btn small primary" @click="importSkill(item.id)" :disabled="loading || item.status === 'installed'">{{ t("common.import") }}</button>
            <button class="btn small" @click="ignoreSkill(item.id)" :disabled="loading">{{ t("common.ignore") }}</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="detail" class="detail-mask" @click.self="closeDetail">
      <div class="detail-panel">
        <div class="detail-header">
          <div>
            <h3>{{ detail.skill.name }}</h3>
            <p class="muted">{{ detail.skill.description || t("common.noDescription") }}</p>
          </div>
          <button class="btn small" @click="closeDetail">{{ t("common.close") }}</button>
        </div>
        <div class="detail-meta">
          <div>{{ t("common.version") }}：{{ detail.skill.version || t("common.unknown") }}</div>
          <div>{{ t("skills.sourceLabel", { value: detail.skill.origin || t("common.unknown") }) }}</div>
          <div>{{ t("skills.statusLabel", { value: statusLabel(detail.skill.status) }) }}</div>
        </div>
        <div class="detail-body">
          <div class="detail-section">
            <div class="detail-title">{{ t("skills.readmeTitle") }}</div>
            <pre class="detail-content">{{ detail.content || t("common.noContent") }}</pre>
          </div>
          <div class="detail-section">
            <div class="detail-title">{{ t("skillStore.fileList") }}</div>
            <ul class="file-list">
              <li v-for="file in detail.files" :key="file">{{ file }}</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useI18n } from '../i18n';

interface StoreSkillItem {
  id: string;
  name: string;
  description?: string;
  version?: string;
  hash?: string;
  origin?: string;
  scope?: string;
  updatedAt: number;
  size: number;
  status: string;
  checksumOk: boolean;
}

interface StoreSkillDetail {
  skill: StoreSkillItem;
  content: string;
  files: string[];
}

const { t, locale } = useI18n();
const items = ref<StoreSkillItem[]>([]);
const loading = ref(false);
const search = ref('');
const statusFilter = ref('');
const originFilter = ref('');
const detail = ref<StoreSkillDetail | null>(null);

const filteredItems = computed(() => {
  const kw = search.value.trim().toLowerCase();
  return items.value.filter(item => {
    if (statusFilter.value && item.status !== statusFilter.value) return false;
    if (originFilter.value && (item.origin || '') !== originFilter.value) return false;
    if (!kw) return true;
    const name = (item.name || '').toLowerCase();
    const desc = (item.description || '').toLowerCase();
    return name.includes(kw) || desc.includes(kw);
  });
});

const originOptions = computed(() => {
  const set = new Set<string>();
  items.value.forEach(item => {
    if (item.origin) set.add(item.origin);
  });
  return Array.from(set.values());
});

const availableCount = computed(() => items.value.filter(i => i.status === 'available').length);
const updateCount = computed(() => items.value.filter(i => i.status === 'update_available').length);
const invalidCount = computed(() => items.value.filter(i => i.status === 'invalid').length);

onMounted(() => {
  refresh();
});

async function refresh() {
  loading.value = true;
  try {
    const res = await fetch('/api/store/skills');
    if (!res.ok) throw new Error(await res.text());
    items.value = await res.json();
  } finally {
    loading.value = false;
  }
}

async function openDetail(id: string) {
  loading.value = true;
  try {
    const res = await fetch(`/api/store/skills/${encodeURIComponent(id)}`);
    if (!res.ok) throw new Error(await res.text());
    detail.value = await res.json();
  } finally {
    loading.value = false;
  }
}

function closeDetail() {
  detail.value = null;
}

async function importSkill(id: string) {
  loading.value = true;
  try {
    const res = await fetch(`/api/store/skills/${encodeURIComponent(id)}/import`, { method: 'POST' });
    if (!res.ok) throw new Error(await res.text());
    await refresh();
  } finally {
    loading.value = false;
  }
}

async function ignoreSkill(id: string) {
  loading.value = true;
  try {
    const res = await fetch(`/api/store/skills/${encodeURIComponent(id)}/ignore`, { method: 'POST' });
    if (!res.ok) throw new Error(await res.text());
    await refresh();
  } finally {
    loading.value = false;
  }
}

function statusLabel(status: string) {
  switch (status) {
    case 'available':
      return t('skillStore.status.available');
    case 'installed':
      return t('skillStore.status.installed');
    case 'update_available':
      return t('skillStore.status.update_available');
    case 'conflict':
      return t('skillStore.status.conflict');
    case 'invalid':
      return t('skillStore.status.invalid');
    default:
      return status || t('common.unknown');
  }
}

function statusPillClass(status: string) {
  if (status === 'installed') return 'pill-success';
  if (status === 'available') return 'pill-primary';
  if (status === 'update_available') return 'pill-warning';
  if (status === 'invalid') return 'pill-warning';
  if (status === 'conflict') return 'pill-warning';
  return 'pill';
}

function formatTime(ts: number) {
  if (!ts) return '-';
  return new Date(ts).toLocaleString(locale.value);
}
</script>


<style scoped>
.skills-view {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
}

.section-title {
  margin: 0;
  font-size: 20px;
}

.subtitle {
  margin: 4px 0 0 0;
  color: var(--muted);
  font-size: 13px;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.search,
.select {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--hover);
  color: inherit;
  min-width: 200px;
}

.content {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}

.summary {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}

.card {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.03);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.skill-name {
  font-weight: 700;
  font-size: 16px;
}

.meta {
  color: var(--muted);
  font-size: 12px;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  color: var(--muted);
  font-size: 12px;
}

.desc {
  font-size: 13px;
  color: var(--muted);
  line-height: 1.5;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.btn {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--hover);
  color: inherit;
  cursor: pointer;
}

.btn.small {
  padding: 6px 10px;
  font-size: 12px;
}

.btn.primary {
  border-color: var(--primary);
  color: var(--primary);
}

.pill {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid var(--border);
}

.pill-primary {
  border-color: var(--primary);
  color: var(--primary);
}

.pill-success {
  color: #10b981;
  border-color: #10b981;
}

.pill-warning {
  color: #f59e0b;
  border-color: #f59e0b;
}

.muted {
  color: var(--muted);
  font-size: 13px;
}

.center {
  text-align: center;
  padding: 20px 0;
}

.detail-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 10;
}

.detail-panel {
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 12px;
  width: min(920px, 92vw);
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  overflow: hidden;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.detail-meta {
  display: flex;
  gap: 16px;
  color: var(--muted);
  font-size: 12px;
}

.detail-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  overflow: hidden;
}

.detail-section {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.detail-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.detail-content {
  flex: 1;
  margin: 0;
  white-space: pre-wrap;
  font-size: 12px;
  color: var(--muted);
  overflow: auto;
}

.file-list {
  margin: 0;
  padding-left: 16px;
  color: var(--muted);
  font-size: 12px;
  overflow: auto;
}

@media (max-width: 900px) {
  .header { flex-direction: column; align-items: flex-start; gap: 10px; }
  .grid { grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); }
  .detail-body { grid-template-columns: 1fr; }
  .detail-meta { flex-direction: column; gap: 6px; }
  .search, .select { min-width: 160px; }
}
</style>
