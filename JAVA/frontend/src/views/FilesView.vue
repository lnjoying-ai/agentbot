<template>
  <section>
    <h2 class="section-title">{{ t("files.title") }}</h2>

    <div class="card">
      <div class="files-header">
        <div class="files-summary">
          <span>{{ t("files.subtitle") }}</span>
          <span class="muted" v-if="lastUpdated">{{ t("files.updatedAt", { time: lastUpdated }) }}</span>
        </div>
        <div class="files-actions">
          <span v-if="selectedCount" class="muted">{{ t("files.selected", { count: selectedCount }) }}</span>
          <button
            class="icon-button"
            :disabled="selectedCount === 0"
            :title="t('files.bulkDownload')"
            :aria-label="t('files.bulkDownload')"
            @click="bulkDownload"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <path d="M7 10l5 5 5-5" />
              <path d="M12 15V3" />
            </svg>
          </button>
          <button
            class="icon-button danger"
            :disabled="selectedCount === 0"
            :title="t('files.bulkDelete')"
            :aria-label="t('files.bulkDelete')"
            @click="bulkDelete"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 6h18" />
              <path d="M8 6V4h8v2" />
              <path d="M6 6l1 14a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-14" />
              <path d="M10 11v6" />
              <path d="M14 11v6" />
            </svg>
          </button>
          <button class="button secondary" :disabled="loading" @click="fetchFiles">
            {{ t("common.refresh") }}
          </button>
        </div>
      </div>


      <div v-if="loading" class="muted">{{ t("common.loading") }}</div>
      <div v-else-if="files.length === 0" class="muted">{{ t("files.empty") }}</div>
      <div v-else class="file-table">
        <div class="file-row file-header">
          <div class="checkbox-cell">
            <input
              type="checkbox"
              :checked="allSelected"
              :aria-label="t('files.selectAll')"
              @change="toggleSelectAll"
            />
          </div>
          <div>{{ t("files.name") }}</div>
          <div>{{ t("files.size") }}</div>
          <div>{{ t("files.modified") }}</div>
          <div class="actions">{{ t("files.actions") }}</div>
        </div>
        <div class="file-row" v-for="file in files" :key="file.name">
          <div class="checkbox-cell">
            <input
              type="checkbox"
              :checked="selectedNames.has(file.name)"
              :aria-label="file.name"
              @change="toggleSelect(file.name)"
            />
          </div>
          <div class="file-name">{{ file.name }}</div>
          <div>{{ formatSize(file.size) }}</div>
          <div>{{ formatTime(file.modifiedAt) }}</div>
          <div class="actions">

            <button
              class="icon-button"
              :title="t('files.download')"
              :aria-label="t('files.download')"
              @click="downloadFile(file)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <path d="M7 10l5 5 5-5" />
                <path d="M12 15V3" />
              </svg>
            </button>
            <button
              class="icon-button danger"
              :title="t('common.delete')"
              :aria-label="t('common.delete')"
              @click="deleteFile(file)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 6h18" />
                <path d="M8 6V4h8v2" />
                <path d="M6 6l1 14a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-14" />
                <path d="M10 11v6" />
                <path d="M14 11v6" />
              </svg>
            </button>
          </div>

        </div>
      </div>

      <div v-if="error" class="error-tip">{{ error }}</div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useI18n } from "../i18n";
import { getApiBaseUrl } from "../store/config";


interface TmpFileItem {
  name: string;
  size: number;
  modifiedAt?: string | null;
}

