<template>
  <section class="world-page">
    <div class="world-header">
      <div>
        <h2 class="section-title">{{ t("world.title") }}</h2>
        <p class="subtitle">{{ t("world.subtitle") }}</p>
      </div>
      <div class="world-controls">
        <div class="speed-control">
          <span class="label">{{ t("world.speed") }}</span>
          <button
            v-for="value in speedOptions"
            :key="value"
            class="chip"
            :class="{ active: speed === value }"
            @click="speed = value"
          >
            {{ value }}x
          </button>
        </div>
        <button class="button" @click="togglePause">
          {{ paused ? t("world.resume") : t("world.pause") }}
        </button>
        <button class="button secondary" @click="scatterAgents">{{ t("world.scatter") }}</button>
      </div>
    </div>

    <div class="world-grid">
      <div class="world-map">
        <div class="world-bg"></div>
        <div class="world-decor">
          <span class="road road-main"></span>
          <span class="road road-side"></span>
          <span class="fence fence-top"></span>
          <span class="fence fence-right"></span>
          <span class="flowers flowers-left"></span>
          <span class="flowers flowers-right"></span>
        </div>
        <div class="zone" v-for="zone in zones" :key="zone.id" :style="zoneStyle(zone)">
          <span>{{ zone.label }}</span>
        </div>
        <div
          v-for="agent in worldAgents"
          :key="agent.id"
          class="agent-dot"
          :class="agent.state"
          :style="agentStyle(agent)"
        >
          <span class="agent-label">{{ agent.name }}</span>
        </div>
      </div>

      <aside class="world-panel">
        <div class="panel-card">
          <h3>{{ t("world.summary") }}</h3>
          <div class="summary-row">
            <span>{{ t("world.total") }}</span>
            <strong>{{ totals.total }}</strong>
          </div>
          <div class="summary-row">
            <span>{{ t("world.active") }}</span>
            <strong>{{ totals.active }}</strong>
          </div>
          <div class="summary-row">
            <span>{{ t("world.working") }}</span>
            <strong>{{ totals.working }}</strong>
          </div>
          <div class="summary-row">
            <span>{{ t("world.idle") }}</span>
            <strong>{{ totals.idle }}</strong>
          </div>
          <div class="summary-row">
            <span>{{ t("world.offline") }}</span>
            <strong>{{ totals.offline }}</strong>
          </div>
        </div>

        <div class="panel-card">
          <h3>{{ t("world.agentStatus") }}</h3>
          <div class="agent-list">
            <div v-for="agent in worldAgents" :key="agent.id" class="agent-card" :class="agent.state">
              <div class="agent-info">
                <strong>{{ agent.name }}</strong>
                <span class="muted">{{ agent.id }}</span>
              </div>
              <span class="status-pill">{{ statusLabel(agent.state) }}</span>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useI18n } from "../i18n";
import { useAgentStore, Agent } from "../store/agents";

const { t } = useI18n();
const store = useAgentStore();

const speedOptions = [0.5, 1, 2, 4];
const speed = ref(1);
const paused = ref(false);

const zones = computed(() => [
  { id: "build", label: t("world.zone.build"), x: 18, y: 26, color: "#7c8cff" },
  { id: "test", label: t("world.zone.test"), x: 70, y: 22, color: "#8ce0b1" },
  { id: "monitor", label: t("world.zone.monitor"), x: 58, y: 68, color: "#f5b36a" },
  { id: "rest", label: t("world.zone.rest"), x: 26, y: 72, color: "#f58ad8" }
]);

type AgentState = "working" | "idle" | "offline";


interface WorldAgent {
  id: string;
  name: string;
  status?: Agent["status"];
  enabled?: boolean;
  x: number;
  y: number;
  targetX: number;
  targetY: number;
  speed: number;
  state: AgentState;
}


const worldAgents = ref<WorldAgent[]>([]);
let timer: number | null = null;
let statusTimer: number | null = null;


const totals = computed(() => {
  const total = worldAgents.value.length;
  const active = worldAgents.value.filter(agent => agent.state !== "offline").length;
  const working = worldAgents.value.filter(agent => agent.state === "working").length;
  const idle = worldAgents.value.filter(agent => agent.state === "idle").length;
  const offline = worldAgents.value.filter(agent => agent.state === "offline").length;
  return { total, active, working, idle, offline };
});

