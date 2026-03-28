---
name: web-access
description: |
  所有联网操作必须通过此 skill 处理，包括：搜索、网页抓取、登录后操作、浏览器自动化等。
  触发场景：用户要求搜索信息、查看网页内容、访问需要登录的网站、操作网页界面、
  抓取社交媒体内容（小红书、微博、推特等）、读取动态渲染页面、以及任何需要真实浏览器环境的网络任务。
  本 skill 为 AgentBot 适配版，融合原 Claude Code web-access skill 的核心理念与 AgentBot 现有工具集。
metadata:
  author: 一泽Eze
  version: "2.4.0-agentbot"
---

# web-access Skill (AgentBot 适配版)

## 核心架构差异

| 原 Claude Code | AgentBot 对应 |
|---------------|---------------|
| CDP Proxy (Node.js) | `browser_control` 工具（内置 Playwright CDP） |
| WebSearch | `web_search` 工具（Bocha AI Search API） |
| WebFetch/curl | `browser_control` + `content` / `shell` |
| 子 Agent | `spawn` 工具 |

## 前置检查

执行联网操作前，先检查浏览器工具可用性：

```bash
# 检查 browser_control 状态
# 通过调用 browser_control action="status" 验证
```

若浏览器未启动，使用 `browser_control action="start"` 启动。

## 浏览哲学（核心指导思想）

**像人一样思考，兼顾高效与适应性地完成任务。**

执行任务时不预设固定步骤，而是带着目标进入，边看边判断，遇到阻碍就解决，发现内容不够就深入——全程围绕「我要达成什么」做决策。

### 执行流程

**① 拿到请求** — 明确用户要做什么，定义成功标准

**② 选择起点** — 根据任务性质选择最可能直达的方式：
- 需要操作页面、登录态、动态渲染平台（小红书等）→ `browser_control` CDP 模式
- 信息发现、快速搜索 → `web_search`
- 已知 URL 提取内容 → `browser_control` + `content`

**③ 过程校验** — 用结果对照成功标准，发现方向错了立即调整，不在同一方式上反复重试

**④ 完成判断** — 对照成功标准确认完成，不过度操作

## 联网工具选择策略

| 场景 | 推荐工具 | 调用方式 |
|------|---------|---------|
| 搜索摘要、关键词结果 | **web_search** | `web_search query="关键词"` |
| 已知 URL，提取页面内容 | **browser_control content** | `browser_control action="content" url="..."` |
| 需登录态、操作页面、动态渲染 | **browser_control CDP** | `browser_control action="navigate/act/screenshot"` |
| 获取原始 HTML/meta | **shell curl** | `shell command="curl -s ..."` |

**Jina 预处理**（可选）：可通过 `https://r.jina.ai/http://example.com` 获取 Markdown 化内容，节省 token。限 20 RPM，适合文章类页面。

## 浏览器自动化（CDP 模式）

AgentBot 的 `browser_control` 工具已内置 CDP 能力：

### 基础操作

```javascript
// 启动浏览器
browser_control action="start"

// 导航到页面
browser_control action="navigate" url="https://example.com"

// 获取页面内容
browser_control action="content"

// 执行操作（点击、输入等）
browser_control action="act" kind="click" selector="button.submit"
browser_control action="act" kind="type" selector="input#search" text="关键词"

// 截图
browser_control action="screenshot"

// 查看状态
browser_control action="status"

// 停止浏览器
browser_control action="stop"
```

### 页面内导航策略

- **单页连续操作**：使用 `act` 在当前 tab 内点击、翻页
- **多页并行**：使用 `navigate` 打开新 URL，配合 `spawn` 分治
- **元素探测优先**：先用 `content` 或 `snapshot` 了解页面结构，再执行操作

### 媒体资源提取

用 `content` 或 `act` + JavaScript 从 DOM 提取图片/视频 URL，定向读取。

### 视频内容获取

通过 `act` 执行 JS 操控 `<video>` 元素（seek、play/pause），配合 `screenshot` 采帧分析。

### 登录判断

核心问题：**目标内容拿到了吗？**

只有当确认目标内容无法获取且判断登录能解决时，才提示用户登录。

## 并行调研：子 Agent 分治策略

多独立目标时，使用 `spawn` 分治：

```javascript
// 主 Agent 分发任务
spawn task="加载 web-access skill，调研 https://site1.com 的 XXX 信息，返回结构化摘要"
spawn task="加载 web-access skill，调研 https://site2.com 的 XXX 信息，返回结构化摘要"
```

**子 Agent Prompt 原则**：
- 必须写 `加载 web-access skill 并遵循其指引`
- 描述目标（「获取」「调研」），避免暗示手段（「搜索」「爬取」）
- 说清楚要什么，仅在必要时限定怎么做

## 信息核实类任务

核实目标是**一手来源**，而非二手报道。

| 信息类型 | 一手来源 |
|----------|----------|
| 政策/法规 | 发布机构官网 |
| 企业公告 | 公司官方新闻页 |
| 工具能力/用法 | 官方文档、源码 |

搜索引擎仅用于**定位**信息，找到来源后直接访问原文。

## 站点经验

操作中积累的特定网站经验，按域名存储在 `references/site-patterns/` 下。

目标网站匹配已有经验时，必须读取对应文件获取先验知识。

CDP 操作成功后，如发现有必要记录的新模式，主动写入站点经验文件。

文件格式：
```markdown
---
domain: example.com
aliases: [示例]
updated: 2026-03-19
---
## 平台特征
架构、反爬行为、登录需求等

## 有效模式
已验证的 URL 模式、操作策略

## 已知陷阱
什么会失败以及为什么
```

## References 索引

| 文件 | 何时加载 |
|------|---------|
| `references/cdp-api.md` | 需要 CDP API 详细参考时 |
| `references/site-patterns/{domain}.md` | 确定目标网站后读取对应经验 |

## AgentBot 工具快速参考

### web_search
```javascript
web_search query="搜索关键词" freshness="noLimit"  // freshness: noLimit/oneDay/oneWeek/oneMonth/oneYear
```

### browser_control
```javascript
// 状态/生命周期
browser_control action="status|start|stop"

// 导航
browser_control action="navigate" url="https://..."
browser_control action="goto" url="https://..."
browser_control action="back"

// 页面操作
browser_control action="act" kind="click|type|hover|press" selector="CSS选择器" [text="输入文本"]
browser_control action="click" selector="..."
browser_control action="type" selector="..." text="..."

// 内容获取
browser_control action="content" [url="..."]         // 页面文本内容
browser_control action="snapshot" [snapshotFormat="ai|aria|role"]  // 结构化快照
browser_control action="screenshot"                   // 截图

// 标签页管理
browser_control action="tabs"
browser_control action="focus" targetId="..."
browser_control action="close" targetId="..."

// 文件上传
browser_control action="upload" selector="..." filePath="/path/to/file"
```

### shell
```javascript
shell command="curl -s https://example.com | head -100"
```

### spawn
```javascript
spawn task="加载 web-access skill，执行具体调研任务" label="research-task"
```
