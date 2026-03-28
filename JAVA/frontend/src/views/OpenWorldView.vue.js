/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useI18n } from "../i18n";
import { useAgentStore } from "../store/agents";
const { t } = useI18n();
const store = useAgentStore();
const speedOptions = [0.5, 1, 2, 4];
const speed = ref(1);
const paused = ref(false);
const zones = computed(() => [
    { id: "build", label: t("world.zone.build"), x: 18, y: 26, color: "#7c8cff" },
    { id: "test", label: t("world.zone.test"), x: 70, y: 22, color: "#8ce0b1" },
    { id: "monitor", label: t("world.zone.monitor"), x: 58, y: 68, color: "#f5b36a" },
    { id: "rest", label: t("world.zone.rest"), x: 26, y: 72, color: "#f58ad8" }
]);
const worldAgents = ref([]);
let timer = null;
let statusTimer = null;
const totals = computed(() => {
    const total = worldAgents.value.length;
    const active = worldAgents.value.filter(agent => agent.state !== "offline").length;
    const working = worldAgents.value.filter(agent => agent.state === "working").length;
    const idle = worldAgents.value.filter(agent => agent.state === "idle").length;
    const offline = worldAgents.value.filter(agent => agent.state === "offline").length;
    return { total, active, working, idle, offline };
});
const statusLabel = (state) => {
    if (state === "working")
        return t("world.status.working");
    if (state === "idle")
        return t("world.status.idle");
    return t("world.status.offline");
};
const zoneStyle = (zone) => ({
    left: `${zone.x}%`,
    top: `${zone.y}%`,
    borderColor: zone.color,
    boxShadow: `0 0 24px ${zone.color}33`
});
const agentStyle = (agent) => ({
    left: `${agent.x}%`,
    top: `${agent.y}%`
});
const randomBetween = (min, max) => Math.random() * (max - min) + min;
const clamp = (value, min, max) => Math.min(max, Math.max(min, value));
const MIN_POS = 4;
const MAX_POS = 96;
const isOffline = (agent) => agent.enabled === false || agent.status === "inactive";
const syncAgents = () => {
    const current = new Map(worldAgents.value.map(agent => [agent.id, agent]));
    const next = [];
    store.agents.value.forEach(agent => {
        const existing = current.get(agent.id);
        const state = isOffline(agent)
            ? "offline"
            : agent.sessionStatus === "working"
                ? "working"
                : "idle";
        const base = existing ?? {
            id: agent.id,
            name: agent.displayName || agent.name || agent.id,
            status: agent.status,
            enabled: agent.enabled,
            x: randomBetween(6, 94),
            y: randomBetween(6, 94),
            targetX: randomBetween(0, 100),
            targetY: randomBetween(0, 100),
            speed: randomBetween(0.4, 0.9),
            state
        };
        base.name = agent.displayName || agent.name || agent.id;
        base.status = agent.status;
        base.enabled = agent.enabled;
        base.state = state;
        next.push(base);
    });
    worldAgents.value = next;
};
const moveAgent = (agent) => {
    const dx = agent.targetX - agent.x;
    const dy = agent.targetY - agent.y;
    const dist = Math.hypot(dx, dy);
    if (dist < 1)
        return;
    const speedFactor = agent.state === "working" ? 0.5 : 1;
    const step = agent.speed * speed.value * speedFactor;
    agent.x = clamp(agent.x + (dx / dist) * step, MIN_POS, MAX_POS);
    agent.y = clamp(agent.y + (dy / dist) * step, MIN_POS, MAX_POS);
};
const chooseTarget = (agent) => {
    const zone = zones.value[Math.floor(Math.random() * zones.value.length)];
    agent.targetX = Math.min(100, Math.max(0, zone.x + randomBetween(-12, 12)));
    agent.targetY = Math.min(100, Math.max(0, zone.y + randomBetween(-12, 12)));
};
const tick = () => {
    if (paused.value)
        return;
    worldAgents.value.forEach(agent => {
        if (agent.state === "offline")
            return;
        const dx = agent.targetX - agent.x;
        const dy = agent.targetY - agent.y;
        const dist = Math.hypot(dx, dy);
        if (dist < 1 || Math.random() < 0.02) {
            chooseTarget(agent);
        }
        moveAgent(agent);
    });
};
const scatterAgents = () => {
    worldAgents.value = worldAgents.value.map(agent => ({
        ...agent,
        x: randomBetween(MIN_POS, MAX_POS),
        y: randomBetween(MIN_POS, MAX_POS),
        targetX: randomBetween(MIN_POS, MAX_POS),
        targetY: randomBetween(MIN_POS, MAX_POS),
        state: agent.state === "offline" ? "offline" : agent.state
    }));
};
const togglePause = () => {
    paused.value = !paused.value;
};
onMounted(async () => {
    await store.fetchAgents();
    syncAgents();
    timer = window.setInterval(tick, 200);
    statusTimer = window.setInterval(() => {
        store.fetchAgents();
    }, 3000);
});
watch(store.agents, syncAgents);
onBeforeUnmount(() => {
    if (timer)
        window.clearInterval(timer);
    if (statusTimer)
        window.clearInterval(statusTimer);
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['speed-control']} */ ;
/** @type {__VLS_StyleScopedClasses['chip']} */ ;
/** @type {__VLS_StyleScopedClasses['world-map']} */ ;
/** @type {__VLS_StyleScopedClasses['world-map']} */ ;
/** @type {__VLS_StyleScopedClasses['world-map']} */ ;
/** @type {__VLS_StyleScopedClasses['world-map']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['panel-card']} */ ;
/** @type {__VLS_StyleScopedClasses['summary-row']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-card']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-card']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-card']} */ ;
/** @type {__VLS_StyleScopedClasses['working']} */ ;
/** @type {__VLS_StyleScopedClasses['status-pill']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-card']} */ ;
/** @type {__VLS_StyleScopedClasses['idle']} */ ;
/** @type {__VLS_StyleScopedClasses['status-pill']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-card']} */ ;
/** @type {__VLS_StyleScopedClasses['offline']} */ ;
/** @type {__VLS_StyleScopedClasses['status-pill']} */ ;
/** @type {__VLS_StyleScopedClasses['world-grid']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "world-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "world-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
(__VLS_ctx.t("world.title"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "subtitle" },
});
(__VLS_ctx.t("world.subtitle"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "world-controls" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "speed-control" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "label" },
});
(__VLS_ctx.t("world.speed"));
for (const [value] of __VLS_getVForSourceType((__VLS_ctx.speedOptions))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.speed = value;
            } },
        key: (value),
        ...{ class: "chip" },
        ...{ class: ({ active: __VLS_ctx.speed === value }) },
    });
    (value);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.togglePause) },
    ...{ class: "button" },
});
(__VLS_ctx.paused ? __VLS_ctx.t("world.resume") : __VLS_ctx.t("world.pause"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.scatterAgents) },
    ...{ class: "button secondary" },
});
(__VLS_ctx.t("world.scatter"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "world-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "world-map" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "world-bg" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "world-decor" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "flowers flowers-left" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "flowers flowers-right" },
});
for (const [zone] of __VLS_getVForSourceType((__VLS_ctx.zones))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "zone" },
        key: (zone.id),
        ...{ style: (__VLS_ctx.zoneStyle(zone)) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (zone.label);
}
for (const [agent] of __VLS_getVForSourceType((__VLS_ctx.worldAgents))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (agent.id),
        ...{ class: "agent-dot" },
        ...{ class: (agent.state) },
        ...{ style: (__VLS_ctx.agentStyle(agent)) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "agent-label" },
    });
    (agent.name);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
    ...{ class: "world-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "panel-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("world.summary"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "summary-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.t("world.total"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
(__VLS_ctx.totals.total);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "summary-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.t("world.active"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
(__VLS_ctx.totals.active);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "summary-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.t("world.working"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
(__VLS_ctx.totals.working);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "summary-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.t("world.idle"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
(__VLS_ctx.totals.idle);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "summary-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.t("world.offline"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
(__VLS_ctx.totals.offline);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "panel-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("world.agentStatus"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "agent-list" },
});
for (const [agent] of __VLS_getVForSourceType((__VLS_ctx.worldAgents))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (agent.id),
        ...{ class: "agent-card" },
        ...{ class: (agent.state) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "agent-info" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (agent.name);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "muted" },
    });
    (agent.id);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "status-pill" },
    });
    (__VLS_ctx.statusLabel(agent.state));
}
/** @type {__VLS_StyleScopedClasses['world-page']} */ ;
/** @type {__VLS_StyleScopedClasses['world-header']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['subtitle']} */ ;
/** @type {__VLS_StyleScopedClasses['world-controls']} */ ;
/** @type {__VLS_StyleScopedClasses['speed-control']} */ ;
/** @type {__VLS_StyleScopedClasses['label']} */ ;
/** @type {__VLS_StyleScopedClasses['chip']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['world-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['world-map']} */ ;
/** @type {__VLS_StyleScopedClasses['world-bg']} */ ;
/** @type {__VLS_StyleScopedClasses['world-decor']} */ ;
/** @type {__VLS_StyleScopedClasses['flowers']} */ ;
/** @type {__VLS_StyleScopedClasses['flowers-left']} */ ;
/** @type {__VLS_StyleScopedClasses['flowers']} */ ;
/** @type {__VLS_StyleScopedClasses['flowers-right']} */ ;
/** @type {__VLS_StyleScopedClasses['zone']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-dot']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-label']} */ ;
/** @type {__VLS_StyleScopedClasses['world-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['panel-card']} */ ;
/** @type {__VLS_StyleScopedClasses['summary-row']} */ ;
/** @type {__VLS_StyleScopedClasses['summary-row']} */ ;
/** @type {__VLS_StyleScopedClasses['summary-row']} */ ;
/** @type {__VLS_StyleScopedClasses['summary-row']} */ ;
/** @type {__VLS_StyleScopedClasses['summary-row']} */ ;
/** @type {__VLS_StyleScopedClasses['panel-card']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-list']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-card']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-info']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['status-pill']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            t: t,
            speedOptions: speedOptions,
            speed: speed,
            paused: paused,
            zones: zones,
            worldAgents: worldAgents,
            totals: totals,
            statusLabel: statusLabel,
            zoneStyle: zoneStyle,
            agentStyle: agentStyle,
            scatterAgents: scatterAgents,
            togglePause: togglePause,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
