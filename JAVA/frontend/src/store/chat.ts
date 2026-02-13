import { ref } from "vue";
import type { ChatMessage } from "../types";
import { useConfigStore } from "./config";

const messagesList = ref<ChatMessage[]>([
  {
    id: crypto.randomUUID(),
    role: "assistant",
    content: "欢迎来到 Agentbot 控制台，请描述你的任务，我会同步工具与上下文。",
    timestamp: new Date().toLocaleTimeString()
  }
]);

const config = useConfigStore();

async function sendUserMessage(text: string) {
  const userMsg: ChatMessage = {
    id: crypto.randomUUID(),
    role: "user",
    content: text,
    timestamp: new Date().toLocaleTimeString()
  };
  messagesList.value.push(userMsg);

  try {
    const baseUrl = config.state.apiBaseUrl || "http://localhost:8080";
    const response = await fetch(`${baseUrl}/api/chat/send`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        channel: "web",
        senderId: "web-user",
        chatId: "default",
        content: text
      })
    });

    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }

    const data = await response.json();
    messagesList.value.push({
      id: data.id || crypto.randomUUID(),
      role: "assistant",
      content: data.content,
      timestamp: new Date(data.timestamp).toLocaleTimeString(),
      toolResults: data.toolResults || []
    });
  } catch (error) {
    console.error("Failed to send message:", error);
    messagesList.value.push({
      id: crypto.randomUUID(),
      role: "assistant",
      content: "抱歉，由于网络或后端服务异常，我暂时无法处理您的请求。",
      timestamp: new Date().toLocaleTimeString()
    });
  }
}

export function useChatStore() {
  return { 
    messages: messagesList, 
    sendUserMessage 
  };
}
