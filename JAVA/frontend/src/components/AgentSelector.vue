<template>
  <div class="agent-selector">
    <div class="selector-header">
      <h3>Agent 列表</h3>
      <button class="btn-icon" @click="showCreateDialog = true" title="创建新 Agent">
        <span>+</span>
      </button>
    </div>
    
    <div class="search-box">
      <input v-model="searchTerm" placeholder="搜索会话或 Agent" />
    </div>

    <div class="agent-list">
      <div
        v-for="agent in filteredAgents"
        :key="agent.id"
        :class="['agent-item', { active: agent.id === agentStore.currentAgentId.value }]"
        @click="selectAgent(agent.id)"
      >
        <div class="agent-info">
          <div class="agent-header">
            <span class="agent-name">{{ agent.displayName || agent.name || agent.id }}</span>
            <span :class="['agent-status', getStatusClass(agent)]">{{ getStatusText(agent) }}</span>
          </div>
          <div class="agent-desc" v-if="agent.description">{{ agent.description }}</div>
          <div class="agent-meta">
            <span class="meta-item">{{ agent.id }}</span>
            <span v-if="agent.updatedAt" class="meta-item">更新于 {{ formatUpdatedAt(agent.updatedAt) }}</span>
          </div>
        </div>

        
        <div v-if="getUnreadCount(agent.id) > 0" class="unread-badge">
          {{ getUnreadCount(agent.id) }}
        </div>
      </div>
    </div>
    
    <!-- Create Agent Dialog -->
    <div v-if="showCreateDialog" class="modal-overlay" @click.self="showCreateDialog = false">
      <div class="modal-dialog">
        <div class="modal-header">
          <h3>创建新 Agent</h3>
          <button class="btn-close" @click="showCreateDialog = false">×</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="createAgent">
            <div class="form-group">
              <label>Agent ID</label>
              <input v-model="newAgent.id" required placeholder="例: code-reviewer" />
            </div>
            <div class="form-group">
              <label>名称</label>
              <input v-model="newAgent.name" required placeholder="例: 代码审查助手" />
            </div>
            <div class="form-group">
              <label>描述</label>
              <textarea v-model="newAgent.description" rows="3" placeholder="描述此 Agent 的功能"></textarea>
            </div>
            <div class="form-group">
              <label>优先级 (0-10)</label>
              <input v-model.number="newAgent.routing.priority" type="number" min="0" max="10" />
            </div>
            <div class="form-actions">
              <button type="button" class="btn-secondary" @click="showCreateDialog = false">取消</button>
              <button type="submit" class="btn-primary" :disabled="creating">
                {{ creating ? '创建中...' : '创建' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useAgentStore } from '../store/agents';

const agentStore = useAgentStore();
const emit = defineEmits<{
  (e: 'select', agentId: string): void;
}>();
const showCreateDialog = ref(false);

const creating = ref(false);
const searchTerm = ref('');

const newAgent = ref({
  id: '',
  name: '',
  description: '',
  routing: {
    keywords: [],
    channels: [],
    priority: 5
  }
});

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


function selectAgent(agentId: string) {
  agentStore.switchToAgent(agentId);
  emit('select', agentId);
}


function getStatusClass(agent: any): string {
  if (!agent) return 'inactive';
  if (agent.enabled === false) return 'inactive';
  if (agent.healthy === false) return 'error';
  return 'active';
}

function getStatusText(agent: any): string {
  if (!agent) return '未知';
  if (agent.enabled === false) return '已禁用';
  if (agent.healthy === false) return '异常';
  return '正常';
}

function formatUpdatedAt(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}


function getUnreadCount(agentId: string): number {
  const conv = agentStore.conversations.get(agentId);
  return conv ? conv.unreadCount : 0;
}

async function createAgent() {
  creating.value = true;
  try {
    await agentStore.createAgent(newAgent.value);
    showCreateDialog.value = false;
    
    // Reset form
    newAgent.value = {
      id: '',
      name: '',
      description: '',
      routing: {
        keywords: [],
        channels: [],
        priority: 5
      }
    };
  } catch (e) {
    console.error('Failed to create agent:', e);
    alert('创建 Agent 失败: ' + (e as Error).message);
  } finally {
    creating.value = false;
  }
}
</script>

<style scoped>
.agent-selector {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}

.selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid var(--border);
}

.search-box {
  padding: 10px 12px;
  border-bottom: 1px solid var(--border);
}

.search-box input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 13px;
  background: var(--bg);
  color: var(--text);
}


.selector-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.btn-icon {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  border: 1px solid var(--border);
  background: var(--bg);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.btn-icon:hover {
  background: var(--hover);
}

.agent-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.agent-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 12px;
  margin-bottom: 4px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.agent-item:hover {
  background: var(--hover);
}

.agent-item.active {
  background: var(--primary);
  color: white;
}

.agent-info {
  flex: 1;
  min-width: 0;
}

.agent-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.agent-name {
  font-weight: 600;
  font-size: 14px;
}

.agent-status {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.1);
}

.agent-status.active {
  background: #10b981;
  color: white;
}

.agent-status.error {
  background: #ef4444;
  color: white;
}

.agent-status.inactive {
  background: #6b7280;
  color: white;
}


.agent-desc {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-item.active .agent-desc {
  color: rgba(255, 255, 255, 0.8);
}

.agent-meta {
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: var(--muted);
}

.agent-item.active .agent-meta {
  color: rgba(255, 255, 255, 0.7);
}

.meta-item {
  padding: 2px 6px;
  border-radius: 3px;
  background: rgba(0, 0, 0, 0.05);
}

.agent-item.active .meta-item {
  background: rgba(255, 255, 255, 0.2);
}

.unread-badge {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: #ef4444;
  color: white;
  font-size: 11px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 8px;
}

/* Modal styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-dialog {
  background: var(--bg);
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
}

.btn-close {
  border: none;
  background: none;
  font-size: 24px;
  cursor: pointer;
  color: var(--muted);
}

.modal-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 600;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 4px;
  font-size: 14px;
  font-family: inherit;
}

.form-group textarea {
  resize: vertical;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
}

.btn-primary,
.btn-secondary {
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  border: none;
}

.btn-primary {
  background: var(--primary);
  color: white;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background: var(--hover);
  color: var(--text);
}
</style>
