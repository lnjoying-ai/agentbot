<template>
  <section class="chat-coop">
    <h2 class="section-title">{{ t("chat.title.coop") }}</h2>
    <div class="chat-shell">

      <AgentSelector class="agent-sidebar" @select="handleAgentSelect" />
      <div class="chat-panel">
        <div class="chat-header">
          <div class="current-agent-info">
            <span class="agent-name">{{ currentAgentName }}</span>
            <span v-if="agentStore.currentAgent.value" :class="['status-dot', currentAgentStatus]"></span>
          </div>
          <div class="chat-actions">
            <button class="btn-icon" @click="handleLoadHistory" :disabled="!agentStore.currentAgentId.value || currentHistoryLoading" :title="t('chat.loadHistory')" :aria-label="t('chat.loadHistory')">

              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M12 6v6l4 2" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
                <path d="M20 12a8 8 0 1 1-2.34-5.66" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
                <path d="M20 4v4h-4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </button>
            <button class="btn-icon" @click="clearChat" :title="t('chat.clearChat')" :aria-label="t('chat.clearChat')">

              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M4 7h16" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
                <path d="M9 7V5h6v2" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
                <path d="M7 7l1 12h8l1-12" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </button>
          </div>

        </div>

        <div ref="chatStreamRef" class="chat-stream" @scroll.passive="handleScroll">
          <div v-if="currentHistoryLoading" class="history-loading">{{ t("chat.loadingHistory") }}</div>
          <div v-else-if="!currentHistoryHasMore && currentMessages.length" class="history-end">{{ t("chat.historyEnd") }}</div>

          <ChatMessage v-for="msg in currentMessages" :key="msg.id" :message="msg" />
          <div v-if="currentMessages.length === 0" class="empty-chat">
            <p>{{ t("chat.startWith", { name: currentAgentName }) }}</p>

          </div>

        </div>


        <ChatComposer @send="sendMessage" />
      </div>

    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, watch, ref, nextTick, onBeforeUnmount, reactive } from 'vue';


import ChatMessage from "../components/ChatMessage.vue";
import { useI18n } from "../i18n";

import ChatComposer from "../components/ChatComposer.vue";
import AgentSelector from "../components/AgentSelector.vue";
import { useAgentStore } from "../store/agents";
import { useChatStore } from "../store/chat";
import { useConfigStore } from "../store/config";

const agentStore = useAgentStore();
const chat = useChatStore();
const config = useConfigStore();
const { t } = useI18n();



const chatStreamRef = ref<HTMLDivElement | null>(null);
const isAtBottom = ref(true);
const pageSize = 10;
const pollLimit = 20;
const pollIntervalMs = 30000;



const historyState = reactive<Record<string, { loading: boolean; cursor?: string; hasMore: boolean; loaded: boolean }>>({});

const activeStream = ref<{ agentId: string; stop: () => void } | null>(null);
const sseActivated = ref(false);
const pollTimer = ref<number | null>(null);
const pollRunning = ref(false);



function getHistoryState(agentId: string) {
  if (!historyState[agentId]) {
    historyState[agentId] = { loading: false, cursor: undefined, hasMore: true, loaded: false };
  }

  return historyState[agentId];
}

const currentHistoryLoading = computed(() => {
  const agentId = agentStore.currentAgentId.value;
  if (!agentId) return false;
  return getHistoryState(agentId).loading;
});

const currentHistoryHasMore = computed(() => {
  const agentId = agentStore.currentAgentId.value;
  if (!agentId) return false;
  return getHistoryState(agentId).hasMore;
});


const currentMessages = computed(() => {
  const conv = agentStore.currentConversation.value;
  const messages = conv ? conv.messages : [];
  return messages.filter((msg) => {
    const content = (msg.content || "").trim();
    const hasTools = Array.isArray(msg.toolResults) && msg.toolResults.length > 0;
    return content.length > 0 || hasTools;
  });
});


watch(currentMessages, () => {
  if (!isAtBottom.value) return;
  nextTick(() => {
    if (!chatStreamRef.value) return;
    chatStreamRef.value.scrollTop = chatStreamRef.value.scrollHeight;
  });
});


const currentAgentName = computed(() => {
  const agent = agentStore.currentAgent.value;
  return agent?.displayName || agent?.name || agent?.id || t("chat.noAgentSelected");
});


const currentAgentStatus = computed(() => {
  const agent = agentStore.currentAgent.value;
  if (!agent) return 'inactive';
  if (agent.enabled === false) return 'inactive';
  if (agent.healthy === false) return 'error';
  return 'active';
});

const chatMode = computed(() => {
  const value = config.state.config?.agentbot?.ops?.chatMode;
  if (typeof value === "string" && value.trim()) {
    return value;
  }
  return "qa";
});

