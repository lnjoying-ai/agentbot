# 🕵️‍♂️ Agentbot (Codename: Smith)

> "The iconic Agent from The Matrix, representing infinite self-replication and system-level infiltration."

**Agentbot** is a **full-stack AI Agent framework** developed based on Java (Spring Boot). It is designed to be a personal intelligent assistant with "physical execution capabilities," combining the reasoning power of Large Language Models (LLMs) with local system execution permissions to achieve a complete task loop from information retrieval to automated web operations.

---

## 🚀 Key Features

### 🧠 Intelligent Brain
- **Multi-Model Support**: Seamless integration with mainstream models like Kimi-k2.5, GPT-4o, Claude 3, GLM, etc.
- **Autonomous Planning**: Capable of multi-round reasoning, breaking down complex instructions into executable task flows.

### 🛠️ Execution Tools
- **Browser Control**: Powered by Playwright, supporting automated navigation, clicks, form filling, screenshots, and content extraction.
- **System Commands (Shell)**: Ability to execute local shell scripts, supporting any command-line tools (e.g., Git, Docker, Python).
- **File Operations**: Read, write, and manage files within a controlled workspace.
- **Multidimensional Search**: Integrated with Bocha/Brave Search for real-time information access.

### 📜 Experience & Skills
- **Markdown Driven**: Teach the Agent specific business logic and SOPs (Standard Operating Procedures) by writing `.md` files in `workspace/skills`, no code changes required.
- **Hot-Pluggable**: Skill files are applied instantly upon modification, with YAML Frontmatter for metadata definition.

### 🖼️ Modern Console (Frontend)
- **Multimedia Chat**: Full Markdown rendering support, automatically identifying and displaying images/screenshots from the workspace in real-time.
- **Configuration Center**: Graphical interface for managing settings, with auto-save to `agentbot.yml`.
- **Execution Monitoring**: Real-time monitoring of Agent status, tool execution time, and system resource usage.

---

## 📂 Project Structure

```text
agentbot/
├── JAVA/
│   ├── src/                 # Backend source code (Spring Boot)
│   │   ├── core/            # Core Engine (Agent Loop, Tools, Skills)
│   │   └── gateway/         # API Controllers
│   ├── frontend/            # Frontend source code (Vue.js 3 + Vite)
│   └── workspace/           # Agent's workspace (Data, Screenshots, Skills)
└── config/                  # Configuration files (agentbot.yml)
```

---

## 🛠️ Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- Maven 3.8+

### Setup Steps
1. **Install Browser Engine**:
   ```bash
   mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
   ```
2. **Start Backend**:
   ```bash
   mvn spring-boot:run
   ```
3. **Start Frontend**:
   ```bash
   cd frontend && npm install && npm run dev
   ```

---

## 🤝 Contribution & Extension

Agentbot is designed with a plugin architecture. You can extend its capabilities by:
- **Adding Tools**: Implement the `ToolWithDefinition` interface and register it in `ToolRegistry`.
- **Adding Skills**: Create new `.md` files under `workspace/skills`.

---

*Agentbot is more than a chatbot; it's your proxy in the digital world.*
