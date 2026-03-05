/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, computed } from 'vue';
import { useAgentStore } from '../store/agents';
const agentStore = useAgentStore();
const emit = defineEmits();
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
    if (!keyword)
        return agentStore.agents.value;
    return agentStore.agents.value.filter((agent) => {
        const haystack = [agent.displayName, agent.name, agent.id, agent.description]
            .filter(Boolean)
            .join(' ')
            .toLowerCase();
        return haystack.includes(keyword);
    });
});
function selectAgent(agentId) {
    agentStore.switchToAgent(agentId);
    emit('select', agentId);
}
function getStatusClass(agent) {
    if (!agent)
        return 'inactive';
    if (agent.enabled === false)
        return 'inactive';
    if (agent.healthy === false)
        return 'error';
    return 'active';
}
function getStatusText(agent) {
    if (!agent)
        return '未知';
    if (agent.enabled === false)
        return '已禁用';
    if (agent.healthy === false)
        return '异常';
    return '正常';
}
function formatUpdatedAt(value) {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}
function getUnreadCount(agentId) {
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
    }
    catch (e) {
        console.error('Failed to create agent:', e);
        alert('创建 Agent 失败: ' + e.message);
    }
    finally {
        creating.value = false;
    }
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['search-box']} */ ;
/** @type {__VLS_StyleScopedClasses['selector-header']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-item']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-item']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-status']} */ ;
/** @type {__VLS_StyleScopedClasses['active']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-status']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-status']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-item']} */ ;
/** @type {__VLS_StyleScopedClasses['active']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-item']} */ ;
/** @type {__VLS_StyleScopedClasses['active']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-item']} */ ;
/** @type {__VLS_StyleScopedClasses['active']} */ ;
/** @type {__VLS_StyleScopedClasses['meta-item']} */ ;
/** @type {__VLS_StyleScopedClasses['modal-header']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-secondary']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "agent-selector" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "selector-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.showCreateDialog = true;
        } },
    ...{ class: "btn-icon" },
    title: "创建新 Agent",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "search-box" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "搜索会话或 Agent",
});
(__VLS_ctx.searchTerm);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "agent-list" },
});
for (const [agent] of __VLS_getVForSourceType((__VLS_ctx.filteredAgents))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.selectAgent(agent.id);
            } },
        key: (agent.id),
        ...{ class: (['agent-item', { active: agent.id === __VLS_ctx.agentStore.currentAgentId.value }]) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "agent-info" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "agent-header" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "agent-name" },
    });
    (agent.displayName || agent.name || agent.id);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: (['agent-status', __VLS_ctx.getStatusClass(agent)]) },
    });
    (__VLS_ctx.getStatusText(agent));
    if (agent.description) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "agent-desc" },
        });
        (agent.description);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "agent-meta" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "meta-item" },
    });
    (agent.id);
    if (agent.updatedAt) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "meta-item" },
        });
        (__VLS_ctx.formatUpdatedAt(agent.updatedAt));
    }
    if (__VLS_ctx.getUnreadCount(agent.id) > 0) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "unread-badge" },
        });
        (__VLS_ctx.getUnreadCount(agent.id));
    }
}
if (__VLS_ctx.showCreateDialog) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showCreateDialog))
                    return;
                __VLS_ctx.showCreateDialog = false;
            } },
        ...{ class: "modal-overlay" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "modal-dialog" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "modal-header" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showCreateDialog))
                    return;
                __VLS_ctx.showCreateDialog = false;
            } },
        ...{ class: "btn-close" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "modal-body" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.form, __VLS_intrinsicElements.form)({
        ...{ onSubmit: (__VLS_ctx.createAgent) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        required: true,
        placeholder: "例: code-reviewer",
    });
    (__VLS_ctx.newAgent.id);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        required: true,
        placeholder: "例: 代码审查助手",
    });
    (__VLS_ctx.newAgent.name);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
        value: (__VLS_ctx.newAgent.description),
        rows: "3",
        placeholder: "描述此 Agent 的功能",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "form-group" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        type: "number",
        min: "0",
        max: "10",
    });
    (__VLS_ctx.newAgent.routing.priority);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "form-actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.showCreateDialog))
                    return;
                __VLS_ctx.showCreateDialog = false;
            } },
        type: "button",
        ...{ class: "btn-secondary" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        type: "submit",
        ...{ class: "btn-primary" },
        disabled: (__VLS_ctx.creating),
    });
    (__VLS_ctx.creating ? '创建中...' : '创建');
}
/** @type {__VLS_StyleScopedClasses['agent-selector']} */ ;
/** @type {__VLS_StyleScopedClasses['selector-header']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['search-box']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-list']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-info']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-header']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-name']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['meta-item']} */ ;
/** @type {__VLS_StyleScopedClasses['meta-item']} */ ;
/** @type {__VLS_StyleScopedClasses['unread-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['modal-overlay']} */ ;
/** @type {__VLS_StyleScopedClasses['modal-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['modal-header']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-close']} */ ;
/** @type {__VLS_StyleScopedClasses['modal-body']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-group']} */ ;
/** @type {__VLS_StyleScopedClasses['form-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-primary']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            agentStore: agentStore,
            showCreateDialog: showCreateDialog,
            creating: creating,
            searchTerm: searchTerm,
            newAgent: newAgent,
            filteredAgents: filteredAgents,
            selectAgent: selectAgent,
            getStatusClass: getStatusClass,
            getStatusText: getStatusText,
            formatUpdatedAt: formatUpdatedAt,
            getUnreadCount: getUnreadCount,
            createAgent: createAgent,
        };
    },
    __typeEmits: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeEmits: {},
});
; /* PartiallyEnd: #4569/main.vue */