const { t } = useI18n();
const files = ref<TmpFileItem[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const lastUpdated = ref<string | null>(null);
const selectedNames = ref<Set<string>>(new Set());

const baseUrl = () => getApiBaseUrl() || window.location.origin;

const selectedCount = computed(() => selectedNames.value.size);
const allSelected = computed(() =>
  files.value.length > 0 && selectedNames.value.size === files.value.length
);


const fetchFiles = async () => {
  loading.value = true;
  error.value = null;
  try {
    const res = await fetch(`${baseUrl()}/api/workspace/files`);
    if (!res.ok) throw new Error(await res.text());
    files.value = await res.json();
    selectedNames.value = new Set();
    lastUpdated.value = new Date().toLocaleTimeString();

  } catch (err) {
    error.value = t("files.loadFailed");
  } finally {
    loading.value = false;
  }
};

const deleteFile = async (file: TmpFileItem) => {
  const confirmed = confirm(t("files.deleteConfirm", { name: file.name }));
  if (!confirmed) return;
  try {
    const res = await fetch(`${baseUrl()}/api/workspace/files/${encodeURIComponent(file.name)}`, {
      method: "DELETE"
    });
    if (!res.ok) throw new Error(await res.text());
    files.value = files.value.filter(item => item.name !== file.name);
    if (selectedNames.value.has(file.name)) {
      const next = new Set(selectedNames.value);
      next.delete(file.name);
      selectedNames.value = next;
    }
  } catch (err) {
    error.value = t("files.deleteFailed");
  }
};


const downloadFile = async (file: TmpFileItem) => {
  try {
    const res = await fetch(`${baseUrl()}/api/workspace/files/${encodeURIComponent(file.name)}`);
    if (!res.ok) throw new Error(await res.text());
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = file.name;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  } catch (err) {
    error.value = t("files.downloadFailed");
  }
};

const toggleSelect = (name: string) => {
  const next = new Set(selectedNames.value);
  if (next.has(name)) {
    next.delete(name);
  } else {
    next.add(name);
  }
  selectedNames.value = next;
};

const toggleSelectAll = () => {
  if (allSelected.value) {
    selectedNames.value = new Set();
  } else {
    selectedNames.value = new Set(files.value.map(file => file.name));
  }
};

const bulkDownload = async () => {
  if (selectedNames.value.size === 0) return;
  for (const name of selectedNames.value) {
    const file = files.value.find(item => item.name === name);
    if (file) await downloadFile(file);
  }
};

const bulkDelete = async () => {
  if (selectedNames.value.size === 0) return;
  const confirmed = confirm(t("files.bulkDeleteConfirm", { count: selectedNames.value.size }));
  if (!confirmed) return;
  try {
    const targets = Array.from(selectedNames.value);
    for (const name of targets) {
      const res = await fetch(`${baseUrl()}/api/workspace/files/${encodeURIComponent(name)}`, {
        method: "DELETE"
      });
      if (!res.ok) throw new Error(await res.text());
    }
    files.value = files.value.filter(item => !selectedNames.value.has(item.name));
    selectedNames.value = new Set();
  } catch (err) {
    error.value = t("files.deleteFailed");
  }
};


const formatSize = (size: number) => {
  if (!Number.isFinite(size)) return "-";
  if (size < 1024) return `${size} B`;
  const kb = size / 1024;
  if (kb < 1024) return `${kb.toFixed(1)} KB`;
  const mb = kb / 1024;
  return `${mb.toFixed(1)} MB`;
};

const formatTime = (value?: string | null) => {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
};

onMounted(fetchFiles);
</script>

<style scoped>
.files-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.files-summary {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.files-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 6px;
  border-radius: 10px;
  flex-wrap: wrap;
}

.files-actions .muted {
  margin-right: 4px;
}

.file-table {
  display: grid;
  gap: 8px;
}

.file-row {
  display: grid;
  grid-template-columns: 36px 2fr 1fr 1fr 180px;
  gap: 12px;
  padding: 10px 12px;

  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.03);
  align-items: center;
}

.file-row.file-header {
  font-size: 12px;
  color: var(--muted);
  background: transparent;
  border: none;
  padding: 0 4px;
}

.checkbox-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.checkbox-cell input {
  width: 16px;
  height: 16px;
  accent-color: var(--primary);
}

.file-name {

  word-break: break-all;
}

.actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.icon-button {
  width: 36px;
  height: 32px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.06);
  color: var(--text);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.icon-button:hover {
  background: rgba(255, 255, 255, 0.12);
}

.icon-button svg {
  width: 16px;
  height: 16px;
}

.icon-button.danger {
  background: rgba(255, 99, 71, 0.2);
  border: 1px solid rgba(255, 99, 71, 0.4);
  color: #ffb3a6;
}

.muted {
  color: var(--muted);
  font-size: 12px;
}

.error-tip {
  margin-top: 12px;
  color: #f87171;
  font-size: 12px;
}

</style>
