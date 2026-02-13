/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref } from "vue";
const emit = defineEmits();
const text = ref("");
const handleSend = () => {
    if (!text.value.trim())
        return;
    emit("send", text.value.trim());
    text.value = "";
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "chat-composer" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
    value: (__VLS_ctx.text),
    rows: "3",
    placeholder: "输入指令或问题，支持多轮对话",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.handleSend) },
    ...{ class: "button" },
    disabled: (!__VLS_ctx.text.trim()),
});
/** @type {__VLS_StyleScopedClasses['chat-composer']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            text: text,
            handleSend: handleSend,
        };
    },
    __typeEmits: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeEmits: {},
});
; /* PartiallyEnd: #4569/main.vue */
