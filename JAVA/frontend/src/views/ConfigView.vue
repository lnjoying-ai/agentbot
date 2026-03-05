<template>
  <section>
    <h2 class="section-title">配置中心</h2>

    <div class="card" style="margin-bottom: 20px">
      <h3>核心参数</h3>
      <div class="form-grid">
        <div class="form-field">
          <label>控制台 API Base URL（后端）</label>
          <input v-model="draft.serverBaseUrl" placeholder="http://localhost:8080" />
        </div>
        <div class="form-field" style="grid-column: 1 / -1">
          <label>配置文件路径</label>
          <input :value="config.state.configPath || '未获取'" disabled />
        </div>
      </div>
    </div>

    <div v-if="categoryEntries.length === 0" class="card" style="margin-bottom: 20px">
      <h3>配置加载中</h3>
      <div class="muted">正在从后端读取配置，请稍候。</div>
    </div>

    <div v-for="[categoryKey, categoryValue] in categoryEntries" :key="String(categoryKey)" class="card" style="margin-bottom: 20px">
      <div class="card-header">
        <div>
          <h3>{{ formatLabel(String(categoryKey)) }}</h3>
          <p class="muted">按分类展示所有配置项，支持直接编辑与保存。</p>
        </div>
      </div>
      <div class="config-grid">
        <ConfigNode
          v-if="isObject(categoryValue)"
          v-for="(childValue, childKey) in categoryValue"
          :key="String(childKey)"
          :label="String(childKey)"
          :value="childValue"
          :path="[String(categoryKey), String(childKey)]"
          :level="0"
          @update="updatePath"
        />
        <ConfigNode
          v-else
          :label="String(categoryKey)"
          :value="categoryValue"
          :path="[String(categoryKey)]"
          :level="0"
          @update="updatePath"
        />
      </div>
    </div>

    <div class="card">
      <h3>保存配置</h3>
      <div style="color: var(--muted); font-size: 13px">
        配置将直接同步至后端配置文件 (`agentbot.yml`)。敏感信息在加载时会被掩码处理。
      </div>
      <div class="config-actions">
        <button class="button" @click="save">保存</button>
        <button class="button secondary" @click="reset">重新加载</button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from "vue";
import { useConfigStore } from "../store/config";
import ConfigNode from "../components/ConfigNode.vue";

const config = useConfigStore();
const draft = reactive({ serverBaseUrl: config.state.serverBaseUrl });
const draftConfig = reactive<Record<string, any>>({});

const clone = (value: any) => JSON.parse(JSON.stringify(value ?? {}));

const syncDraft = () => {
  draft.serverBaseUrl = config.state.serverBaseUrl;
  Object.keys(draftConfig).forEach((key) => delete draftConfig[key]);
  Object.assign(draftConfig, clone(config.state.config));
};

onMounted(async () => {
  await config.fetch();
  syncDraft();
});

const categoryEntries = computed(() => Object.entries(draftConfig));

const updatePath = (path: string[], value: any) => {
  if (!path.length) return;
  let current: any = draftConfig;
  for (let i = 0; i < path.length - 1; i += 1) {
    const key = path[i];
    if (current[key] === null || current[key] === undefined) {
      current[key] = {};
    }
    current = current[key];
  }
  current[path[path.length - 1]] = value;
};

const save = async () => {
  config.state.serverBaseUrl = draft.serverBaseUrl;
  config.state.config = clone(draftConfig);
  await config.save();
  alert("配置已保存并同步至服务器");
};

const reset = async () => {
  await config.fetch();
  syncDraft();
};

const formatLabel = (raw: string) => {
  if (!raw) return "";
  return raw
    .replace(/_/g, " ")
    .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
    .replace(/\s+/g, " ")
    .trim();
};

const isObject = (value: any) => value !== null && typeof value === "object" && !Array.isArray(value);
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.muted {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.config-grid {
  display: grid;
  gap: 12px;
}
</style>


