/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed } from "vue";
import { marked } from "marked";
import DOMPurify from "dompurify";
import ToolResultCard from "./ToolResultCard.vue";
import { useI18n } from "../i18n";
const props = defineProps();
const { t } = useI18n();
const role = computed(() => props.message.role);
const roleLabel = computed(() => {
    if (props.message.role === "user")
        return t("chat.role.user");
    if (props.message.role === "assistant")
        return t("chat.role.assistant");
    if (props.message.role === "tool")
        return t("chat.role.tool");
    return t("chat.role.system");
});
const formattedTimestamp = computed(() => {
    const raw = props.message.timestamp;
    if (!raw)
        return "";
    const date = new Date(raw);
    if (Number.isNaN(date.getTime()))
        return raw;
    return date.toLocaleString();
});
const isDaytime = computed(() => {
    const raw = props.message.timestamp;
    if (!raw)
        return true;
    const date = new Date(raw);
    if (Number.isNaN(date.getTime()))
        return true;
    const hour = date.getHours();
    return hour >= 6 && hour < 18;
});
function extractLlmError(content) {
    const text = content || "";
    if (!text.includes("[LLM_ERROR]"))
        return null;
    const body = text.replace(/^\s*\[LLM_ERROR\]\s*/i, "");
    const messageMatch = body.match(/message=([^|]+)(\||$)/i);
    const hintMatch = body.match(/提示：(.+)$/);
    const message = messageMatch ? messageMatch[1].trim() : body.trim();
    const hint = hintMatch ? hintMatch[1].trim() : "";
    return { message, hint };
}
function formatDisplayContent(content) {
    const error = extractLlmError(content);
    if (!error)
        return content || "";
    const lines = [`**${t("chat.llmError.title")}**`, `- ${t("chat.llmError.error")}：${error.message}`];
    if (error.hint) {
        lines.push(`- ${t("chat.llmError.hint")}：${error.hint}`);
    }
    return lines.join("\n");
}
const renderedContent = computed(() => {
    const content = formatDisplayContent(props.message.content || "");
    const rawHtml = marked.parse(content);
    return DOMPurify.sanitize(rawHtml);
});
const hasContent = computed(() => {
    const content = props.message.content || "";
    return content.trim().length > 0;
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
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "time-with-icon" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "time-icon" },
    'aria-hidden': "true",
});
(__VLS_ctx.isDaytime ? "☀️" : "🌙");
(__VLS_ctx.formattedTimestamp);
if (__VLS_ctx.hasContent) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "markdown-body" },
    });
    __VLS_asFunctionalDirective(__VLS_directives.vHtml)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.renderedContent) }, null, null);
}
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
/** @type {__VLS_StyleScopedClasses['time-with-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['time-icon']} */ ;
/** @type {__VLS_StyleScopedClasses['markdown-body']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ToolResultCard: ToolResultCard,
            role: role,
            roleLabel: roleLabel,
            formattedTimestamp: formattedTimestamp,
            isDaytime: isDaytime,
            renderedContent: renderedContent,
            hasContent: hasContent,
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
