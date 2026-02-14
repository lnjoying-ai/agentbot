# 🕵️‍♂️ Agentbot (Codename: Smith)

> "The iconic Agent from The Matrix, representing infinite self-replication and system-level infiltration."

**Agentbot** 是一个基于 Java (Spring Boot) 开发的**全栈 AI Agent 框架**。它旨在作为一个具备“物理执行能力”的个人智能助理，通过将大语言模型（LLM）的推理能力与本地系统执行权限相结合，实现从信息检索到自动化网页操作的完整任务闭环。

---

## 🚀 核心功能

### 🧠 智能大脑 (Brain)
- **多模型支持**：无缝接入 Kimi-k2.5, GPT-4o, Claude 3, GLM 等主流大模型。
- **自主规划**：具备多轮思考能力，能将复杂指令拆解为可执行的任务流。

### 🛠️ 执行手脚 (Tools)
- **浏览器控制 (Browser Control)**：基于 Playwright，支持自动化导航、点击、表单填充、网页截图及内容提取。
- **系统命令 (Shell)**：具备本地执行 Shell 脚本的能力，支持任何命令行工具（如 Git, Docker, Python）。
- **文件操作**：在受控的工作区（Workspace）内进行文件的读写与管理。
- **多维搜索**：集成 Bocha/Brave 搜索，支持联网获取实时资讯。

### 📜 经验积累 (Skills)
- **Markdown 驱动**：通过在 `workspace/skills` 编写 `.md` 文件，即可教导 Agent 特定的业务逻辑和 SOP（标准作业程序），无需修改代码。
- **热插拔设计**：技能文件即改即用，支持 YAML Frontmatter 定义元数据。

### 🖼️ 现代控制台 (Frontend Console)
- **多媒体对话**：完美支持 Markdown 渲染，自动识别并实时展示工作区内的图片/截图。
- **配置中心**：全图形化界面管理配置项，支持自动保存至 `agentbot.yml`。
- **运行监控**：实时监控 Agent 状态、工具调用耗时及系统资源占用。

---

## 📂 项目结构

```text
agentbot/
├── JAVA/
│   ├── src/                 # 后端源码 (Spring Boot)
│   │   ├── core/            # 核心引擎 (Agent Loop, Tools, Skills)
│   │   └── gateway/         # API 控制器
│   ├── frontend/            # 前端源码 (Vue.js 3 + Vite)
│   └── workspace/           # Agent 的工作空间 (数据、截图、技能)
└── config/                  # 配置文件 (agentbot.yml)
```

---

## 🛠️ 快速开始

### 环境要求
- Java 17+
- Node.js 18+
- Maven 3.8+

### 启动步骤
1. **安装浏览器引擎**:
   ```bash
   mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
   ```
2. **启动后端**:
   ```bash
   mvn spring-boot:run
   ```
3. **启动前端**:
   ```bash
   cd frontend && npm install && npm run dev
   ```

---

## 🤝 贡献与扩展

Agentbot 采用插件化设计。你可以通过以下方式扩展它的能力：
- **新增工具**: 实现 `ToolWithDefinition` 接口并注册到 `ToolRegistry`。
- **新增技能**: 在 `workspace/skills` 下创建新的 `.md` 文件。

---

*Agentbot is more than a chatbot; it's your proxy in the digital world.*