const statusLabel = (state: AgentState) => {
  if (state === "working") return t("world.status.working");
  if (state === "idle") return t("world.status.idle");
  return t("world.status.offline");
};


const zoneStyle = (zone: { x: number; y: number; color: string }) => ({
  left: `${zone.x}%`,
  top: `${zone.y}%`,
  borderColor: zone.color,
  boxShadow: `0 0 24px ${zone.color}33`
});

const agentStyle = (agent: WorldAgent) => ({
  left: `${agent.x}%`,
  top: `${agent.y}%`
});

const randomBetween = (min: number, max: number) => Math.random() * (max - min) + min;

const isOffline = (agent: Agent) => agent.enabled === false || agent.status === "inactive";

const syncAgents = () => {
  const current = new Map(worldAgents.value.map(agent => [agent.id, agent]));
  const next: WorldAgent[] = [];

  store.agents.value.forEach(agent => {
    const existing = current.get(agent.id);
    const state: AgentState = isOffline(agent)
      ? "offline"
      : agent.sessionStatus === "working"
        ? "working"
        : "idle";
    const base: WorldAgent = existing ?? {
      id: agent.id,
      name: agent.displayName || agent.name || agent.id,
      status: agent.status,
      enabled: agent.enabled,
      x: randomBetween(6, 94),
      y: randomBetween(6, 94),
      targetX: randomBetween(0, 100),
      targetY: randomBetween(0, 100),

      speed: randomBetween(0.4, 0.9),
      state
    };

    base.name = agent.displayName || agent.name || agent.id;
    base.status = agent.status;
    base.enabled = agent.enabled;
    base.state = state;
    next.push(base);
  });


  worldAgents.value = next;
};

const moveAgent = (agent: WorldAgent) => {
  const dx = agent.targetX - agent.x;
  const dy = agent.targetY - agent.y;
  const dist = Math.hypot(dx, dy);
  if (dist < 1) return;
  const speedFactor = agent.state === "working" ? 0.5 : 1;
  const step = agent.speed * speed.value * speedFactor;
  agent.x += (dx / dist) * step;
  agent.y += (dy / dist) * step;
};


const chooseTarget = (agent: WorldAgent) => {
  const zone = zones.value[Math.floor(Math.random() * zones.value.length)];
  agent.targetX = Math.min(100, Math.max(0, zone.x + randomBetween(-12, 12)));
  agent.targetY = Math.min(100, Math.max(0, zone.y + randomBetween(-12, 12)));

};

const tick = () => {
  if (paused.value) return;
  worldAgents.value.forEach(agent => {
    if (agent.state === "offline") return;
    const dx = agent.targetX - agent.x;
    const dy = agent.targetY - agent.y;
    const dist = Math.hypot(dx, dy);
    if (dist < 1 || Math.random() < 0.02) {
      chooseTarget(agent);
    }
    moveAgent(agent);
  });
};


const scatterAgents = () => {
  worldAgents.value = worldAgents.value.map(agent => ({
    ...agent,
    x: randomBetween(6, 94),
    y: randomBetween(6, 94),
    targetX: randomBetween(0, 100),
    targetY: randomBetween(0, 100),

    state: agent.state === "offline" ? "offline" : agent.state
  }));
};


const togglePause = () => {
  paused.value = !paused.value;
};

onMounted(async () => {
  await store.fetchAgents();
  syncAgents();
  timer = window.setInterval(tick, 200);
  statusTimer = window.setInterval(() => {
    store.fetchAgents();
  }, 3000);
});

watch(store.agents, syncAgents);

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer);
  if (statusTimer) window.clearInterval(statusTimer);
});

</script>

<style scoped>
.world-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.world-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.subtitle {
  margin-top: 6px;
  color: var(--muted);
  font-size: 13px;
}

.world-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.speed-control {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid rgba(111, 140, 255, 0.2);
  background: rgba(111, 140, 255, 0.08);
}

.speed-control .label {
  font-size: 12px;
  color: var(--muted);
}

