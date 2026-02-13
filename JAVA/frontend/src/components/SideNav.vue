<template>
  <aside class="side-nav">
    <div class="brand">
      <img src="/logo.png" class="logo-img" alt="Logo" />
      <div>
        <h1>Agentbot 控制台</h1>
      </div>
    </div>
    <nav class="nav-list">
      <RouterLink to="/chat" class="nav-item" :class="{ active: route.path === '/chat' }">
        聊天协作
      </RouterLink>
      <RouterLink to="/monitor" class="nav-item" :class="{ active: route.path === '/monitor' }">
        运行监控
      </RouterLink>
      <RouterLink to="/config" class="nav-item" :class="{ active: route.path === '/config' }">
        配置中心
      </RouterLink>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { RouterLink, useRoute } from "vue-router";

const route = useRoute();
</script>
