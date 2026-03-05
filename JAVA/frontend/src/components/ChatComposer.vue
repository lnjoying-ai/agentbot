<template>
  <div class="chat-composer">
    <textarea v-model="text" rows="4" placeholder="输入指令或问题，支持多轮对话" @keydown="onKeydown"></textarea>
    <button class="button" :disabled="!text.trim()" @click="handleSend">发送</button>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";

const emit = defineEmits<{ (event: "send", value: string): void }>();
const text = ref("");

const handleSend = () => {
  if (!text.value.trim()) return;
  emit("send", text.value.trim());
  text.value = "";
};

const onKeydown = (event: KeyboardEvent) => {
  if (event.key === "Enter" && !event.shiftKey) {
    event.preventDefault();
    handleSend();
  }
};
</script>

<style scoped>
.chat-composer {
  display: flex;
  gap: 12px;
  padding: 12px 20px 16px;
  border-top: 1px solid var(--border);
  background: var(--bg);
}

.chat-composer textarea {
  flex: 1;
  min-height: 96px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--hover);
  color: var(--text);
  line-height: 1.5;
  resize: vertical;
}

.chat-composer .button {
  align-self: flex-end;
  height: 40px;
  padding: 0 16px;
}
</style>