.chip {
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.08);
  color: var(--text);
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.chip.active {
  border-color: rgba(124, 140, 255, 0.6);
  background: rgba(124, 140, 255, 0.25);
}

.world-grid {
  display: grid;
  grid-template-columns: 2.2fr 1fr;
  gap: 18px;
}

.world-map {
  position: relative;
  height: clamp(520px, 70vh, 760px);
  border-radius: 18px;
  border: 1px solid rgba(124, 140, 255, 0.15);
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 22%, rgba(120, 190, 110, 0.45), transparent 52%),
    radial-gradient(circle at 76% 34%, rgba(90, 170, 100, 0.38), transparent 58%),
    radial-gradient(circle at 62% 76%, rgba(110, 180, 120, 0.32), transparent 60%),
    linear-gradient(135deg, rgba(18, 48, 26, 0.92), rgba(8, 26, 14, 0.98));
}


.world-map::before,
.world-map::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.world-map::before {
  background:
    radial-gradient(circle at 20% 18%, rgba(140, 210, 120, 0.18), transparent 46%),
    radial-gradient(circle at 82% 70%, rgba(120, 190, 90, 0.2), transparent 50%),
    radial-gradient(circle at 60% 46%, rgba(80, 140, 70, 0.18), transparent 58%);
  mix-blend-mode: screen;
  opacity: 0.6;
}

.world-map::after {
  background:
    linear-gradient(0deg, rgba(6, 14, 8, 0.6) 0%, transparent 35%, transparent 65%, rgba(6, 14, 8, 0.7) 100%),
    radial-gradient(circle at 30% 40%, rgba(12, 24, 14, 0.5), transparent 60%),
    radial-gradient(circle at 70% 62%, rgba(10, 20, 12, 0.55), transparent 62%);
  opacity: 0.7;
}

.world-bg {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(45deg, rgba(255, 255, 255, 0.02) 0 6px, transparent 6px 12px),
    radial-gradient(circle at 18% 26%, rgba(110, 170, 90, 0.5) 0 10px, transparent 12px),
    radial-gradient(circle at 64% 32%, rgba(120, 190, 100, 0.45) 0 12px, transparent 14px),
    radial-gradient(circle at 52% 76%, rgba(90, 150, 80, 0.4) 0 12px, transparent 16px),
    radial-gradient(circle at 40% 58%, rgba(126, 96, 58, 0.35) 0 12px, transparent 16px),
    radial-gradient(circle at 74% 52%, rgba(118, 92, 54, 0.32) 0 10px, transparent 14px);
  background-size: 48px 48px, 48px 48px, 80px 80px, 320px 320px, 360px 360px, 400px 400px, 300px 300px, 340px 340px;
  background-position: center;
  opacity: 0.95;
  image-rendering: pixelated;
}



.world-decor {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.road {
  position: absolute;
  border-radius: 26px;
  background:
    linear-gradient(90deg, rgba(90, 66, 44, 0.85), rgba(120, 92, 62, 0.9)),
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.08) 0 10px, transparent 10px 20px);
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.08), 0 6px 14px rgba(6, 10, 18, 0.45);
  opacity: 0.9;
}

.road-main {
  width: 78%;
  height: 90px;
  left: 12%;
  top: 42%;
  transform: rotate(-4deg);
}

.road-side {
  width: 40%;
  height: 70px;
  left: 6%;
  top: 62%;
  transform: rotate(18deg);
}

.fence {
  position: absolute;
  height: 18px;
  background:
    repeating-linear-gradient(90deg, rgba(146, 108, 62, 0.95) 0 10px, rgba(110, 78, 44, 0.95) 10px 16px),
    linear-gradient(0deg, rgba(255, 255, 255, 0.2), transparent);
  box-shadow: 0 4px 10px rgba(8, 10, 16, 0.35);
  border-radius: 8px;
}

.fence-top {
  width: 46%;
  left: 18%;
  top: 28%;
}

.fence-right {
  width: 38%;
  right: 10%;
  top: 58%;
}

