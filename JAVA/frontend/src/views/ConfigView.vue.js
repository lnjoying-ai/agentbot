/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, reactive } from "vue";
import { useConfigStore } from "../store/config";
import ConfigNode from "../components/ConfigNode.vue";
import { useI18n } from "../i18n";
const config = useConfigStore();
const { t } = useI18n();
const draft = reactive({ serverBaseUrl: config.state.serverBaseUrl });
const draftConfig = reactive({});
const clone = (value) => JSON.parse(JSON.stringify(value ?? {}));
const syncDraft = () => {
    draft.serverBaseUrl = config.state.serverBaseUrl;
    Object.keys(draftConfig).forEach((key) => delete draftConfig[key]);
    Object.assign(draftConfig, clone(config.state.config));
};
onMounted(async () => {
    await config.fetch();
    syncDraft();
});
const categoryEntries = computed(() => Object.entries(draftConfig));
const updatePath = (path, value) => {
    if (!path.length)
        return;
    let current = draftConfig;
    for (let i = 0; i < path.length - 1; i += 1) {
        const key = path[i];
        if (current[key] === null || current[key] === undefined) {
            current[key] = {};
        }
        current = current[key];
    }
    current[path[path.length - 1]] = value;
};
const save = async () => {
    config.state.serverBaseUrl = draft.serverBaseUrl;
    config.state.config = clone(draftConfig);
    await config.save();
    alert(t("config.saved"));
};
const reset = async () => {
    await config.fetch();
    syncDraft();
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
const isObject = (value) => value !== null && typeof value === "object" && !Array.isArray(value);
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
(__VLS_ctx.t("config.title"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("config.core"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("config.baseUrl"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "http://localhost:8080",
});
(__VLS_ctx.draft.serverBaseUrl);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("config.configPath"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    value: (__VLS_ctx.config.state.configPath || __VLS_ctx.t('config.notAvailable')),
    disabled: true,
});
if (__VLS_ctx.categoryEntries.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "card" },
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
    (__VLS_ctx.t("config.loadingTitle"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "muted" },
    });
    (__VLS_ctx.t("config.loadingDesc"));
}
for (const [[categoryKey, categoryValue]] of __VLS_getVForSourceType((__VLS_ctx.categoryEntries))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (String(categoryKey)),
        ...{ class: "card" },
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "card-header" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
    (__VLS_ctx.translateLabel(String(categoryKey)));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "muted" },
    });
    (__VLS_ctx.t("config.categoryDesc"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "config-grid" },
    });
    if (__VLS_ctx.isObject(categoryValue)) {
        for (const [childValue, childKey] of __VLS_getVForSourceType((categoryValue))) {
            /** @type {[typeof ConfigNode, ]} */ ;
            // @ts-ignore
            const __VLS_0 = __VLS_asFunctionalComponent(ConfigNode, new ConfigNode({
                ...{ 'onUpdate': {} },
                key: (String(childKey)),
                label: (String(childKey)),
                value: (childValue),
                path: ([String(categoryKey), String(childKey)]),
                level: (0),
            }));
            const __VLS_1 = __VLS_0({
                ...{ 'onUpdate': {} },
                key: (String(childKey)),
                label: (String(childKey)),
                value: (childValue),
                path: ([String(categoryKey), String(childKey)]),
                level: (0),
            }, ...__VLS_functionalComponentArgsRest(__VLS_0));
            let __VLS_3;
            let __VLS_4;
            let __VLS_5;
            const __VLS_6 = {
                onUpdate: (__VLS_ctx.updatePath)
            };
            var __VLS_2;
        }
    }
    else {
        /** @type {[typeof ConfigNode, ]} */ ;
        // @ts-ignore
        const __VLS_7 = __VLS_asFunctionalComponent(ConfigNode, new ConfigNode({
            ...{ 'onUpdate': {} },
            label: (String(categoryKey)),
            value: (categoryValue),
            path: ([String(categoryKey)]),
            level: (0),
        }));
        const __VLS_8 = __VLS_7({
            ...{ 'onUpdate': {} },
            label: (String(categoryKey)),
            value: (categoryValue),
            path: ([String(categoryKey)]),
            level: (0),
        }, ...__VLS_functionalComponentArgsRest(__VLS_7));
        let __VLS_10;
        let __VLS_11;
        let __VLS_12;
        const __VLS_13 = {
            onUpdate: (__VLS_ctx.updatePath)
        };
        var __VLS_9;
    }
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("config.saveTitle"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
(__VLS_ctx.t("config.saveDesc"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "config-actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.save) },
    ...{ class: "button" },
});
(__VLS_ctx.t("common.save"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.reset) },
    ...{ class: "button secondary" },
});
(__VLS_ctx.t("common.reload"));
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['card-header']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['config-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['config-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ConfigNode: ConfigNode,
            config: config,
            t: t,
            draft: draft,
            categoryEntries: categoryEntries,
            updatePath: updatePath,
            save: save,
            reset: reset,
            translateLabel: translateLabel,
            isObject: isObject,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
