import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import { setAuthUnauthenticated } from "./store/auth";
import "./styles.css";

const rawFetch = window.fetch.bind(window);
window.fetch = async (input, init) => {
  const response = await rawFetch(input, init);
  const status = response.status;
  if (status === 401 || status === 403) {
    const url = typeof input === "string"
      ? input
      : input instanceof Request
        ? input.url
        : input instanceof URL
          ? input.toString()
          : String(input);
    const isAuthRequest = url.includes("/api/auth/login") || url.includes("/api/auth/logout");

    if (!isAuthRequest) {
      setAuthUnauthenticated();
      const current = router.currentRoute.value;
      if (current.path !== "/login") {
        const redirect = current.fullPath || "/";
        void router.isReady().then(() =>
          router.replace({ path: "/login", query: { redirect } })
        );
      }
    }
  }
  return response;
};

createApp(App).use(router).mount("#app");

