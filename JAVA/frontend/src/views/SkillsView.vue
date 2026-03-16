<template>
  <section class="skills-view">
    <div class="header">
      <div>
        <h2 class="section-title">{{ t("skills.title") }}</h2>
        <p class="subtitle">{{ t("skills.subtitle") }}</p>
      </div>
      <div class="header-actions">
        <input v-model="search" class="search" :placeholder="t('skills.searchPlaceholder')" />
        <button class="btn" @click="refresh" :disabled="loading">{{ t("common.refresh") }}</button>
      </div>
    </div>

    <div class="content">
      <div class="summary">
        <div class="pill pill-primary">{{ t("skills.totalCount", { count: skillsData?.available?.length || 0 }) }}</div>
        <div v-if="blockedCount" class="pill pill-warning">{{ t("skills.blockedCount", { count: blockedCount }) }}</div>
        <div v-if="installableCount" class="pill pill-warning">{{ t("skills.installableCount", { count: installableCount }) }}</div>
      </div>

      <div v-if="loading" class="muted center">{{ t("common.loading") }}</div>
      <div v-else-if="!filteredSkills.length" class="muted center">{{ t("skills.empty") }}</div>
      <div v-else class="grid">
        <div v-for="skill in filteredSkills" :key="skill.name" class="card">
          <div class="card-header">
            <div class="title-row">
              <div class="skill-name">{{ skill.name }}</div>
              <span class="pill" :class="skill.blocked ? 'pill-warning' : 'pill-success'">
                {{ skill.blocked ? t("common.blocked") : t("common.available") }}
              </span>
              <span v-if="canInstall(skill)" class="pill pill-warning">{{ t("common.installable") }}</span>
            </div>
            <div class="meta">{{ t("skills.sourceLabel", { value: skill.source || t("common.unknown") }) }}</div>
          </div>
          <div class="desc">{{ skill.description || t("common.noDescription") }}</div>
          <div v-if="missingText(skill)" class="missing">{{ t("skills.missing", { value: missingText(skill) }) }}</div>

          <div class="actions">
            <div class="inline">
              <label class="toggle">
                <input type="checkbox" :checked="isSkillEnabled(skill.name)" @change="onToggleSkill(skill.name, $event)" />
                <span>{{ t("skills.enable") }}</span>
              </label>
              <button class="btn small" @click="openDetail(skill)" :disabled="loading">{{ t("common.viewDetails") }}</button>
              <button v-if="canInstall(skill)" class="btn small" @click="onInstallSkill(skill)" :disabled="loading">{{ installLabel(skill) }}</button>
            </div>

            <div v-if="skill.primaryEnv" class="field">
              <input
                type="password"
                :value="apiKeyDraft[skill.name] || ''"
                @input="e => onEditApiKey(skill.name, (e.target as HTMLInputElement).value)"
                :placeholder="t('skills.apiKeyPlaceholder', { env: skill.primaryEnv })"
              />
              <button class="btn small" @click="onSaveSkill(skill.name)" :disabled="loading || saving === skill.name">{{ t("common.save") }}</button>
            </div>
            <div class="field">
              <textarea
                class="env"
                :value="envDraft[skill.name] || ''"
                @input="e => onEditEnv(skill.name, (e.target as HTMLTextAreaElement).value)"
                :placeholder="t('skills.envPlaceholder')"
                rows="3"
              ></textarea>
            </div>
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
          <div>{{ t("skills.sourceLabel", { value: detail.skill.source || t("common.unknown") }) }}</div>
          <div>{{ t("skills.statusLabel", { value: detail.skill.blocked ? t("common.blocked") : t("common.available") }) }}</div>
        </div>
        <div class="detail-body">
          <div class="detail-section">
            <div class="detail-title">{{ t("skills.readmeTitle") }}</div>
            <pre class="detail-content">{{ detail.content || t("common.noContent") }}</pre>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>


<script setup lang="ts">
import { computed, onMounted, reactive, ref, toRefs, watch } from 'vue';
import { useAgentStore } from '../store/agents';
import { useI18n } from '../i18n';

const agentStore = useAgentStore();
const { t } = useI18n();
const loading = ref(false);
const saving = ref<string | null>(null);
const search = ref('');
const currentAgentId = ref<string>('default');

const state = reactive({
  apiKeyDraft: {} as Record<string, string>,
  envDraft: {} as Record<string, string>
});
const { apiKeyDraft, envDraft } = toRefs(state);

const skillsData = computed(() => agentStore.agentSkills.get(currentAgentId.value) || null);
const detail = ref<{ skill: any; content: string } | null>(null);

const filteredSkills = computed(() => {
  const list = skillsData.value?.available || [];
  const kw = search.value.trim().toLowerCase();
  if (!kw) return list;
  return list.filter(s => (s.name || '').toLowerCase().includes(kw) || (s.description || '').toLowerCase().includes(kw));
});

const blockedCount = computed(() =>
  (skillsData.value?.available || []).filter(skill => skill.blocked).length
);

const installableCount = computed(() =>
  (skillsData.value?.available || []).filter(skill => canInstall(skill)).length
);

