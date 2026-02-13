<template>
  <div class="card">
    <h3>{{ title }}</h3>
    <div style="font-size: 24px; font-weight: 600">{{ value }}</div>
    <div v-if="subtitle" style="color: var(--muted); margin-top: 6px; font-size: 12px">
      {{ subtitle }}
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string; value: string | number; subtitle?: string }>();
</script>
