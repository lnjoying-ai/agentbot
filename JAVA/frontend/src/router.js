import { createRouter, createWebHistory } from "vue-router";
import ChatView from "./views/ChatView.vue";
import MultiAgentChatView from "./views/MultiAgentChatView.vue";
import MultiAgentManageView from "./views/MultiAgentManageView.vue";
import MonitorView from "./views/MonitorView.vue";
import ConfigView from "./views/ConfigView.vue";
import SkillsView from "./views/SkillsView.vue";
import SkillStoreView from "./views/SkillStoreView.vue";
import P2pChatView from "./views/P2pChatView.vue";
import CronView from "./views/CronView.vue";
import FilesView from "./views/FilesView.vue";
import OpenWorldView from "./views/OpenWorldView.vue";
import LoginView from "./views/LoginView.vue";
import { fetchAuthState } from "./store/auth";
const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: "/", redirect: "/open-world" },
        { path: "/login", component: LoginView },
        { path: "/chat", component: ChatView },
        { path: "/multi-agent", component: MultiAgentChatView },
        { path: "/p2p-chat", component: P2pChatView },
        { path: "/agents", component: MultiAgentManageView },
        { path: "/skills", component: SkillsView },
        { path: "/skills/store", component: SkillStoreView },
        { path: "/cron", component: CronView },
        { path: "/workspace/files", component: FilesView },
        { path: "/open-world", component: OpenWorldView },
        { path: "/monitor", component: MonitorView },
        { path: "/config", component: ConfigView }
    ]
});
router.beforeEach(async (to) => {
    const auth = await fetchAuthState();
    if (!auth.enabled)
        return true;
    if (to.path === "/login") {
        return auth.authenticated ? "/open-world" : true;
    }
    if (!auth.authenticated) {
        return { path: "/login", query: { redirect: to.fullPath } };
    }
    return true;
});
export default router;
