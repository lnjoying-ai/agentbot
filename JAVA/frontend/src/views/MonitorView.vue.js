/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed } from "vue";
import StatCard from "../components/StatCard.vue";
import HealthBadge from "../components/HealthBadge.vue";
import { useMonitorStore } from "../store/monitor";
import { useI18n } from "../i18n";
const { health, stats, logEntries, lastInit, refresh, fetchLogs, initWorkspace } = useMonitorStore();
const { t, locale } = useI18n();
fetchLogs();
const healthLabel = computed(() => health.value === "ok"
    ? t("monitor.health.healthy")
    : health.value === "degraded"
        ? t("monitor.health.warning")
        : t("monitor.health.unavailable"));
const serviceStatusText = computed(() => {
    if (stats.status === "ok")
        return t("topbar.health.ok");
    if (stats.status === "degraded")
        return t("topbar.health.degraded");
    if (stats.status === "error")
        return t("topbar.health.error");
    return stats.status;
});
function formatPayload(payload) {
    try {
        return JSON.stringify(payload);
    }
    catch (e) {
        return "";
    }
}
function formatTimestamp(value) {
    if (!value)
        return "-";
    const date = new Date(value);
    if (Number.isNaN(date.getTime()))
        return value;
    return date.toLocaleString(locale.value);
}
function formatInterval(seconds) {
    if (seconds === undefined || seconds === null)
        return "-";
    return t("monitor.intervalSeconds", { count: seconds });
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
(__VLS_ctx.t("monitor.title"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card-grid" },
    ...{ style: {} },
});
/** @type {[typeof StatCard, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(StatCard, new StatCard({
    title: (__VLS_ctx.t('monitor.uptime')),
    value: (__VLS_ctx.stats.uptime),
    subtitle: (__VLS_ctx.t('monitor.uptimeSubtitle')),
}));
const __VLS_1 = __VLS_0({
    title: (__VLS_ctx.t('monitor.uptime')),
    value: (__VLS_ctx.stats.uptime),
    subtitle: (__VLS_ctx.t('monitor.uptimeSubtitle')),
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
/** @type {[typeof StatCard, ]} */ ;
// @ts-ignore
const __VLS_3 = __VLS_asFunctionalComponent(StatCard, new StatCard({
    title: (__VLS_ctx.t('monitor.toolCalls')),
    value: (__VLS_ctx.stats.toolCalls),
    subtitle: (__VLS_ctx.t('monitor.toolCallsSubtitle')),
}));
const __VLS_4 = __VLS_3({
    title: (__VLS_ctx.t('monitor.toolCalls')),
    value: (__VLS_ctx.stats.toolCalls),
    subtitle: (__VLS_ctx.t('monitor.toolCallsSubtitle')),
}, ...__VLS_functionalComponentArgsRest(__VLS_3));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("monitor.serviceStatus"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
/** @type {[typeof HealthBadge, ]} */ ;
// @ts-ignore
const __VLS_6 = __VLS_asFunctionalComponent(HealthBadge, new HealthBadge({
    status: (__VLS_ctx.health),
    text: (__VLS_ctx.healthLabel),
}));
const __VLS_7 = __VLS_6({
    status: (__VLS_ctx.health),
    text: (__VLS_ctx.healthLabel),
}, ...__VLS_functionalComponentArgsRest(__VLS_6));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ style: {} },
});
(__VLS_ctx.serviceStatusText);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("monitor.modelConfig"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
(__VLS_ctx.stats.model);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.refresh) },
    ...{ class: "button secondary" },
});
(__VLS_ctx.t("monitor.syncHealth"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card-grid" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("monitor.systemInfo"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
(__VLS_ctx.t("monitor.workspace"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
(__VLS_ctx.stats.workspace);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("monitor.heartbeatCron"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
(__VLS_ctx.t("monitor.heartbeat"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
(__VLS_ctx.stats.heartbeat?.enabled ? __VLS_ctx.t("common.enabled") : __VLS_ctx.t("common.disabled"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ style: {} },
});
(__VLS_ctx.t("monitor.interval", { value: __VLS_ctx.formatInterval(__VLS_ctx.stats.heartbeat?.intervalSeconds) }));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
(__VLS_ctx.t("nav.cron"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
(__VLS_ctx.stats.cron?.enabled ? __VLS_ctx.t("common.enabled") : __VLS_ctx.t("common.disabled"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ style: {} },
});
(__VLS_ctx.t("monitor.defaultInterval", { value: __VLS_ctx.formatInterval(__VLS_ctx.stats.cron?.defaultIntervalSeconds) }));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("monitor.p2pMetrics"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
(__VLS_ctx.t("monitor.connectionsOpened", { count: __VLS_ctx.stats.p2p?.connectionsOpened ?? 0 }));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
(__VLS_ctx.t("monitor.connectionsClosed", { count: __VLS_ctx.stats.p2p?.connectionsClosed ?? 0 }));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
(__VLS_ctx.t("monitor.handshakesCompleted", { count: __VLS_ctx.stats.p2p?.handshakesCompleted ?? 0 }));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
(__VLS_ctx.t("monitor.messages", { received: __VLS_ctx.stats.p2p?.messagesReceived ?? 0, sent: __VLS_ctx.stats.p2p?.messagesSent ?? 0 }));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
(__VLS_ctx.stats.p2p?.acks ?? 0);
(__VLS_ctx.stats.p2p?.nacks ?? 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
(__VLS_ctx.t("monitor.retries", { count: __VLS_ctx.stats.p2p?.retries ?? 0 }));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("monitor.channelStatus"));
if (Object.keys(__VLS_ctx.stats.channelStatus).length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    (__VLS_ctx.t("monitor.noChannels"));
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    for (const [status, channel] of __VLS_getVForSourceType((__VLS_ctx.stats.channelStatus))) {
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
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
(__VLS_ctx.t("monitor.opsTitle"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("monitor.workspaceInit"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
(__VLS_ctx.t("monitor.workspaceInitDesc"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.initWorkspace) },
    ...{ class: "button" },
});
(__VLS_ctx.t("monitor.runInit"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.refresh) },
    ...{ class: "button secondary" },
});
(__VLS_ctx.t("monitor.refreshStatus"));
if (__VLS_ctx.lastInit) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    (__VLS_ctx.t("monitor.lastInit", { status: __VLS_ctx.lastInit.ok ? __VLS_ctx.t("monitor.health.healthy") : __VLS_ctx.t("monitor.health.unavailable"), count: __VLS_ctx.lastInit.files?.length ?? 0 }));
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("monitor.logs"));
if (__VLS_ctx.logEntries.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    (__VLS_ctx.t("monitor.noLogs"));
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    for (const [entry] of __VLS_getVForSourceType((__VLS_ctx.logEntries))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            key: (entry.timestamp + entry.type),
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        (__VLS_ctx.formatTimestamp(entry.timestamp));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        (entry.type);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        (__VLS_ctx.formatPayload(entry.payload));
    }
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (() => __VLS_ctx.fetchLogs()) },
    ...{ class: "button secondary" },
});
(__VLS_ctx.t("monitor.refreshLogs"));
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['card-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['card-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['card-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            StatCard: StatCard,
            HealthBadge: HealthBadge,
            health: health,
            stats: stats,
            logEntries: logEntries,
            lastInit: lastInit,
            refresh: refresh,
            fetchLogs: fetchLogs,
            initWorkspace: initWorkspace,
            t: t,
            healthLabel: healthLabel,
            serviceStatusText: serviceStatusText,
            formatPayload: formatPayload,
            formatTimestamp: formatTimestamp,
            formatInterval: formatInterval,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
