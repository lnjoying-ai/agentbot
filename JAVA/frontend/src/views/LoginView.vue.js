/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, reactive, ref } from "vue";
import { useI18n } from "../i18n";
import { useRoute, useRouter } from "vue-router";
import { getApiBaseUrl } from "../store/config";
import { setAuthAuthenticated, resetAuthState } from "../store/auth";
const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const loading = ref(false);
const error = ref(null);
const form = reactive({
    username: "",
    password: ""
});
const showRequiredHint = computed(() => Boolean(route.query.redirect));
const baseUrl = () => getApiBaseUrl() || window.location.origin;
async function handleLogin() {
    loading.value = true;
    error.value = null;
    try {
        const res = await fetch(`${baseUrl()}/api/auth/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(form)
        });
        if (!res.ok)
            throw new Error(await res.text());
        const data = await res.json();
        setAuthAuthenticated(data.username);
        const redirect = route.query.redirect || "/chat";
        await router.replace(redirect);
    }
    catch (e) {
        resetAuthState();
        error.value = t("auth.invalid");
    }
    finally {
        loading.value = false;
    }
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['login-card']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "login-shell" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
(__VLS_ctx.t("auth.title"));
if (__VLS_ctx.showRequiredHint) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "hint" },
    });
    (__VLS_ctx.t("auth.required"));
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.form, __VLS_intrinsicElements.form)({
    ...{ onSubmit: (__VLS_ctx.handleLogin) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.t("auth.username"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    autocomplete: "username",
    required: true,
});
(__VLS_ctx.form.username);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.t("auth.password"));
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "password",
    autocomplete: "current-password",
    required: true,
});
(__VLS_ctx.form.password);
if (__VLS_ctx.error) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "error" },
    });
    (__VLS_ctx.error);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ class: "button" },
    type: "submit",
    disabled: (__VLS_ctx.loading),
});
(__VLS_ctx.loading ? __VLS_ctx.t("auth.loggingIn") : __VLS_ctx.t("auth.login"));
/** @type {__VLS_StyleScopedClasses['login-shell']} */ ;
/** @type {__VLS_StyleScopedClasses['login-card']} */ ;
/** @type {__VLS_StyleScopedClasses['hint']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['field']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['button']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            t: t,
            loading: loading,
            error: error,
            form: form,
            showRequiredHint: showRequiredHint,
            handleLogin: handleLogin,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
