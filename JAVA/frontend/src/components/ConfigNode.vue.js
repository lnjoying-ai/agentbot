/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, ref, watch } from "vue";
import { useI18n } from "../i18n";
defineOptions({ name: "ConfigNode" });
const props = defineProps();
const emit = defineEmits();
const { t } = useI18n();
const level = computed(() => props.level ?? 0);
const isObject = computed(() => props.value !== null && typeof props.value === "object" && !Array.isArray(props.value));
const isArray = computed(() => Array.isArray(props.value));
const isBoolean = computed(() => typeof props.value === "boolean");
const isNumber = computed(() => typeof props.value === "number");
const isSecret = computed(() => isSecretKey(props.label));
const groupStyle = computed(() => ({ marginLeft: `${level.value * 12}px` }));
const arrayText = ref("");
watch(() => props.value, (val) => {
    if (isArray.value) {
        arrayText.value = JSON.stringify(val ?? [], null, 2);
    }
}, { immediate: true, deep: true });
const onTextInput = (event) => {
    const target = event.target;
    emit("update", props.path, target.value);
};
const onNumberInput = (event) => {
    const target = event.target;
    const raw = target.value;
    if (raw === "") {
        emit("update", props.path, null);
        return;
    }
    const parsed = Number(raw);
    emit("update", props.path, Number.isNaN(parsed) ? null : parsed);
};
const onBooleanChange = (event) => {
    const target = event.target;
    emit("update", props.path, target.value === "true");
};
const applyArray = () => {
    if (!isArray.value)
        return;
    try {
        const parsed = JSON.parse(arrayText.value || "[]");
        emit("update", props.path, parsed);
    }
    catch (error) {
        // ignore parse errors until user fixes JSON
    }
};
const forwardUpdate = (path, value) => {
    emit("update", path, value);
};
const formatLabel = (raw) => {
    if (!raw)
        return "";
    return raw
        .replace(/_/g, " ")
        .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
        .replace(/\s+/g, " ")
        .trim();
};
const translateLabel = (raw) => {
    const normalized = raw.replace(/[\s_-]/g, "").toLowerCase();
    const key = `config.label.${normalized}`;
    const translated = t(key);
    return translated === key ? formatLabel(raw) : translated;
};
const isSecretKey = (raw) => {
    const lower = raw.toLowerCase();
    return (lower.includes("key") ||
        lower.includes("token") ||
        lower.includes("secret") ||
        lower.includes("password"));
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
if (__VLS_ctx.isObject) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "config-group" },
        ...{ style: (__VLS_ctx.groupStyle) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "group-title" },
    });
    (__VLS_ctx.translateLabel(__VLS_ctx.label));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "group-body" },
    });
    for (const [childValue, childKey] of __VLS_getVForSourceType((__VLS_ctx.value))) {
        const __VLS_0 = {}.ConfigNode;
        /** @type {[typeof __VLS_components.ConfigNode, ]} */ ;
        // @ts-ignore
        const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
            ...{ 'onUpdate': {} },
            key: (String(childKey)),
            label: (String(childKey)),
            value: (childValue),
            path: ([...__VLS_ctx.path, String(childKey)]),
            level: (__VLS_ctx.level + 1),
        }));
        const __VLS_2 = __VLS_1({
            ...{ 'onUpdate': {} },
            key: (String(childKey)),
            label: (String(childKey)),
            value: (childValue),
            path: ([...__VLS_ctx.path, String(childKey)]),
            level: (__VLS_ctx.level + 1),
        }, ...__VLS_functionalComponentArgsRest(__VLS_1));
        let __VLS_4;
        let __VLS_5;
        let __VLS_6;
        const __VLS_7 = {
            onUpdate: (__VLS_ctx.forwardUpdate)
        };
        var __VLS_3;
    }
}
else if (__VLS_ctx.isArray) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "form-field" },
        ...{ style: (__VLS_ctx.groupStyle) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    (__VLS_ctx.translateLabel(__VLS_ctx.label));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
        ...{ onBlur: (__VLS_ctx.applyArray) },
        value: (__VLS_ctx.arrayText),
        rows: "4",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "muted" },
    });
    (__VLS_ctx.t("configNode.arrayHint"));
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "form-field" },
        ...{ style: (__VLS_ctx.groupStyle) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    (__VLS_ctx.translateLabel(__VLS_ctx.label));
    if (__VLS_ctx.isBoolean) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
            ...{ onChange: (__VLS_ctx.onBooleanChange) },
            value: (String(__VLS_ctx.value)),
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
            value: "true",
        });
        (__VLS_ctx.t("common.enable"));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
            value: "false",
        });
        (__VLS_ctx.t("common.disable"));
    }
    else if (__VLS_ctx.isNumber) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
            ...{ onInput: (__VLS_ctx.onNumberInput) },
            type: "number",
            value: (__VLS_ctx.value ?? ''),
        });
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
            ...{ onInput: (__VLS_ctx.onTextInput) },
            type: (__VLS_ctx.isSecret ? 'password' : 'text'),
            value: (__VLS_ctx.value ?? ''),
        });
    }
}
/** @type {__VLS_StyleScopedClasses['config-group']} */ ;
/** @type {__VLS_StyleScopedClasses['group-title']} */ ;
/** @type {__VLS_StyleScopedClasses['group-body']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            t: t,
            level: level,
            isObject: isObject,
            isArray: isArray,
            isBoolean: isBoolean,
            isNumber: isNumber,
            isSecret: isSecret,
            groupStyle: groupStyle,
            arrayText: arrayText,
            onTextInput: onTextInput,
            onNumberInput: onNumberInput,
            onBooleanChange: onBooleanChange,
            applyArray: applyArray,
            forwardUpdate: forwardUpdate,
            translateLabel: translateLabel,
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
