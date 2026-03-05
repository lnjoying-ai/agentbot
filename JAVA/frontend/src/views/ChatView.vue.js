/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, watch, ref, nextTick, onBeforeUnmount, reactive } from 'vue';
import ChatMessage from "../components/ChatMessage.vue";
import ChatComposer from "../components/ChatComposer.vue";
import AgentSelector from "../components/AgentSelector.vue";
import { useAgentStore } from "../store/agents";
import { useChatStore } from "../store/chat";
import { useConfigStore } from "../store/config";
const agentStore = useAgentStore();
const chat = useChatStore();
const config = useConfigStore();
const chatStreamRef = ref(null);
const isAtBottom = ref(true);
const pageSize = 10;
const pollLimit = 20;
const pollIntervalMs = 30000;
const historyState = reactive({});
const activeStream = ref(null);
const sseActivated = ref(false);
const pollTimer = ref(null);
const pollRunning = ref(false);
function getHistoryState(agentId) {
    if (!historyState[agentId]) {
        historyState[agentId] = { loading: false, cursor: undefined, hasMore: true, loaded: false };
    }
    return historyState[agentId];
}
const currentHistoryLoading = computed(() => {
    const agentId = agentStore.currentAgentId.value;
    if (!agentId)
        return false;
    return getHistoryState(agentId).loading;
});
const currentHistoryHasMore = computed(() => {
    const agentId = agentStore.currentAgentId.value;
    if (!agentId)
        return false;
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
    if (!isAtBottom.value)
        return;
    nextTick(() => {
        if (!chatStreamRef.value)
            return;
        chatStreamRef.value.scrollTop = chatStreamRef.value.scrollHeight;
    });
});
const currentAgentName = computed(() => {
    const agent = agentStore.currentAgent.value;
    return agent?.displayName || agent?.name || agent?.id || '未选择';
});
const currentAgentStatus = computed(() => {
    const agent = agentStore.currentAgent.value;
    if (!agent)
        return 'inactive';
    if (agent.enabled === false)
        return 'inactive';
    if (agent.healthy === false)
        return 'error';
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
async function loadHistory(agentId, reset = false) {
    const state = getHistoryState(agentId);
    if (state.loading || (!state.hasMore && !reset))
        return;
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
        }
        else {
            agentStore.prependMessages(agentId, messages);
        }
        state.cursor = history.nextCursor;
        if (!messages.length) {
            state.hasMore = false;
        }
        else {
            state.hasMore = messages.length >= pageSize && Boolean(history.nextCursor);
        }
    }
    catch (error) {
        console.warn("Failed to load chat history", error);
    }
    finally {
        state.loading = false;
    }
}
function disconnectActiveStream() {
    if (!activeStream.value)
        return;
    activeStream.value.stop();
    activeStream.value = null;
}
function connectActiveStream(agentId) {
    if (!isSseMode.value)
        return;
    if (activeStream.value?.agentId === agentId)
        return;
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
async function pollOnce(agentId) {
    if (!agentId || pollRunning.value)
        return;
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
    }
    catch (error) {
        console.warn("Failed to poll chat history", error);
    }
    finally {
        pollRunning.value = false;
    }
}
function startPolling(agentId) {
    if (!agentId || isSseMode.value)
        return;
    stopPolling();
    pollTimer.value = window.setInterval(() => {
        void pollOnce(agentId);
    }, pollIntervalMs);
    void pollOnce(agentId);
}
function handleAgentSelect(agentId) {
    if (!isSseMode.value) {
        sseActivated.value = false;
        disconnectActiveStream();
        startPolling(agentId);
        return;
    }
    sseActivated.value = true;
    if (!agentId)
        return;
    connectActiveStream(agentId);
}
function handleLoadHistory() {
    const agentId = agentStore.currentAgentId.value;
    if (!agentId)
        return;
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
async function sendMessage(text) {
    const agentId = agentStore.currentAgentId.value;
    if (!agentId) {
        alert('请先选择一个 Agent');
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
    }
    catch (error) {
        agentStore.addMessage(agentId, {
            id: Date.now().toString(),
            role: 'assistant',
            content: '抱歉，由于网络或后端服务异常，我暂时无法处理您的请求。',
            timestamp: new Date().toISOString()
        }, { suppressUnread: true });
    }
}
function clearChat() {
    const agentId = agentStore.currentAgentId.value;
    if (!agentId)
        return;
    if (confirm('确定要清空当前对话吗？')) {
        const conv = agentStore.conversations.get(agentId);
        if (conv) {
            conv.messages = [];
        }
    }
}
function handleScroll() {
    if (!chatStreamRef.value)
        return;
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
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['status-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['status-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['status-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-shell']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "chat-coop" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "chat-shell" },
});
/** @type {[typeof AgentSelector, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(AgentSelector, new AgentSelector({
    ...{ 'onSelect': {} },
    ...{ class: "agent-sidebar" },
}));
const __VLS_1 = __VLS_0({
    ...{ 'onSelect': {} },
    ...{ class: "agent-sidebar" },
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
let __VLS_3;
let __VLS_4;
let __VLS_5;
const __VLS_6 = {
    onSelect: (__VLS_ctx.handleAgentSelect)
};
var __VLS_2;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "chat-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "chat-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "current-agent-info" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "agent-name" },
});
(__VLS_ctx.currentAgentName);
if (__VLS_ctx.agentStore.currentAgent.value) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: (['status-dot', __VLS_ctx.currentAgentStatus]) },
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "chat-actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.handleLoadHistory) },
    ...{ class: "btn-icon" },
    disabled: (!__VLS_ctx.agentStore.currentAgentId.value || __VLS_ctx.currentHistoryLoading),
    title: "加载历史",
    'aria-label': "加载历史",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
    width: "16",
    height: "16",
    viewBox: "0 0 24 24",
    fill: "none",
    'aria-hidden': "true",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M12 6v6l4 2",
    stroke: "currentColor",
    'stroke-width': "1.6",
    'stroke-linecap': "round",
    'stroke-linejoin': "round",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M20 12a8 8 0 1 1-2.34-5.66",
    stroke: "currentColor",
    'stroke-width': "1.6",
    'stroke-linecap': "round",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M20 4v4h-4",
    stroke: "currentColor",
    'stroke-width': "1.6",
    'stroke-linecap': "round",
    'stroke-linejoin': "round",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.clearChat) },
    ...{ class: "btn-icon" },
    title: "清空对话",
    'aria-label': "清空对话",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
    width: "16",
    height: "16",
    viewBox: "0 0 24 24",
    fill: "none",
    'aria-hidden': "true",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M4 7h16",
    stroke: "currentColor",
    'stroke-width': "1.6",
    'stroke-linecap': "round",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M9 7V5h6v2",
    stroke: "currentColor",
    'stroke-width': "1.6",
    'stroke-linecap': "round",
    'stroke-linejoin': "round",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M7 7l1 12h8l1-12",
    stroke: "currentColor",
    'stroke-width': "1.6",
    'stroke-linecap': "round",
    'stroke-linejoin': "round",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ onScroll: (__VLS_ctx.handleScroll) },
    ref: "chatStreamRef",
    ...{ class: "chat-stream" },
});
/** @type {typeof __VLS_ctx.chatStreamRef} */ ;
if (__VLS_ctx.currentHistoryLoading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "history-loading" },
    });
}
else if (!__VLS_ctx.currentHistoryHasMore && __VLS_ctx.currentMessages.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "history-end" },
    });
}
for (const [msg] of __VLS_getVForSourceType((__VLS_ctx.currentMessages))) {
    /** @type {[typeof ChatMessage, ]} */ ;
    // @ts-ignore
    const __VLS_7 = __VLS_asFunctionalComponent(ChatMessage, new ChatMessage({
        key: (msg.id),
        message: (msg),
    }));
    const __VLS_8 = __VLS_7({
        key: (msg.id),
        message: (msg),
    }, ...__VLS_functionalComponentArgsRest(__VLS_7));
}
if (__VLS_ctx.currentMessages.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "empty-chat" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (__VLS_ctx.currentAgentName);
}
/** @type {[typeof ChatComposer, ]} */ ;
// @ts-ignore
const __VLS_10 = __VLS_asFunctionalComponent(ChatComposer, new ChatComposer({
    ...{ 'onSend': {} },
}));
const __VLS_11 = __VLS_10({
    ...{ 'onSend': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_10));
let __VLS_13;
let __VLS_14;
let __VLS_15;
const __VLS_16 = {
    onSend: (__VLS_ctx.sendMessage)
};
var __VLS_12;
/** @type {__VLS_StyleScopedClasses['chat-coop']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-shell']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-sidebar']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-header']} */ ;
/** @type {__VLS_StyleScopedClasses['current-agent-info']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-name']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-stream']} */ ;
/** @type {__VLS_StyleScopedClasses['history-loading']} */ ;
/** @type {__VLS_StyleScopedClasses['history-end']} */ ;
/** @type {__VLS_StyleScopedClasses['empty-chat']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ChatMessage: ChatMessage,
            ChatComposer: ChatComposer,
            AgentSelector: AgentSelector,
            agentStore: agentStore,
            chatStreamRef: chatStreamRef,
            currentHistoryLoading: currentHistoryLoading,
            currentHistoryHasMore: currentHistoryHasMore,
            currentMessages: currentMessages,
            currentAgentName: currentAgentName,
            currentAgentStatus: currentAgentStatus,
            handleAgentSelect: handleAgentSelect,
            handleLoadHistory: handleLoadHistory,
            sendMessage: sendMessage,
            clearChat: clearChat,
            handleScroll: handleScroll,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
