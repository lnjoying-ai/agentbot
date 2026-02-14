export type MessageRole = "user" | "assistant" | "system" | "tool";

export interface ToolResult {
  id: string;
  name: string;
  status: "success" | "error";
  output: string;
  latencyMs?: number;
}

export interface ChatMessage {
  id: string;
  role: MessageRole;
  content: string;
  timestamp: string;
  toolResults?: ToolResult[];
}

export interface ConfigState {
  apiBaseUrl: string;
  defaultModel: string;
  apiKey: string;
  provider: string;
  fallbackOrder: string;
  parallelTools: boolean;
  maxToolRounds: number;
  toolParallelism: number;
  telegramToken: string;
  whatsappBridgeUrl: string;
  wechatWebhook: string;
  workspaceDir?: string;
  openrouterKey?: string;
  glmKey?: string;
  kimiKey?: string;
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
}
