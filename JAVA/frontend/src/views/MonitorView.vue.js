/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import StatCard from "../components/StatCard.vue";
import { useMonitorStore } from "../store/monitor";
const monitor = useMonitorStore();
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card-grid" },
    ...{ style: {} },
});
/** @type {[typeof StatCard, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(StatCard, new StatCard({
    title: "运行时长",
    value: (__VLS_ctx.monitor.stats.uptime),
    subtitle: "自上次启动",
}));
const __VLS_1 = __VLS_0({
    title: "运行时长",
    value: (__VLS_ctx.monitor.stats.uptime),
    subtitle: "自上次启动",
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
/** @type {[typeof StatCard, ]} */ ;
// @ts-ignore
const __VLS_3 = __VLS_asFunctionalComponent(StatCard, new StatCard({
    title: "活跃会话",
    value: (__VLS_ctx.monitor.stats.activeSessions),
    subtitle: "最近 15 分钟",
}));
const __VLS_4 = __VLS_3({
    title: "活跃会话",
    value: (__VLS_ctx.monitor.stats.activeSessions),
    subtitle: "最近 15 分钟",
}, ...__VLS_functionalComponentArgsRest(__VLS_3));
/** @type {[typeof StatCard, ]} */ ;
// @ts-ignore
const __VLS_6 = __VLS_asFunctionalComponent(StatCard, new StatCard({
    title: "工具调用",
    value: (__VLS_ctx.monitor.stats.toolCalls),
    subtitle: "今日累计",
}));
const __VLS_7 = __VLS_6({
    title: "工具调用",
    value: (__VLS_ctx.monitor.stats.toolCalls),
    subtitle: "今日累计",
}, ...__VLS_functionalComponentArgsRest(__VLS_6));
/** @type {[typeof StatCard, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(StatCard, new StatCard({
    title: "队列深度",
    value: (__VLS_ctx.monitor.stats.queueDepth),
    subtitle: "等待任务",
}));
const __VLS_10 = __VLS_9({
    title: "队列深度",
    value: (__VLS_ctx.monitor.stats.queueDepth),
    subtitle: "等待任务",
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "sparkline" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
(__VLS_ctx.monitor.stats.latencyP50);
(__VLS_ctx.monitor.stats.latencyP95);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
(__VLS_ctx.monitor.stats.errorRate);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
for (const [status, channel] of __VLS_getVForSourceType((__VLS_ctx.monitor.stats.channelStatus))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (channel),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (channel);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ style: {} },
    });
    (status);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
(__VLS_ctx.monitor.stats.model);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.monitor.refresh) },
    ...{ class: "button secondary" },
});
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['card-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['card-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['sparkline']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            StatCard: StatCard,
            monitor: monitor,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
