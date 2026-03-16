<template>
  <div v-if="isObject" class="config-group" :style="groupStyle">
    <div class="group-title">{{ translateLabel(label) }}</div>
    <div class="group-body">
      <ConfigNode
        v-for="(childValue, childKey) in value"
        :key="String(childKey)"
        :label="String(childKey)"
        :value="childValue"
        :path="[...path, String(childKey)]"
        :level="level + 1"
        @update="forwardUpdate"
      />
    </div>
  </div>

  <div v-else-if="isArray" class="form-field" :style="groupStyle">
    <label>{{ translateLabel(label) }}</label>
    <textarea v-model="arrayText" rows="4" @blur="applyArray"></textarea>
    <div class="muted">{{ t("configNode.arrayHint") }}</div>
  </div>

  <div v-else class="form-field" :style="groupStyle">
    <label>{{ translateLabel(label) }}</label>
    <select v-if="isBoolean" :value="String(value)" @change="onBooleanChange">
      <option value="true">{{ t("common.enable") }}</option>
      <option value="false">{{ t("common.disable") }}</option>
    </select>
    <input
      v-else-if="isNumber"
      type="number"
      :value="value ?? ''"
      @input="onNumberInput"
    />
    <input
      v-else
      :type="isSecret ? 'password' : 'text'"
      :value="value ?? ''"
      @input="onTextInput"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useI18n } from "../i18n";

defineOptions({ name: "ConfigNode" });

const props = defineProps<{ label: string; value: any; path: string[]; level?: number }>();
const emit = defineEmits<{ (event: "update", path: string[], value: any): void }>();
const { t } = useI18n();

const level = computed(() => props.level ?? 0);
const isObject = computed(() => props.value !== null && typeof props.value === "object" && !Array.isArray(props.value));
const isArray = computed(() => Array.isArray(props.value));
const isBoolean = computed(() => typeof props.value === "boolean");
const isNumber = computed(() => typeof props.value === "number");
const isSecret = computed(() => isSecretKey(props.label));

const groupStyle = computed(() => ({ marginLeft: `${level.value * 12}px` }));

const arrayText = ref("");
watch(
  () => props.value,
  (val) => {
    if (isArray.value) {
      arrayText.value = JSON.stringify(val ?? [], null, 2);
    }
  },
  { immediate: true, deep: true }
);

const onTextInput = (event: Event) => {
  const target = event.target as HTMLInputElement;
  emit("update", props.path, target.value);
};

const onNumberInput = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const raw = target.value;
  if (raw === "") {
    emit("update", props.path, null);
    return;
  }
  const parsed = Number(raw);
  emit("update", props.path, Number.isNaN(parsed) ? null : parsed);
};

const onBooleanChange = (event: Event) => {
  const target = event.target as HTMLSelectElement;
  emit("update", props.path, target.value === "true");
};

const applyArray = () => {
  if (!isArray.value) return;
  try {
    const parsed = JSON.parse(arrayText.value || "[]");
    emit("update", props.path, parsed);
  } catch (error) {
    // ignore parse errors until user fixes JSON
  }
};

const forwardUpdate = (path: string[], value: any) => {
  emit("update", path, value);
};

const formatLabel = (raw: string) => {
  if (!raw) return "";
  return raw
    .replace(/_/g, " ")
    .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
    .replace(/\s+/g, " ")
    .trim();
};

const translateLabel = (raw: string) => {
  const normalized = raw.replace(/[\s_-]/g, "").toLowerCase();
  const key = `config.label.${normalized}`;
  const translated = t(key);
  return translated === key ? formatLabel(raw) : translated;
};

const isSecretKey = (raw: string) => {
  const lower = raw.toLowerCase();
  return (
    lower.includes("key") ||
    lower.includes("token") ||
    lower.includes("secret") ||
    lower.includes("password")
  );
};
</script>


<style scoped>
.group-title {
  font-weight: 600;
  margin: 12px 0 8px;
}

.group-body {
  display: grid;
  gap: 12px;
}

.muted {
  margin-top: 6px;
  color: var(--muted);
  font-size: 12px;
}
</style>
