<template>
  <header class="top-bar">
    <div>
      <div class="status-group">
        <HealthBadge :status="healthStatus" :text="healthLabel" />
        <span class="badge badge-truncate">{{ t("topbar.model") }}: {{ monitor.stats.model }}</span>
        <span class="badge badge-truncate">{{ t("topbar.backend") }}: {{ config.state.serverBaseUrl || t("topbar.local") }}</span>

      </div>

    </div>

    <div class="status-group">
      <span class="badge">{{ t("topbar.activeSessions") }} {{ monitor.stats.activeSessions }}</span>
      <span class="badge">{{ t("topbar.toolCalls") }} {{ monitor.stats.toolCalls }}</span>
      <span v-if="showAuth" class="badge badge-truncate">{{ t("auth.user") }}: {{ auth.username || "-" }}</span>
      <button
        v-if="showAuth"
        class="icon-button"
        :title="t('auth.logout')"
        :aria-label="t('auth.logout')"
        @click="handleLogout"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M10 17l5-5-5-5" />
          <path d="M15 12H3" />
          <path d="M21 4v16a2 2 0 0 1-2 2H9" />
        </svg>
      </button>
      <button
        class="icon-button"
        :title="t('topbar.refresh')"
        :aria-label="t('topbar.refresh')"
        @click="monitor.refresh"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 12a9 9 0 1 1-2.64-6.36" />
          <path d="M21 3v6h-6" />
        </svg>
      </button>
      <div class="lang-switch" :title="t('topbar.language')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="9" />
          <path d="M3 12h18" />
          <path d="M12 3a15 15 0 0 1 0 18" />
          <path d="M12 3a15 15 0 0 0 0 18" />
        </svg>
        <select v-model="currentLocale" :aria-label="t('topbar.language')">
          <option value="zh-CN">{{ t("topbar.language.zh") }}</option>
          <option value="en-US">{{ t("topbar.language.en") }}</option>
        </select>
      </div>
    </div>



  </header>
</template>


<script setup lang="ts">
import { computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import HealthBadge from "./HealthBadge.vue";
import { useConfigStore } from "../store/config";
import { useMonitorStore } from "../store/monitor";
import { useI18n } from "../i18n";
import { fetchAuthState, getAuthState, logout } from "../store/auth";

const config = useConfigStore();
const monitor = useMonitorStore();
const router = useRouter();
const { t, locale, setLocale } = useI18n();
const auth = getAuthState();

const currentLocale = computed({
  get: () => locale.value,
  set: (value) => setLocale(value)
});

const showAuth = computed(() => auth.value.enabled && auth.value.authenticated);

const healthStatus = computed(() => monitor.health.value);
const healthLabel = computed(() => {
  if (monitor.health.value === "ok") return t("topbar.health.ok");
  if (monitor.health.value === "degraded") return t("topbar.health.degraded");
  return t("topbar.health.error");
});

onMounted(() => {
  fetchAuthState();
});

async function handleLogout() {
  await logout();
  await router.replace("/login");
}

</script>

<style scoped>
.icon-button {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  border: 1px solid rgba(111, 140, 255, 0.2);
  background: rgba(111, 140, 255, 0.08);
  color: var(--text);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 8px 16px rgba(10, 16, 34, 0.35);
  backdrop-filter: blur(6px);
}

.icon-button:hover {
  background: rgba(111, 140, 255, 0.16);
  border-color: rgba(111, 140, 255, 0.35);
}

.icon-button svg {
  width: 16px;
  height: 16px;
}

.badge-truncate {
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
}

.lang-switch {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid rgba(111, 140, 255, 0.2);
  background: rgba(111, 140, 255, 0.08);
  font-size: 12px;
  color: var(--text);
  transition: all 0.2s ease;
  min-height: 32px;
  box-shadow: 0 8px 16px rgba(10, 16, 34, 0.35);
  backdrop-filter: blur(6px);
}

.lang-switch svg {
  width: 16px;
  height: 16px;
  opacity: 0.85;
}

.lang-switch::after {
  content: "";
  position: absolute;
  right: 10px;
  width: 8px;
  height: 8px;
  border-right: 2px solid rgba(255, 255, 255, 0.7);
  border-bottom: 2px solid rgba(255, 255, 255, 0.7);
  transform: rotate(45deg);
  pointer-events: none;
}

.lang-switch:hover {
  background: rgba(111, 140, 255, 0.16);
  border-color: rgba(111, 140, 255, 0.35);
}

.lang-switch:focus-within {
  border-color: rgba(111, 140, 255, 0.55);
  box-shadow: 0 0 0 2px rgba(111, 140, 255, 0.25), 0 8px 16px rgba(10, 16, 34, 0.35);
}

.lang-switch select {
  border: none;
  background: transparent;
  color: var(--text);
  font-size: 12px;
  line-height: 1.2;
  outline: none;
  padding-right: 18px;
  appearance: none;
  min-width: 60px;

  letter-spacing: 0.2px;
}

.lang-switch select option {
  background: #ffffff;
  color: #1f2a44;
  font-size: 14px;
  font-weight: 500;
  padding: 8px 14px;
}





</style>