watch(
  skillsData,
  val => {
    if (!val) return;
    const apiKey: Record<string, string> = {};
    const env: Record<string, string> = {};
    Object.entries(val.entries || {}).forEach(([k, v]) => {
      if (v.apiKey) apiKey[k] = v.apiKey;
      if (v.env) env[k] = JSON.stringify(v.env, null, 2);
    });
    apiKeyDraft.value = apiKey;
    envDraft.value = env;
  },
  { immediate: true }
);

onMounted(async () => {
  loading.value = true;
  try {
    await agentStore.fetchAgents();
    if (!agentStore.currentAgent.value && agentStore.agents.value.length) {
      currentAgentId.value = agentStore.agents.value[0].id;
      agentStore.switchToAgent(currentAgentId.value);
    } else {
      currentAgentId.value = agentStore.currentAgentId.value || 'default';
    }
    await agentStore.fetchAgentSkills(currentAgentId.value);
  } finally {
    loading.value = false;
  }
});

async function refresh() {
  loading.value = true;
  try {
    await agentStore.fetchAgentSkills(currentAgentId.value);
    await agentStore.fetchAgents();
  } finally {
    loading.value = false;
  }
}

function canInstall(skill: any) {
  const missingBins = skill?.missing?.bins || [];
  const installs = skill?.install || [];
  return missingBins.length > 0 && installs.length > 0;
}

function installLabel(skill: any) {
  const option = skill?.install?.[0];
  return option?.label || t('common.install');
}

function missingText(skill: any) {
  const missing = skill?.missing;
  if (!missing) return '';
  const parts: string[] = [];
  if (missing.bins?.length) parts.push(`bins:${missing.bins.join(', ')}`);
  if (missing.anyBins?.length) parts.push(`anyBins:${missing.anyBins.join(', ')}`);
  if (missing.env?.length) parts.push(`env:${missing.env.join(', ')}`);
  if (missing.config?.length) parts.push(`config:${missing.config.join(', ')}`);
  if (missing.os?.length) parts.push(`os:${missing.os.join(', ')}`);
  return parts.join(' / ');
}

function isSkillEnabled(name: string) {
  const entry = skillsData.value?.entries?.[name];
  if (entry && typeof entry.enabled === 'boolean') return entry.enabled;
  return true;
}

function onEditApiKey(name: string, value: string) {
  apiKeyDraft.value = { ...apiKeyDraft.value, [name]: value };
}

function onEditEnv(name: string, value: string) {
  envDraft.value = { ...envDraft.value, [name]: value };
}

async function onToggleSkill(name: string, evt: Event) {
  const checked = (evt.target as HTMLInputElement).checked;
  if (!currentAgentId.value) return;
  loading.value = true;
  try {
    await agentStore.updateAgentSkillEntry(currentAgentId.value, name, { enabled: checked });
  } finally {
    loading.value = false;
  }
}

async function onSaveSkill(name: string) {
  if (!currentAgentId.value) return;
  saving.value = name;
  const patch: any = { apiKey: apiKeyDraft.value[name] || '' };
  const envText = envDraft.value[name];
  if (envText && envText.trim()) {
    try {
      patch.env = JSON.parse(envText);
    } catch (e) {
      alert(t('skills.envInvalid'));
      saving.value = null;
      return;
    }
  }
  try {
    await agentStore.updateAgentSkillEntry(currentAgentId.value, name, patch);
  } finally {
    saving.value = null;
  }
}

async function onInstallSkill(skill: any) {
  if (!currentAgentId.value) return;
  const installId = skill?.install?.[0]?.id;
  loading.value = true;
  try {
    await agentStore.installSkill(currentAgentId.value, skill.name, installId);
  } catch (e) {
    console.error(e);
  } finally {
    await refresh();
  }
}

async function openDetail(skill: any) {
  if (!skill?.name) return;
  loading.value = true;
  try {
    const res = await fetch(`/api/skills/detail?name=${encodeURIComponent(skill.name)}&agentId=${encodeURIComponent(currentAgentId.value)}`);
    if (!res.ok) throw new Error(await res.text());
    const data = await res.json();
    detail.value = {
      skill,
      content: data?.content || ''
    };
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
}

function closeDetail() {
  detail.value = null;
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
}

.search {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--hover);
  color: inherit;
  min-width: 240px;
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

.desc {
  font-size: 13px;
  color: var(--muted);
  line-height: 1.5;
}

.missing {
  font-size: 12px;
  color: #f59e0b;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.inline {
  display: flex;
  gap: 10px;
  align-items: center;
}

.toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.field {
  display: flex;
  gap: 8px;
}

.field input {
  flex: 1;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg);
  color: inherit;
}

.field .env {
  width: 100%;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg);
  color: inherit;
  padding: 8px 10px;
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
  grid-template-columns: 1fr;
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

@media (max-width: 900px) {
  .header { flex-direction: column; align-items: flex-start; gap: 10px; }
  .grid { grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); }
  .field { flex-direction: column; }
  .field input { width: 100%; }
  .detail-meta { flex-direction: column; gap: 6px; }
}
</style>

