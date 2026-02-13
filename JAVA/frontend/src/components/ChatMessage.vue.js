/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed } from "vue";
import { marked } from "marked";
import DOMPurify from "dompurify";
import ToolResultCard from "./ToolResultCard.vue";
const props = defineProps();
const role = computed(() => props.message.role);
const roleLabel = computed(() => {
    if (props.message.role === "user")
        return "你";
    if (props.message.role === "assistant")
        return "Agent";
    if (props.message.role === "tool")
        return "工具";
    return "系统";
});
const renderedContent = computed(() => {
    const rawHtml = marked.parse(props.message.content || "");
    return DOMPurify.sanitize(rawHtml);
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "message" },
    ...{ class: (__VLS_ctx.role) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "message-meta" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.roleLabel);
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.message.timestamp);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "markdown-body" },
});
__VLS_asFunctionalDirective(__VLS_directives.vHtml)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.renderedContent) }, null, null);
if (__VLS_ctx.message.toolResults?.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    for (const [tool] of __VLS_getVForSourceType((__VLS_ctx.message.toolResults))) {
        /** @type {[typeof ToolResultCard, ]} */ ;
        // @ts-ignore
        const __VLS_0 = __VLS_asFunctionalComponent(ToolResultCard, new ToolResultCard({
            key: (tool.id),
            tool: (tool),
        }));
        const __VLS_1 = __VLS_0({
            key: (tool.id),
            tool: (tool),
        }, ...__VLS_functionalComponentArgsRest(__VLS_0));
    }
}
/** @type {__VLS_StyleScopedClasses['message']} */ ;
/** @type {__VLS_StyleScopedClasses['message-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ToolResultCard: ToolResultCard,
            role: role,
            roleLabel: roleLabel,
            renderedContent: renderedContent,
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
