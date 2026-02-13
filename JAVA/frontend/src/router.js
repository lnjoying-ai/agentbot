import { createRouter, createWebHistory } from "vue-router";
import ChatView from "./views/ChatView.vue";
import MonitorView from "./views/MonitorView.vue";
import ConfigView from "./views/ConfigView.vue";
const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: "/", redirect: "/chat" },
        { path: "/chat", component: ChatView },
        { path: "/monitor", component: MonitorView },
        { path: "/config", component: ConfigView }
    ]
});
export default router;
