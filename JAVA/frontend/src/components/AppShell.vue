<template>
  <div class="app-shell">
    <SideNav />
    <main class="main-area">
      <TopBar />
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import { RouterView } from "vue-router";
import SideNav from "./SideNav.vue";
import TopBar from "./TopBar.vue";
import { useMonitorStore } from "../store/monitor";

const monitor = useMonitorStore();

onMounted(() => {
  monitor.refresh();
});
</script>

