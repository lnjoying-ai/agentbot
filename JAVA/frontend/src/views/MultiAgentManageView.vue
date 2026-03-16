<template>
  <section class="agent-manage">
    <h2 class="section-title">{{ t("agent.manage.title") }}</h2>

    <div class="card" style="margin-bottom: 16px">
      <div class="toolbar">
        <div class="toolbar-actions">
          <button class="button" @click="newAgent">{{ t("agent.manage.new") }}</button>
          <button class="button secondary" @click="refresh" :disabled="agentStore.loading.value">
            {{ t("agent.manage.refreshList") }}
          </button>
        </div>
        <div class="toolbar-status" v-if="agentStore.error.value">
          {{ agentStore.error.value }}
        </div>
      </div>
    </div>

    <div class="manage-grid">
      <div class="card list-panel">
        <div class="list-header">
          <h3>{{ t("agent.listTitle") }}</h3>
          <div class="search-box">
            <input v-model="searchTerm" :placeholder="t('agent.searchPlaceholder')" />
          </div>
        </div>
        <div v-if="agentStore.loading.value" class="muted">{{ t("common.loading") }}</div>
        <div v-else class="agent-list">
          <button
            v-for="agent in filteredAgents"
            :key="agent.id"
            class="agent-item"
            :class="{ active: selectedId === agent.id }"
            @click="selectAgent(agent.id)"
          >
            <div class="agent-title">
              <strong>{{ agent.displayName || agent.name || agent.id }}</strong>
              <span v-if="agent.enabled === false" class="pill">{{ t("agent.status.disabled") }}</span>
              <span v-else-if="agent.healthy === false" class="pill warning">{{ t("agent.status.error") }}</span>
              <span v-else class="pill">{{ t("agent.status.ok") }}</span>
            </div>
            <div class="agent-meta">
              <span>{{ agent.id }}</span>
              <span v-if="agent.updatedAt">{{ t("agent.updatedAt", { time: agent.updatedAt }) }}</span>
            </div>
          </button>
          <div v-if="!filteredAgents.length" class="muted">{{ t("agent.manage.empty") }}</div>
        </div>
      </div>

      <div class="card form-panel">
        <h3>{{ isEditing ? t("agent.manage.editTitle") : t("agent.manage.createTitle") }}</h3>
        <div class="form-grid">
          <div class="form-field">
            <label>{{ t("agent.dialog.id") }}</label>
            <input v-model="draft.id" :disabled="isEditing" placeholder="planner" />
          </div>
          <div class="form-field">
            <label>{{ t("agent.dialog.name") }}</label>
            <input v-model="draft.name" placeholder="Planner" />
          </div>
          <div class="form-field">
            <label>{{ t("agent.manage.displayName") }}</label>
            <input v-model="draft.displayName" :placeholder="t('agent.manage.displayNamePlaceholder')" />
          </div>
          <div class="form-field">
            <label>{{ t("agent.manage.avatarUrl") }}</label>
            <input v-model="draft.avatar" placeholder="https://..." />
          </div>
          <div class="form-field">
            <label>{{ t("agent.manage.enabledState") }}</label>
            <select v-model="draft.enabled">
              <option :value="true">{{ t("common.enable") }}</option>
              <option :value="false">{{ t("common.disable") }}</option>
            </select>
          </div>
          <div class="form-field">
            <label>{{ t("agent.dialog.description") }}</label>
            <textarea v-model="draft.description" rows="3" :placeholder="t('agent.manage.descriptionPlaceholder')"></textarea>
          </div>
        </div>

        <div class="card" style="margin-top: 16px">
          <h3>{{ t("agent.manage.routingTitle") }}</h3>
          <div class="form-grid">
            <div class="form-field">
              <label>{{ t("agent.manage.keywords") }}</label>
              <input v-model="keywordsText" :placeholder="t('agent.manage.keywordsPlaceholder')" />
            </div>
            <div class="form-field">
              <label>{{ t("agent.manage.channels") }}</label>
              <input v-model="channelsText" :placeholder="t('agent.manage.channelsPlaceholder')" />
            </div>
            <div class="form-field">
              <label>{{ t("agent.manage.priority") }}</label>
              <input v-model.number="draft.routing.priority" type="number" min="0" />
            </div>
            <div class="form-field">
              <label>{{ t("agent.manage.autoRoute") }}</label>
              <select v-model="draft.routing.autoRoute">
                <option :value="true">{{ t("common.enable") }}</option>
                <option :value="false">{{ t("common.disable") }}</option>
              </select>
            </div>
          </div>
        </div>

        <div class="card" style="margin-top: 16px">
          <h3>{{ t("agent.manage.capabilitiesTitle") }}</h3>
          <div class="form-grid">
            <div class="form-field">
              <label>{{ t("agent.manage.inheritedTools") }}</label>
              <input v-model="toolsInheritedText" placeholder="echo,time_now" />
            </div>
            <div class="form-field">
              <label>{{ t("agent.manage.disabledTools") }}</label>
              <input v-model="toolsDisabledText" placeholder="shell" />
            </div>
            <div class="form-field">
              <label>{{ t("agent.manage.customTools") }}</label>
              <input v-model="toolsCustomText" placeholder="custom_tool" />
            </div>
          </div>

          <div class="skills-section">
            <div class="skills-header">
              <div>
                <div class="label">{{ t("agent.manage.currentAgent") }}</div>
                <div class="title">{{ selectedAgentName }}</div>
              </div>
              <div class="status" :class="statusClass">{{ statusText }}</div>
            </div>

            <div class="form-grid">
              <div class="form-field">
                <label>{{ t("agent.manage.inheritSystemSkills") }}</label>
                <select v-model="skillInherited">
                  <option :value="true">{{ t("agent.manage.inherit") }}</option>
                  <option :value="false">{{ t("agent.manage.noInherit") }}</option>
                </select>
              </div>
              <div class="form-field">
                <label>{{ t("agent.manage.customSkillPath") }}</label>
                <input v-model="skillCustomPath" placeholder="skills/" />
              </div>
              <div class="form-field" style="align-self: flex-end">
                <button class="button" @click="saveSkillSettings" :disabled="skillsLoading || !selectedId">{{ t("agent.manage.saveSkillSettings") }}</button>
              </div>
            </div>

            <div class="skills-list-card">
              <div class="skills-list-header">
                <div class="title">{{ t("agent.manage.skillList") }}</div>
                <button class="button secondary" @click="refreshSkills" :disabled="skillsLoading || !selectedId">{{ t("common.reload") }}</button>
              </div>

              <div v-if="skillsLoading" class="muted">{{ t("common.loading") }}</div>
              <div v-else-if="skillsData?.available?.length" class="skill-list">
                <div v-if="blockedCount || installableCount" class="muted">
                  <span v-if="blockedCount">{{ t("skills.blockedCount", { count: blockedCount }) }}</span>
                  <span v-if="blockedCount && installableCount"> · </span>
                  <span v-if="installableCount">{{ t("skills.installableCount", { count: installableCount }) }}</span>
                </div>

                <div v-for="skill in skillsData.available" :key="skill.name" class="skill-row">
                  <div class="skill-main">
                    <div class="skill-name">
                      {{ skill.name }}
                      <span class="pill" :class="skill.blocked ? 'pill-warning' : 'pill-success'">
                        {{ skill.blocked ? t("common.blocked") : t("common.available") }}
                      </span>
                      <span v-if="canInstall(skill)" class="pill pill-warning">{{ t("common.installable") }}</span>
                    </div>
                    <div class="skill-desc">{{ skill.description || t("common.noDescription") }}</div>
                    <div v-if="missingText(skill)" class="skill-missing">{{ t("skills.missing", { value: missingText(skill) }) }}</div>
                  </div>
                  <div class="skill-actions">
                    <div class="inline-actions">
                      <label class="toggle">
                        <input type="checkbox" :checked="isSkillEnabled(skill.name)" @change="onToggleSkill(skill.name, $event)" />
                        <span>{{ t("skills.enable") }}</span>
                      </label>
                      <button v-if="canInstall(skill)" class="button small" @click="onInstallSkill(skill)" :disabled="skillsLoading">{{ installLabel(skill) }}</button>
                    </div>
                    <div v-if="skill.primaryEnv" class="field">
                      <input
                        type="password"
                        :value="skillApiKeyDraft[skill.name] || ''"
                        @input="e => onEditSkillApiKey(skill.name, (e.target as HTMLInputElement).value)"
                        :placeholder="t('skills.apiKeyPlaceholder', { env: skill.primaryEnv })"
                      />
                      <button class="button small" @click="onSaveSkill(skill.name)" :disabled="skillsLoading || savingSkill === skill.name">{{ t("common.save") }}</button>
                    </div>
                    <div class="field">
                      <textarea
                        class="env"
                        :value="skillEnvDraft[skill.name] || ''"
                        @input="e => onEditSkillEnv(skill.name, (e.target as HTMLTextAreaElement).value)"
                        :placeholder="t('skills.envPlaceholder')"
                        rows="3"
                      ></textarea>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="muted">{{ t("skills.notFoundFiles") }}</div>
            </div>
          </div>
        </div>

        <div class="config-actions">
          <button class="button" @click="save" :disabled="saving">
            {{ isEditing ? t("agent.manage.saveChanges") : t("agent.manage.createTitle") }}
          </button>
          <button class="button secondary" @click="reset" :disabled="saving">{{ t("common.reset") }}</button>
          <button
            v-if="isEditing"
            class="button secondary"
            style="color: var(--danger)"
            @click="remove"
            :disabled="saving"
          >
            {{ t("agent.manage.delete") }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useAgentStore } from '../store/agents';
