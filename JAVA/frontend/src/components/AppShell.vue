<template>
  <div class="app-shell" :class="{ collapsed: isNavCollapsed }">
    <SideNav :collapsed="isNavCollapsed" @toggle="toggleNav" />
    <main class="main-area">
      <TopBar />
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { RouterView } from "vue-router";
import SideNav from "./SideNav.vue";
import TopBar from "./TopBar.vue";
import { useMonitorStore } from "../store/monitor";

const NAV_COLLAPSE_KEY = "agentbot.nav.collapsed";
const monitor = useMonitorStore();

const isNavCollapsed = ref(true);

function toggleNav() {
  isNavCollapsed.value = !isNavCollapsed.value;
  if (typeof window !== "undefined") {
    window.localStorage.setItem(NAV_COLLAPSE_KEY, String(isNavCollapsed.value));
  }
}

onMounted(() => {
  if (typeof window !== "undefined") {
    const stored = window.localStorage.getItem(NAV_COLLAPSE_KEY);
    if (stored !== null) {
      isNavCollapsed.value = stored === "true";
    }
  }
  monitor.refresh();
});

</script>


