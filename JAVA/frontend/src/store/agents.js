import { reactive, ref, computed } from 'vue';
const agents = ref([]);
const currentAgentId = ref('default');
const conversations = reactive(new Map());
const agentSkills = reactive(new Map());
const loading = ref(false);
const error = ref(null);
export function useAgentStore() {
    // Get current agent
    const currentAgent = computed(() => agents.value.find(a => a.id === currentAgentId.value));
    // Get agent conversation
    const currentConversation = computed(() => conversations.get(currentAgentId.value) || {
        agentId: currentAgentId.value,
        messages: [],
        unreadCount: 0,
        lastActivity: new Date().toISOString()
    });
    // Get total unread count
    const totalUnreadCount = computed(() => {
        let total = 0;
        conversations.forEach(conv => {
            total += conv.unreadCount;
        });
        return total;
    });
    // Fetch all agents
    async function fetchAgents() {
        loading.value = true;
        error.value = null;
        try {
            const response = await fetch('/api/agents');
            if (!response.ok) {
                throw new Error(`Failed to fetch agents: ${response.statusText}`);
            }
            const data = await response.json();
            agents.value = data;
            // Initialize conversations for all agents
            data.forEach((agent) => {
                if (!conversations.has(agent.id)) {
                    conversations.set(agent.id, {
                        agentId: agent.id,
                        messages: [],
                        unreadCount: 0,
                        lastActivity: new Date().toISOString()
                    });
                }
            });
        }
        catch (e) {
            error.value = e.message;
            console.error('Error fetching agents:', e);
        }
        finally {
            loading.value = false;
        }
    }
    // Get agent by ID
    function getAgent(agentId) {
        return agents.value.find(a => a.id === agentId);
    }
    // Switch to agent
    function switchToAgent(agentId) {
        const agent = getAgent(agentId);
        if (!agent) {
            console.warn(`Agent not found: ${agentId}`);
            return;
        }
        currentAgentId.value = agentId;
        // Mark conversation as read
        markConversationRead(agentId);
    }
    async function fetchAgentSkills(agentId) {
        try {
            const res = await fetch(`/api/skills/status?agentId=${encodeURIComponent(agentId)}`);
            if (!res.ok)
                throw new Error(await res.text());
            const data = await res.json();
            agentSkills.set(agentId, data);
        }
        catch (e) {
            console.error('Error fetching agent skills:', e);
        }
    }
    async function updateAgentSkills(agentId, payload) {
        try {
            const res = await fetch(`/api/agents/${agentId}/skills`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok)
                throw new Error(await res.text());
            await fetchAgentSkills(agentId);
            await fetchAgents();
        }
        catch (e) {
            console.error('Error updating agent skills:', e);
            throw e;
        }
    }
    async function installSkill(agentId, name, installId, timeout) {
        const params = new URLSearchParams({ agentId, name });
        if (installId)
            params.set('installId', installId);
        if (timeout)
            params.set('timeout', String(timeout));
        const res = await fetch(`/api/skills/install?${params.toString()}`, { method: 'POST' });
        if (!res.ok)
            throw new Error(await res.text());
        await fetchAgentSkills(agentId);
    }
    async function updateAgentSkillEntry(agentId, skillKey, patch) {
        const current = agentSkills.get(agentId);
        const entries = { ...(current?.entries ?? {}) };
        entries[skillKey] = { ...(entries[skillKey] ?? {}), ...patch };
        await updateAgentSkills(agentId, { entries });
    }
    // Create new agent
    async function createAgent(agentData) {
        loading.value = true;
        error.value = null;
        try {
            const response = await fetch('/api/agents', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(agentData)
            });
            if (!response.ok) {
                throw new Error(`Failed to create agent: ${response.statusText}`);
            }
            const newAgent = await response.json();
            agents.value.push(newAgent);
            // Initialize conversation
            conversations.set(newAgent.id, {
                agentId: newAgent.id,
                messages: [],
                unreadCount: 0,
                lastActivity: new Date().toISOString()
            });
            return newAgent;
        }
        catch (e) {
            error.value = e.message;
            console.error('Error creating agent:', e);
            throw e;
        }
        finally {
            loading.value = false;
        }
    }
    // Update agent
    async function updateAgent(agentId, updates) {
        loading.value = true;
        error.value = null;
        try {
            const response = await fetch(`/api/agents/${agentId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(updates)
            });
            if (!response.ok) {
                throw new Error(`Failed to update agent: ${response.statusText}`);
            }
            const updatedAgent = await response.json();
            const index = agents.value.findIndex(a => a.id === agentId);
            if (index !== -1) {
                agents.value[index] = updatedAgent;
            }
            return updatedAgent;
        }
        catch (e) {
            error.value = e.message;
            console.error('Error updating agent:', e);
            throw e;
        }
        finally {
            loading.value = false;
        }
    }
    // Delete agent
    async function deleteAgent(agentId) {
        loading.value = true;
        error.value = null;
        try {
            const response = await fetch(`/api/agents/${agentId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                throw new Error(`Failed to delete agent: ${response.statusText}`);
            }
            agents.value = agents.value.filter(a => a.id !== agentId);
            conversations.delete(agentId);
            // Switch to default agent if current was deleted
            if (currentAgentId.value === agentId) {
                currentAgentId.value = 'default';
            }
        }
        catch (e) {
            error.value = e.message;
            console.error('Error deleting agent:', e);
            throw e;
        }
        finally {
            loading.value = false;
        }
    }
    function isLocalUserDuplicate(messages, incoming) {
        if (!incoming || incoming.role !== 'user' || !incoming.content)
            return false;
        const incomingTime = Date.parse(incoming.timestamp || '') || Date.now();
        return messages.some((msg) => {
            if (!msg || msg.role !== 'user' || msg.content !== incoming.content || !msg.local)
                return false;
            const msgTime = Date.parse(msg.timestamp || '') || 0;
            return Math.abs(incomingTime - msgTime) <= 120000;
        });
    }
    // Add message to conversation
    function addMessage(agentId, message, options = {}) {
        let conv = conversations.get(agentId);
        if (!conv) {
            conv = {
                agentId,
                messages: [],
                unreadCount: 0,
                lastActivity: new Date().toISOString()
            };
            conversations.set(agentId, conv);
        }
        if (!message?.local && isLocalUserDuplicate(conv.messages, message)) {
            return;
        }
        const last = conv.messages.length ? conv.messages[conv.messages.length - 1] : null;
        if (last && last.role === message.role && last.content === message.content) {
            return;
        }
        conv.messages.push(message);
        conv.lastActivity = new Date().toISOString();
        const isCurrent = agentId === currentAgentId.value;
        const shouldIncrement = options.markUnread ? true : (!isCurrent && !options.suppressUnread);
        if (shouldIncrement) {
            conv.unreadCount++;
        }
    }
    function setMessages(agentId, messages) {
        let conv = conversations.get(agentId);
        if (!conv) {
            conv = {
                agentId,
                messages: [],
                unreadCount: 0,
                lastActivity: new Date().toISOString()
            };
            conversations.set(agentId, conv);
        }
        conv.messages = messages || [];
        conv.unreadCount = 0;
        conv.lastActivity = new Date().toISOString();
    }
    function prependMessages(agentId, messages) {
        if (!messages || messages.length === 0)
            return;
        let conv = conversations.get(agentId);
        if (!conv) {
            conv = {
                agentId,
                messages: [],
                unreadCount: 0,
                lastActivity: new Date().toISOString()
            };
            conversations.set(agentId, conv);
        }
        const existingIds = new Set(conv.messages.map((msg) => msg.id));
        const existingKeys = new Set(conv.messages.map((msg) => `${msg.role}|${msg.content}|${msg.timestamp}`));
        const toPrepend = messages.filter((msg) => {
            if (msg?.id && existingIds.has(msg.id))
                return false;
            if (isLocalUserDuplicate(conv.messages, msg))
                return false;
            const key = `${msg.role}|${msg.content}|${msg.timestamp}`;
            return !existingKeys.has(key);
        });
        conv.messages = [...toPrepend, ...conv.messages];
    }
    function appendMessages(agentId, messages) {
        if (!messages || messages.length === 0)
            return;
        let conv = conversations.get(agentId);
        if (!conv) {
            conv = {
                agentId,
                messages: [],
                unreadCount: 0,
                lastActivity: new Date().toISOString()
            };
            conversations.set(agentId, conv);
        }
        const existingIds = new Set(conv.messages.map((msg) => msg.id));
        const existingKeys = new Set(conv.messages.map((msg) => `${msg.role}|${msg.content}|${msg.timestamp}`));
        const toAppend = messages.filter((msg) => {
            if (msg?.id && existingIds.has(msg.id))
                return false;
            if (isLocalUserDuplicate(conv.messages, msg))
                return false;
            const key = `${msg.role}|${msg.content}|${msg.timestamp}`;
            return !existingKeys.has(key);
        });
        conv.messages = [...conv.messages, ...toAppend];
        conv.lastActivity = new Date().toISOString();
    }
    function markConversationRead(agentId) {
        const conv = conversations.get(agentId);
        if (conv) {
            conv.unreadCount = 0;
        }
    }
    // Get agent statistics
    async function fetchAgentStatistics(agentId) {
        try {
            const response = await fetch(`/api/agents/${agentId}/statistics`);
            if (!response.ok) {
                throw new Error(`Failed to fetch statistics: ${response.statusText}`);
            }
            const stats = await response.json();
            const agent = getAgent(agentId);
            if (agent) {
                agent.statistics = stats;
            }
            return stats;
        }
        catch (e) {
            console.error('Error fetching agent statistics:', e);
            throw e;
        }
    }
    return {
        // State
        agents,
        currentAgentId,
        currentAgent,
        conversations,
        currentConversation,
        totalUnreadCount,
        agentSkills,
        loading,
        error,
        // Actions
        fetchAgents,
        getAgent,
        switchToAgent,
        fetchAgentSkills,
        updateAgentSkills,
        updateAgentSkillEntry,
        installSkill,
        createAgent,
        updateAgent,
        deleteAgent,
        addMessage,
        setMessages,
        prependMessages,
        appendMessages,
        markConversationRead,
        fetchAgentStatistics
    };
}