const isSseMode = computed(() => {
  return chatMode.value.toLowerCase() === "sse";
});

async function loadHistory(agentId: string, reset = false) {

  const state = getHistoryState(agentId);
  if (state.loading || (!state.hasMore && !reset)) return;
  state.loading = true;
  try {
    if (reset) {
      state.cursor = undefined;
      state.hasMore = true;
      state.loaded = true;
    }

    const history = await chat.fetchHistory(agentId, agentId, "web", pageSize, state.cursor);
    const messages = (history.messages || []).map((msg) => ({
      id: msg.id,
      role: msg.role,
      content: msg.content,
      timestamp: new Date(msg.timestamp).toISOString()
    }));
    if (reset) {
      agentStore.setMessages(agentId, messages);
    } else {
      agentStore.prependMessages(agentId, messages);
    }
    state.cursor = history.nextCursor;
    if (!messages.length) {
      state.hasMore = false;
    } else {
      state.hasMore = messages.length >= pageSize && Boolean(history.nextCursor);
    }

  } catch (error) {
    console.warn("Failed to load chat history", error);
  } finally {
    state.loading = false;
  }
}



function disconnectActiveStream() {
  if (!activeStream.value) return;
  activeStream.value.stop();
  activeStream.value = null;
}

function connectActiveStream(agentId: string) {
  if (!isSseMode.value) return;
  if (activeStream.value?.agentId === agentId) return;
  disconnectActiveStream();
  const stop = chat.connectStream({
    channel: "web",
    chatId: agentId,
    onMessage: (message) => agentStore.addMessage(agentId, message)
  });
  activeStream.value = { agentId, stop };
}

function stopPolling() {
  if (pollTimer.value) {
    window.clearInterval(pollTimer.value);
    pollTimer.value = null;
  }
}

async function pollOnce(agentId: string) {
  if (!agentId || pollRunning.value) return;
  pollRunning.value = true;
  try {
    const history = await chat.fetchHistory(agentId, agentId, "web", pollLimit);
    const messages = (history.messages || []).map((msg) => ({
      id: msg.id,
      role: msg.role,
      content: msg.content,
      timestamp: new Date(msg.timestamp).toISOString()
    }));
    if (messages.length) {
      agentStore.appendMessages(agentId, messages);
    }
  } catch (error) {
    console.warn("Failed to poll chat history", error);
  } finally {
    pollRunning.value = false;
  }
}

function startPolling(agentId: string) {
  if (!agentId || isSseMode.value) return;
  stopPolling();
  pollTimer.value = window.setInterval(() => {
    void pollOnce(agentId);
  }, pollIntervalMs);
  void pollOnce(agentId);
}

function handleAgentSelect(agentId: string) {
  if (!isSseMode.value) {
    sseActivated.value = false;
    disconnectActiveStream();
    startPolling(agentId);
    return;
  }
  sseActivated.value = true;
  if (!agentId) return;
  connectActiveStream(agentId);
}



function handleLoadHistory() {
  const agentId = agentStore.currentAgentId.value;
  if (!agentId) return;
  const state = getHistoryState(agentId);
  void loadHistory(agentId, !state.loaded);
}






onMounted(async () => {
  await agentStore.fetchAgents();
  if (!agentStore.currentAgent.value && agentStore.agents.value.length) {
    agentStore.switchToAgent(agentStore.agents.value[0].id);
  }
  const agentId = agentStore.currentAgentId.value;
  if (agentId && sseActivated.value && isSseMode.value) {
    connectActiveStream(agentId);
  }
  if (agentId && !isSseMode.value) {
    startPolling(agentId);
  }


  await nextTick();
  if (chatStreamRef.value) {
    chatStreamRef.value.scrollTop = chatStreamRef.value.scrollHeight;
  }
});




watch(agentStore.currentAgentId, async (agentId) => {
  if (!agentId) {
    disconnectActiveStream();
    stopPolling();
    return;
  }
  if (!sseActivated.value || !isSseMode.value) {
    startPolling(agentId);
    return;
  }
  connectActiveStream(agentId);
  await nextTick();

  if (chatStreamRef.value) {
    chatStreamRef.value.scrollTop = chatStreamRef.value.scrollHeight;
  }
});





watch(agentStore.agents, () => {
  if (!agentStore.agents.value.length) {
    disconnectActiveStream();
    stopPolling();
    return;
  }
  const currentId = agentStore.currentAgentId.value;
  const exists = agentStore.agents.value.some((agent) => agent.id === currentId);
  if (!exists) {
    agentStore.switchToAgent(agentStore.agents.value[0].id);
    return;
  }
  if (!sseActivated.value || !isSseMode.value) {
    startPolling(currentId);
    return;
  }
  connectActiveStream(currentId);
});






