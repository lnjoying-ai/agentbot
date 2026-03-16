export type MessageRole = "user" | "assistant" | "system" | "tool";

export interface ToolResult {
  id: string;
  name: string;
  status: "success" | "error" | "PENDING_APPROVAL";
  output: string;
  latencyMs?: number;
  agentId?: string;
}


export interface ChatMessage {
  id: string;
  role: MessageRole;
  content: string;
  timestamp: string;
  toolResults?: ToolResult[];
}

export interface ChatSessionSummary {
  chatId: string;
  channel: string;
  agentId?: string;
  lastMessageAt?: string;
  messageCount?: number;
}

export interface ChatHistoryMessage {
  id: string;
  role: MessageRole;
  content: string;
  timestamp: string;
}

export interface ChatUploadResult {
  ok: boolean;
  path?: string;
  storedName?: string;
  originalName?: string;
  size?: number;
  error?: string;
  timestamp?: string;
}

export type P2pChatDirection = "inbound" | "outbound";

export type P2pChatStatus = "SENT" | "ACKED" | "NACKED" | "FAILED" | "RECEIVED";

export interface P2pChatMessage {
  id: string;
  chatId: string;
  direction: P2pChatDirection;
  content: string;
  timestamp: string;
  status?: P2pChatStatus;
  reason?: string;
  msgId?: string;
  traceId?: string;
  fromNodeId?: string;
  fromAgentId?: string;
  toNodeId?: string;
  toAgentId?: string;
}

export interface P2pChatSession {
  chatId: string;
  title: string;
  messages: P2pChatMessage[];
  unreadCount: number;
  lastActivity: string;
  remoteNodeId?: string;
  remoteAgentId?: string;
}

export interface ConfigState {
  serverBaseUrl: string;
  config: Record<string, any>;
  configPath?: string;
}





export interface OpsHeartbeat {
  enabled: boolean;
  intervalSeconds: number;
}

export interface OpsCron {
  enabled: boolean;
  defaultIntervalSeconds: number;
}

export interface P2pSnapshot {
  connectionsOpened: number;
  connectionsClosed: number;
  handshakesCompleted: number;
  messagesReceived: number;
  messagesSent: number;
  acks: number;
  nacks: number;
  retries: number;
}

export interface OpsLogEntry {
  timestamp: string;
  type: string;
  payload: Record<string, any>;
}

export interface OpsInitResult {
  ok: boolean;
  workspace: string;
  files: string[];
}

export interface MonitorStats {
  uptime: string;
  activeSessions: number;
  toolCalls: number;
  queueDepth: number;
  errorRate: number;
  latencyP50: number;
  latencyP95: number;
  channelStatus: Record<string, "online" | "offline" | "degraded">;
  model: string;
  status: string;
  workspace?: string;
  heartbeat?: OpsHeartbeat;

  cron?: OpsCron;
  p2p?: P2pSnapshot;
}

