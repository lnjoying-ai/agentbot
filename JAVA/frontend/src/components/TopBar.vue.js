/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import HealthBadge from "./HealthBadge.vue";
import { useConfigStore } from "../store/config";
import { useMonitorStore } from "../store/monitor";
import { useI18n } from "../i18n";
import { fetchAuthState, getAuthState, logout } from "../store/auth";
const config = useConfigStore();
const monitor = useMonitorStore();
const router = useRouter();
const { t, locale, setLocale } = useI18n();
const auth = getAuthState();
const currentLocale = computed({
    get: () => locale.value,
    set: (value) => setLocale(value)
});
const showAuth = computed(() => auth.value.enabled && auth.value.authenticated);
const healthStatus = computed(() => monitor.health.value);
const healthLabel = computed(() => {
    if (monitor.health.value === "ok")
        return t("topbar.health.ok");
    if (monitor.health.value === "degraded")
        return t("topbar.health.degraded");
    return t("topbar.health.error");
});
onMounted(() => {
    fetchAuthState();
});
async function handleLogout() {
    await logout();
    await router.replace("/login");
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['icon-button']} */ ;
/** @type {__VLS_StyleScopedClasses['icon-button']} */ ;
/** @type {__VLS_StyleScopedClasses['lang-switch']} */ ;
/** @type {__VLS_StyleScopedClasses['lang-switch']} */ ;
/** @type {__VLS_StyleScopedClasses['lang-switch']} */ ;
/** @type {__VLS_StyleScopedClasses['lang-switch']} */ ;
/** @type {__VLS_StyleScopedClasses['lang-switch']} */ ;
/** @type {__VLS_StyleScopedClasses['lang-switch']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.header, __VLS_intrinsicElements.header)({
    ...{ class: "top-bar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
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
    ...{ class: "badge badge-truncate" },
});
(__VLS_ctx.t("topbar.model"));
(__VLS_ctx.monitor.stats.model);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "badge badge-truncate" },
});
(__VLS_ctx.t("topbar.backend"));
(__VLS_ctx.config.state.serverBaseUrl || __VLS_ctx.t("topbar.local"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "status-group" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "badge" },
});
(__VLS_ctx.t("topbar.activeSessions"));
(__VLS_ctx.monitor.stats.activeSessions);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "badge" },
});
(__VLS_ctx.t("topbar.toolCalls"));
(__VLS_ctx.monitor.stats.toolCalls);
if (__VLS_ctx.showAuth) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "badge badge-truncate" },
    });
    (__VLS_ctx.t("auth.user"));
    (__VLS_ctx.auth.username || "-");
}
if (__VLS_ctx.showAuth) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.handleLogout) },
        ...{ class: "icon-button" },
        title: (__VLS_ctx.t('auth.logout')),
        'aria-label': (__VLS_ctx.t('auth.logout')),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
        viewBox: "0 0 24 24",
        fill: "none",
        stroke: "currentColor",
        'stroke-width': "1.8",
        'stroke-linecap': "round",
        'stroke-linejoin': "round",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
        d: "M10 17l5-5-5-5",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
        d: "M15 12H3",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
        d: "M21 4v16a2 2 0 0 1-2 2H9",
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.monitor.refresh) },
    ...{ class: "icon-button" },
    title: (__VLS_ctx.t('topbar.refresh')),
    'aria-label': (__VLS_ctx.t('topbar.refresh')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    'stroke-width': "1.8",
    'stroke-linecap': "round",
    'stroke-linejoin': "round",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M21 12a9 9 0 1 1-2.64-6.36",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M21 3v6h-6",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "lang-switch" },
    title: (__VLS_ctx.t('topbar.language')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    'stroke-width': "1.8",
    'stroke-linecap': "round",
    'stroke-linejoin': "round",
    'aria-hidden': "true",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.circle)({
    cx: "12",
    cy: "12",
    r: "9",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M3 12h18",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M12 3a15 15 0 0 1 0 18",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M12 3a15 15 0 0 0 0 18",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.currentLocale),
    'aria-label': (__VLS_ctx.t('topbar.language')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "zh-CN",
});
(__VLS_ctx.t("topbar.language.zh"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "en-US",
});
(__VLS_ctx.t("topbar.language.en"));
/** @type {__VLS_StyleScopedClasses['top-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['status-group']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
/** @type {__VLS_StyleScopedClasses['badge-truncate']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
/** @type {__VLS_StyleScopedClasses['badge-truncate']} */ ;
/** @type {__VLS_StyleScopedClasses['status-group']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
/** @type {__VLS_StyleScopedClasses['badge-truncate']} */ ;
/** @type {__VLS_StyleScopedClasses['icon-button']} */ ;
/** @type {__VLS_StyleScopedClasses['icon-button']} */ ;
/** @type {__VLS_StyleScopedClasses['lang-switch']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            HealthBadge: HealthBadge,
            config: config,
            monitor: monitor,
            t: t,
            auth: auth,
            currentLocale: currentLocale,
            showAuth: showAuth,
            healthStatus: healthStatus,
            healthLabel: healthLabel,
            handleLogout: handleLogout,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
