<template>
  <div class="agent-info-panel">
    <div v-if="agentStore.currentAgent.value" class="agent-details">
      <div class="panel-header">
        <h3>{{ currentAgentName }}</h3>
        <span :class="['status-indicator', statusClass]"></span>
      </div>

      
      <div class="info-section">
        <h4>基本信息</h4>
        <div class="info-row">
          <span class="label">ID:</span>
          <span class="value">{{ agentStore.currentAgent.value.id }}</span>
        </div>
        <div class="info-row">
          <span class="label">描述:</span>
          <span class="value">{{ currentAgentDescription }}</span>
        </div>
        <div class="info-row">
          <span class="label">状态:</span>
          <span class="value">{{ statusText }}</span>
        </div>

      </div>
      
      <div class="info-section">
        <h4>能力配置</h4>
        <div class="capability-item">
          <span class="capability-label">技能</span>
          <div v-if="skillsData" class="skill-list">
            <div v-for="skill in skillsData.available" :key="skill.name" class="skill-row">
              <div class="skill-main">
                <div class="skill-name">
                  {{ skill.name }}
                  <span class="pill" :class="skill.blocked ? 'pill-warning' : 'pill-success'">
                    {{ skill.blocked ? '阻塞' : '可用' }}
                  </span>
                </div>
                <div class="skill-desc">{{ skill.description || '暂无描述' }}</div>
                <div v-if="missingText(skill)" class="skill-missing">缺失：{{ missingText(skill) }}</div>
              </div>
              <div class="skill-actions">
                <label class="toggle">
                  <input type="checkbox" :checked="isSkillEnabled(skill.name)" @change="onToggleSkill(skill.name, $event)" />
                  <span>启用</span>
                </label>
                <div v-if="skill.primaryEnv" class="apikey" >
                  <input
                    type="password"
                    :value="editingApiKey[skill.name] || ''"
                    @input="(e:any) => onEditApiKey(skill.name, e.target.value)"
                    :placeholder="`API Key (${skill.primaryEnv})`"
                  />
                  <button class="btn btn-small" @click="onSaveApiKey(skill.name)" :disabled="savingSkill === skill.name">保存</button>
                </div>
              </div>
            </div>
            <div v-if="skillsData.available.length === 0" class="empty-text">暂无技能文件</div>
          </div>
          <div v-else class="empty-text">未加载技能</div>
        </div>
        <div class="capability-item">
          <span class="capability-label">继承工具 ({{ inheritedTools.length }})</span>
          <div class="tag-list">
            <span v-for="tool in inheritedTools" :key="tool" class="tag tag-tool">
              {{ tool }}
            </span>
            <span v-if="inheritedTools.length === 0" class="empty-text">
              继承全部系统工具
            </span>
          </div>
        </div>
        <div v-if="disabledTools.length > 0" class="capability-item">
          <span class="capability-label">禁用工具 ({{ disabledTools.length }})</span>
          <div class="tag-list">
            <span v-for="tool in disabledTools" :key="tool" class="tag tag-disabled">
              {{ tool }}
            </span>
          </div>
        </div>

      </div>

      
      <div class="info-section">
        <h4>路由配置</h4>
        <div class="info-row">
          <span class="label">优先级:</span>
          <span class="value">
            <span class="priority-badge">{{ routingPriority }}</span>
          </span>
        </div>
        <div class="capability-item">
          <span class="capability-label">关键词</span>
          <div class="tag-list">
            <span v-for="keyword in routingKeywords" :key="keyword" class="tag tag-keyword">
              {{ keyword }}
            </span>
            <span v-if="routingKeywords.length === 0" class="empty-text">
              无关键词路由
            </span>
          </div>
        </div>
        <div class="capability-item">
          <span class="capability-label">渠道绑定</span>
          <div class="tag-list">
            <span v-for="channel in routingChannels" :key="channel" class="tag tag-channel">
              {{ channel }}
            </span>
            <span v-if="routingChannels.length === 0" class="empty-text">
              无渠道绑定
            </span>
          </div>
        </div>

      </div>
      
      <div v-if="statistics" class="info-section">
        <h4>统计信息</h4>
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-value">{{ statistics.messagesProcessed }}</div>
            <div class="stat-label">处理消息数</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ statistics.averageResponseTime }}ms</div>
            <div class="stat-label">平均响应时间</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ (statistics.successRate * 100).toFixed(1) }}%</div>
            <div class="stat-label">成功率</div>
          </div>
        </div>
      </div>
      
      <div class="panel-actions">
        <button class="btn btn-secondary" @click="refreshStats">刷新统计</button>
        <button class="btn btn-primary" @click="editAgent">编辑配置</button>
      </div>
    </div>
    
    <div v-else class="empty-state">
      <p>未选择 Agent</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useAgentStore } from '../store/agents';

const agentStore = useAgentStore();
const statistics = ref<any>(null);
const savingSkill = ref<string | null>(null);
const editingApiKey = ref<Record<string, string>>({});


const currentAgentName = computed(() => {
  const agent = agentStore.currentAgent.value;
  return agent?.displayName || agent?.name || agent?.id || '未选择';
});

const currentAgentDescription = computed(() => {
  const agent = agentStore.currentAgent.value;
  return agent?.description || '暂无描述';
});

const statusClass = computed(() => {
  const agent = agentStore.currentAgent.value;
  if (!agent) return 'inactive';
  if (agent.enabled === false) return 'inactive';
  if (agent.healthy === false) return 'error';
  return 'active';
});

