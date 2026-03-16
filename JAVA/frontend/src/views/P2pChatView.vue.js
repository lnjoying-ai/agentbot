/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
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
    set: (value) => agentStore.switchToAgent(value)
});
onMounted(async () => {
    await agentStore.fetchAgents();
    if (!agentStore.currentAgentId.value && agentStore.agents.value.length) {
        agentStore.switchToAgent(agentStore.agents.value[0].id);
    }
    p2p.connect();
});
watch(() => p2p.currentSession.value, (session) => {
    if (!session)
        return;
    if (session.remoteNodeId)
        toNodeId.value = session.remoteNodeId;
    if (session.remoteAgentId)
        toAgentId.value = session.remoteAgentId;
}, { immediate: true });
function selectSession(chatId) {
    p2p.selectChat(chatId);
}
async function handleSend(content) {
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
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['panel-header']} */ ;
/** @type {__VLS_StyleScopedClasses['status']} */ ;
/** @type {__VLS_StyleScopedClasses['status']} */ ;
/** @type {__VLS_StyleScopedClasses['session-item']} */ ;
/** @type {__VLS_StyleScopedClasses['session-item']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-select']} */ ;
/** @type {__VLS_StyleScopedClasses['target-row']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-container']} */ ;
/** @type {__VLS_StyleScopedClasses['session-panel']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "p2p-chat" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
(__VLS_ctx.t("p2p.title"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "chat-container" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
    ...{ class: "session-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "panel-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("p2p.sessionList"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "status" },
    ...{ class: (__VLS_ctx.p2p.connected.value ? 'online' : 'offline') },
});
(__VLS_ctx.p2p.connected.value ? __VLS_ctx.t("p2p.online") : __VLS_ctx.t("p2p.offline"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "session-list" },
});
for (const [session] of __VLS_getVForSourceType((__VLS_ctx.p2p.sessionList.value))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.selectSession(session.chatId);
            } },
        key: (session.chatId),
        ...{ class: "session-item" },
        ...{ class: ({ active: session.chatId === __VLS_ctx.p2p.currentChatId.value }) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "session-title" },
    });
    (session.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "session-sub" },
    });
    (session.chatId);
    if (session.unreadCount) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "badge" },
        });
        (session.unreadCount);
    }
}
if (!__VLS_ctx.p2p.sessionList.value.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "empty" },
    });
    (__VLS_ctx.t("p2p.emptySessions"));
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "chat-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "chat-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "current-title" },
});
(__VLS_ctx.currentTitle);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "current-sub" },
});
(__VLS_ctx.p2p.currentChatId.value || __VLS_ctx.t("p2p.selectSession"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "agent-select" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("p2p.localAgent"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.currentAgentId),
});
for (const [agent] of __VLS_getVForSourceType((__VLS_ctx.agentStore.agents.value))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        key: (agent.id),
        value: (agent.id),
    });
    (agent.displayName || agent.name || agent.id);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "chat-stream" },
});
for (const [msg] of __VLS_getVForSourceType((__VLS_ctx.currentMessages))) {
    /** @type {[typeof P2pChatMessageItem, ]} */ ;
    // @ts-ignore
    const __VLS_0 = __VLS_asFunctionalComponent(P2pChatMessageItem, new P2pChatMessageItem({
        key: (msg.id),
        message: (msg),
    }));
    const __VLS_1 = __VLS_0({
        key: (msg.id),
        message: (msg),
    }, ...__VLS_functionalComponentArgsRest(__VLS_0));
}
if (__VLS_ctx.currentMessages.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "empty" },
    });
    (__VLS_ctx.t("p2p.emptyMessages"));
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "composer" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "target-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: (__VLS_ctx.t('p2p.targetNodeId')),
});
(__VLS_ctx.toNodeId);
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: (__VLS_ctx.t('p2p.targetAgentId')),
});
(__VLS_ctx.toAgentId);
/** @type {[typeof ChatComposer, ]} */ ;
// @ts-ignore
const __VLS_3 = __VLS_asFunctionalComponent(ChatComposer, new ChatComposer({
    ...{ 'onSend': {} },
}));
const __VLS_4 = __VLS_3({
    ...{ 'onSend': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_3));
let __VLS_6;
let __VLS_7;
let __VLS_8;
const __VLS_9 = {
    onSend: (__VLS_ctx.handleSend)
};
var __VLS_5;
/** @type {__VLS_StyleScopedClasses['p2p-chat']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-container']} */ ;
/** @type {__VLS_StyleScopedClasses['session-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['panel-header']} */ ;
/** @type {__VLS_StyleScopedClasses['status']} */ ;
/** @type {__VLS_StyleScopedClasses['session-list']} */ ;
/** @type {__VLS_StyleScopedClasses['session-item']} */ ;
/** @type {__VLS_StyleScopedClasses['session-title']} */ ;
/** @type {__VLS_StyleScopedClasses['session-sub']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
/** @type {__VLS_StyleScopedClasses['empty']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-header']} */ ;
/** @type {__VLS_StyleScopedClasses['current-title']} */ ;
/** @type {__VLS_StyleScopedClasses['current-sub']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-select']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-stream']} */ ;
/** @type {__VLS_StyleScopedClasses['empty']} */ ;
/** @type {__VLS_StyleScopedClasses['composer']} */ ;
/** @type {__VLS_StyleScopedClasses['target-row']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ChatComposer: ChatComposer,
            P2pChatMessageItem: P2pChatMessageItem,
            p2p: p2p,
            agentStore: agentStore,
            t: t,
            toNodeId: toNodeId,
            toAgentId: toAgentId,
            currentMessages: currentMessages,
            currentTitle: currentTitle,
            currentAgentId: currentAgentId,
            selectSession: selectSession,
            handleSend: handleSend,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
