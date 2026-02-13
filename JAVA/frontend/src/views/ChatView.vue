<template>
  <section>
    <h2 class="section-title">多渠道对话</h2>
    <div class="chat-shell">
      <div class="chat-panel">
        <div class="chat-stream">
          <ChatMessage v-for="msg in chat.messages.value" :key="msg.id" :message="msg" />
        </div>

        <ChatComposer @send="chat.sendUserMessage" />
      </div>
      <aside class="chat-sidebar">
        <div class="card">
          <h3>实时上下文</h3>
          <div style="display: grid; gap: 10px">
            <div class="tool-card">
              <div class="title">会话摘要</div>
              <div style="font-size: 12px; color: var(--muted)">已关联 4 条历史会话</div>
            </div>
            <div class="tool-card">
              <div class="title">工具状态</div>
              <div style="font-size: 12px; color: var(--muted)">已启用 12 个工具</div>
            </div>
          </div>
        </div>
        <div class="card">
          <h3>建议动作</h3>
          <ul style="margin: 0; padding-left: 18px; color: var(--muted)">
            <li>汇总本轮对话目标</li>
            <li>开启并行工具调用</li>
            <li>生成执行计划与风险评估</li>
          </ul>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import ChatMessage from "../components/ChatMessage.vue";
import ChatComposer from "../components/ChatComposer.vue";
import { useChatStore } from "../store/chat";

const chat = useChatStore();
</script>