const statusText = computed(() => {
  const agent = agentStore.currentAgent.value;
  if (!agent) return '未知';
  if (agent.enabled === false) return '已禁用';
  if (agent.healthy === false) return '异常';
  return '正常';
});

const skillsData = computed(() => agentStore.agentSkills.get(agentStore.currentAgentId.value) || null);
const inheritedTools = computed(() => agentStore.currentAgent.value?.capabilities?.tools?.inherited || []);
const disabledTools = computed(() => agentStore.currentAgent.value?.capabilities?.tools?.disabled || []);

const routingPriority = computed(() => agentStore.currentAgent.value?.routing?.priority ?? 0);

const routingKeywords = computed(() => agentStore.currentAgent.value?.routing?.keywords || []);
const routingChannels = computed(() => agentStore.currentAgent.value?.routing?.channels || []);

watch(() => agentStore.currentAgentId.value, async (newAgentId) => {
  if (newAgentId) {
    await loadStatistics(newAgentId);
    await agentStore.fetchAgentSkills(newAgentId);
    syncEditingKeys();
  }
}, { immediate: true });


async function loadStatistics(agentId: string) {
  try {
    statistics.value = await agentStore.fetchAgentStatistics(agentId);
  } catch (e) {
    console.error('Failed to load statistics:', e);
    statistics.value = null;
  }
}

watch(skillsData, () => {
  syncEditingKeys();
});

function syncEditingKeys() {
  const data = skillsData.value;
  if (!data) return;
  const next: Record<string, string> = {};
  Object.entries(data.entries || {}).forEach(([k, v]) => {
    if (v.apiKey) next[k] = v.apiKey;
  });
  editingApiKey.value = next;
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

function onToggleSkill(name: string, evt: Event) {
  const checked = (evt.target as HTMLInputElement).checked;
  agentStore.updateAgentSkillEntry(agentStore.currentAgentId.value, name, { enabled: checked });
}

function onEditApiKey(name: string, value: string) {
  editingApiKey.value = { ...editingApiKey.value, [name]: value };
}

async function onSaveApiKey(name: string) {
  savingSkill.value = name;
  await agentStore.updateAgentSkillEntry(agentStore.currentAgentId.value, name, { apiKey: editingApiKey.value[name] || '' });
  savingSkill.value = null;
}


async function refreshStats() {
  if (agentStore.currentAgentId.value) {
    await loadStatistics(agentStore.currentAgentId.value);
  }
}

function editAgent() {
  // TODO: Open edit dialog
  alert('编辑功能待实现');
}
</script>



<style scoped>
.agent-info-panel {
  padding: 20px;
  background: var(--bg);
  overflow-y: auto;
}

.agent-details {
  max-width: 600px;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid var(--border);
}

.panel-header h3 {
  margin: 0;
  font-size: 20px;
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.status-indicator.active {
  background: #10b981;
  box-shadow: 0 0 8px #10b981;
}

.status-indicator.inactive {
  background: #6b7280;
}

.status-indicator.error {
  background: #ef4444;
  box-shadow: 0 0 8px #ef4444;
}

.info-section {
  margin-bottom: 24px;
}

.info-section h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-row {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
}

.info-row:last-child {
  border-bottom: none;
}

.info-row .label {
  width: 100px;
  font-size: 13px;
  color: var(--muted);
}

.info-row .value {
  flex: 1;
  font-size: 13px;
}

.capability-item {
  margin-bottom: 16px;
}

.capability-label {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 8px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  background: #e0e7ff;
  color: #4338ca;
}

.tag-tool {
  background: #dbeafe;
  color: #1e40af;
}

.tag-disabled {
  background: #fee2e2;
  color: #991b1b;
  text-decoration: line-through;
}

.tag-keyword {
  background: #fef3c7;
  color: #92400e;
}

.tag-channel {
  background: #d1fae5;
  color: #065f46;
}

.empty-text {
  font-size: 12px;
  color: var(--muted);
  font-style: italic;
}

.skill-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skill-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--hover);
}

.skill-main {
  min-width: 0;
}

.skill-name {
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.pill {
  display: inline-flex;
  align-items: center;
  padding: 2px 6px;
  border-radius: 999px;
  font-size: 11px;
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

.skill-desc {
  color: var(--muted);
  font-size: 12px;
  margin-top: 4px;
}

.skill-missing {
  font-size: 12px;
  color: #f59e0b;
  margin-top: 4px;
}

.skill-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end;
}

.skill-actions .toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.skill-actions .apikey {
  display: flex;
  gap: 8px;
  align-items: center;
}

.skill-actions input[type="password"] {
  padding: 6px 8px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg);
  color: inherit;
}

.btn-small {
  padding: 6px 10px;
  font-size: 12px;
}

.priority-badge {

  display: inline-block;
  min-width: 28px;
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--primary);
  color: white;
  font-weight: 600;
  text-align: center;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
}

.stat-card {
  padding: 12px;
  border-radius: 6px;
  background: var(--hover);
  text-align: center;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--primary);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 11px;
  color: var(--muted);
}

.panel-actions {
  display: flex;
  gap: 8px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.btn {
  flex: 1;
  padding: 10px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.btn-primary {
  background: var(--primary);
  color: white;
}

.btn-primary:hover {
  opacity: 0.9;
}

.btn-secondary {
  background: var(--hover);
  color: var(--text);
  border: 1px solid var(--border);
}

.btn-secondary:hover {
  background: var(--border);
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--muted);
  font-size: 14px;
}
</style>