import { useI18n } from '../i18n';

const agentStore = useAgentStore();
const { t } = useI18n();
const selectedId = ref<string | null>(null);
const saving = ref(false);
const skillsLoading = ref(false);
const savingSkill = ref<string | null>(null);
const searchTerm = ref('');

const skillInherited = ref(true);
const skillCustomPath = ref('');
const skillApiKeyDraft = ref<Record<string, string>>({});
const skillEnvDraft = ref<Record<string, string>>({});

const draft = reactive({
  id: '',
  name: '',
  displayName: '',
  description: '',
  avatar: '',
  enabled: true,
  routing: {
    keywords: [] as string[],
    channels: [] as string[],
    priority: 0,
    autoRoute: false
  },
  capabilities: {
    tools: {
      inherited: [] as string[],
      disabled: [] as string[],
      custom: [] as string[]
    },
    skills: {
      inherited: true,
      customPath: ''
    }
  }
});

const isEditing = computed(() => selectedId.value !== null);

const filteredAgents = computed(() => {
  const keyword = searchTerm.value.trim().toLowerCase();
  if (!keyword) return agentStore.agents.value;
  return agentStore.agents.value.filter((agent) => {
    const haystack = [agent.displayName, agent.name, agent.id, agent.description]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();
    return haystack.includes(keyword);
  });
});

