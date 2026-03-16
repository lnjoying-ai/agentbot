/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref } from "vue";
import { useI18n } from "../i18n";
import { useChatStore } from "../store/chat";
const emit = defineEmits();
const text = ref("");
const { t } = useI18n();
const chat = useChatStore();
const isDragging = ref(false);
const dragDepth = ref(0);
const uploading = ref(false);
const uploadError = ref(null);
const lastUpload = ref(null);
const handleSend = () => {
    if (!text.value.trim())
        return;
    emit("send", text.value.trim());
    text.value = "";
};
const onKeydown = (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        handleSend();
    }
};
const onDragEnter = () => {
    dragDepth.value += 1;
    isDragging.value = true;
};
const onDragOver = () => {
    if (!isDragging.value) {
        isDragging.value = true;
    }
};
const onDragLeave = () => {
    dragDepth.value = Math.max(0, dragDepth.value - 1);
    if (dragDepth.value === 0) {
        isDragging.value = false;
    }
};
const onDrop = (event) => {
    dragDepth.value = 0;
    isDragging.value = false;
    const files = event.dataTransfer?.files;
    if (files && files.length) {
        void handleFiles(files);
    }
};
const onPaste = (event) => {
    const items = event.clipboardData?.items;
    if (!items || !items.length)
        return;
    const files = [];
    for (const item of Array.from(items)) {
        if (item.kind === "file") {
            const file = item.getAsFile();
            if (file)
                files.push(file);
        }
    }
    if (!files.length)
        return;
    event.preventDefault();
    void handleFiles(files);
};
const handleFiles = async (files) => {
    const list = Array.from(files);
    if (!list.length)
        return;
    uploading.value = true;
    uploadError.value = null;
    lastUpload.value = null;
    for (const file of list) {
        try {
            const result = await chat.uploadFile(file);
            if (!result.ok || !result.path) {
                throw new Error(result.error || "upload failed");
            }
            uploadError.value = null;
            lastUpload.value = {
                name: result.originalName || result.storedName || file.name,
                path: result.path
            };
            appendToText(result.path);
        }
        catch (error) {
            uploadError.value = t("chat.composer.uploadFailed");
        }
    }
    uploading.value = false;
};
const appendToText = (path) => {
    const prefix = text.value.trim().length ? "\n" : "";
    text.value = `${text.value}${prefix}${path}`;
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['chat-composer']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-composer']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-status']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-status']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ onDragenter: (__VLS_ctx.onDragEnter) },
    ...{ onDragover: (__VLS_ctx.onDragOver) },
    ...{ onDragleave: (__VLS_ctx.onDragLeave) },
    ...{ onDrop: (__VLS_ctx.onDrop) },
    ...{ class: "chat-composer" },
});
if (__VLS_ctx.isDragging) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "drop-mask" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.t("chat.composer.dragHint"));
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
    ...{ onKeydown: (__VLS_ctx.onKeydown) },
    ...{ onPaste: (__VLS_ctx.onPaste) },
    value: (__VLS_ctx.text),
    rows: "4",
    placeholder: (__VLS_ctx.t('chat.composer.placeholder')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "composer-actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.handleSend) },
    ...{ class: "button" },
    disabled: (!__VLS_ctx.text.trim()),
});
(__VLS_ctx.t("chat.composer.send"));
if (__VLS_ctx.uploading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "upload-status" },
    });
    (__VLS_ctx.t("chat.composer.uploading"));
}
else if (__VLS_ctx.uploadError) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "upload-status error" },
    });
    (__VLS_ctx.uploadError);
}
else if (__VLS_ctx.lastUpload) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "upload-status success" },
    });
    (__VLS_ctx.t("chat.composer.uploaded", { name: __VLS_ctx.lastUpload.name }));
}
/** @type {__VLS_StyleScopedClasses['chat-composer']} */ ;
/** @type {__VLS_StyleScopedClasses['drop-mask']} */ ;
/** @type {__VLS_StyleScopedClasses['composer-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-status']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-status']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['upload-status']} */ ;
/** @type {__VLS_StyleScopedClasses['success']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            text: text,
            t: t,
            isDragging: isDragging,
            uploading: uploading,
            uploadError: uploadError,
            lastUpload: lastUpload,
            handleSend: handleSend,
            onKeydown: onKeydown,
            onDragEnter: onDragEnter,
            onDragOver: onDragOver,
            onDragLeave: onDragLeave,
            onDrop: onDrop,
            onPaste: onPaste,
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
