/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, ref, watch } from 'vue';
import { useAgentStore } from '../store/agents';
import { useI18n } from "../i18n";
const agentStore = useAgentStore();
const { t } = useI18n();
const statistics = ref(null);
const savingSkill = ref(null);
const editingApiKey = ref({});
const currentAgentName = computed(() => {
    const agent = agentStore.currentAgent.value;
    return agent?.displayName || agent?.name || agent?.id || t("chat.noAgentSelected");
});
const currentAgentDescription = computed(() => {
    const agent = agentStore.currentAgent.value;
    return agent?.description || t("agent.info.skillNoDesc");
});
const statusClass = computed(() => {
    const agent = agentStore.currentAgent.value;
    if (!agent)
        return 'inactive';
    if (agent.enabled === false)
        return 'inactive';
    if (agent.healthy === false)
        return 'error';
    return 'active';
});
const statusText = computed(() => {
    const agent = agentStore.currentAgent.value;
    if (!agent)
        return t("agent.status.unknown");
    if (agent.enabled === false)
        return t("agent.status.disabled");
    if (agent.healthy === false)
        return t("agent.status.error");
    return t("agent.status.ok");
});
const skillsData = computed(() => agentStore.agentSkills.get(agentStore.currentAgentId.value) || null);
const inheritedTools = computed(() => agentStore.currentAgent.value?.capabilities?.tools?.inherited || []);
const disabledTools = computed(() => agentStore.currentAgent.value?.capabilities?.tools?.disabled || []);
const routingPriority = computed(() => agentStore.currentAgent.value?.routing?.priority ?? 0);
const routingKeywords = computed(() => agentStore.currentAgent.value?.routing?.keywords || []);
const routingChannels = computed(() => agentStore.currentAgent.value?.routing?.channels || []);
watch(() => agentStore.currentAgentId.value, async (newAgentId) => {
    if (newAgentId) {
        await loadStatistics(newAgentId);
        await agentStore.fetchAgentSkills(newAgentId);
        syncEditingKeys();
    }
}, { immediate: true });
async function loadStatistics(agentId) {
    try {
        statistics.value = await agentStore.fetchAgentStatistics(agentId);
    }
    catch (e) {
        console.error('Failed to load statistics:', e);
        statistics.value = null;
    }
}
watch(skillsData, () => {
    syncEditingKeys();
});
function syncEditingKeys() {
    const data = skillsData.value;
    if (!data)
        return;
    const next = {};
    Object.entries(data.entries || {}).forEach(([k, v]) => {
        if (v.apiKey)
            next[k] = v.apiKey;
    });
    editingApiKey.value = next;
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
function onToggleSkill(name, evt) {
    const checked = evt.target.checked;
    agentStore.updateAgentSkillEntry(agentStore.currentAgentId.value, name, { enabled: checked });
}
function onEditApiKey(name, value) {
    editingApiKey.value = { ...editingApiKey.value, [name]: value };
}
async function onSaveApiKey(name) {
    savingSkill.value = name;
    await agentStore.updateAgentSkillEntry(agentStore.currentAgentId.value, name, { apiKey: editingApiKey.value[name] || '' });
    savingSkill.value = null;
}
async function refreshStats() {
    if (agentStore.currentAgentId.value) {
        await loadStatistics(agentStore.currentAgentId.value);
    }
}
function editAgent() {
    // TODO: Open edit dialog
    alert(t("agent.info.editTodo"));
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['panel-header']} */ ;
/** @type {__VLS_StyleScopedClasses['status-indicator']} */ ;
/** @type {__VLS_StyleScopedClasses['status-indicator']} */ ;
/** @type {__VLS_StyleScopedClasses['status-indicator']} */ ;
/** @type {__VLS_StyleScopedClasses['info-section']} */ ;
/** @type {__VLS_StyleScopedClasses['info-row']} */ ;
/** @type {__VLS_StyleScopedClasses['info-row']} */ ;
/** @type {__VLS_StyleScopedClasses['info-row']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-secondary']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "agent-info-panel" },
});
if (__VLS_ctx.agentStore.currentAgent.value) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "agent-details" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "panel-header" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
    (__VLS_ctx.currentAgentName);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: (['status-indicator', __VLS_ctx.statusClass]) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "info-section" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
    (__VLS_ctx.t("agent.info.base"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "info-row" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "label" },
    });
    (__VLS_ctx.t("agent.info.id"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "value" },
    });
    (__VLS_ctx.agentStore.currentAgent.value.id);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "info-row" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "label" },
    });
    (__VLS_ctx.t("agent.info.desc"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "value" },
    });
    (__VLS_ctx.currentAgentDescription);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "info-row" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "label" },
    });
    (__VLS_ctx.t("agent.info.status"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "value" },
    });
    (__VLS_ctx.statusText);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "info-section" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
    (__VLS_ctx.t("agent.info.capabilities"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "capability-item" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "capability-label" },
    });
    (__VLS_ctx.t("agent.info.skills"));
    if (__VLS_ctx.skillsData) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "skill-list" },
        });
        for (const [skill] of __VLS_getVForSourceType((__VLS_ctx.skillsData.available))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                key: (skill.name),
                ...{ class: "skill-row" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "skill-main" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "skill-name" },
            });
            (skill.name);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "pill" },
                ...{ class: (skill.blocked ? 'pill-warning' : 'pill-success') },
            });
            (skill.blocked ? __VLS_ctx.t("agent.info.skillBlocked") : __VLS_ctx.t("agent.info.skillAvailable"));
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "skill-desc" },
            });
            (skill.description || __VLS_ctx.t("agent.info.skillNoDesc"));
            if (__VLS_ctx.missingText(skill)) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                    ...{ class: "skill-missing" },
                });
                (__VLS_ctx.t("agent.info.skillMissing", { text: __VLS_ctx.missingText(skill) }));
            }
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "skill-actions" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
                ...{ class: "toggle" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
                ...{ onChange: (...[$event]) => {
                        if (!(__VLS_ctx.agentStore.currentAgent.value))
                            return;
                        if (!(__VLS_ctx.skillsData))
                            return;
                        __VLS_ctx.onToggleSkill(skill.name, $event);
                    } },
                type: "checkbox",
                checked: (__VLS_ctx.isSkillEnabled(skill.name)),
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            (__VLS_ctx.t("agent.info.enable"));
            if (skill.primaryEnv) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                    ...{ class: "apikey" },
                });
                __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
                    ...{ onInput: ((e) => __VLS_ctx.onEditApiKey(skill.name, e.target.value)) },
                    type: "password",
                    value: (__VLS_ctx.editingApiKey[skill.name] || ''),
                    placeholder: (`API Key (${skill.primaryEnv})`),
                });
                __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                    ...{ onClick: (...[$event]) => {
                            if (!(__VLS_ctx.agentStore.currentAgent.value))
                                return;
                            if (!(__VLS_ctx.skillsData))
                                return;
                            if (!(skill.primaryEnv))
                                return;
                            __VLS_ctx.onSaveApiKey(skill.name);
                        } },
                    ...{ class: "btn btn-small" },
                    disabled: (__VLS_ctx.savingSkill === skill.name),
                });
                (__VLS_ctx.t("agent.info.save"));
            }
        }
        if (__VLS_ctx.skillsData.available.length === 0) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "empty-text" },
            });
            (__VLS_ctx.t("agent.info.noSkillFiles"));
        }
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "empty-text" },
        });
        (__VLS_ctx.t("agent.info.noSkillsLoaded"));
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "capability-item" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "capability-label" },
    });
    (__VLS_ctx.t("agent.info.inheritedTools", { count: __VLS_ctx.inheritedTools.length }));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "tag-list" },
    });
    for (const [tool] of __VLS_getVForSourceType((__VLS_ctx.inheritedTools))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            key: (tool),
            ...{ class: "tag tag-tool" },
        });
        (tool);
    }
    if (__VLS_ctx.inheritedTools.length === 0) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "empty-text" },
        });
        (__VLS_ctx.t("agent.info.inheritAll"));
    }
    if (__VLS_ctx.disabledTools.length > 0) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "capability-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "capability-label" },
        });
        (__VLS_ctx.t("agent.info.disabledTools", { count: __VLS_ctx.disabledTools.length }));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "tag-list" },
        });
        for (const [tool] of __VLS_getVForSourceType((__VLS_ctx.disabledTools))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                key: (tool),
                ...{ class: "tag tag-disabled" },
            });
            (tool);
        }
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "info-section" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
    (__VLS_ctx.t("agent.info.routing"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "info-row" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "label" },
    });
    (__VLS_ctx.t("agent.info.priority"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "value" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "priority-badge" },
    });
    (__VLS_ctx.routingPriority);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "capability-item" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "capability-label" },
    });
    (__VLS_ctx.t("agent.info.keywords"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "tag-list" },
    });
    for (const [keyword] of __VLS_getVForSourceType((__VLS_ctx.routingKeywords))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            key: (keyword),
            ...{ class: "tag tag-keyword" },
        });
        (keyword);
    }
    if (__VLS_ctx.routingKeywords.length === 0) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "empty-text" },
        });
        (__VLS_ctx.t("agent.info.noKeywords"));
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "capability-item" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "capability-label" },
    });
    (__VLS_ctx.t("agent.info.channels"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "tag-list" },
    });
    for (const [channel] of __VLS_getVForSourceType((__VLS_ctx.routingChannels))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            key: (channel),
            ...{ class: "tag tag-channel" },
        });
        (channel);
    }
    if (__VLS_ctx.routingChannels.length === 0) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "empty-text" },
        });
        (__VLS_ctx.t("agent.info.noChannels"));
    }
    if (__VLS_ctx.statistics) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "info-section" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.h4, __VLS_intrinsicElements.h4)({});
        (__VLS_ctx.t("agent.info.stats"));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "stats-grid" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "stat-card" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "stat-value" },
        });
        (__VLS_ctx.statistics.messagesProcessed);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "stat-label" },
        });
        (__VLS_ctx.t("agent.info.messagesProcessed"));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "stat-card" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "stat-value" },
        });
        (__VLS_ctx.statistics.averageResponseTime);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "stat-label" },
        });
        (__VLS_ctx.t("agent.info.avgResponse"));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "stat-card" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "stat-value" },
        });
        ((__VLS_ctx.statistics.successRate * 100).toFixed(1));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "stat-label" },
        });
        (__VLS_ctx.t("agent.info.successRate"));
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "panel-actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.refreshStats) },
        ...{ class: "btn btn-secondary" },
    });
    (__VLS_ctx.t("agent.info.refreshStats"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.editAgent) },
        ...{ class: "btn btn-primary" },
    });
    (__VLS_ctx.t("agent.info.editConfig"));
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "empty-state" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (__VLS_ctx.t("agent.info.empty"));
}
/** @type {__VLS_StyleScopedClasses['agent-info-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-details']} */ ;
/** @type {__VLS_StyleScopedClasses['panel-header']} */ ;
/** @type {__VLS_StyleScopedClasses['info-section']} */ ;
/** @type {__VLS_StyleScopedClasses['info-row']} */ ;
/** @type {__VLS_StyleScopedClasses['label']} */ ;
/** @type {__VLS_StyleScopedClasses['value']} */ ;
/** @type {__VLS_StyleScopedClasses['info-row']} */ ;
/** @type {__VLS_StyleScopedClasses['label']} */ ;
/** @type {__VLS_StyleScopedClasses['value']} */ ;
/** @type {__VLS_StyleScopedClasses['info-row']} */ ;
/** @type {__VLS_StyleScopedClasses['label']} */ ;
/** @type {__VLS_StyleScopedClasses['value']} */ ;
/** @type {__VLS_StyleScopedClasses['info-section']} */ ;
/** @type {__VLS_StyleScopedClasses['capability-item']} */ ;
/** @type {__VLS_StyleScopedClasses['capability-label']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-list']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-row']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-main']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-name']} */ ;
/** @type {__VLS_StyleScopedClasses['pill']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-missing']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['toggle']} */ ;
/** @type {__VLS_StyleScopedClasses['apikey']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-small']} */ ;
/** @type {__VLS_StyleScopedClasses['empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['capability-item']} */ ;
/** @type {__VLS_StyleScopedClasses['capability-label']} */ ;
/** @type {__VLS_StyleScopedClasses['tag-list']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['tag-tool']} */ ;
/** @type {__VLS_StyleScopedClasses['empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['capability-item']} */ ;
/** @type {__VLS_StyleScopedClasses['capability-label']} */ ;
/** @type {__VLS_StyleScopedClasses['tag-list']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['tag-disabled']} */ ;
/** @type {__VLS_StyleScopedClasses['info-section']} */ ;
/** @type {__VLS_StyleScopedClasses['info-row']} */ ;
/** @type {__VLS_StyleScopedClasses['label']} */ ;
/** @type {__VLS_StyleScopedClasses['value']} */ ;
/** @type {__VLS_StyleScopedClasses['priority-badge']} */ ;
/** @type {__VLS_StyleScopedClasses['capability-item']} */ ;
/** @type {__VLS_StyleScopedClasses['capability-label']} */ ;
/** @type {__VLS_StyleScopedClasses['tag-list']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['tag-keyword']} */ ;
/** @type {__VLS_StyleScopedClasses['empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['capability-item']} */ ;
/** @type {__VLS_StyleScopedClasses['capability-label']} */ ;
/** @type {__VLS_StyleScopedClasses['tag-list']} */ ;
/** @type {__VLS_StyleScopedClasses['tag']} */ ;
/** @type {__VLS_StyleScopedClasses['tag-channel']} */ ;
/** @type {__VLS_StyleScopedClasses['empty-text']} */ ;
/** @type {__VLS_StyleScopedClasses['info-section']} */ ;
/** @type {__VLS_StyleScopedClasses['stats-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['stat-card']} */ ;
/** @type {__VLS_StyleScopedClasses['stat-value']} */ ;
/** @type {__VLS_StyleScopedClasses['stat-label']} */ ;
/** @type {__VLS_StyleScopedClasses['panel-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['empty-state']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            agentStore: agentStore,
            t: t,
            statistics: statistics,
            savingSkill: savingSkill,
            editingApiKey: editingApiKey,
            currentAgentName: currentAgentName,
            currentAgentDescription: currentAgentDescription,
            statusClass: statusClass,
            statusText: statusText,
            skillsData: skillsData,
            inheritedTools: inheritedTools,
            disabledTools: disabledTools,
            routingPriority: routingPriority,
            routingKeywords: routingKeywords,
            routingChannels: routingChannels,
            missingText: missingText,
            isSkillEnabled: isSkillEnabled,
            onToggleSkill: onToggleSkill,
            onEditApiKey: onEditApiKey,
            onSaveApiKey: onSaveApiKey,
            refreshStats: refreshStats,
            editAgent: editAgent,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
