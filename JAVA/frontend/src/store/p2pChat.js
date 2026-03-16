import { computed, reactive, ref } from "vue";
import { getApiBaseUrl } from "./config";
function generateId() {
    if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
        return crypto.randomUUID();
    }
    return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}
const sessions = reactive(new Map());
const currentChatId = ref("");
const connected = ref(false);
const error = ref(null);
const messageIndex = reactive(new Map());
let eventSource = null;
function buildChatId(fromAgentId, toNodeId, toAgentId) {
    const safeFrom = fromAgentId && fromAgentId.trim() ? fromAgentId.trim() : "-";
    const safeNode = toNodeId && toNodeId.trim() ? toNodeId.trim() : "-";
    const safeAgent = toAgentId && toAgentId.trim() ? toAgentId.trim() : "-";
    return `p2p:${safeFrom}:${safeNode}:${safeAgent}`;
}
function ensureSession(chatId, payload) {
    let session = sessions.get(chatId);
    if (!session) {
        session = {
            chatId,
            title: payload?.title || chatId,
            messages: [],
            unreadCount: 0,
            lastActivity: new Date().toISOString(),
            remoteNodeId: payload?.remoteNodeId,
            remoteAgentId: payload?.remoteAgentId
        };
        sessions.set(chatId, session);
    }
    return session;
}
function resolveTitle(payload, direction) {
    const chatId = payload.chatId || "P2P 会话";
    const nodeId = payload.fromNodeId || (direction === "inbound" ? payload.fromNodeId : payload.toNodeId);
    const agentId = payload.fromAgentId || (direction === "inbound" ? payload.fromAgentId : payload.toAgentId);
    const fromLabel = [nodeId, agentId].filter(Boolean).join("/");
    if (fromLabel) {
        return `${chatId} | ${fromLabel}`;
    }
    return chatId;
}
function formatTimestamp(value) {
    if (!value)
        return new Date().toLocaleTimeString();
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleTimeString();
}
function addMessageFromPayload(payload) {
    if (!payload)
        return;
    const direction = payload.direction === "outbound" ? "outbound" : "inbound";
    const chatId = payload.chatId || buildChatId(payload.fromAgentId, payload.toNodeId, payload.toAgentId);
    const session = ensureSession(chatId, {
        title: resolveTitle(payload, direction),
        remoteNodeId: direction === "inbound" ? payload.fromNodeId : payload.toNodeId,
        remoteAgentId: direction === "inbound" ? payload.fromAgentId : payload.toAgentId
    });
    const msgId = payload.msgId || generateId();
    const message = {
        id: msgId,
        chatId,
        direction,
        content: payload.content || "",
        timestamp: formatTimestamp(payload.timestamp),
        status: payload.status,
        reason: payload.reason,
        msgId: payload.msgId,
        traceId: payload.traceId,
        fromNodeId: payload.fromNodeId,
        fromAgentId: payload.fromAgentId,
        toNodeId: payload.toNodeId,
        toAgentId: payload.toAgentId
    };
    session.messages.push(message);
    session.lastActivity = new Date().toISOString();
    session.title = resolveTitle(payload, direction);
    session.remoteNodeId = direction === "inbound" ? payload.fromNodeId : payload.toNodeId;
    session.remoteAgentId = direction === "inbound" ? payload.fromAgentId : payload.toAgentId;
    if (direction === "inbound" && chatId !== currentChatId.value) {
        session.unreadCount += 1;
    }
    if (payload.msgId) {
        messageIndex.set(payload.msgId, message);
    }
}
function updateStatusFromPayload(payload) {
    if (!payload || !payload.msgId)
        return;
    const message = messageIndex.get(payload.msgId);
    if (!message)
        return;
    message.status = payload.status;
    if (payload.reason) {
        message.reason = payload.reason;
    }
}
function connect() {
    if (eventSource)
        return;
    const baseUrl = getApiBaseUrl() || window.location.origin;
    eventSource = new EventSource(`${baseUrl}/api/p2p/chat/events`);
    eventSource.onopen = () => {
        connected.value = true;
        error.value = null;
    };
    eventSource.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data || "{}");
            const type = data.type;
            const payload = data.payload || {};
            if (type === "p2p.chat.inbound" || type === "p2p.chat.outbound") {
                addMessageFromPayload(payload);
            }
            else if (type === "p2p.chat.ack" || type === "p2p.chat.nack") {
                updateStatusFromPayload(payload);
            }
        }
        catch (e) {
            console.warn("Failed to parse p2p chat event", e);
        }
    };
    eventSource.onerror = () => {
        connected.value = false;
    };
}
function disconnect() {
    if (!eventSource)
        return;
    eventSource.close();
    eventSource = null;
    connected.value = false;
}
async function sendMessage(options) {
    const baseUrl = getApiBaseUrl() || window.location.origin;
    const msgId = generateId();
    const response = await fetch(`${baseUrl}/api/chat/p2psend`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            fromAgentId: options.agentId || "p2p-ui",
            toNodeId: options.toNodeId,
            toAgentId: options.toAgentId,
            content: options.content,
            msgId,
            ackRequired: true
        })
    });
    if (!response.ok) {
        throw new Error(`API error: ${response.status}`);
    }
    return response.json();
}
function selectChat(chatId) {
    currentChatId.value = chatId;
    const session = sessions.get(chatId);
    if (session) {
        session.unreadCount = 0;
    }
}
const sessionList = computed(() => {
    return Array.from(sessions.values()).sort((a, b) => {
        return new Date(b.lastActivity).getTime() - new Date(a.lastActivity).getTime();
    });
});
const currentSession = computed(() => {
    if (!currentChatId.value && sessionList.value.length) {
        currentChatId.value = sessionList.value[0].chatId;
    }
    return sessions.get(currentChatId.value) || null;
});
export function useP2pChatStore() {
    return {
        sessions,
        sessionList,
        currentChatId,
        currentSession,
        connected,
        error,
        connect,
        disconnect,
        sendMessage,
        selectChat
    };
}
