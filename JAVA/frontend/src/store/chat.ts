import { ref } from "vue";
import { getApiBaseUrl } from "./config";
import type { ChatHistoryMessage, ChatMessage, ChatSessionSummary, ChatUploadResult } from "../types";



function generateId() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}


const messagesList = ref<ChatMessage[]>([

  {
    id: generateId(),
    role: "assistant",
    content: "欢迎来到 Agentbot，请描述你的任务，我会同步工具与上下文。",
    timestamp: new Date().toLocaleTimeString()
  }


]);



type SendChatOptions = {
  agentId?: string;
  chatId?: string;
  metadata?: Record<string, unknown>;
};

type StreamOptions = {
  channel?: string;
  chatId: string;
  onMessage: (message: ChatMessage) => void;
};


function buildAssistantMessage(data: any, agentId?: string): ChatMessage {
  const baseToolResults = data.metadata?.status === "PENDING_APPROVAL"
    ? [
      {
        id: data.metadata.toolCallId,
        name: data.metadata.toolName,
        status: "PENDING_APPROVAL",
        output: data.content,
        agentId
      }
    ]
    : (data.toolResults || []);

  const toolResults = agentId && Array.isArray(baseToolResults)
    ? baseToolResults.map((tool: any) => ({ ...tool, agentId }))
    : baseToolResults;

  return {
    id: data.id || generateId(),

    role: "assistant",
    content: data.content,
    timestamp: new Date(data.timestamp).toISOString(),

    toolResults
  };
}

async function postChat(text: string, options: SendChatOptions = {}) {
  const metadata = { ...(options.metadata || {}) } as Record<string, unknown>;
  if (options.agentId) {
    metadata.agentId = options.agentId;
  }

  const baseUrl = getApiBaseUrl() || window.location.origin;
  const response = await fetch(`${baseUrl}/api/chat/send`, {

    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      channel: "web",
      senderId: "web-user",
      chatId: options.chatId || "default",
      content: text,
      metadata: Object.keys(metadata).length ? metadata : undefined
    })
  });

  if (!response.ok) {
    throw new Error(`API error: ${response.status}`);
  }

  return response.json();
}

async function uploadFile(file: File): Promise<ChatUploadResult> {
  const baseUrl = getApiBaseUrl() || window.location.origin;
  const form = new FormData();
  form.append("file", file);
  const response = await fetch(`${baseUrl}/api/chat/upload`, {
    method: "POST",
    body: form
  });

  if (!response.ok) {
    throw new Error(await response.text());
  }

  return response.json();
}

async function sendUserMessage(text: string) {
  const userMsg: ChatMessage = {
    id: generateId(),
    role: "user",
    content: text,
    timestamp: new Date().toLocaleTimeString()
  };

  messagesList.value.push(userMsg);

  try {
    const data = await postChat(text);
    const assistantMessage = buildAssistantMessage(data);
    messagesList.value.push(assistantMessage);
    return assistantMessage;
  } catch (error) {
    console.error("Failed to send message:", error);
    messagesList.value.push({
      id: generateId(),
      role: "assistant",

      content: "抱歉，由于网络或后端服务异常，我暂时无法处理您的请求。",
      timestamp: new Date().toISOString()
    });

    return null;
  }
}


async function sendUserMessageForAgent(text: string, agentId: string) {
  const data = await postChat(text, { agentId, chatId: agentId });
  return buildAssistantMessage(data, agentId);
}

async function confirmTool(toolCallId: string, options: SendChatOptions = {}) {
  const userMsg: ChatMessage = {
    id: generateId(),
    role: "user",
    content: "确认执行工具操作",
    timestamp: new Date().toLocaleTimeString()
  };

  messagesList.value.push(userMsg);

  try {
    const metadata = {
      ...(options.metadata || {}),
      confirmedToolCallId: toolCallId,
      confirmed: true
    };

    const data = await postChat("Confirmed", {
      ...options,
      metadata
    });
    const assistantMessage = buildAssistantMessage(data, options.agentId);
    messagesList.value.push(assistantMessage);
    return assistantMessage;
  } catch (error) {
    console.error("Confirmation failed:", error);
    return null;
  }
}

async function cancelTool(toolCallId: string, options: SendChatOptions = {}) {
  const userMsg: ChatMessage = {
    id: generateId(),
    role: "user",
    content: "取消执行工具操作",

    timestamp: new Date().toLocaleTimeString()
  };
  messagesList.value.push(userMsg);

  try {
    const metadata = {
      ...(options.metadata || {}),
      confirmedToolCallId: toolCallId,
      confirmed: false
    };

    const data = await postChat("Cancelled", {
      ...options,
      metadata
    });
    const assistantMessage = buildAssistantMessage(data, options.agentId);
    messagesList.value.push(assistantMessage);
    return assistantMessage;
  } catch (error) {
    console.error("Cancellation failed:", error);
    return null;
  }
}



async function fetchSessions(agentId?: string, channel = "web") {
  const params = new URLSearchParams({ channel });
  if (agentId) params.set("agentId", agentId);
  const baseUrl = getApiBaseUrl() || window.location.origin;
  const res = await fetch(`${baseUrl}/api/chats?${params.toString()}`);

  if (!res.ok) throw new Error(await res.text());
  return (await res.json()) as ChatSessionSummary[];
}

async function fetchHistory(agentId: string, chatId: string, channel = "web", limit = 50, before?: string) {
  const params = new URLSearchParams({ channel, limit: String(limit) });
  if (agentId) params.set("agentId", agentId);
  if (before) params.set("before", before);

  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), 15000);
  try {
    const baseUrl = getApiBaseUrl() || window.location.origin;
    const res = await fetch(`${baseUrl}/api/chats/${encodeURIComponent(chatId)}/messages?${params.toString()}`, {

      signal: controller.signal
    });
    if (!res.ok) throw new Error(await res.text());
    return (await res.json()) as { messages: ChatHistoryMessage[]; nextCursor?: string };
  } finally {
    window.clearTimeout(timeout);
  }
}


const eventSources = new Map<string, EventSource>();

function connectStream(options: StreamOptions) {
  const channel = options.channel || "web";
  const key = `${channel}:${options.chatId}`;
  if (eventSources.has(key)) return () => {};

  const params = new URLSearchParams({ channel, chatId: options.chatId });
  const baseUrl = getApiBaseUrl() || window.location.origin;
  const source = new EventSource(`${baseUrl}/api/chat/stream?${params.toString()}`);

  eventSources.set(key, source);

  source.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data || "{}");
      const payload = data.payload || {};
      const role = payload.role === "assistant" ? "assistant" : "user";
      const message: ChatMessage = {
        id: `${payload.timestamp || Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        role,
        content: payload.content || "",
        timestamp: payload.timestamp || new Date().toISOString()
      };
      options.onMessage(message);
    } catch (error) {
      console.warn("Failed to parse chat stream event", error);
    }
  };
  source.onerror = () => {};

  return () => {
    const current = eventSources.get(key);
    if (current) {
      current.close();
      eventSources.delete(key);
    }
  };
}

export function useChatStore() {
  return { 
    messages: messagesList, 
    sendUserMessage,
    sendUserMessageForAgent,
    confirmTool,
    cancelTool,
    fetchSessions,
    fetchHistory,
    connectStream,
    uploadFile
  };
}