.flowers {
  position: absolute;
  width: 180px;
  height: 80px;
  border-radius: 20px;
  background:
    radial-gradient(circle at 14% 30%, rgba(255, 111, 145, 0.9) 0 10px, transparent 12px),
    radial-gradient(circle at 36% 52%, rgba(255, 205, 112, 0.9) 0 10px, transparent 12px),
    radial-gradient(circle at 64% 36%, rgba(122, 225, 178, 0.9) 0 10px, transparent 12px),
    radial-gradient(circle at 82% 58%, rgba(255, 154, 90, 0.9) 0 10px, transparent 12px),
    radial-gradient(circle at 50% 75%, rgba(140, 180, 255, 0.8) 0 10px, transparent 12px);
  filter: drop-shadow(0 6px 10px rgba(6, 10, 18, 0.35));
  opacity: 0.9;
  image-rendering: pixelated;
}

.flowers-left {
  left: 10%;
  top: 20%;
}

.flowers-right {
  right: 14%;
  top: 70%;
}

.zone {
  position: absolute;
  transform: translate(-50%, -50%);
  padding: 10px 14px;
  border-radius: 999px;
  border: 1px dashed rgba(255, 255, 255, 0.3);
  font-size: 12px;
  color: var(--text);
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(6px);
}

.agent-dot {
  position: absolute;
  transform: translate(-50%, -50%);
  width: 26px;
  height: 26px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(8, 12, 20, 0.55);
  box-shadow: 0 10px 20px rgba(6, 10, 18, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  image-rendering: pixelated;
  --agent-color: #f6d365;
}

.agent-dot::before {
  content: "";
  position: absolute;
  width: 2px;
  height: 2px;
  background: var(--agent-color);
  left: 50%;
  top: 50%;
  transform: translate(-50%, -55%);
  box-shadow:
    2px 0 var(--agent-color),
    0 2px var(--agent-color),
    2px 2px var(--agent-color),
    -2px 4px var(--agent-color),
    0 4px var(--agent-color),
    2px 4px var(--agent-color),
    4px 4px var(--agent-color),
    0 6px var(--agent-color),
    2px 6px var(--agent-color),
    -2px 8px var(--agent-color),
    4px 8px var(--agent-color);
}

.agent-dot::after {
  content: "";
  position: absolute;
  bottom: 2px;
  width: 16px;
  height: 6px;
  background: rgba(0, 0, 0, 0.35);
  border-radius: 50%;
  filter: blur(1px);
}

.agent-dot .agent-label {
  position: absolute;
  top: 30px;
  left: 50%;
  transform: translateX(-50%);
  white-space: nowrap;
  font-size: 11px;
  color: var(--text);
  background: rgba(0, 0, 0, 0.4);
  padding: 2px 6px;
  border-radius: 999px;
}

.agent-dot.working {
  --agent-color: #f6d365;
  box-shadow: 0 0 18px rgba(246, 211, 101, 0.45);
}

.agent-dot.idle {
  --agent-color: #7de2a2;
  box-shadow: 0 0 18px rgba(125, 226, 162, 0.45);
}


.agent-dot.offline {
  --agent-color: #ff6b6b;
  background: rgba(30, 10, 12, 0.65);
  border-color: rgba(255, 107, 107, 0.45);
  box-shadow: 0 0 12px rgba(255, 107, 107, 0.35);
}

.world-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-card {
  border-radius: 16px;
  border: 1px solid rgba(124, 140, 255, 0.2);
  padding: 16px;
  background: rgba(14, 18, 36, 0.8);
  box-shadow: 0 20px 40px rgba(8, 12, 24, 0.35);
}

.panel-card h3 {
  margin: 0 0 12px;
  font-size: 14px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--muted);
  padding: 4px 0;
}

.summary-row strong {
  color: var(--text);
}

.agent-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.agent-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.agent-card .agent-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.agent-card .muted {
  font-size: 11px;
}

.status-pill {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
}

.agent-card.working .status-pill {
  background: rgba(246, 211, 101, 0.2);
  color: #f6d365;
}

.agent-card.idle .status-pill {
  background: rgba(125, 226, 162, 0.2);
  color: #7de2a2;
}


.agent-card.offline .status-pill {
  background: rgba(255, 107, 107, 0.2);
  color: #ff6b6b;
}

@media (max-width: 1100px) {
  .world-grid {
    grid-template-columns: 1fr;
  }
}
</style>
