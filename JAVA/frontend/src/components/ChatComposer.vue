<template>
  <div class="chat-composer">
    <textarea v-model="text" rows="3" placeholder="输入指令或问题，支持多轮对话"></textarea>
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
</script>
