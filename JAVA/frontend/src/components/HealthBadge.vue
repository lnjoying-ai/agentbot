<template>
  <span class="badge" :class="badgeClass">{{ text }}</span>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{ status: "ok" | "degraded" | "down"; text: string }>();

const badgeClass = computed(() => {
  if (props.status === "ok") return "health-ok";
  if (props.status === "degraded") return "health-warn";
  return "health-bad";
});
</script>
