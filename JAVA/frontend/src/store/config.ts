import { reactive } from "vue";
import type { ConfigState } from "../types";

const STORAGE_KEY = "agentbot.config";

const defaultState: ConfigState = {
  serverBaseUrl: "",
  config: {},
  configPath: ""
};

function saveLocal() {
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      serverBaseUrl: state.serverBaseUrl
    })
  );
}

function loadState(): ConfigState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<ConfigState>;
      return {
        ...defaultState,
        serverBaseUrl: parsed.serverBaseUrl ?? ""
      };
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
      const stored = data.stored || {};
      const effective = data.effective || {};
      const resolvedConfig = Object.keys(stored).length
        ? stored
        : Object.keys(effective).length
          ? { agentbot: effective }
          : {};

      state.config = resolvedConfig;
      state.configPath = data.path || "";
    }
  } catch (error) {
    console.warn("Failed to fetch config from server", error);
  }
}

async function saveToServer() {
  const baseUrl = getApiBaseUrl() || window.location.origin;
  try {
    const payload = state.config && Object.keys(state.config).length ? state.config : {};
    const response = await fetch(`${baseUrl}/api/config`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    if (!response.ok) throw new Error("Save failed");
    saveLocal();
  } catch (error) {
    console.error("Failed to save config to server", error);
    saveLocal();
  }
}

function update(patch: Partial<ConfigState>) {
  Object.assign(state, patch);
  saveToServer();
}

async function reset() {
  await fetchFromServer();
}

// Initial fetch
fetchFromServer();

export function useConfigStore() {
  return { state, update, save: saveToServer, reset, fetch: fetchFromServer };
}

export function getApiBaseUrl() {
  const raw = state.serverBaseUrl;
  if (raw && raw.trim()) {
    return raw.replace(/\/$/, "");
  }
  return window.location.origin;
}



