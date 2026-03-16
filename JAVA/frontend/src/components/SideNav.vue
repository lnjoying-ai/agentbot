<template>
  <aside class="side-nav" :class="{ collapsed }">
    <div class="brand">
      <div class="brand-main">
        <img src="/logo.png" class="logo-img" alt="Logo" />
        <h1 v-if="!collapsed" class="brand-title">{{ t("app.title") }}</h1>
      </div>
      <button
        class="nav-toggle"
        type="button"
        :aria-label="collapsed ? t('nav.expand') : t('nav.collapse')"
        :title="collapsed ? t('nav.expand') : t('nav.collapse')"
        @click="emit('toggle')"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M16 5l-8 7 8 7" />
        </svg>
      </button>

    </div>
    <nav class="nav-list">
      <RouterLink to="/open-world" class="nav-item" :class="{ active: route.path === '/open-world' }" :title="t('nav.openWorld')">
        <span class="nav-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="8" />
            <path d="M2 12h20" />
            <path d="M12 4a16 16 0 0 1 0 16" />
            <path d="M12 4a16 16 0 0 0 0 16" />
          </svg>
        </span>
        <span class="nav-label">{{ t("nav.openWorld") }}</span>
      </RouterLink>
      <RouterLink to="/chat" class="nav-item" :class="{ active: route.path === '/chat' }" :title="t('nav.chatCoop')">

        <span class="nav-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4z" />
          </svg>
        </span>
        <span class="nav-label">{{ t("nav.chatCoop") }}</span>
      </RouterLink>
      <RouterLink to="/p2p-chat" class="nav-item" :class="{ active: route.path === '/p2p-chat' }" :title="t('nav.a2aChat')">
        <span class="nav-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M10 13a5 5 0 0 1 0-7l1-1a5 5 0 0 1 7 7l-1 1" />
            <path d="M14 11a5 5 0 0 1 0 7l-1 1a5 5 0 0 1-7-7l1-1" />
          </svg>
        </span>
        <span class="nav-label">{{ t("nav.a2aChat") }}</span>
      </RouterLink>


      <RouterLink to="/agents" class="nav-item" :class="{ active: route.path === '/agents' }" :title="t('nav.agentManage')">
        <span class="nav-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="8" r="3" />
            <circle cx="17" cy="8" r="3" />
            <path d="M3 20a6 6 0 0 1 12 0" />
            <path d="M13 20a5 5 0 0 1 8 0" />
          </svg>
        </span>
        <span class="nav-label">{{ t("nav.agentManage") }}</span>
      </RouterLink>

      <RouterLink to="/skills" class="nav-item" :class="{ active: route.path === '/skills' }" :title="t('nav.skills')">
        <span class="nav-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 7l3 3-7 7H7v-3z" />
            <path d="M3 21h6" />
          </svg>
        </span>
        <span class="nav-label">{{ t("nav.skills") }}</span>
      </RouterLink>
      <RouterLink to="/skills/store" class="nav-item" :class="{ active: route.path === '/skills/store' }" :title="t('nav.skillStore')">
        <span class="nav-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 7h18l-2 4H5z" />
            <path d="M5 11v8h14v-8" />
            <path d="M9 19v-6h6v6" />
          </svg>
        </span>
        <span class="nav-label">{{ t("nav.skillStore") }}</span>
      </RouterLink>
      <RouterLink to="/cron" class="nav-item" :class="{ active: route.path === '/cron' }" :title="t('nav.cron')">
        <span class="nav-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="8" />
            <path d="M12 8v5l3 2" />
          </svg>
        </span>
        <span class="nav-label">{{ t("nav.cron") }}</span>
      </RouterLink>
      <RouterLink to="/workspace/files" class="nav-item" :class="{ active: route.path === '/workspace/files' }" :title="t('nav.cronFiles')">

        <span class="nav-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z" />
            <path d="M14 3v6h6" />
          </svg>
        </span>
        <span class="nav-label">{{ t("nav.cronFiles") }}</span>
      </RouterLink>
      <RouterLink to="/monitor" class="nav-item" :class="{ active: route.path === '/monitor' }" :title="t('nav.monitor')">



        <span class="nav-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="4" width="18" height="12" rx="2" />
            <path d="M8 20h8" />
            <path d="M12 16v4" />
          </svg>
        </span>
        <span class="nav-label">{{ t("nav.monitor") }}</span>
      </RouterLink>

      <RouterLink to="/config" class="nav-item" :class="{ active: route.path === '/config' }" :title="t('nav.config')">
        <span class="nav-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="3" />
            <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 0 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 0 1-4 0v-.2a1.7 1.7 0 0 0-1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 0 1 0-4h.2a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3 1.7 1.7 0 0 0 1-1.5V3a2 2 0 0 1 4 0v.2a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8 1.7 1.7 0 0 0 1.5 1H21a2 2 0 0 1 0 4h-.2a1.7 1.7 0 0 0-1.5 1z" />
          </svg>
        </span>
        <span class="nav-label">{{ t("nav.config") }}</span>
      </RouterLink>
    </nav>


  </aside>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { useI18n } from "../i18n";

type Props = {
  collapsed?: boolean;
};

const props = defineProps<Props>();
const emit = defineEmits<{ (event: "toggle"): void }>();

const route = useRoute();
const { t } = useI18n();

const collapsed = computed(() => props.collapsed ?? false);
</script>



<style scoped>
.brand {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.brand-main {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.brand-title {
  margin: 0;
  font-size: 18px;
}

.logo-img {
  width: 46px;
  height: 46px;
  object-fit: contain;
  flex-shrink: 0;
  display: block;
}

.nav-toggle {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.04);
  color: var(--text);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.nav-toggle:hover {
  background: rgba(255, 255, 255, 0.12);
}

.nav-toggle svg {
  width: 16px;
  height: 16px;
  transition: transform 0.2s ease;
}

.side-nav.collapsed .nav-toggle svg {
  transform: rotate(180deg);
}

.nav-icon {
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: inherit;
  opacity: 0.9;
}

.nav-icon svg {
  width: 18px;
  height: 18px;
  display: block;
}

.nav-item.active .nav-icon {
  opacity: 1;
}

.nav-label {
  line-height: 1;
}
</style>



