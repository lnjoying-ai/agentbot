/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { onMounted, ref } from "vue";
import { RouterView } from "vue-router";
import SideNav from "./SideNav.vue";
import TopBar from "./TopBar.vue";
import { useMonitorStore } from "../store/monitor";
const NAV_COLLAPSE_KEY = "agentbot.nav.collapsed";
const monitor = useMonitorStore();
const isNavCollapsed = ref(true);
function toggleNav() {
    isNavCollapsed.value = !isNavCollapsed.value;
    if (typeof window !== "undefined") {
        window.localStorage.setItem(NAV_COLLAPSE_KEY, String(isNavCollapsed.value));
    }
}
onMounted(() => {
    if (typeof window !== "undefined") {
        const stored = window.localStorage.getItem(NAV_COLLAPSE_KEY);
        if (stored !== null) {
            isNavCollapsed.value = stored === "true";
        }
    }
    monitor.refresh();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "app-shell" },
    ...{ class: ({ collapsed: __VLS_ctx.isNavCollapsed }) },
});
/** @type {[typeof SideNav, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(SideNav, new SideNav({
    ...{ 'onToggle': {} },
    collapsed: (__VLS_ctx.isNavCollapsed),
}));
const __VLS_1 = __VLS_0({
    ...{ 'onToggle': {} },
    collapsed: (__VLS_ctx.isNavCollapsed),
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
let __VLS_3;
let __VLS_4;
let __VLS_5;
const __VLS_6 = {
    onToggle: (__VLS_ctx.toggleNav)
};
var __VLS_2;
__VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({
    ...{ class: "main-area" },
});
/** @type {[typeof TopBar, ]} */ ;
// @ts-ignore
const __VLS_7 = __VLS_asFunctionalComponent(TopBar, new TopBar({}));
const __VLS_8 = __VLS_7({}, ...__VLS_functionalComponentArgsRest(__VLS_7));
const __VLS_10 = {}.RouterView;
/** @type {[typeof __VLS_components.RouterView, ]} */ ;
// @ts-ignore
const __VLS_11 = __VLS_asFunctionalComponent(__VLS_10, new __VLS_10({}));
const __VLS_12 = __VLS_11({}, ...__VLS_functionalComponentArgsRest(__VLS_11));
/** @type {__VLS_StyleScopedClasses['app-shell']} */ ;
/** @type {__VLS_StyleScopedClasses['main-area']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            RouterView: RouterView,
            SideNav: SideNav,
            TopBar: TopBar,
            isNavCollapsed: isNavCollapsed,
            toggleNav: toggleNav,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
