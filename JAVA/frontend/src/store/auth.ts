import { ref } from "vue";
import { getApiBaseUrl } from "./config";

export type AuthState = {
  enabled: boolean;
  authenticated: boolean;
  username?: string | null;
  checked: boolean;
};

const authState = ref<AuthState>({
  enabled: false,
  authenticated: true,
  username: null,
  checked: false
});

const baseUrl = () => getApiBaseUrl() || window.location.origin;

export async function fetchAuthState() {
  if (authState.value.checked) return authState.value;
  try {
    const res = await fetch(`${baseUrl()}/api/auth/me`);
    if (!res.ok) throw new Error(await res.text());
    const data = await res.json();
    authState.value = {
      enabled: Boolean(data.enabled),
      authenticated: Boolean(data.authenticated),
      username: data.username ?? null,
      checked: true
    };
  } catch (error) {
    authState.value = {
      enabled: true,
      authenticated: false,
      username: null,
      checked: true
    };
  }
  return authState.value;
}

export function resetAuthState() {
  authState.value.checked = false;
}

export function setAuthAuthenticated(username?: string | null) {
  authState.value = {
    enabled: true,
    authenticated: true,
    username: username ?? null,
    checked: true
  };
}

export async function logout() {
  try {
    await fetch(`${baseUrl()}/api/auth/logout`, { method: "POST" });
  } finally {
    authState.value = {
      enabled: true,
      authenticated: false,
      username: null,
      checked: true
    };
  }
}

export function getAuthState() {
  return authState;
}

