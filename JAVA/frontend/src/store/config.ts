import { reactive } from "vue";
import type { ConfigState } from "../types";

const STORAGE_KEY = "agentbot.config";

const defaultState: ConfigState = {
  apiBaseUrl: "",
  defaultModel: "gpt-4o-mini",
  apiKey: "",
  provider: "openai",
  fallbackOrder: "openai,openrouter,glm,kimi",
  parallelTools: true,
  maxToolRounds: 3,
  toolParallelism: 3,
  telegramToken: "",
  whatsappBridgeUrl: "ws://localhost:8088",
  wechatWebhook: "http://localhost:8080/webhook/wechat",
  workspaceDir: ""
};

function save() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function loadState(): ConfigState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<ConfigState>;
      return { ...defaultState, ...parsed };
    }
  } catch (error) {
    console.warn("Config load failed", error);
  }
  return { ...defaultState };
}

const state = reactive<ConfigState>(loadState());

async function fetchFromServer() {
  const baseUrl = getApiBaseUrl() || window.location.origin;
  try {
    const response = await fetch(`${baseUrl}/api/config`);
    if (response.ok) {
      const data = await response.json();
      
      // 统一使用 effective 配置（即 agentbot.yml），不再区分 stored
      const source = data.effective;


      if (source) {
        // Flatten the nested structure from backend (AgentbotProperties) to frontend (ConfigState)
        const newState: Partial<ConfigState> = {};
        
        if (source.llm) {
          if (source.llm.apiBaseUrl) newState.apiBaseUrl = source.llm.apiBaseUrl;
          else if (source.llm.baseUrl) newState.apiBaseUrl = source.llm.baseUrl;
          
          // 注意：如果 API Key 包含掩码 (****)，则不填充到编辑框，避免误保存掩码字符串

          if (source.llm.apiKey && !source.llm.apiKey.includes("****")) newState.apiKey = source.llm.apiKey;
          if (source.llm.model) newState.defaultModel = source.llm.model;
          if (source.llm.provider) newState.provider = source.llm.provider;
          if (source.llm.fallbackOrder) newState.fallbackOrder = source.llm.fallbackOrder;
          if (source.llm.parallelTools !== undefined) newState.parallelTools = source.llm.parallelTools;
          if (source.llm.maxToolRounds !== undefined) newState.maxToolRounds = source.llm.maxToolRounds;
          if (source.llm.toolParallelism !== undefined) newState.toolParallelism = source.llm.toolParallelism;
          
          const orKey = source.llm.openrouter?.apiKey || source.llm.openrouterKey;
          if (orKey && !orKey.includes("****")) newState.openrouterKey = orKey;
          
          const gKey = source.llm.glm?.apiKey || source.llm.glmKey;
          if (gKey && !gKey.includes("****")) newState.glmKey = gKey;
          
          const kKey = source.llm.kimi?.apiKey || source.llm.kimiKey;
          if (kKey && !kKey.includes("****")) newState.kimiKey = kKey;
        }

        if (source.channels) {
          if (source.channels.telegram?.token && !source.channels.telegram.token.includes("****")) {
            newState.telegramToken = source.channels.telegram.token;
          }
          if (source.channels.whatsapp?.bridgeUrl) {
            newState.whatsappBridgeUrl = source.channels.whatsapp.bridgeUrl;
          }
          if (source.channels.wechat?.token && !source.channels.wechat.token.includes("****")) {
            newState.wechatWebhook = source.channels.wechat.token; 
          }
        }

        if (source.workspaceDir) {
          newState.workspaceDir = source.workspaceDir;
        }

        Object.assign(state, newState);
      }
    }
  } catch (error) {
    console.warn("Failed to fetch config from server", error);
  }
}


async function saveToServer() {
  const baseUrl = getApiBaseUrl() || window.location.origin;
  try {
    // 构造发送给后端的嵌套结构
    const payload: any = {
      llm: {
        apiBaseUrl: state.apiBaseUrl,
        baseUrl: state.apiBaseUrl,
        model: state.defaultModel,

        provider: state.provider,
        fallbackOrder: state.fallbackOrder,
        parallelTools: state.parallelTools,
        maxToolRounds: state.maxToolRounds,
        toolParallelism: state.toolParallelism,
        openrouter: {},
        glm: {},
        kimi: {}
      },
      channels: {
        telegram: {},
        whatsapp: { bridgeUrl: state.whatsappBridgeUrl },
        wechat: {}
      }
    };

    // 只有当 Key 不为空且不是掩码时才发送，避免覆盖后端正确配置
    if (state.apiKey && !state.apiKey.includes("****")) payload.llm.apiKey = state.apiKey;
    if (state.openrouterKey && !state.openrouterKey.includes("****")) payload.llm.openrouter.apiKey = state.openrouterKey;
    if (state.glmKey && !state.glmKey.includes("****")) payload.llm.glm.apiKey = state.glmKey;
    if (state.kimiKey && !state.kimiKey.includes("****")) payload.llm.kimi.apiKey = state.kimiKey;
    if (state.telegramToken && !state.telegramToken.includes("****")) payload.channels.telegram.token = state.telegramToken;
    if (state.wechatWebhook && !state.wechatWebhook.includes("****")) payload.channels.wechat.token = state.wechatWebhook;

    const response = await fetch(`${baseUrl}/api/config`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    if (!response.ok) throw new Error("Save failed");
    save();
  } catch (error) {
    console.error("Failed to save config to server", error);
    save();
  }
}



function update(patch: Partial<ConfigState>) {
  Object.assign(state, patch);
  saveToServer();
}

function reset() {
  Object.assign(state, { ...defaultState });
  saveToServer();
}

// Initial fetch
fetchFromServer();

export function useConfigStore() {
  return { state, update, save: saveToServer, reset, fetch: fetchFromServer };
}


export function getApiBaseUrl() {
  // 恢复为从浏览器 URL 组合，通常是当前 origin
  return window.location.origin;
}

