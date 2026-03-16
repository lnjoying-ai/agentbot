<template>
  <section class="login-shell">
    <div class="login-card">
      <h2>{{ t("auth.title") }}</h2>
      <p v-if="showRequiredHint" class="hint">{{ t("auth.required") }}</p>
      <form @submit.prevent="handleLogin">
        <label class="field">
          <span>{{ t("auth.username") }}</span>
          <input v-model="form.username" autocomplete="username" required />
        </label>
        <label class="field">
          <span>{{ t("auth.password") }}</span>
          <input v-model="form.password" type="password" autocomplete="current-password" required />
        </label>
        <div v-if="error" class="error">{{ error }}</div>
        <button class="button" type="submit" :disabled="loading">
          {{ loading ? t("auth.loggingIn") : t("auth.login") }}
        </button>
      </form>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { useI18n } from "../i18n";
import { useRoute, useRouter } from "vue-router";
import { getApiBaseUrl } from "../store/config";
import { setAuthAuthenticated, resetAuthState } from "../store/auth";

const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const loading = ref(false);
const error = ref<string | null>(null);
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
    if (!res.ok) throw new Error(await res.text());
    const data = await res.json();
    setAuthAuthenticated(data.username);
    const redirect = (route.query.redirect as string) || "/chat";
    await router.replace(redirect);
  } catch (e) {
    resetAuthState();
    error.value = t("auth.invalid");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-shell {
  min-height: calc(100vh - 40px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-card {
  width: 360px;
  background: rgba(17, 24, 44, 0.9);
  border: 1px solid rgba(111, 140, 255, 0.2);
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 20px 50px rgba(8, 12, 24, 0.45);
}

.login-card h2 {
  margin: 0 0 12px;
}

.hint {
  margin-bottom: 16px;
  color: var(--muted);
  font-size: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
  font-size: 12px;
}

.field input {
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: rgba(0, 0, 0, 0.2);
  color: var(--text);
}

.button {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  border: none;
  background: var(--primary);
  color: #fff;
  cursor: pointer;
}

.button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  margin: 8px 0 12px;
  color: #f87171;
  font-size: 12px;
}
</style>