const keywordsText = computed({
  get: () => draft.routing.keywords.join(', '),
  set: (val: string) => {
    draft.routing.keywords = parseList(val);
  }
});

const channelsText = computed({
  get: () => draft.routing.channels.join(', '),
  set: (val: string) => {
    draft.routing.channels = parseList(val);
  }
});

const toolsInheritedText = computed({
  get: () => draft.capabilities.tools.inherited.join(', '),
  set: (val: string) => {
    draft.capabilities.tools.inherited = parseList(val);
  }
});

const toolsDisabledText = computed({
  get: () => draft.capabilities.tools.disabled.join(', '),
  set: (val: string) => {
    draft.capabilities.tools.disabled = parseList(val);
  }
});

const toolsCustomText = computed({
  get: () => draft.capabilities.tools.custom.join(', '),
  set: (val: string) => {
    draft.capabilities.tools.custom = parseList(val);
  }
});

const selectedAgent = computed(() => agentStore.getAgent(selectedId.value || ''));
const selectedAgentName = computed(() => selectedAgent.value?.displayName || selectedAgent.value?.name || selectedAgent.value?.id || t('common.notSelected'));

const statusClass = computed(() => {
  if (!selectedAgent.value) return 'inactive';
  if (selectedAgent.value.enabled === false) return 'inactive';
  if (selectedAgent.value.healthy === false) return 'error';
  return 'active';
});

const statusText = computed(() => {
  if (!selectedAgent.value) return t('common.notSelected');
  if (selectedAgent.value.enabled === false) return t('agent.status.disabled');
  if (selectedAgent.value.healthy === false) return t('agent.status.error');
  return t('agent.status.ok');
});

