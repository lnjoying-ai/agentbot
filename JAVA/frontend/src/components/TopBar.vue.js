/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed } from "vue";
import HealthBadge from "./HealthBadge.vue";
import { useConfigStore } from "../store/config";
import { useMonitorStore } from "../store/monitor";
const config = useConfigStore();
const monitor = useMonitorStore();
const healthStatus = computed(() => monitor.health.value);
const healthLabel = computed(() => {
    if (monitor.health.value === "ok")
        return "健康";
    if (monitor.health.value === "degraded")
        return "波动";
    return "异常";
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.header, __VLS_intrinsicElements.header)({
    ...{ class: "top-bar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "status-group" },
});
/** @type {[typeof HealthBadge, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(HealthBadge, new HealthBadge({
    status: (__VLS_ctx.healthStatus),
    text: (__VLS_ctx.healthLabel),
}));
const __VLS_1 = __VLS_0({
    status: (__VLS_ctx.healthStatus),
    text: (__VLS_ctx.healthLabel),
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "badge" },
});
(__VLS_ctx.monitor.stats.model);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "badge" },
});
(__VLS_ctx.config.state.apiBaseUrl || '本地');
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "status-group" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "badge" },
});
(__VLS_ctx.monitor.stats.activeSessions);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "badge" },
});
(__VLS_ctx.monitor.stats.toolCalls);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.monitor.refresh) },
    ...{ class: "button secondary" },
});
/** @type {__VLS_StyleScopedClasses['top-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['status-group']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
/** @type {__VLS_StyleScopedClasses['status-group']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            HealthBadge: HealthBadge,
            config: config,
            monitor: monitor,
            healthStatus: healthStatus,
            healthLabel: healthLabel,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
