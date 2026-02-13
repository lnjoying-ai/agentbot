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
  wechatWebhook: "http://localhost:8080/webhook/wechat"
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
  const baseUrl = getApiBaseUrl();
  if (!baseUrl) return;
  try {
    const response = await fetch(`${baseUrl}/api/config`);
    if (response.ok) {
      const data = await response.json();
      // data has { effective: {...}, stored: {...}, path: "..." }
      // We prioritize 'stored' for editing, but can show 'effective' as well.
      // For simplicity, we'll merge stored into our state.
      if (data.stored) {
        Object.assign(state, data.stored);
      }
    }
  } catch (error) {
    console.warn("Failed to fetch config from server", error);
  }
}

async function saveToServer() {
  const baseUrl = getApiBaseUrl();
  if (!baseUrl) {
    save(); // Still save to localStorage
    return;
  }
  try {
    const response = await fetch(`${baseUrl}/api/config`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(state)
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
  return state.apiBaseUrl?.trim() || "";
}
