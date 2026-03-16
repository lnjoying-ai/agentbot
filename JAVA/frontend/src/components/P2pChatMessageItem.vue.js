/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed } from "vue";
import { useI18n } from "../i18n";
const props = defineProps();
const { t } = useI18n();
const headerLabel = computed(() => {
    if (props.message.direction === "outbound") {
        const target = `${props.message.toNodeId || ""}${props.message.toAgentId ? "/" + props.message.toAgentId : ""}`.trim();
        return t("p2p.header.localTo", { target });
    }
    const source = `${props.message.fromNodeId || t("p2p.source.external")}${props.message.fromAgentId ? "/" + props.message.fromAgentId : ""}`.trim();
    return t("p2p.header.remoteToLocal", { source });
});
const statusLabel = computed(() => {
    switch (props.message.status) {
        case "ACKED":
            return t("p2p.status.acked");
        case "NACKED":
            return t("p2p.status.nacked");
        case "FAILED":
            return t("p2p.status.failed");
        case "RECEIVED":
            return t("p2p.status.received");
        default:
            return t("p2p.status.sent");
    }
});
const formattedTimestamp = computed(() => {
    const raw = props.message.timestamp;
    if (!raw)
        return "";
    const date = new Date(raw);
    if (Number.isNaN(date.getTime()))
        return raw;
    return date.toLocaleString();
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
(__VLS_ctx.formattedTimestamp);
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
    (__VLS_ctx.t("p2p.failReason", { reason: __VLS_ctx.message.reason }));
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
            t: t,
            headerLabel: headerLabel,
            statusLabel: statusLabel,
            formattedTimestamp: formattedTimestamp,
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
