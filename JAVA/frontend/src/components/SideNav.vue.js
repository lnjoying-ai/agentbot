/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { useI18n } from "../i18n";
const props = defineProps();
const emit = defineEmits();
const route = useRoute();
const { t } = useI18n();
const collapsed = computed(() => props.collapsed ?? false);
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['nav-toggle']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-toggle']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-toggle']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-icon']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
    ...{ class: "side-nav" },
    ...{ class: ({ collapsed: __VLS_ctx.collapsed }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "brand" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "brand-main" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.img)({
    src: "/logo.png",
    ...{ class: "logo-img" },
    alt: "Logo",
});
if (!__VLS_ctx.collapsed) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({
        ...{ class: "brand-title" },
    });
    (__VLS_ctx.t("app.title"));
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.emit('toggle');
        } },
    ...{ class: "nav-toggle" },
    type: "button",
    'aria-label': (__VLS_ctx.collapsed ? __VLS_ctx.t('nav.expand') : __VLS_ctx.t('nav.collapse')),
    title: (__VLS_ctx.collapsed ? __VLS_ctx.t('nav.expand') : __VLS_ctx.t('nav.collapse')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    'stroke-width': "2",
    'stroke-linecap': "round",
    'stroke-linejoin': "round",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M16 5l-8 7 8 7",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.nav, __VLS_intrinsicElements.nav)({
    ...{ class: "nav-list" },
});
const __VLS_0 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    to: "/open-world",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/open-world' }) },
    title: (__VLS_ctx.t('nav.openWorld')),
}));
const __VLS_2 = __VLS_1({
    to: "/open-world",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/open-world' }) },
    title: (__VLS_ctx.t('nav.openWorld')),
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-icon" },
    'aria-hidden': "true",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    'stroke-width': "1.8",
    'stroke-linecap': "round",
    'stroke-linejoin': "round",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.circle)({
    cx: "12",
    cy: "12",
    r: "8",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M2 12h20",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M12 4a16 16 0 0 1 0 16",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M12 4a16 16 0 0 0 0 16",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-label" },
});
(__VLS_ctx.t("nav.openWorld"));
var __VLS_3;
const __VLS_4 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    to: "/chat",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/chat' }) },
    title: (__VLS_ctx.t('nav.chatCoop')),
}));
const __VLS_6 = __VLS_5({
    to: "/chat",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/chat' }) },
    title: (__VLS_ctx.t('nav.chatCoop')),
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
__VLS_7.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-icon" },
    'aria-hidden': "true",
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
    d: "M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4z",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-label" },
});
(__VLS_ctx.t("nav.chatCoop"));
var __VLS_7;
const __VLS_8 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    to: "/p2p-chat",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/p2p-chat' }) },
    title: (__VLS_ctx.t('nav.a2aChat')),
}));
const __VLS_10 = __VLS_9({
    to: "/p2p-chat",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/p2p-chat' }) },
    title: (__VLS_ctx.t('nav.a2aChat')),
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
__VLS_11.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-icon" },
    'aria-hidden': "true",
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
    d: "M10 13a5 5 0 0 1 0-7l1-1a5 5 0 0 1 7 7l-1 1",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M14 11a5 5 0 0 1 0 7l-1 1a5 5 0 0 1-7-7l1-1",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-label" },
});
(__VLS_ctx.t("nav.a2aChat"));
var __VLS_11;
const __VLS_12 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
    to: "/agents",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/agents' }) },
    title: (__VLS_ctx.t('nav.agentManage')),
}));
const __VLS_14 = __VLS_13({
    to: "/agents",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/agents' }) },
    title: (__VLS_ctx.t('nav.agentManage')),
}, ...__VLS_functionalComponentArgsRest(__VLS_13));
__VLS_15.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-icon" },
    'aria-hidden': "true",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    'stroke-width': "1.8",
    'stroke-linecap': "round",
    'stroke-linejoin': "round",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.circle)({
    cx: "9",
    cy: "8",
    r: "3",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.circle)({
    cx: "17",
    cy: "8",
    r: "3",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M3 20a6 6 0 0 1 12 0",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M13 20a5 5 0 0 1 8 0",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-label" },
});
(__VLS_ctx.t("nav.agentManage"));
var __VLS_15;
const __VLS_16 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
    to: "/skills",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/skills' }) },
    title: (__VLS_ctx.t('nav.skills')),
}));
const __VLS_18 = __VLS_17({
    to: "/skills",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/skills' }) },
    title: (__VLS_ctx.t('nav.skills')),
}, ...__VLS_functionalComponentArgsRest(__VLS_17));
__VLS_19.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-icon" },
    'aria-hidden': "true",
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
    d: "M14 7l3 3-7 7H7v-3z",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M3 21h6",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-label" },
});
(__VLS_ctx.t("nav.skills"));
var __VLS_19;
const __VLS_20 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
    to: "/skills/store",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/skills/store' }) },
    title: (__VLS_ctx.t('nav.skillStore')),
}));
const __VLS_22 = __VLS_21({
    to: "/skills/store",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/skills/store' }) },
    title: (__VLS_ctx.t('nav.skillStore')),
}, ...__VLS_functionalComponentArgsRest(__VLS_21));
__VLS_23.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-icon" },
    'aria-hidden': "true",
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
    d: "M3 7h18l-2 4H5z",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M5 11v8h14v-8",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M9 19v-6h6v6",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-label" },
});
(__VLS_ctx.t("nav.skillStore"));
var __VLS_23;
const __VLS_24 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
    to: "/cron",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/cron' }) },
    title: (__VLS_ctx.t('nav.cron')),
}));
const __VLS_26 = __VLS_25({
    to: "/cron",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/cron' }) },
    title: (__VLS_ctx.t('nav.cron')),
}, ...__VLS_functionalComponentArgsRest(__VLS_25));
__VLS_27.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-icon" },
    'aria-hidden': "true",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    'stroke-width': "1.8",
    'stroke-linecap': "round",
    'stroke-linejoin': "round",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.circle)({
    cx: "12",
    cy: "12",
    r: "8",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M12 8v5l3 2",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-label" },
});
(__VLS_ctx.t("nav.cron"));
var __VLS_27;
const __VLS_28 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
    to: "/workspace/files",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/workspace/files' }) },
    title: (__VLS_ctx.t('nav.cronFiles')),
}));
const __VLS_30 = __VLS_29({
    to: "/workspace/files",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/workspace/files' }) },
    title: (__VLS_ctx.t('nav.cronFiles')),
}, ...__VLS_functionalComponentArgsRest(__VLS_29));
__VLS_31.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-icon" },
    'aria-hidden': "true",
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
    d: "M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M14 3v6h6",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-label" },
});
(__VLS_ctx.t("nav.cronFiles"));
var __VLS_31;
const __VLS_32 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
    to: "/monitor",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/monitor' }) },
    title: (__VLS_ctx.t('nav.monitor')),
}));
const __VLS_34 = __VLS_33({
    to: "/monitor",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/monitor' }) },
    title: (__VLS_ctx.t('nav.monitor')),
}, ...__VLS_functionalComponentArgsRest(__VLS_33));
__VLS_35.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-icon" },
    'aria-hidden': "true",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    'stroke-width': "1.8",
    'stroke-linecap': "round",
    'stroke-linejoin': "round",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.rect)({
    x: "3",
    y: "4",
    width: "18",
    height: "12",
    rx: "2",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M8 20h8",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M12 16v4",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-label" },
});
(__VLS_ctx.t("nav.monitor"));
var __VLS_35;
const __VLS_36 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
    to: "/config",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/config' }) },
    title: (__VLS_ctx.t('nav.config')),
}));
const __VLS_38 = __VLS_37({
    to: "/config",
    ...{ class: "nav-item" },
    ...{ class: ({ active: __VLS_ctx.route.path === '/config' }) },
    title: (__VLS_ctx.t('nav.config')),
}, ...__VLS_functionalComponentArgsRest(__VLS_37));
__VLS_39.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-icon" },
    'aria-hidden': "true",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    'stroke-width': "1.8",
    'stroke-linecap': "round",
    'stroke-linejoin': "round",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.circle)({
    cx: "12",
    cy: "12",
    r: "3",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 0 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 0 1-4 0v-.2a1.7 1.7 0 0 0-1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 0 1 0-4h.2a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3 1.7 1.7 0 0 0 1-1.5V3a2 2 0 0 1 4 0v.2a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8 1.7 1.7 0 0 0 1.5 1H21a2 2 0 0 1 0 4h-.2a1.7 1.7 0 0 0-1.5 1z",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "nav-label" },
});
(__VLS_ctx.t("nav.config"));
var __VLS_39;
/** @type {__VLS_StyleScopedClasses['side-nav']} */ ;
/** @type {__VLS_StyleScopedClasses['brand']} */ ;
/** @type {__VLS_StyleScopedClasses['brand-main']} */ ;
/** @type {__VLS_StyleScopedClasses['logo-img']} */ ;
/** @type {__VLS_StyleScopedClasses['brand-title']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-toggle']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-list']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-label']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-label']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-label']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-label']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-label']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-label']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-label']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-label']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-label']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-item']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-label']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            RouterLink: RouterLink,
            emit: emit,
            route: route,
            t: t,
            collapsed: collapsed,
        };
    },
    __typeEmits: {},
    __typeProps: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeEmits: {},
    __typeProps: {},
});
; /* PartiallyEnd: #4569/main.vue */
