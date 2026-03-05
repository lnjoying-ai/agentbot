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
      <RouterLink to="/p2p-chat" class="nav-item" :class="{ active: route.path === '/p2p-chat' }">
        P2P智能体聊天
      </RouterLink>

      <RouterLink to="/agents" class="nav-item" :class="{ active: route.path === '/agents' }">
        智能体管理
      </RouterLink>

      <RouterLink to="/skills" class="nav-item" :class="{ active: route.path === '/skills' }">
        技能中心
      </RouterLink>
      <RouterLink to="/skills/store" class="nav-item" :class="{ active: route.path === '/skills/store' }">
        技能商店
      </RouterLink>
      <RouterLink to="/cron" class="nav-item" :class="{ active: route.path === '/cron' }">
        定时任务
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

<style scoped>
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-img {
  width: 46px;
  height: 46px;
  object-fit: contain;
  flex-shrink: 0;
  display: block;
}
</style>

