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


const router = createRouter({


  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/multi-agent" },
    { path: "/chat", component: ChatView },
    { path: "/multi-agent", component: MultiAgentChatView },
    { path: "/p2p-chat", component: P2pChatView },
    { path: "/agents", component: MultiAgentManageView },
    { path: "/skills", component: SkillsView },
    { path: "/skills/store", component: SkillStoreView },
    { path: "/cron", component: CronView },

    { path: "/monitor", component: MonitorView },

    { path: "/config", component: ConfigView }
  ]
});


export default router;
