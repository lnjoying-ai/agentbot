<template>
  <div class="tool-card">
    <div class="title">{{ tool.name }}</div>
    <div style="font-size: 12px; color: var(--muted)">
      状态: {{ tool.status }} · 耗时 {{ tool.latencyMs ?? 0 }}ms
    </div>
    <div style="margin-top: 6px; white-space: pre-wrap">{{ tool.output }}</div>
  </div>
</template>

<script setup lang="ts">
import type { ToolResult } from "../types";

defineProps<{ tool: ToolResult }>();
</script>
