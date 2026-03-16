/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, reactive, ref, toRefs, watch } from 'vue';
import { useAgentStore } from '../store/agents';
import { useI18n } from '../i18n';
const agentStore = useAgentStore();
const { t } = useI18n();
const loading = ref(false);
const saving = ref(null);
const search = ref('');
const currentAgentId = ref('default');
const state = reactive({
    apiKeyDraft: {},
    envDraft: {}
});
const { apiKeyDraft, envDraft } = toRefs(state);
const skillsData = computed(() => agentStore.agentSkills.get(currentAgentId.value) || null);
const detail = ref(null);
const filteredSkills = computed(() => {
    const list = skillsData.value?.available || [];
    const kw = search.value.trim().toLowerCase();
    if (!kw)
        return list;
    return list.filter(s => (s.name || '').toLowerCase().includes(kw) || (s.description || '').toLowerCase().includes(kw));
});
const blockedCount = computed(() => (skillsData.value?.available || []).filter(skill => skill.blocked).length);
const installableCount = computed(() => (skillsData.value?.available || []).filter(skill => canInstall(skill)).length);
watch(skillsData, val => {
    if (!val)
        return;
    const apiKey = {};
    const env = {};
    Object.entries(val.entries || {}).forEach(([k, v]) => {
        if (v.apiKey)
            apiKey[k] = v.apiKey;
        if (v.env)
            env[k] = JSON.stringify(v.env, null, 2);
    });
    apiKeyDraft.value = apiKey;
    envDraft.value = env;
}, { immediate: true });
onMounted(async () => {
    loading.value = true;
    try {
        await agentStore.fetchAgents();
        if (!agentStore.currentAgent.value && agentStore.agents.value.length) {
            currentAgentId.value = agentStore.agents.value[0].id;
            agentStore.switchToAgent(currentAgentId.value);
        }
        else {
            currentAgentId.value = agentStore.currentAgentId.value || 'default';
        }
        await agentStore.fetchAgentSkills(currentAgentId.value);
    }
    finally {
        loading.value = false;
    }
});
async function refresh() {
    loading.value = true;
    try {
        await agentStore.fetchAgentSkills(currentAgentId.value);
        await agentStore.fetchAgents();
    }
    finally {
        loading.value = false;
    }
}
function canInstall(skill) {
    const missingBins = skill?.missing?.bins || [];
    const installs = skill?.install || [];
    return missingBins.length > 0 && installs.length > 0;
}
function installLabel(skill) {
    const option = skill?.install?.[0];
    return option?.label || t('common.install');
}
function missingText(skill) {
    const missing = skill?.missing;
    if (!missing)
        return '';
    const parts = [];
    if (missing.bins?.length)
        parts.push(`bins:${missing.bins.join(', ')}`);
    if (missing.anyBins?.length)
        parts.push(`anyBins:${missing.anyBins.join(', ')}`);
    if (missing.env?.length)
        parts.push(`env:${missing.env.join(', ')}`);
    if (missing.config?.length)
        parts.push(`config:${missing.config.join(', ')}`);
    if (missing.os?.length)
        parts.push(`os:${missing.os.join(', ')}`);
    return parts.join(' / ');
}
function isSkillEnabled(name) {
    const entry = skillsData.value?.entries?.[name];
    if (entry && typeof entry.enabled === 'boolean')
        return entry.enabled;
    return true;
}
function onEditApiKey(name, value) {
    apiKeyDraft.value = { ...apiKeyDraft.value, [name]: value };
}
function onEditEnv(name, value) {
    envDraft.value = { ...envDraft.value, [name]: value };
}
async function onToggleSkill(name, evt) {
    const checked = evt.target.checked;
    if (!currentAgentId.value)
        return;
    loading.value = true;
    try {
        await agentStore.updateAgentSkillEntry(currentAgentId.value, name, { enabled: checked });
    }
    finally {
        loading.value = false;
    }
}
async function onSaveSkill(name) {
    if (!currentAgentId.value)
        return;
    saving.value = name;
    const patch = { apiKey: apiKeyDraft.value[name] || '' };
    const envText = envDraft.value[name];
    if (envText && envText.trim()) {
        try {
            patch.env = JSON.parse(envText);
        }
        catch (e) {
            alert(t('skills.envInvalid'));
            saving.value = null;
            return;
        }
    }
    try {
        await agentStore.updateAgentSkillEntry(currentAgentId.value, name, patch);
    }
    finally {
        saving.value = null;
    }
}
async function onInstallSkill(skill) {
    if (!currentAgentId.value)
        return;
    const installId = skill?.install?.[0]?.id;
    loading.value = true;
    try {
        await agentStore.installSkill(currentAgentId.value, skill.name, installId);
    }
    catch (e) {
        console.error(e);
    }
    finally {
        await refresh();
    }
}
async function openDetail(skill) {
    if (!skill?.name)
        return;
    loading.value = true;
    try {
        const res = await fetch(`/api/skills/detail?name=${encodeURIComponent(skill.name)}&agentId=${encodeURIComponent(currentAgentId.value)}`);
        if (!res.ok)
            throw new Error(await res.text());
        const data = await res.json();
        detail.value = {
            skill,
            content: data?.content || ''
        };
    }
    catch (e) {
        console.error(e);
    }
    finally {
        loading.value = false;
    }
}
function closeDetail() {
    detail.value = null;
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['header']} */ ;
/** @type {__VLS_StyleScopedClasses['grid']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-meta']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "skills-view" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
(__VLS_ctx.t("skills.title"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "subtitle" },
});
(__VLS_ctx.t("skills.subtitle"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "header-actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ class: "search" },
    placeholder: (__VLS_ctx.t('skills.searchPlaceholder')),
});
(__VLS_ctx.search);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.refresh) },
    ...{ class: "btn" },
    disabled: (__VLS_ctx.loading),
});
(__VLS_ctx.t("common.refresh"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "content" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "summary" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "pill pill-primary" },
});
(__VLS_ctx.t("skills.totalCount", { count: __VLS_ctx.skillsData?.available?.length || 0 }));
if (__VLS_ctx.blockedCount) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "pill pill-warning" },
    });
    (__VLS_ctx.t("skills.blockedCount", { count: __VLS_ctx.blockedCount }));
}
if (__VLS_ctx.installableCount) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "pill pill-warning" },
    });
    (__VLS_ctx.t("skills.installableCount", { count: __VLS_ctx.installableCount }));
}
if (__VLS_ctx.loading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "muted center" },
    });
    (__VLS_ctx.t("common.loading"));
}
else if (!__VLS_ctx.filteredSkills.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "muted center" },
    });
    (__VLS_ctx.t("skills.empty"));
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "grid" },
    });
    for (const [skill] of __VLS_getVForSourceType((__VLS_ctx.filteredSkills))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            key: (skill.name),
            ...{ class: "card" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "card-header" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "title-row" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "skill-name" },
        });
        (skill.name);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "pill" },
            ...{ class: (skill.blocked ? 'pill-warning' : 'pill-success') },
        });
        (skill.blocked ? __VLS_ctx.t("common.blocked") : __VLS_ctx.t("common.available"));
        if (__VLS_ctx.canInstall(skill)) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "pill pill-warning" },
            });
            (__VLS_ctx.t("common.installable"));
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "meta" },
        });
        (__VLS_ctx.t("skills.sourceLabel", { value: skill.source || __VLS_ctx.t("common.unknown") }));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "desc" },
        });
        (skill.description || __VLS_ctx.t("common.noDescription"));
        if (__VLS_ctx.missingText(skill)) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "missing" },
            });
            (__VLS_ctx.t("skills.missing", { value: __VLS_ctx.missingText(skill) }));
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "actions" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "inline" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
            ...{ class: "toggle" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
            ...{ onChange: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    if (!!(!__VLS_ctx.filteredSkills.length))
                        return;
                    __VLS_ctx.onToggleSkill(skill.name, $event);
                } },
            type: "checkbox",
            checked: (__VLS_ctx.isSkillEnabled(skill.name)),
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.t("skills.enable"));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    if (!!(!__VLS_ctx.filteredSkills.length))
                        return;
                    __VLS_ctx.openDetail(skill);
                } },
            ...{ class: "btn small" },
            disabled: (__VLS_ctx.loading),
        });
        (__VLS_ctx.t("common.viewDetails"));
        if (__VLS_ctx.canInstall(skill)) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!!(__VLS_ctx.loading))
                            return;
                        if (!!(!__VLS_ctx.filteredSkills.length))
                            return;
                        if (!(__VLS_ctx.canInstall(skill)))
                            return;
                        __VLS_ctx.onInstallSkill(skill);
                    } },
                ...{ class: "btn small" },
                disabled: (__VLS_ctx.loading),
            });
            (__VLS_ctx.installLabel(skill));
        }
        if (skill.primaryEnv) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "field" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
                ...{ onInput: (e => __VLS_ctx.onEditApiKey(skill.name, e.target.value)) },
                type: "password",
                value: (__VLS_ctx.apiKeyDraft[skill.name] || ''),
                placeholder: (__VLS_ctx.t('skills.apiKeyPlaceholder', { env: skill.primaryEnv })),
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!!(__VLS_ctx.loading))
                            return;
                        if (!!(!__VLS_ctx.filteredSkills.length))
                            return;
                        if (!(skill.primaryEnv))
                            return;
                        __VLS_ctx.onSaveSkill(skill.name);
                    } },
                ...{ class: "btn small" },
                disabled: (__VLS_ctx.loading || __VLS_ctx.saving === skill.name),
            });
            (__VLS_ctx.t("common.save"));
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "field" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
            ...{ onInput: (e => __VLS_ctx.onEditEnv(skill.name, e.target.value)) },
            ...{ class: "env" },
            value: (__VLS_ctx.envDraft[skill.name] || ''),
            placeholder: (__VLS_ctx.t('skills.envPlaceholder')),
            rows: "3",
        });
    }
}
if (__VLS_ctx.detail) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onClick: (__VLS_ctx.closeDetail) },
        ...{ class: "detail-mask" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "detail-panel" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "detail-header" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
    (__VLS_ctx.detail.skill.name);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "muted" },
    });
    (__VLS_ctx.detail.skill.description || __VLS_ctx.t("common.noDescription"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.closeDetail) },
        ...{ class: "btn small" },
    });
    (__VLS_ctx.t("common.close"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "detail-meta" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    (__VLS_ctx.t("skills.sourceLabel", { value: __VLS_ctx.detail.skill.source || __VLS_ctx.t("common.unknown") }));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
    (__VLS_ctx.t("skills.statusLabel", { value: __VLS_ctx.detail.skill.blocked ? __VLS_ctx.t("common.blocked") : __VLS_ctx.t("common.available") }));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "detail-body" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "detail-section" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "detail-title" },
    });
    (__VLS_ctx.t("skills.readmeTitle"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.pre, __VLS_intrinsicElements.pre)({
        ...{ class: "detail-content" },
    });
    (__VLS_ctx.detail.content || __VLS_ctx.t("common.noContent"));
}
/** @type {__VLS_StyleScopedClasses['skills-view']} */ ;
/** @type {__VLS_StyleScopedClasses['header']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['subtitle']} */ ;
/** @type {__VLS_StyleScopedClasses['header-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['search']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['content']} */ ;
/** @type {__VLS_StyleScopedClasses['summary']} */ ;
/** @type {__VLS_StyleScopedClasses['pill']} */ ;
/** @type {__VLS_StyleScopedClasses['pill-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['pill']} */ ;
/** @type {__VLS_StyleScopedClasses['pill-warning']} */ ;
/** @type {__VLS_StyleScopedClasses['pill']} */ ;
/** @type {__VLS_StyleScopedClasses['pill-warning']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['center']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['center']} */ ;
/** @type {__VLS_StyleScopedClasses['grid']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['card-header']} */ ;
/** @type {__VLS_StyleScopedClasses['title-row']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-name']} */ ;
/** @type {__VLS_StyleScopedClasses['pill']} */ ;
/** @type {__VLS_StyleScopedClasses['pill']} */ ;
/** @type {__VLS_StyleScopedClasses['pill-warning']} */ ;
/** @type {__VLS_StyleScopedClasses['meta']} */ ;
/** @type {__VLS_StyleScopedClasses['desc']} */ ;
/** @type {__VLS_StyleScopedClasses['missing']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['inline']} */ ;
/** @type {__VLS_StyleScopedClasses['toggle']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['small']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['small']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['small']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['env']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-mask']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-header']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['small']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-body']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-section']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-title']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-content']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            t: t,
            loading: loading,
            saving: saving,
            search: search,
            apiKeyDraft: apiKeyDraft,
            envDraft: envDraft,
            skillsData: skillsData,
            detail: detail,
            filteredSkills: filteredSkills,
            blockedCount: blockedCount,
            installableCount: installableCount,
            refresh: refresh,
            canInstall: canInstall,
            installLabel: installLabel,
            missingText: missingText,
            isSkillEnabled: isSkillEnabled,
            onEditApiKey: onEditApiKey,
            onEditEnv: onEditEnv,
            onToggleSkill: onToggleSkill,
            onSaveSkill: onSaveSkill,
            onInstallSkill: onInstallSkill,
            openDetail: openDetail,
            closeDetail: closeDetail,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
