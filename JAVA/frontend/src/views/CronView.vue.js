/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { reactive, ref, onMounted } from "vue";
import { getApiBaseUrl } from "../store/config";
import { useI18n } from "../i18n";
const { t } = useI18n();
const loading = ref(false);
const jobs = ref([]);
const draft = reactive({
    name: "cron-job",
    sessionKey: "cron",
    scheduleType: "every",
    intervalSeconds: 3600,
    cronExpr: "",
    runAt: "",
    prompt: "",
    deliver: false,
    to: "",
    channel: ""
});
async function fetchJobs() {
    const baseUrl = getApiBaseUrl();
    if (!baseUrl)
        return;
    loading.value = true;
    try {
        const res = await fetch(`${baseUrl}/api/cron/jobs`);
        if (res.ok) {
            jobs.value = await res.json();
        }
    }
    finally {
        loading.value = false;
    }
}
async function createJob() {
    const baseUrl = getApiBaseUrl();
    if (!baseUrl)
        return;
    if (!draft.prompt.trim()) {
        alert(t("cron.promptRequired"));
        return;
    }
    const payload = {
        intervalSeconds: Math.max(5, Number(draft.intervalSeconds || 0)),
        prompt: draft.prompt,
        sessionKey: draft.sessionKey,
        name: draft.name,
        cronExpr: draft.scheduleType === "cron" ? draft.cronExpr : "",
        runAt: draft.scheduleType === "at" ? draft.runAt : "",
        deliver: draft.deliver,
        to: draft.to,
        channel: draft.channel
    };
    const res = await fetch(`${baseUrl}/api/cron/schedule`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
    });
    if (res.ok) {
        resetDraft();
        await fetchJobs();
    }
    else {
        alert(t("cron.createFailed"));
    }
}
async function toggleJob(job) {
    const baseUrl = getApiBaseUrl();
    if (!baseUrl)
        return;
    const res = await fetch(`${baseUrl}/api/cron/enable`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ id: job.id, enabled: !job.enabled })
    });
    if (res.ok) {
        await fetchJobs();
    }
}
async function removeJob(job) {
    const baseUrl = getApiBaseUrl();
    if (!baseUrl)
        return;
    const res = await fetch(`${baseUrl}/api/cron/${job.id}`, { method: "DELETE" });
    if (res.ok) {
        await fetchJobs();
    }
}
function resetDraft() {
    draft.name = "cron-job";
    draft.sessionKey = "cron";
    draft.scheduleType = "every";
    draft.intervalSeconds = 3600;
    draft.cronExpr = "";
    draft.runAt = "";
    draft.prompt = "";
    draft.deliver = false;
    draft.to = "";
    draft.channel = "";
}
function scheduleTypeLabel(value) {
    if (value === "every")
        return t("cron.type.every");
    if (value === "cron")
        return t("cron.type.cron");
    if (value === "at")
        return t("cron.type.at");
    return value;
}
onMounted(fetchJobs);
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
(__VLS_ctx.t("cron.title"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("cron.listTitle"));
if (__VLS_ctx.loading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    (__VLS_ctx.t("common.loading"));
}
else if (__VLS_ctx.jobs.length === 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    (__VLS_ctx.t("cron.empty"));
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "job-list" },
    });
    for (const [job] of __VLS_getVForSourceType((__VLS_ctx.jobs))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "job-item" },
            key: (job.id),
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "job-header" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "job-title" },
        });
        (job.name);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "job-meta" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.scheduleTypeLabel(job.scheduleType));
        if (job.scheduleType === 'every') {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            (__VLS_ctx.t("cron.everySeconds", { count: job.everySeconds ?? 0 }));
        }
        if (job.scheduleType === 'cron') {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            (job.cronExpr);
        }
        if (job.scheduleType === 'at') {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            (job.runAt);
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "job-actions" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    if (!!(__VLS_ctx.jobs.length === 0))
                        return;
                    __VLS_ctx.toggleJob(job);
                } },
            ...{ class: "button secondary" },
        });
        (job.enabled ? __VLS_ctx.t("common.disable") : __VLS_ctx.t("common.enable"));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!!(__VLS_ctx.loading))
                        return;
                    if (!!(__VLS_ctx.jobs.length === 0))
                        return;
                    __VLS_ctx.removeJob(job);
                } },
            ...{ class: "button danger" },
        });
        (__VLS_ctx.t("common.delete"));
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "job-body" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "job-field" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
        (__VLS_ctx.t("cron.promptLabel"));
        (job.prompt);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "job-field" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
        (__VLS_ctx.t("cron.sessionLabel"));
        (job.sessionKey);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "job-field" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
        (__VLS_ctx.t("cron.deliverLabel"));
        (job.deliver ? __VLS_ctx.t("common.yes") : __VLS_ctx.t("common.no"));
    }
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
(__VLS_ctx.t("cron.newTitle"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("cron.name"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "cron-job",
});
(__VLS_ctx.draft.name);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("cron.sessionKey"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: "cron",
});
(__VLS_ctx.draft.sessionKey);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("cron.type"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.draft.scheduleType),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "every",
});
(__VLS_ctx.t("cron.type.every"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "cron",
});
(__VLS_ctx.t("cron.type.cron"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "at",
});
(__VLS_ctx.t("cron.type.at"));
if (__VLS_ctx.draft.scheduleType === 'every') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "form-field" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    (__VLS_ctx.t("cron.intervalSeconds"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        type: "number",
        min: "5",
    });
    (__VLS_ctx.draft.intervalSeconds);
}
if (__VLS_ctx.draft.scheduleType === 'cron') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "form-field" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    (__VLS_ctx.t("cron.cronExpr"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        placeholder: "0 */1 * * *",
    });
    (__VLS_ctx.draft.cronExpr);
}
if (__VLS_ctx.draft.scheduleType === 'at') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "form-field" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    (__VLS_ctx.t("cron.runAt"));
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        placeholder: "2026-02-23T10:30:00Z",
    });
    (__VLS_ctx.draft.runAt);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("cron.prompt"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: (__VLS_ctx.t('cron.promptPlaceholder')),
});
(__VLS_ctx.draft.prompt);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("cron.deliver"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.draft.deliver),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: (true),
});
(__VLS_ctx.t("common.yes"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: (false),
});
(__VLS_ctx.t("common.no"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("cron.to"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: (__VLS_ctx.t('common.optional')),
});
(__VLS_ctx.draft.to);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "form-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
(__VLS_ctx.t("cron.channel"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    placeholder: (__VLS_ctx.t('common.optional')),
});
(__VLS_ctx.draft.channel);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.createJob) },
    ...{ class: "button" },
});
(__VLS_ctx.t("cron.create"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.resetDraft) },
    ...{ class: "button secondary" },
});
(__VLS_ctx.t("common.reset"));
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['job-list']} */ ;
/** @type {__VLS_StyleScopedClasses['job-item']} */ ;
/** @type {__VLS_StyleScopedClasses['job-header']} */ ;
/** @type {__VLS_StyleScopedClasses['job-title']} */ ;
/** @type {__VLS_StyleScopedClasses['job-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['job-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['danger']} */ ;
/** @type {__VLS_StyleScopedClasses['job-body']} */ ;
/** @type {__VLS_StyleScopedClasses['job-field']} */ ;
/** @type {__VLS_StyleScopedClasses['job-field']} */ ;
/** @type {__VLS_StyleScopedClasses['job-field']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-field']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
/** @type {__VLS_StyleScopedClasses['secondary']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            t: t,
            loading: loading,
            jobs: jobs,
            draft: draft,
            createJob: createJob,
            toggleJob: toggleJob,
            removeJob: removeJob,
            resetDraft: resetDraft,
            scheduleTypeLabel: scheduleTypeLabel,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
