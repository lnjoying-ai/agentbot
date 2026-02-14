/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed } from "vue";
import { marked } from "marked";
import DOMPurify from "dompurify";
import { useConfigStore } from "../store/config";
const props = defineProps();
const { state: config } = useConfigStore();
const renderedOutput = computed(() => {
    let output = props.tool.output || "";
    // Convert absolute workspace paths to relative URLs if possible
    if (config.workspaceDir) {
        const escapedDir = config.workspaceDir.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        const regex = new RegExp(escapedDir.replace(/\\\\/g, '[\\\\/]') + '[\\\\/]([\\w\\.-]+\\.(png|jpg|jpeg|gif|webp))', 'gi');
        output = output.replace(regex, (match, filename) => {
            return `![${filename}](/workspace/${filename})`;
        });
    }
    const rawHtml = marked.parse(output);
    return DOMPurify.sanitize(rawHtml);
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "tool-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "title" },
});
(__VLS_ctx.tool.name);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
(__VLS_ctx.tool.status);
(__VLS_ctx.tool.latencyMs ?? 0);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "markdown-body" },
});
__VLS_asFunctionalDirective(__VLS_directives.vHtml)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.renderedOutput) }, null, null);
/** @type {__VLS_StyleScopedClasses['tool-card']} */ ;
/** @type {__VLS_StyleScopedClasses['title']} */ ;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            renderedOutput: renderedOutput,
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