async function sendMessage(text: string) {
  const agentId = agentStore.currentAgentId.value;
  if (!agentId) {
    alert(t("chat.selectAgentFirst"));

    return;
  }

  const userMessage = {
    id: `local-${Date.now()}`,
    role: 'user',
    content: text,
    timestamp: new Date().toISOString(),
    local: true
  };

  agentStore.addMessage(agentId, userMessage, { suppressUnread: true });
  await nextTick();
  if (chatStreamRef.value) {
    chatStreamRef.value.scrollTop = chatStreamRef.value.scrollHeight;
  }

  try {
    const assistantMessage = await chat.sendUserMessageForAgent(text, agentId);
    if (assistantMessage) {
      agentStore.addMessage(agentId, assistantMessage, { suppressUnread: true });
    }

    if (!isSseMode.value) {
      void pollOnce(agentId);
    }
  } catch (error) {
    agentStore.addMessage(agentId, {
      id: Date.now().toString(),
      role: 'assistant',
      content: t("chat.sendFailed"),

      timestamp: new Date().toISOString()
    }, { suppressUnread: true });
  }


}


function clearChat() {
  const agentId = agentStore.currentAgentId.value;
  if (!agentId) return;
  if (confirm(t("chat.confirmClear"))) {

    const conv = agentStore.conversations.get(agentId);
    if (conv) {
      conv.messages = [];
    }
  }
}

function handleScroll() {
  if (!chatStreamRef.value) return;
  const el = chatStreamRef.value;
  const threshold = 24;
  const atTop = el.scrollTop <= threshold;
  const atBottom = el.scrollHeight - (el.scrollTop + el.clientHeight) <= threshold;
  isAtBottom.value = atBottom;

  if (atTop) {
    return;
  }



}


watch(isSseMode, (enabled) => {
  if (!enabled) {
    sseActivated.value = false;
    disconnectActiveStream();
    const agentId = agentStore.currentAgentId.value;
    if (agentId) {
      startPolling(agentId);
    }
    return;
  }
  stopPolling();
  const agentId = agentStore.currentAgentId.value;
  if (agentId && sseActivated.value) {
    connectActiveStream(agentId);
  }
});

onBeforeUnmount(() => {
  disconnectActiveStream();
  stopPolling();
});



</script>

<style scoped>
.chat-coop {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.section-title {
  padding: 12px 18px;
  margin: 0 0 8px;
  font-size: 18px;
  color: var(--text);
}

.chat-shell {
  flex: 1;
  display: grid;
  grid-template-columns: 280px 1fr;
  overflow: hidden;
  padding: 12px 16px 16px;
  box-sizing: border-box;
  gap: 16px;
}

.agent-sidebar {
  border-right: none;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, rgba(17, 24, 44, 0.95), rgba(10, 15, 29, 0.98));
  min-width: 0;
  border-radius: 18px;
  border: 1px solid rgba(111, 140, 255, 0.16);
  box-shadow: 0 24px 60px rgba(8, 12, 24, 0.45);
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(111, 140, 255, 0.12);
  background: rgba(18, 26, 47, 0.65);
  backdrop-filter: blur(6px);
}

.current-agent-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.agent-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.08);
}

.status-dot.active {
  background: #10b981;
}

.status-dot.inactive {
  background: #6b7280;
}

.status-dot.error {
  background: #ef4444;
}

.chat-actions {
  display: flex;
  gap: 8px;
}

.btn-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  border: 1px solid rgba(111, 140, 255, 0.2);
  background: rgba(111, 140, 255, 0.08);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text);
  transition: all 0.2s ease;
}

.btn-icon:hover {
  background: rgba(111, 140, 255, 0.18);
  transform: translateY(-1px);
}

.btn-icon:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none;
}

.chat-stream {
  flex: 1;
  overflow: auto;
  padding: 22px;
  min-width: 0;
  height: 520px;
  background: linear-gradient(180deg, rgba(12, 18, 35, 0.6), rgba(9, 14, 26, 0.9));
}

:deep(.message) {
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  box-shadow: 0 12px 30px rgba(6, 9, 20, 0.35);
  backdrop-filter: blur(4px);
}

:deep(.message.user) {
  align-self: flex-end;
  background: linear-gradient(135deg, rgba(111, 140, 255, 0.22), rgba(89, 112, 208, 0.28));
  border-color: rgba(111, 140, 255, 0.4);
}

:deep(.message.assistant) {
  align-self: flex-start;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.04), rgba(255, 255, 255, 0.02));
}

:deep(.message-meta) {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 11px;
  color: rgba(180, 195, 235, 0.85);
  margin-bottom: 8px;
}

.history-loading,
.history-end {
  text-align: center;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 12px;
}

.empty-chat {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--muted);
  font-size: 14px;
}

@media (max-width: 900px) {
  .chat-shell {
    grid-template-columns: 200px 1fr;
  }
}

</style>

