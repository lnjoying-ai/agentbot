/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, ref } from "vue";
import { marked } from "marked";
import DOMPurify from "dompurify";
import { useChatStore } from "../store/chat";
const props = defineProps();
const chat = useChatStore();
const confirming = ref(false);
const confirmed = ref(false);
const confirmError = ref(null);
const canceling = ref(false);
const cancelled = ref(false);
const cancelError = ref(null);
async function confirmTool() {
    if (confirming.value || confirmed.value || canceling.value || cancelled.value)
        return;
    confirming.value = true;
    confirmError.value = null;
    try {
        if (props.tool.agentId) {
            await chat.confirmTool(props.tool.id, { agentId: props.tool.agentId, chatId: props.tool.agentId });
        }
        else {
            await chat.confirmTool(props.tool.id);
        }
        confirmed.value = true;
    }
    catch (error) {
        console.error("工具确认失败:", error);
        confirmError.value = "确认失败，请稍后重试。";
    }
    finally {
        confirming.value = false;
    }
}
async function cancelTool() {
    if (confirming.value || confirmed.value || canceling.value || cancelled.value)
        return;
    canceling.value = true;
    cancelError.value = null;
    try {
        if (props.tool.agentId) {
            await chat.cancelTool(props.tool.id, { agentId: props.tool.agentId, chatId: props.tool.agentId });
        }
        else {
            await chat.cancelTool(props.tool.id);
        }
        cancelled.value = true;
    }
    catch (error) {
        console.error("工具取消失败:", error);
        cancelError.value = "取消失败，请稍后重试。";
    }
    finally {
        canceling.value = false;
    }
}
const approvalHint = computed(() => {
    if (confirmError.value) {
        return { text: confirmError.value, type: "error" };
    }
    if (cancelError.value) {
        return { text: cancelError.value, type: "error" };
    }
    if (confirmed.value) {
        return { text: "已确认，等待工具执行结果...", type: "success" };
    }
    if (cancelled.value) {
        return { text: "已取消，本次工具不会执行。", type: "success" };
    }
    if (confirming.value) {
        return { text: "正在提交确认...", type: "info" };
    }
    if (canceling.value) {
        return { text: "正在提交取消...", type: "info" };
    }
    return null;
});
const renderedOutput = computed(() => {
    const output = props.tool.output || "";
    const rawHtml = marked.parse(output);
    return DOMPurify.sanitize(rawHtml);
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
/** @type {__VLS_StyleScopedClasses['confirm-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['cancel-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['approval-hint']} */ ;
/** @type {__VLS_StyleScopedClasses['approval-hint']} */ ;
/** @type {__VLS_StyleScopedClasses['approval-hint']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "tool-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "title" },
});
(__VLS_ctx.tool.name);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
(__VLS_ctx.tool.status);
(__VLS_ctx.tool.latencyMs ?? 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "markdown-body" },
});
__VLS_asFunctionalDirective(__VLS_directives.vHtml)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.renderedOutput) }, null, null);
if (__VLS_ctx.tool.status === 'PENDING_APPROVAL') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "approval-actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.confirmTool) },
        ...{ class: "confirm-btn" },
        disabled: (__VLS_ctx.confirming || __VLS_ctx.confirmed || __VLS_ctx.canceling || __VLS_ctx.cancelled),
    });
    (__VLS_ctx.confirmed ? "已确认" : (__VLS_ctx.confirming ? "确认中..." : "确认执行"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.cancelTool) },
        ...{ class: "cancel-btn" },
        disabled: (__VLS_ctx.confirming || __VLS_ctx.confirmed || __VLS_ctx.canceling || __VLS_ctx.cancelled),
    });
    (__VLS_ctx.cancelled ? "已取消" : (__VLS_ctx.canceling ? "取消中..." : "取消"));
}
if (__VLS_ctx.approvalHint) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "approval-hint" },
        ...{ class: (__VLS_ctx.approvalHint.type) },
    });
    (__VLS_ctx.approvalHint.text);
}
/** @type {__VLS_StyleScopedClasses['tool-card']} */ ;
/** @type {__VLS_StyleScopedClasses['title']} */ ;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
/** @type {__VLS_StyleScopedClasses['approval-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['confirm-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['cancel-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['approval-hint']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            confirming: confirming,
            confirmed: confirmed,
            canceling: canceling,
            cancelled: cancelled,
            confirmTool: confirmTool,
            cancelTool: cancelTool,
            approvalHint: approvalHint,
            renderedOutput: renderedOutput,
        };
    },
    __typeProps: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeProps: {},
});
; /* PartiallyEnd: #4569/main.vue */
