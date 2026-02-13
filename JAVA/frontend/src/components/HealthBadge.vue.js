/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed } from "vue";
const props = defineProps();
const badgeClass = computed(() => {
    if (props.status === "ok")
        return "health-ok";
    if (props.status === "degraded")
        return "health-warn";
    return "health-bad";
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "badge" },
    ...{ class: (__VLS_ctx.badgeClass) },
});
(__VLS_ctx.text);
/** @type {__VLS_StyleScopedClasses['badge']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            badgeClass: badgeClass,
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
