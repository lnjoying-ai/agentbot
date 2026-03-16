<template>
  <section class="p2p-chat">
    <h2 class="section-title">{{ t("p2p.title") }}</h2>


    <div class="chat-container">
      <aside class="session-panel">
        <div class="panel-header">
          <h3>{{ t("p2p.sessionList") }}</h3>
          <span class="status" :class="p2p.connected.value ? 'online' : 'offline'">
            {{ p2p.connected.value ? t("p2p.online") : t("p2p.offline") }}
          </span>
        </div>

        <div class="session-list">
          <div
            v-for="session in p2p.sessionList.value"
            :key="session.chatId"
            class="session-item"
            :class="{ active: session.chatId === p2p.currentChatId.value }"
            @click="selectSession(session.chatId)"
          >
            <div class="session-title">{{ session.title }}</div>
            <div class="session-sub">{{ session.chatId }}</div>
            <span v-if="session.unreadCount" class="badge">{{ session.unreadCount }}</span>
          </div>
          <div v-if="!p2p.sessionList.value.length" class="empty">{{ t("p2p.emptySessions") }}</div>

        </div>
      </aside>

      <div class="chat-panel">
        <div class="chat-header">
          <div>
            <div class="current-title">{{ currentTitle }}</div>
            <div class="current-sub">{{ p2p.currentChatId.value || t("p2p.selectSession") }}</div>
          </div>
          <div class="agent-select">
            <label>{{ t("p2p.localAgent") }}</label>

            <select v-model="currentAgentId">
              <option v-for="agent in agentStore.agents.value" :key="agent.id" :value="agent.id">
                {{ agent.displayName || agent.name || agent.id }}
              </option>
            </select>
          </div>
        </div>

        <div class="chat-stream">
          <P2pChatMessageItem
            v-for="msg in currentMessages"
            :key="msg.id"
            :message="msg"
          />
          <div v-if="currentMessages.length === 0" class="empty">{{ t("p2p.emptyMessages") }}</div>

        </div>

        <div class="composer">
          <div class="target-row">
            <input v-model="toNodeId" :placeholder="t('p2p.targetNodeId')" />
            <input v-model="toAgentId" :placeholder="t('p2p.targetAgentId')" />
          </div>

          <ChatComposer @send="handleSend" />
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import ChatComposer from "../components/ChatComposer.vue";
import P2pChatMessageItem from "../components/P2pChatMessageItem.vue";
import { useP2pChatStore } from "../store/p2pChat";
import { useAgentStore } from "../store/agents";
import { useI18n } from "../i18n";

const p2p = useP2pChatStore();
const agentStore = useAgentStore();
const { t } = useI18n();


const toNodeId = ref("");
const toAgentId = ref("");

const currentMessages = computed(() => {
  const messages = p2p.currentSession.value?.messages || [];
  return messages.filter((msg) => {
    const content = (msg.content || "").trim();
    const reason = (msg.reason || "").trim();
    return content.length > 0 || reason.length > 0;
  });
});

const currentTitle = computed(() => p2p.currentSession.value?.title || t("p2p.sessionDefault"));


const currentAgentId = computed({
  get: () => agentStore.currentAgentId.value,
  set: (value: string) => agentStore.switchToAgent(value)
});

onMounted(async () => {
  await agentStore.fetchAgents();
  if (!agentStore.currentAgentId.value && agentStore.agents.value.length) {
    agentStore.switchToAgent(agentStore.agents.value[0].id);
  }
  p2p.connect();
});

watch(
  () => p2p.currentSession.value,
  (session) => {
    if (!session) return;
    if (session.remoteNodeId) toNodeId.value = session.remoteNodeId;
    if (session.remoteAgentId) toAgentId.value = session.remoteAgentId;
  },
  { immediate: true }
);

function selectSession(chatId: string) {
  p2p.selectChat(chatId);
}

async function handleSend(content: string) {
  if (!toNodeId.value.trim() || !toAgentId.value.trim()) {
    alert(t("p2p.fillTargets"));

    return;
  }
  if (!currentAgentId.value) {
    alert(t("p2p.selectLocalAgent"));

    return;
  }
  await p2p.sendMessage({
    toNodeId: toNodeId.value.trim(),
    toAgentId: toAgentId.value.trim(),
    content,
    agentId: currentAgentId.value
  });
}
</script>

<style scoped>
.p2p-chat {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.section-title {
  padding: 16px 20px;
  margin: 0;
  font-size: 18px;
  border-bottom: 1px solid var(--border);
}

.chat-container {
  flex: 1;
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 12px;
  padding: 12px 16px 16px;
  overflow: hidden;
}

.session-panel {
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
}

.panel-header h3 {
  margin: 0;
  font-size: 14px;
}

.status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--hover);
}

.status.online {
  color: #10b981;
}

.status.offline {
  color: #ef4444;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 6px;
  border: 1px solid transparent;
}

.session-item:hover {
  background: var(--hover);
}

.session-item.active {
  border-color: var(--primary);
  background: rgba(99, 102, 241, 0.15);
}

.session-title {
  font-weight: 600;
  font-size: 13px;
}

.session-sub {
  font-size: 11px;
  color: var(--muted);
  margin-top: 4px;
  word-break: break-all;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 6px;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  background: var(--hover);
}

.current-title {
  font-weight: 600;
  font-size: 14px;
}

.current-sub {
  font-size: 11px;
  color: var(--muted);
}

.agent-select {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
}

.agent-select select {
  padding: 4px 8px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--bg);
  color: var(--text);
}

.chat-stream {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.composer {
  border-top: 1px solid var(--border);
}

.target-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 12px 20px 0;
}

.target-row input {
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--hover);
  color: var(--text);
}

.empty {
  text-align: center;
  color: var(--muted);
  font-size: 12px;
  padding: 20px 0;
}

@media (max-width: 900px) {
  .chat-container {
    grid-template-columns: 1fr;
  }

  .session-panel {
    max-height: 220px;
  }
}
</style>
