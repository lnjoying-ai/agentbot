<template>
  <div
    class="chat-composer"
    @dragenter.prevent="onDragEnter"
    @dragover.prevent="onDragOver"
    @dragleave.prevent="onDragLeave"
    @drop.prevent="onDrop"
  >
    <div v-if="isDragging" class="drop-mask">
      <span>{{ t("chat.composer.dragHint") }}</span>
    </div>
    <textarea
      v-model="text"
      rows="4"
      :placeholder="t('chat.composer.placeholder')"
      @keydown="onKeydown"
      @paste="onPaste"
    ></textarea>

    <div class="composer-actions">
      <button class="button" :disabled="!text.trim()" @click="handleSend">{{ t("chat.composer.send") }}</button>
      <span v-if="uploading" class="upload-status">{{ t("chat.composer.uploading") }}</span>
      <span v-else-if="uploadError" class="upload-status error">{{ uploadError }}</span>
      <span v-else-if="lastUpload" class="upload-status success">{{ t("chat.composer.uploaded", { name: lastUpload.name }) }}</span>

    </div>
  </div>

</template>

<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "../i18n";
import { useChatStore } from "../store/chat";

const emit = defineEmits<{ (event: "send", value: string): void }>();
const text = ref("");
const { t } = useI18n();
const chat = useChatStore();

const isDragging = ref(false);
const dragDepth = ref(0);
const uploading = ref(false);
const uploadError = ref<string | null>(null);
const lastUpload = ref<{ name: string; path: string } | null>(null);


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

const onDragEnter = () => {
  dragDepth.value += 1;
  isDragging.value = true;
};

const onDragOver = () => {
  if (!isDragging.value) {
    isDragging.value = true;
  }
};

const onDragLeave = () => {
  dragDepth.value = Math.max(0, dragDepth.value - 1);
  if (dragDepth.value === 0) {
    isDragging.value = false;
  }
};

const onDrop = (event: DragEvent) => {
  dragDepth.value = 0;
  isDragging.value = false;
  const files = event.dataTransfer?.files;
  if (files && files.length) {
    void handleFiles(files);
  }
};

const onPaste = (event: ClipboardEvent) => {
  const items = event.clipboardData?.items;
  if (!items || !items.length) return;
  const files: File[] = [];
  for (const item of Array.from(items)) {
    if (item.kind === "file") {
      const file = item.getAsFile();
      if (file) files.push(file);
    }
  }
  if (!files.length) return;
  event.preventDefault();
  void handleFiles(files);
};


const handleFiles = async (files: FileList | File[]) => {
  const list = Array.from(files);
  if (!list.length) return;
  uploading.value = true;
  uploadError.value = null;
  lastUpload.value = null;

  for (const file of list) {
    try {
      const result = await chat.uploadFile(file);
      if (!result.ok || !result.path) {
        throw new Error(result.error || "upload failed");
      }
      uploadError.value = null;
      lastUpload.value = {
        name: result.originalName || result.storedName || file.name,
        path: result.path
      };
      appendToText(result.path);

    } catch (error) {
      uploadError.value = t("chat.composer.uploadFailed");
    }
  }

  uploading.value = false;
};

const appendToText = (path: string) => {
  const prefix = text.value.trim().length ? "\n" : "";
  text.value = `${text.value}${prefix}${path}`;
};
</script>

<style scoped>
.chat-composer {
  position: relative;
  display: flex;
  gap: 12px;
  padding: 12px 20px 16px;
  border-top: 1px solid var(--border);
  background: var(--bg);
}

.drop-mask {
  position: absolute;
  inset: 8px 12px 8px 12px;
  border-radius: 12px;
  border: 1px dashed rgba(111, 140, 255, 0.6);
  background: rgba(15, 23, 42, 0.65);
  color: var(--text);
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  z-index: 2;
  pointer-events: none;
  font-size: 13px;
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

.composer-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  min-width: 140px;
}

.chat-composer .button {
  align-self: flex-end;
  height: 40px;
  padding: 0 16px;
}

.upload-status {
  font-size: 12px;
  color: var(--muted);
}

.upload-status.error {
  color: #f87171;
}

.upload-status.success {
  color: #34d399;
}
</style>



