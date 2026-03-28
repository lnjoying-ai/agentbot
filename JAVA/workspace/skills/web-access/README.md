# web-access Skill (AgentBot 适配版)

为 AgentBot 适配的 web-access skill，融合原 Claude Code web-access skill 的核心理念与 AgentBot 现有工具集。

## 原版与适配版差异

| 特性 | 原版 (Claude Code) | 适配版 (AgentBot) |
|------|-------------------|------------------|
| CDP 实现 | CDP Proxy (Node.js + Chrome 远程调试) | `browser_control` 工具 (内置 Playwright) |
| WebSearch | 多种搜索引擎 | `web_search` 工具 (Bocha AI Search) |
| 配置要求 | 需 Chrome 远程调试配置 | 开箱即用，无需额外配置 |
| 登录态 | 直接使用用户 Chrome | 内置浏览器，需手动登录 |
| 子 Agent | 原生支持 | `spawn` 工具 |

## 核心理念保留

1. **浏览哲学**：像人一样思考，目标驱动而非步骤驱动
2. **分层工具策略**：WebSearch → 页面抓取 → CDP 浏览器自动化
3. **并行分治**：多目标时使用 `spawn` 分发给子 Agent
4. **站点经验**：按域名积累经验，跨 session 复用

## 安装

已自动安装到 `workspace/skills/web-access/`。

## 使用

直接在对话中使用，AgentBot 会自动加载 skill 指引：

```
帮我搜索 xxx 最新进展
读取这个页面：https://example.com
去小红书搜索 xxx 的账号
同时调研这 5 个产品的官网
```

## 工具快速参考

### web_search
```javascript
web_search query="关键词" freshness="oneWeek"
```

### browser_control
```javascript
// 启动/停止
browser_control action="start"
browser_control action="stop"

// 导航
browser_control action="navigate" url="https://..."

// 获取内容
browser_control action="content"
browser_control action="snapshot"

// 操作
browser_control action="click" selector="button"
browser_control action="type" selector="input" text="..."

// 截图
browser_control action="screenshot"
```

### spawn (并行)
```javascript
spawn task="加载 web-access skill，调研 https://site.com" label="research-1"
```

## 环境检查

```bash
python workspace/skills/web-access/scripts/check-env.py
```

## 项目结构

```
web-access/
├── SKILL.md                    # 核心技能定义
├── README.md                   # 本文件
├── scripts/
│   └── check-env.py           # 环境检查脚本
└── references/
    ├── cdp-api.md             # CDP API 参考
    └── site-patterns/         # 站点经验目录
        └── .gitkeep
```

## 致谢

原 skill 作者：一泽 Eze (https://github.com/eze-is)

原项目：https://github.com/eze-is/web-access

## License

MIT (与原项目一致)
