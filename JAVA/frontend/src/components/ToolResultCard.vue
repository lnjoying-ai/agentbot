<template>
  <div class="tool-card">
    <div class="title">{{ tool.name }}</div>
    <div style="font-size: 12px; color: var(--muted)">
      状态: {{ tool.status }} · 耗时 {{ tool.latencyMs ?? 0 }}ms
    </div>
    <div class="markdown-body" v-html="renderedOutput"></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { marked } from "marked";
import DOMPurify from "dompurify";
import type { ToolResult } from "../types";
import { useConfigStore } from "../store/config";

const props = defineProps<{ tool: ToolResult }>();
const { state: config } = useConfigStore();

const renderedOutput = computed(() => {
  let output = props.tool.output || "";
  
  // Convert absolute workspace paths to relative URLs if possible
  if (config.workspaceDir) {
    const escapedDir = config.workspaceDir.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const regex = new RegExp(escapedDir.replace(/\\\\/g, '[\\\\/]') + '[\\\\/]([\\w\\.-]+\\.(png|jpg|jpeg|gif|webp))', 'gi');
    output = output.replace(regex, (match, filename) => {
      return `![${filename}](/workspace/${filename})`;
    });
  }

  const rawHtml = marked.parse(output) as string;
  return DOMPurify.sanitize(rawHtml);
});
</script>

<style scoped>
.markdown-body {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.5;
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin-top: 8px;
  border: 1px solid var(--border);
}
</style>
