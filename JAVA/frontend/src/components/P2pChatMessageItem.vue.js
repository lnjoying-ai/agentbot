/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed } from "vue";
const props = defineProps();
const headerLabel = computed(() => {
    if (props.message.direction === "outbound") {
        return `本地 → ${props.message.toNodeId || ""}${props.message.toAgentId ? "/" + props.message.toAgentId : ""}`;
    }
    return `${props.message.fromNodeId || "外部"}${props.message.fromAgentId ? "/" + props.message.fromAgentId : ""} → 本地`;
});
const statusLabel = computed(() => {
    switch (props.message.status) {
        case "ACKED":
            return "已送达";
        case "NACKED":
            return "失败";
        case "FAILED":
            return "发送失败";
        case "RECEIVED":
            return "已接收";
        default:
            return "已发送";
    }
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['p2p-message']} */ ;
/** @type {__VLS_StyleScopedClasses['p2p-message']} */ ;
/** @type {__VLS_StyleScopedClasses['status']} */ ;
/** @type {__VLS_StyleScopedClasses['status']} */ ;
/** @type {__VLS_StyleScopedClasses['status']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "p2p-message" },
    ...{ class: (__VLS_ctx.message.direction) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "message-meta" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.headerLabel);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.message.timestamp);
if (__VLS_ctx.message.status) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "status" },
        ...{ class: (__VLS_ctx.message.status.toLowerCase()) },
    });
    (__VLS_ctx.statusLabel);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "message-content" },
});
(__VLS_ctx.message.content);
if (__VLS_ctx.message.reason) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "message-reason" },
    });
    (__VLS_ctx.message.reason);
}
/** @type {__VLS_StyleScopedClasses['p2p-message']} */ ;
/** @type {__VLS_StyleScopedClasses['message-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['status']} */ ;
/** @type {__VLS_StyleScopedClasses['message-content']} */ ;
/** @type {__VLS_StyleScopedClasses['message-reason']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            headerLabel: headerLabel,
            statusLabel: statusLabel,
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
