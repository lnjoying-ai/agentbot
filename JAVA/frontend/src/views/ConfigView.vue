<template>
  <section>
    <h2 class="section-title">配置中心</h2>
    <div class="card" style="margin-bottom: 20px">
      <h3>核心参数</h3>
      <div class="form-grid">
        <div class="form-field">
          <label>API Base URL</label>
          <input v-model="draft.apiBaseUrl" placeholder="http://localhost:8080" />
        </div>
        <div class="form-field">
          <label>默认 API Key (通用)</label>
          <input v-model="draft.apiKey" type="password" placeholder="sk-..." />
        </div>
        <div class="form-field">
          <label>默认模型</label>
          <input v-model="draft.defaultModel" placeholder="gpt-4o-mini" />
        </div>
        <div class="form-field">
          <label>主 Provider</label>
          <select v-model="draft.provider">
            <option value="openai">OpenAI</option>
            <option value="openrouter">OpenRouter</option>
            <option value="glm">GLM</option>
            <option value="kimi">Kimi</option>
          </select>
        </div>
        <div class="form-field">
          <label>Fallback 顺序</label>
          <input v-model="draft.fallbackOrder" placeholder="openai,openrouter,glm,kimi" />
        </div>
        <div class="form-field">
          <label>并行工具</label>
          <select v-model="draft.parallelTools">
            <option :value="true">启用</option>
            <option :value="false">禁用</option>
          </select>
        </div>
        <div class="form-field">
          <label>最大工具轮数</label>
          <input v-model.number="draft.maxToolRounds" type="number" min="1" max="8" />
        </div>
        <div class="form-field">
          <label>工具并发数</label>
          <input v-model.number="draft.toolParallelism" type="number" min="1" max="8" />
        </div>
      </div>
    </div>

    <div class="card" style="margin-bottom: 20px">
      <h3>Provider 独立 Key (可选)</h3>
      <div class="form-grid">
        <div class="form-field">
          <label>OpenRouter Key</label>
          <input v-model="draft.openrouterKey" type="password" placeholder="留空则使用默认 Key" />
        </div>
        <div class="form-field">
          <label>GLM (智谱) Key</label>
          <input v-model="draft.glmKey" type="password" placeholder="留空则使用默认 Key" />
        </div>
        <div class="form-field">
          <label>Kimi (Moonshot) Key</label>
          <input v-model="draft.kimiKey" type="password" placeholder="留空则使用默认 Key" />
        </div>
      </div>
    </div>


    <div class="card" style="margin-bottom: 20px">
      <h3>渠道配置</h3>
      <div class="form-grid">
        <div class="form-field">
          <label>Telegram Token</label>
          <input v-model="draft.telegramToken" placeholder="Bot Token" />
        </div>
        <div class="form-field">
          <label>WhatsApp Bridge URL</label>
          <input v-model="draft.whatsappBridgeUrl" placeholder="ws://localhost:8088" />
        </div>
        <div class="form-field">
          <label>WeChat Webhook</label>
          <input v-model="draft.wechatWebhook" placeholder="http://localhost:8080/webhook/wechat" />
        </div>
      </div>
    </div>

    <div class="card">
      <h3>保存配置</h3>
      <div style="color: var(--muted); font-size: 13px">
        配置将同步至后端服务 (`agentbot-config.json`)。敏感信息在加载时会被掩码处理。
      </div>
      <div class="config-actions">

        <button class="button" @click="save">保存</button>
        <button class="button secondary" @click="reset">重置默认</button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive } from "vue";
import { useConfigStore } from "../store/config";

const config = useConfigStore();
const draft = reactive({ ...config.state });

const save = () => {
  config.update({ ...draft });
};

const reset = () => {
  config.reset();
  Object.assign(draft, config.state);
};
</script>
