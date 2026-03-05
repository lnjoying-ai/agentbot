<template>
  <div class="tool-card">
    <div class="title">{{ tool.name }}</div>
    <div style="font-size: 12px; color: var(--muted)">
      状态: {{ tool.status }} · 耗时 {{ tool.latencyMs ?? 0 }}ms
    </div>
    <div class="markdown-body" v-html="renderedOutput"></div>

    <div v-if="tool.status === 'PENDING_APPROVAL'" class="approval-actions">
      <button class="confirm-btn" :disabled="confirming || confirmed || canceling || cancelled" @click="confirmTool">
        {{ confirmed ? "已确认" : (confirming ? "确认中..." : "确认执行") }}
      </button>

      <button class="cancel-btn" :disabled="confirming || confirmed || canceling || cancelled" @click="cancelTool">
        {{ cancelled ? "已取消" : (canceling ? "取消中..." : "取消") }}
      </button>
    </div>

    <div v-if="approvalHint" class="approval-hint" :class="approvalHint.type">
      {{ approvalHint.text }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { marked } from "marked";
import DOMPurify from "dompurify";
import type { ToolResult } from "../types";
import { useChatStore } from "../store/chat";

const props = defineProps<{ tool: ToolResult }>();
const chat = useChatStore();

const confirming = ref(false);
const confirmed = ref(false);
const confirmError = ref<string | null>(null);
const canceling = ref(false);
const cancelled = ref(false);
const cancelError = ref<string | null>(null);

async function confirmTool() {
  if (confirming.value || confirmed.value || canceling.value || cancelled.value) return;
  confirming.value = true;
  confirmError.value = null;
  try {
    if (props.tool.agentId) {
      await chat.confirmTool(props.tool.id, { agentId: props.tool.agentId, chatId: props.tool.agentId });
    } else {
      await chat.confirmTool(props.tool.id);
    }
    confirmed.value = true;
  } catch (error) {
    console.error("工具确认失败:", error);
    confirmError.value = "确认失败，请稍后重试。";
  } finally {
    confirming.value = false;
  }
}

async function cancelTool() {
  if (confirming.value || confirmed.value || canceling.value || cancelled.value) return;
  canceling.value = true;
  cancelError.value = null;
  try {
    if (props.tool.agentId) {
      await chat.cancelTool(props.tool.id, { agentId: props.tool.agentId, chatId: props.tool.agentId });
    } else {
      await chat.cancelTool(props.tool.id);
    }
    cancelled.value = true;
  } catch (error) {
    console.error("工具取消失败:", error);
    cancelError.value = "取消失败，请稍后重试。";
  } finally {
    canceling.value = false;
  }
}

const approvalHint = computed(() => {
  if (confirmError.value) {
    return { text: confirmError.value, type: "error" };
  }
  if (cancelError.value) {
    return { text: cancelError.value, type: "error" };
  }
  if (confirmed.value) {
    return { text: "已确认，等待工具执行结果...", type: "success" };
  }
  if (cancelled.value) {
    return { text: "已取消，本次工具不会执行。", type: "success" };
  }
  if (confirming.value) {
    return { text: "正在提交确认...", type: "info" };
  }
  if (canceling.value) {
    return { text: "正在提交取消...", type: "info" };
  }
  return null;
});


const renderedOutput = computed(() => {

  const output = props.tool.output || "";

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

.approval-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.confirm-btn {
  background: var(--primary);
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.confirm-btn:disabled {
  background: rgba(255, 255, 255, 0.2);
  color: var(--muted);
  cursor: not-allowed;
}

.cancel-btn {
  background: rgba(255, 255, 255, 0.1);
  color: var(--muted);
  border: 1px solid var(--border);
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.cancel-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.approval-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--muted);
}

.approval-hint.success {
  color: #7bd389;
}

.approval-hint.error {
  color: #ff7b7b;
}

.approval-hint.info {
  color: var(--muted);
}
</style>