const skillsData = computed(() => {
  if (!selectedId.value) return null;
  return agentStore.agentSkills.get(selectedId.value) || null;
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
    skillInherited.value = val.inherited ?? true;
    skillCustomPath.value = val.customPath ?? '';
    const apiKey: Record<string, string> = {};
    const env: Record<string, string> = {};
    Object.entries(val.entries || {}).forEach(([k, v]) => {
      if (v.apiKey) apiKey[k] = v.apiKey;
      if (v.env) env[k] = JSON.stringify(v.env, null, 2);
    });
    skillApiKeyDraft.value = apiKey;
    skillEnvDraft.value = env;
  },
  { immediate: true }
);

onMounted(async () => {
  await refresh();
  newAgent();
});

async function refresh() {
  await agentStore.fetchAgents();
}

async function refreshSkills() {
  if (!selectedId.value) return;
  skillsLoading.value = true;
  try {
    await agentStore.fetchAgentSkills(selectedId.value);
  } finally {
    skillsLoading.value = false;
  }
}

function parseList(value: string) {
  return value
    .split(',')
    .map(item => item.trim())
    .filter(Boolean);
}

function applyConfig(config: any) {
  draft.id = config.id || '';
  draft.name = config.name || '';
  draft.displayName = config.displayName || '';
  draft.description = config.description || '';
  draft.avatar = config.avatar || '';
  draft.enabled = config.enabled ?? true;

  draft.routing.keywords = config.routing?.keywords || [];
  draft.routing.channels = config.routing?.channels || [];
  draft.routing.priority = config.routing?.priority ?? 0;
  draft.routing.autoRoute = config.routing?.autoRoute ?? false;

  draft.capabilities.tools.inherited = config.capabilities?.tools?.inherited || [];
  draft.capabilities.tools.disabled = config.capabilities?.tools?.disabled || [];
  draft.capabilities.tools.custom = config.capabilities?.tools?.custom || [];
  draft.capabilities.skills.inherited = config.capabilities?.skills?.inherited ?? true;
  draft.capabilities.skills.customPath = config.capabilities?.skills?.customPath || '';

  skillInherited.value = draft.capabilities.skills.inherited;
  skillCustomPath.value = draft.capabilities.skills.customPath;
}

function reset() {
  if (selectedId.value) {
    selectAgent(selectedId.value);
  } else {
    newAgent();
  }
}

function newAgent() {
  selectedId.value = null;
  skillApiKeyDraft.value = {};
  skillEnvDraft.value = {};
  applyConfig({
    id: '',
    name: '',
    displayName: '',
    description: '',
    avatar: '',
    enabled: true,
    routing: { keywords: [], channels: [], priority: 0, autoRoute: false },
    capabilities: {
      tools: { inherited: [], disabled: [], custom: [] },
      skills: { inherited: true, customPath: '' }
    }
  });
}

async function selectAgent(agentId: string) {
  selectedId.value = agentId;
  const response = await fetch(`/api/agents/${agentId}/config`);
  if (!response.ok) {
    alert(t('agent.manage.loadFailed', { message: response.statusText }));
    return;
  }
  const config = await response.json();
  applyConfig(config);
  await refreshSkills();
}

async function save() {
  if (!draft.id || !draft.name) {
    alert(t('agent.manage.validationRequired'));
    return;
  }
  saving.value = true;
  try {
    const payload = JSON.parse(JSON.stringify(draft));
    if (isEditing.value) {
      await agentStore.updateAgent(draft.id, payload);
    } else {
      await agentStore.createAgent(payload);
      selectedId.value = draft.id;
    }
    await refresh();
    if (selectedId.value) {
      await refreshSkills();
    }
  } finally {
    saving.value = false;
  }
}

