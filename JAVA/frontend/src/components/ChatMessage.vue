<template>
  <div class="message" :class="role">
    <div class="message-meta">
      <span>{{ roleLabel }}</span>
      <span>{{ message.timestamp }}</span>
    </div>
    <div class="markdown-body" v-html="renderedContent"></div>
    <div v-if="message.toolResults?.length" style="margin-top: 10px; display: grid; gap: 8px;">
      <ToolResultCard v-for="tool in message.toolResults" :key="tool.id" :tool="tool" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { marked } from "marked";
import DOMPurify from "dompurify";
import ToolResultCard from "./ToolResultCard.vue";
import type { ChatMessage } from "../types";

const props = defineProps<{ message: ChatMessage }>();

const role = computed(() => props.message.role);
const roleLabel = computed(() => {
  if (props.message.role === "user") return "你";
  if (props.message.role === "assistant") return "Agent";
  if (props.message.role === "tool") return "工具";
  return "系统";
});

const renderedContent = computed(() => {
  const rawHtml = marked.parse(props.message.content || "") as string;
  return DOMPurify.sanitize(rawHtml);
});
</script>

<style scoped>
.markdown-body {
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
}

.markdown-body :deep(p) {
  margin-top: 0;
  margin-bottom: 8px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-body :deep(code) {
  padding: 0.2em 0.4em;
  margin: 0;
  font-size: 85%;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  font-family: ui-monospace, SFMono-Regular, SF Mono, Menlo, Consolas, Liberation Mono, monospace;
}

.markdown-body :deep(pre) {
  padding: 16px;
  overflow: auto;
  font-size: 85%;
  line-height: 1.45;
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  margin-bottom: 12px;
}

.markdown-body :deep(pre code) {
  padding: 0;
  margin: 0;
  background-color: transparent;
  font-size: 100%;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 2em;
  margin-bottom: 8px;
}

.markdown-body :deep(blockquote) {
  padding: 0 1em;
  color: var(--muted);
  border-left: 0.25em solid var(--border);
  margin: 0 0 12px 0;
}
</style>
