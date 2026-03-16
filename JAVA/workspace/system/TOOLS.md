## Tool Reference Guide

本文档根据当前代码实现汇总 agentbot 可用工具的定义、参数与使用说明。

## 工具总览

- **基础**: `echo`, `time_now`
- **文件系统**: `read_file`, `write_file`, `list_dir`
- **内存**: `memory_get`, `memory_search`
- **执行**: `shell`
- **子代理**: `spawn`
- **通信**: `message`, `p2p_message`
- **Web 搜索**: `web_search` (由配置选择 Brave / Bocha / Apimesh)
- **浏览器控制**: `browser_control`

---

## 基础工具

### echo
回显输入文本。

**参数**
- `text` (string, required)

**返回**
- 原样字符串

**示例**
```json
{
  "name": "echo",
  "arguments": {
    "text": "hello"
  }
}
```

---

### time_now
返回当前 UTC 时间 (ISO-8601)。

**参数**
- 无

**返回**
- UTC 时间字符串

**示例**
```json
{
  "name": "time_now",
  "arguments": {}
}
```

---

## 文件系统工具

### read_file
读取文件内容。

**参数**
- `path` (string, required): 绝对路径或相对路径

**路径解析说明**
- 绝对路径：直接读取
- 相对路径：优先在工作区根目录以及 `tmp`、`skills`、`system/skills`、`agents` 下解析
- 以 `workspace/` 开头：强制在工作区根目录下解析

**返回**
- 文件内容文本

**示例**
```json
{
  "name": "read_file",
  "arguments": {
    "path": "workspace/system/TOOLS.md"
  }
}
```

---

### write_file
写入/覆盖文件内容。

**参数**
- `path` (string, required): 文件路径
- `content` (string, required): 写入内容

**路径解析说明**
- 绝对路径：直接写入
- 相对路径：默认写入到 `workspace/tmp` 下
- 以 `workspace/` 开头：写入工作区根目录下对应位置

**返回**
- 写入成功提示

**示例**
```json
{
  "name": "write_file",
  "arguments": {
    "path": "workspace/tmp/demo.txt",
    "content": "hello"
  }
}
```

---

### list_dir
列出目录内容（按名称排序）。

**参数**
- `path` (string, required): 目录路径

**返回**
- 制表符分隔的表格：`name type size modified`

**示例**
```json
{
  "name": "list_dir",
  "arguments": {
    "path": "workspace/system"
  }
}
```

---

## 内存工具

### memory_get
读取长期记忆内容。

**参数**
- 无

**返回**
- 记忆内容文本（或 `memory is empty`）

---

### memory_search
搜索长期记忆与日志。

**参数**
- `query` (string, required)
- `limit` (integer, optional): 返回条数，默认 5

**返回**
- 匹配条目（或 `no memory matched`）

---

## 执行工具

### shell
在宿主机执行命令（谨慎使用）。

**参数**
- `command` (string, required)

**执行细节**
- Windows 优先 PowerShell；若无脚本上下文则用 `cmd`，并强制 UTF-8 输出
- macOS 使用 `zsh`，Linux 使用 `sh`
- 超时时间：60 秒

**审批策略**
`shell` 会根据命令内容判断是否需要审批：
- 含管道/重定向/命令拼接符（如 `|`, `&`, `;`, `>`, `<`, `` ` ``, `$(`）
- 或出现高风险关键字（删除、权限、系统管理、网络、包管理、脚本执行等）

---

## 子代理

### spawn
创建一个子代理任务（异步处理复杂工作）。

**参数**
- `task` (string, required): 子任务描述
- `label` (string, optional): 任务标签

**返回**
- 子代理 ID

---

## 通信工具

### message
向指定通道发送消息。

**参数**
- `content` (string, required)
- `channel` (string, optional)
- `chatId` (string, optional)

**说明**
- 如果未提供 `channel` / `chatId`，将尝试使用上下文默认值

---

### p2p_message
向外部节点的 agent 发送 P2P 消息。

**参数**
- `content` (string, required)
- `toNodeId` (string, required)
- `toAgentId` (string, required)
- `fromAgentId` (string, required)

---

## Web 搜索

### web_search
执行 Web 搜索，具体实现由配置决定。

**配置**
`agentbot.search.type` 决定搜索提供方：
- `brave` → Brave Search
- `bocha` → Bocha Search
- `apimesh` → Apimesh Search

**参数（按提供方）**
- **Brave**
  - `query` (string, required)
  - `count` (integer, optional, 1-10)
- **Bocha / Apimesh**
  - `query` (string, required)
  - `freshness` (string, optional): `noLimit`, `oneDay`, `oneWeek`, `oneMonth`, `oneYear`

**返回**
- 结果标题、URL 与摘要

---

## 浏览器控制

### browser_control
通过本地浏览器控制服务执行浏览器操作。

**参数**
- `action` (string, required):
  - `status`, `start`, `stop`, `profiles`, `tabs`, `open`, `focus`, `close`
  - `snapshot`, `act`, `navigate`, `goto`, `click`, `type`, `screenshot`, `content`, `upload`
- `profile` (string, optional): 浏览器配置文件名
- `target` (string, optional): `host` | `sandbox` | `node`
- `targetId` (string, optional): 目标标签页 ID
- `snapshotFormat` (string, optional): `ai` / `aria` / `role`
- `url` / `targetUrl` (string, optional): 导航 URL
- `selector` (string, optional): CSS 选择器
- `text` (string, optional): 输入文本
- `filePath` (string, optional): 上传文件路径
- `ref` (string, optional): `snapshot` 的引用 ID
- `kind` (string, optional): `act` 类型 (click/type/hover/press)
- `key` (string, optional): `press` 的键值

**审批策略**
- `target` 为 `sandbox` 或 `node` 时需要审批
- `action` 为 `upload` / `screenshot` / `content` 时需要审批

---

## 审批策略配置

审批行为由 `config/agentbot.yml` 的 `approvals.tools` 控制：
- `security`: `deny` | `allowlist` | `full`
- `ask`: `off` | `on-miss` | `always`
- `askFallback`: `deny` | `allow`
- `uiChannels`: 允许弹出审批 UI 的通道
- `allowlist`: 指定工具 + 参数匹配的自动放行规则

**示例**
```yaml
approvals:
  tools:
    security: "allowlist"
    ask: "on-miss"
    askFallback: "deny"
    uiChannels:
      - "web"
    allowlist:
      - tool: "shell"
        match:
          command: '/^(ls|pwd|dir)(\s|$).*/'
```

---

## 扩展工具

新增工具时需要：
1. 实现 `Tool` 或 `ToolWithDefinition`
2. 在 `ToolRegistry` 注册
3. 必要时实现 `requiresApproval()`
4. 更新本文档

---

**参考**
- `config/agentbot.yml`
- `src/main/java/com/agentbot/core/tools/`
- `src/main/java/com/agentbot/config/AgentConfiguration.java`