async function remove() {
  if (!selectedId.value) return;
  if (!confirm(t('agent.manage.deleteConfirm', { id: selectedId.value }))) return;
  saving.value = true;
  try {
    await agentStore.deleteAgent(selectedId.value);
    await refresh();
    newAgent();
  } finally {
    saving.value = false;
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

function onEditSkillApiKey(name: string, value: string) {
  skillApiKeyDraft.value = { ...skillApiKeyDraft.value, [name]: value };
}

function onEditSkillEnv(name: string, value: string) {
  skillEnvDraft.value = { ...skillEnvDraft.value, [name]: value };
}

async function onInstallSkill(skill: any) {
  if (!selectedId.value) return;
  const installId = skill?.install?.[0]?.id;
  skillsLoading.value = true;
  try {
    await agentStore.installSkill(selectedId.value, skill.name, installId);
    await refreshSkills();
  } catch (e) {
    console.error(e);
  } finally {
    skillsLoading.value = false;
  }
}

async function onToggleSkill(name: string, evt: Event) {
  if (!selectedId.value) return;
  const checked = (evt.target as HTMLInputElement).checked;
  skillsLoading.value = true;
  try {
    await agentStore.updateAgentSkillEntry(selectedId.value, name, { enabled: checked });
    await refreshSkills();
  } finally {
    skillsLoading.value = false;
  }
}

async function onSaveSkill(name: string) {
  if (!selectedId.value) return;
  savingSkill.value = name;
  const patch: any = { apiKey: skillApiKeyDraft.value[name] || '' };
  const envText = skillEnvDraft.value[name];
  if (envText && envText.trim()) {
    try {
      patch.env = JSON.parse(envText);
    } catch (e) {
      alert(t('skills.envInvalid'));
      savingSkill.value = null;
      return;
    }
  }
  try {
    await agentStore.updateAgentSkillEntry(selectedId.value, name, patch);
    await refreshSkills();
  } finally {
    savingSkill.value = null;
  }
}

async function saveSkillSettings() {
  if (!selectedId.value) return;
  skillsLoading.value = true;
  try {
    await agentStore.updateAgentSkills(selectedId.value, {
      inherited: skillInherited.value,
      customPath: skillCustomPath.value
    });
    await refreshSkills();
  } finally {
    skillsLoading.value = false;
  }
}

</script>


<style scoped>
.agent-manage {
  display: flex;
  flex-direction: column;
}

.manage-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 20px;
}

.list-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
}

.search-box input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 13px;
  background: rgba(0, 0, 0, 0.2);
  color: var(--text);
}

.agent-list {

  display: flex;
  flex-direction: column;
  gap: 12px;
}

.agent-item {
  text-align: left;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.03);
  color: var(--text);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.agent-item.active {
  border-color: rgba(111, 140, 255, 0.6);
  background: rgba(111, 140, 255, 0.12);
}

.agent-title {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
}

.agent-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: var(--muted);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
}

.toolbar-status {
  color: var(--danger);
  font-size: 12px;
}

.muted {
  color: var(--muted);
  font-size: 12px;
}

.pill.warning {
  background: rgba(243, 180, 75, 0.2);
  border: 1px solid rgba(243, 180, 75, 0.4);
}

.skills-section {
  margin-top: 16px;
  border-top: 1px solid var(--border);
  padding-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skills-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.label {
  font-size: 12px;
  color: var(--muted);
}

.title {
  font-size: 16px;
  font-weight: 700;
}

.status {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  border: 1px solid var(--border);
}

.status.active {
  color: #10b981;
  border-color: #10b981;
}

.status.inactive {
  color: #6b7280;
}

.status.error {
  color: #ef4444;
  border-color: #ef4444;
}

.skills-list-card {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
}

.skills-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.skill-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skill-row {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.skill-main {
  min-width: 0;
}

.skill-name {
  font-weight: 700;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.skill-desc {

  font-size: 13px;
  color: var(--muted);
}

.skill-missing {
  font-size: 12px;
  color: #f59e0b;
}

.skill-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.inline-actions {
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
  background: rgba(0, 0, 0, 0.2);
  color: inherit;
}

.field .env {
  width: 100%;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: rgba(0, 0, 0, 0.2);
  color: inherit;
  padding: 8px 10px;
}

.button.small {
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

.pill-success {
  color: #10b981;
  border-color: #10b981;
}

.pill-warning {
  color: #f59e0b;
  border-color: #f59e0b;
}

@media (max-width: 1100px) {

  .manage-grid {
    grid-template-columns: 1fr;
  }

  .skill-row {
    grid-template-columns: 1fr;
  }
}
</style>
