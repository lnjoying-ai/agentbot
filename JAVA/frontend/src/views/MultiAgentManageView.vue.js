/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useAgentStore } from '../store/agents';
import { useI18n } from '../i18n';
const agentStore = useAgentStore();
const { t } = useI18n();
const selectedId = ref(null);
const saving = ref(false);
const skillsLoading = ref(false);
const savingSkill = ref(null);
const searchTerm = ref('');
const skillInherited = ref(true);
const skillCustomPath = ref('');
const skillApiKeyDraft = ref({});
const skillEnvDraft = ref({});
const draft = reactive({
    id: '',
    name: '',
    displayName: '',
    description: '',
    avatar: '',
    enabled: true,
    routing: {
        keywords: [],
        channels: [],
        priority: 0,
        autoRoute: false
    },
    capabilities: {
        tools: {
            inherited: [],
            disabled: [],
            custom: []
        },
        skills: {
            inherited: true,
            customPath: ''
        }
    }
});
const isEditing = computed(() => selectedId.value !== null);
const filteredAgents = computed(() => {
    const keyword = searchTerm.value.trim().toLowerCase();
    if (!keyword)
        return agentStore.agents.value;
    return agentStore.agents.value.filter((agent) => {
        const haystack = [agent.displayName, agent.name, agent.id, agent.description]
            .filter(Boolean)
            .join(' ')
            .toLowerCase();
        return haystack.includes(keyword);
    });
});
const keywordsText = computed({
    get: () => draft.routing.keywords.join(', '),
    set: (val) => {
        draft.routing.keywords = parseList(val);
    }
});
const channelsText = computed({
    get: () => draft.routing.channels.join(', '),
    set: (val) => {
        draft.routing.channels = parseList(val);
    }
});
const toolsInheritedText = computed({
    get: () => draft.capabilities.tools.inherited.join(', '),
    set: (val) => {
        draft.capabilities.tools.inherited = parseList(val);
    }
});
const toolsDisabledText = computed({
    get: () => draft.capabilities.tools.disabled.join(', '),
    set: (val) => {
        draft.capabilities.tools.disabled = parseList(val);
    }
});
const toolsCustomText = computed({
    get: () => draft.capabilities.tools.custom.join(', '),
    set: (val) => {
        draft.capabilities.tools.custom = parseList(val);
    }
});
const selectedAgent = computed(() => agentStore.getAgent(selectedId.value || ''));
const selectedAgentName = computed(() => selectedAgent.value?.displayName || selectedAgent.value?.name || selectedAgent.value?.id || t('common.notSelected'));
const statusClass = computed(() => {
    if (!selectedAgent.value)
        return 'inactive';
    if (selectedAgent.value.enabled === false)
        return 'inactive';
    if (selectedAgent.value.healthy === false)
        return 'error';
    return 'active';
});
const statusText = computed(() => {
    if (!selectedAgent.value)
        return t('common.notSelected');
    if (selectedAgent.value.enabled === false)
        return t('agent.status.disabled');
    if (selectedAgent.value.healthy === false)
        return t('agent.status.error');
    return t('agent.status.ok');
});
const skillsData = computed(() => {
    if (!selectedId.value)
        return null;
    return agentStore.agentSkills.get(selectedId.value) || null;
});
const blockedCount = computed(() => (skillsData.value?.available || []).filter(skill => skill.blocked).length);
const installableCount = computed(() => (skillsData.value?.available || []).filter(skill => canInstall(skill)).length);
watch(skillsData, val => {
    if (!val)
        return;
    skillInherited.value = val.inherited ?? true;
    skillCustomPath.value = val.customPath ?? '';
    const apiKey = {};
    const env = {};
    Object.entries(val.entries || {}).forEach(([k, v]) => {
        if (v.apiKey)
            apiKey[k] = v.apiKey;
        if (v.env)
            env[k] = JSON.stringify(v.env, null, 2);
    });
    skillApiKeyDraft.value = apiKey;
    skillEnvDraft.value = env;
}, { immediate: true });
onMounted(async () => {
    await refresh();
    newAgent();
});
async function refresh() {
    await agentStore.fetchAgents();
}
async function refreshSkills() {
    if (!selectedId.value)
        return;
    skillsLoading.value = true;
    try {
        await agentStore.fetchAgentSkills(selectedId.value);
    }
    finally {
        skillsLoading.value = false;
    }
}
function parseList(value) {
    return value
        .split(',')
        .map(item => item.trim())
        .filter(Boolean);
}
function applyConfig(config) {
    draft.id = config.id || '';
    draft.name = config.name || '';
    draft.displayName = config.displayName || '';
    draft.description = config.description || '';
    draft.avatar = config.avatar || '';
    draft.enabled = config.enabled ?? true;
    draft.routing.keywords = config.routing?.keywords || [];
    draft.routing.channels = config.routing?.channels || [];
    draft.routing.priority = config.routing?.priority ?? 0;
    draft.routing.autoRoute = config.routing?.autoRoute ?? false;
    draft.capabilities.tools.inherited = config.capabilities?.tools?.inherited || [];
    draft.capabilities.tools.disabled = config.capabilities?.tools?.disabled || [];
    draft.capabilities.tools.custom = config.capabilities?.tools?.custom || [];
    draft.capabilities.skills.inherited = config.capabilities?.skills?.inherited ?? true;
    draft.capabilities.skills.customPath = config.capabilities?.skills?.customPath || '';
    skillInherited.value = draft.capabilities.skills.inherited;
    skillCustomPath.value = draft.capabilities.skills.customPath;
}
function reset() {
    if (selectedId.value) {
        selectAgent(selectedId.value);
    }
    else {
        newAgent();
    }
}
function newAgent() {
    selectedId.value = null;
    skillApiKeyDraft.value = {};
    skillEnvDraft.value = {};
    applyConfig({
        id: '',
        name: '',
        displayName: '',
        description: '',
        avatar: '',
        enabled: true,
        routing: { keywords: [], channels: [], priority: 0, autoRoute: false },
        capabilities: {
            tools: { inherited: [], disabled: [], custom: [] },
            skills: { inherited: true, customPath: '' }
        }
    });
}
async function selectAgent(agentId) {
    selectedId.value = agentId;
    const response = await fetch(`/api/agents/${agentId}/config`);
    if (!response.ok) {
        alert(t('agent.manage.loadFailed', { message: response.statusText }));
        return;
    }
    const config = await response.json();
    applyConfig(config);
    await refreshSkills();
}
async function save() {
    if (!draft.id || !draft.name) {
        alert(t('agent.manage.validationRequired'));
        return;
    }
    saving.value = true;
    try {
        const payload = JSON.parse(JSON.stringify(draft));
        if (isEditing.value) {
            await agentStore.updateAgent(draft.id, payload);
        }
        else {
            await agentStore.createAgent(payload);
            selectedId.value = draft.id;
        }
        await refresh();
        if (selectedId.value) {
            await refreshSkills();
        }
    }
    finally {
        saving.value = false;
    }
}
async function remove() {
    if (!selectedId.value)
        return;
    if (!confirm(t('agent.manage.deleteConfirm', { id: selectedId.value })))
        return;
    saving.value = true;
    try {
        await agentStore.deleteAgent(selectedId.value);
        await refresh();
        newAgent();
    }
    finally {
        saving.value = false;
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
function onEditSkillApiKey(name, value) {
    skillApiKeyDraft.value = { ...skillApiKeyDraft.value, [name]: value };
}
function onEditSkillEnv(name, value) {
    skillEnvDraft.value = { ...skillEnvDraft.value, [name]: value };
}
async function onInstallSkill(skill) {
    if (!selectedId.value)
        return;
    const installId = skill?.install?.[0]?.id;
    skillsLoading.value = true;
    try {
        await agentStore.installSkill(selectedId.value, skill.name, installId);
        await refreshSkills();
    }
    catch (e) {
        console.error(e);
    }
    finally {
        skillsLoading.value = false;
    }
}
async function onToggleSkill(name, evt) {
    if (!selectedId.value)
        return;
    const checked = evt.target.checked;
    skillsLoading.value = true;
    try {
        await agentStore.updateAgentSkillEntry(selectedId.value, name, { enabled: checked });
        await refreshSkills();
    }
    finally {
        skillsLoading.value = false;
    }
}
async function onSaveSkill(name) {
    if (!selectedId.value)
        return;
    savingSkill.value = name;
    const patch = { apiKey: skillApiKeyDraft.value[name] || '' };
    const envText = skillEnvDraft.value[name];
    if (envText && envText.trim()) {
        try {
            patch.env = JSON.parse(envText);
        }
        catch (e) {
            alert(t('skills.envInvalid'));
            savingSkill.value = null;
            return;
        }
    }
    try {
        await agentStore.updateAgentSkillEntry(selectedId.value, name, patch);
        await refreshSkills();
    }
    finally {
        savingSkill.value = null;
    }
}
async function saveSkillSettings() {
    if (!selectedId.value)
        return;
    skillsLoading.value = true;
    try {
        await agentStore.updateAgentSkills(selectedId.value, {
            inherited: skillInherited.value,
            customPath: skillCustomPath.value
        });
        await refreshSkills();
    }
    finally {
        skillsLoading.value = false;
    }
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['agent-item']} */ ;
/** @type {__VLS_StyleScopedClasses['status']} */ ;
/** @type {__VLS_StyleScopedClasses['active']} */ ;
/** @type {__VLS_StyleScopedClasses['status']} */ ;
/** @type {__VLS_StyleScopedClasses['status']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['pill']} */ ;
/** @type {__VLS_StyleScopedClasses['manage-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-row']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "agent-manage" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "section-title" },
});
(__VLS_ctx.t("agent.manage.title"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "toolbar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "toolbar-actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.newAgent) },
    ...{ class: "button" },
});
(__VLS_ctx.t("agent.manage.new"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.refresh) },
    ...{ class: "button secondary" },
    disabled: (__VLS_ctx.agentStore.loading.value),
});
(__VLS_ctx.t("agent.manage.refreshList"));
if (__VLS_ctx.agentStore.error.value) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "toolbar-status" },
    });
    (__VLS_ctx.agentStore.error.value);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "manage-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card list-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "list-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("agent.listTitle"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "search-box" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: (__VLS_ctx.t('agent.searchPlaceholder')),
});
(__VLS_ctx.searchTerm);
if (__VLS_ctx.agentStore.loading.value) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "muted" },
    });
    (__VLS_ctx.t("common.loading"));
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "agent-list" },
    });
    for (const [agent] of __VLS_getVForSourceType((__VLS_ctx.filteredAgents))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(__VLS_ctx.agentStore.loading.value))
                        return;
                    __VLS_ctx.selectAgent(agent.id);
                } },
            key: (agent.id),
            ...{ class: "agent-item" },
            ...{ class: ({ active: __VLS_ctx.selectedId === agent.id }) },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "agent-title" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
        (agent.displayName || agent.name || agent.id);
        if (agent.enabled === false) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "pill" },
            });
            (__VLS_ctx.t("agent.status.disabled"));
        }
        else if (agent.healthy === false) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "pill warning" },
            });
            (__VLS_ctx.t("agent.status.error"));
        }
        else {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "pill" },
            });
            (__VLS_ctx.t("agent.status.ok"));
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "agent-meta" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (agent.id);
        if (agent.updatedAt) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            (__VLS_ctx.t("agent.updatedAt", { time: agent.updatedAt }));
        }
    }
    if (!__VLS_ctx.filteredAgents.length) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "muted" },
        });
        (__VLS_ctx.t("agent.manage.empty"));
    }
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card form-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.isEditing ? __VLS_ctx.t("agent.manage.editTitle") : __VLS_ctx.t("agent.manage.createTitle"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("agent.dialog.id"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    disabled: (__VLS_ctx.isEditing),
    placeholder: "planner",
});
(__VLS_ctx.draft.id);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("agent.dialog.name"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "Planner",
});
(__VLS_ctx.draft.name);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("agent.manage.displayName"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: (__VLS_ctx.t('agent.manage.displayNamePlaceholder')),
});
(__VLS_ctx.draft.displayName);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("agent.manage.avatarUrl"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "https://...",
});
(__VLS_ctx.draft.avatar);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("agent.manage.enabledState"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.draft.enabled),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: (true),
});
(__VLS_ctx.t("common.enable"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: (false),
});
(__VLS_ctx.t("common.disable"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("agent.dialog.description"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
    value: (__VLS_ctx.draft.description),
    rows: "3",
    placeholder: (__VLS_ctx.t('agent.manage.descriptionPlaceholder')),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("agent.manage.routingTitle"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("agent.manage.keywords"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: (__VLS_ctx.t('agent.manage.keywordsPlaceholder')),
});
(__VLS_ctx.keywordsText);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("agent.manage.channels"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: (__VLS_ctx.t('agent.manage.channelsPlaceholder')),
});
(__VLS_ctx.channelsText);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("agent.manage.priority"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "number",
    min: "0",
});
(__VLS_ctx.draft.routing.priority);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("agent.manage.autoRoute"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.draft.routing.autoRoute),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: (true),
});
(__VLS_ctx.t("common.enable"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: (false),
});
(__VLS_ctx.t("common.disable"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("agent.manage.capabilitiesTitle"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("agent.manage.inheritedTools"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "echo,time_now",
});
(__VLS_ctx.toolsInheritedText);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("agent.manage.disabledTools"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "shell",
});
(__VLS_ctx.toolsDisabledText);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("agent.manage.customTools"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "custom_tool",
});
(__VLS_ctx.toolsCustomText);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "skills-section" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "skills-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "label" },
});
(__VLS_ctx.t("agent.manage.currentAgent"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "title" },
});
(__VLS_ctx.selectedAgentName);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "status" },
    ...{ class: (__VLS_ctx.statusClass) },
});
(__VLS_ctx.statusText);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("agent.manage.inheritSystemSkills"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.skillInherited),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: (true),
});
(__VLS_ctx.t("agent.manage.inherit"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: (false),
});
(__VLS_ctx.t("agent.manage.noInherit"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("agent.manage.customSkillPath"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "skills/",
});
(__VLS_ctx.skillCustomPath);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.saveSkillSettings) },
    ...{ class: "button" },
    disabled: (__VLS_ctx.skillsLoading || !__VLS_ctx.selectedId),
});
(__VLS_ctx.t("agent.manage.saveSkillSettings"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "skills-list-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "skills-list-header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "title" },
});
(__VLS_ctx.t("agent.manage.skillList"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.refreshSkills) },
    ...{ class: "button secondary" },
    disabled: (__VLS_ctx.skillsLoading || !__VLS_ctx.selectedId),
});
(__VLS_ctx.t("common.reload"));
if (__VLS_ctx.skillsLoading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "muted" },
    });
    (__VLS_ctx.t("common.loading"));
}
else if (__VLS_ctx.skillsData?.available?.length) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "skill-list" },
    });
    if (__VLS_ctx.blockedCount || __VLS_ctx.installableCount) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "muted" },
        });
        if (__VLS_ctx.blockedCount) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            (__VLS_ctx.t("skills.blockedCount", { count: __VLS_ctx.blockedCount }));
        }
        if (__VLS_ctx.blockedCount && __VLS_ctx.installableCount) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        }
        if (__VLS_ctx.installableCount) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            (__VLS_ctx.t("skills.installableCount", { count: __VLS_ctx.installableCount }));
        }
    }
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
        (skill.blocked ? __VLS_ctx.t("common.blocked") : __VLS_ctx.t("common.available"));
        if (__VLS_ctx.canInstall(skill)) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "pill pill-warning" },
            });
            (__VLS_ctx.t("common.installable"));
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "skill-desc" },
        });
        (skill.description || __VLS_ctx.t("common.noDescription"));
        if (__VLS_ctx.missingText(skill)) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "skill-missing" },
            });
            (__VLS_ctx.t("skills.missing", { value: __VLS_ctx.missingText(skill) }));
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "skill-actions" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "inline-actions" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
            ...{ class: "toggle" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
            ...{ onChange: (...[$event]) => {
                    if (!!(__VLS_ctx.skillsLoading))
                        return;
                    if (!(__VLS_ctx.skillsData?.available?.length))
                        return;
                    __VLS_ctx.onToggleSkill(skill.name, $event);
                } },
            type: "checkbox",
            checked: (__VLS_ctx.isSkillEnabled(skill.name)),
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.t("skills.enable"));
        if (__VLS_ctx.canInstall(skill)) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!!(__VLS_ctx.skillsLoading))
                            return;
                        if (!(__VLS_ctx.skillsData?.available?.length))
                            return;
                        if (!(__VLS_ctx.canInstall(skill)))
                            return;
                        __VLS_ctx.onInstallSkill(skill);
                    } },
                ...{ class: "button small" },
                disabled: (__VLS_ctx.skillsLoading),
            });
            (__VLS_ctx.installLabel(skill));
        }
        if (skill.primaryEnv) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "field" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
                ...{ onInput: (e => __VLS_ctx.onEditSkillApiKey(skill.name, e.target.value)) },
                type: "password",
                value: (__VLS_ctx.skillApiKeyDraft[skill.name] || ''),
                placeholder: (__VLS_ctx.t('skills.apiKeyPlaceholder', { env: skill.primaryEnv })),
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (...[$event]) => {
                        if (!!(__VLS_ctx.skillsLoading))
                            return;
                        if (!(__VLS_ctx.skillsData?.available?.length))
                            return;
                        if (!(skill.primaryEnv))
                            return;
                        __VLS_ctx.onSaveSkill(skill.name);
                    } },
                ...{ class: "button small" },
                disabled: (__VLS_ctx.skillsLoading || __VLS_ctx.savingSkill === skill.name),
            });
            (__VLS_ctx.t("common.save"));
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "field" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
            ...{ onInput: (e => __VLS_ctx.onEditSkillEnv(skill.name, e.target.value)) },
            ...{ class: "env" },
            value: (__VLS_ctx.skillEnvDraft[skill.name] || ''),
            placeholder: (__VLS_ctx.t('skills.envPlaceholder')),
            rows: "3",
        });
    }
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "muted" },
    });
    (__VLS_ctx.t("skills.notFoundFiles"));
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "config-actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.save) },
    ...{ class: "button" },
    disabled: (__VLS_ctx.saving),
});
(__VLS_ctx.isEditing ? __VLS_ctx.t("agent.manage.saveChanges") : __VLS_ctx.t("agent.manage.createTitle"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.reset) },
    ...{ class: "button secondary" },
    disabled: (__VLS_ctx.saving),
});
(__VLS_ctx.t("common.reset"));
if (__VLS_ctx.isEditing) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.remove) },
        ...{ class: "button secondary" },
        ...{ style: {} },
        disabled: (__VLS_ctx.saving),
    });
    (__VLS_ctx.t("agent.manage.delete"));
}
/** @type {__VLS_StyleScopedClasses['agent-manage']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['toolbar-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['toolbar-status']} */ ;
/** @type {__VLS_StyleScopedClasses['manage-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['list-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['list-header']} */ ;
/** @type {__VLS_StyleScopedClasses['search-box']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-list']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-item']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-title']} */ ;
/** @type {__VLS_StyleScopedClasses['pill']} */ ;
/** @type {__VLS_StyleScopedClasses['pill']} */ ;
/** @type {__VLS_StyleScopedClasses['warning']} */ ;
/** @type {__VLS_StyleScopedClasses['pill']} */ ;
/** @type {__VLS_StyleScopedClasses['agent-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['form-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['skills-section']} */ ;
/** @type {__VLS_StyleScopedClasses['skills-header']} */ ;
/** @type {__VLS_StyleScopedClasses['label']} */ ;
/** @type {__VLS_StyleScopedClasses['title']} */ ;
/** @type {__VLS_StyleScopedClasses['status']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['skills-list-card']} */ ;
/** @type {__VLS_StyleScopedClasses['skills-list-header']} */ ;
/** @type {__VLS_StyleScopedClasses['title']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-list']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-row']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-main']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-name']} */ ;
/** @type {__VLS_StyleScopedClasses['pill']} */ ;
/** @type {__VLS_StyleScopedClasses['pill']} */ ;
/** @type {__VLS_StyleScopedClasses['pill-warning']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-desc']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-missing']} */ ;
/** @type {__VLS_StyleScopedClasses['skill-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['inline-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['toggle']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['small']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['small']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['env']} */ ;
/** @type {__VLS_StyleScopedClasses['muted']} */ ;
/** @type {__VLS_StyleScopedClasses['config-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            agentStore: agentStore,
            t: t,
            selectedId: selectedId,
            saving: saving,
            skillsLoading: skillsLoading,
            savingSkill: savingSkill,
            searchTerm: searchTerm,
            skillInherited: skillInherited,
            skillCustomPath: skillCustomPath,
            skillApiKeyDraft: skillApiKeyDraft,
            skillEnvDraft: skillEnvDraft,
            draft: draft,
            isEditing: isEditing,
            filteredAgents: filteredAgents,
            keywordsText: keywordsText,
            channelsText: channelsText,
            toolsInheritedText: toolsInheritedText,
            toolsDisabledText: toolsDisabledText,
            toolsCustomText: toolsCustomText,
            selectedAgentName: selectedAgentName,
            statusClass: statusClass,
            statusText: statusText,
            skillsData: skillsData,
            blockedCount: blockedCount,
            installableCount: installableCount,
            refresh: refresh,
            refreshSkills: refreshSkills,
            reset: reset,
            newAgent: newAgent,
            selectAgent: selectAgent,
            save: save,
            remove: remove,
            canInstall: canInstall,
            installLabel: installLabel,
            missingText: missingText,
            isSkillEnabled: isSkillEnabled,
            onEditSkillApiKey: onEditSkillApiKey,
            onEditSkillEnv: onEditSkillEnv,
            onInstallSkill: onInstallSkill,
            onToggleSkill: onToggleSkill,
            onSaveSkill: onSaveSkill,
            saveSkillSettings: saveSkillSettings,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
