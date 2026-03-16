<template>
  <div class="p2p-message" :class="message.direction">
    <div class="message-meta">
      <span>{{ headerLabel }}</span>
      <span>{{ formattedTimestamp }}</span>
      <span v-if="message.status" class="status" :class="message.status.toLowerCase()">
        {{ statusLabel }}
      </span>
    </div>

    <div class="message-content">
      {{ message.content }}
    </div>
    <div v-if="message.reason" class="message-reason">
      {{ t("p2p.failReason", { reason: message.reason }) }}
    </div>

  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { P2pChatMessage } from "../types";
import { useI18n } from "../i18n";

const props = defineProps<{ message: P2pChatMessage }>();
const { t } = useI18n();

const headerLabel = computed(() => {
  if (props.message.direction === "outbound") {
    const target = `${props.message.toNodeId || ""}${props.message.toAgentId ? "/" + props.message.toAgentId : ""}`.trim();
    return t("p2p.header.localTo", { target });
  }
  const source = `${props.message.fromNodeId || t("p2p.source.external")}${props.message.fromAgentId ? "/" + props.message.fromAgentId : ""}`.trim();
  return t("p2p.header.remoteToLocal", { source });
});

const statusLabel = computed(() => {
  switch (props.message.status) {
    case "ACKED":
      return t("p2p.status.acked");
    case "NACKED":
      return t("p2p.status.nacked");
    case "FAILED":
      return t("p2p.status.failed");
    case "RECEIVED":
      return t("p2p.status.received");
    default:
      return t("p2p.status.sent");
  }
});


const formattedTimestamp = computed(() => {
  const raw = props.message.timestamp;
  if (!raw) return "";
  const date = new Date(raw);
  if (Number.isNaN(date.getTime())) return raw;
  return date.toLocaleString();
});

</script>

<style scoped>
.p2p-message {
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 12px;
  background: var(--hover);
  border: 1px solid var(--border);
}

.p2p-message.outbound {
  background: rgba(99, 102, 241, 0.15);
  border-color: rgba(99, 102, 241, 0.3);
}

.p2p-message.inbound {
  background: rgba(16, 185, 129, 0.12);
  border-color: rgba(16, 185, 129, 0.3);
}

.message-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.status {
  padding: 2px 6px;
  border-radius: 999px;
  font-size: 11px;
  background: rgba(255, 255, 255, 0.1);
}

.status.acked {
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
}

.status.nacked,
.status.failed {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.message-content {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.message-reason {
  margin-top: 6px;
  font-size: 12px;
  color: #ef4444;
}
</style>
