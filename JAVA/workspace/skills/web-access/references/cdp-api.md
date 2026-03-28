# CDP API 参考 (AgentBot 适配版)

本文件详细说明 AgentBot `browser_control` 工具的 CDP 能力，对应原 web-access skill 的 CDP Proxy API。

## 工具映射表

| CDP Proxy (原) | AgentBot browser_control |
|---------------|-------------------------|
| `GET /health` | `browser_control action="status"` |
| `GET /targets` | `browser_control action="tabs"` |
| `GET /new?url=` | `browser_control action="navigate" url="..."` |
| `GET /close?target=` | `browser_control action="close" targetId="..."` |
| `GET /navigate?target=&url=` | `browser_control action="navigate" url="..."` |
| `GET /back?target=` | `browser_control action="back"` |
| `GET /info?target=` | `browser_control action="snapshot"` |
| `POST /eval?target=` | `browser_control action="act" kind="type"` + JS |
| `POST /click?target=` | `browser_control action="act" kind="click"` 或 `action="click"` |
| `POST /clickAt?target=` | `browser_control action="act" kind="click"` |
| `GET /scroll?target=` | `browser_control action="act" kind="type"` + JS scroll |
| `GET /screenshot?target=` | `browser_control action="screenshot"` |

## 常用操作模式

### 1. 启动浏览器并导航

```javascript
// 检查并启动浏览器
browser_control action="status"
// 如果未启动：
browser_control action="start"

// 导航到页面
browser_control action="navigate" url="https://example.com"
```

### 2. 获取页面信息

```javascript
// 获取文本内容
browser_control action="content"

// 获取结构化快照（适合 AI 理解页面结构）
browser_control action="snapshot" snapshotFormat="ai"
```

### 3. 元素点击

```javascript
// 方式1: 使用 act
browser_control action="act" kind="click" selector="button.submit"

// 方式2: 使用 click shortcut
browser_control action="click" selector="button.submit"
```

### 4. 文本输入

```javascript
browser_control action="act" kind="type" selector="input#search" text="搜索关键词"
// 或
browser_control action="type" selector="input#search" text="搜索关键词"
```

### 5. 执行 JavaScript

```javascript
// 通过 type 执行 JS（在输入框中执行复杂操作）
browser_control action="act" kind="type" selector="input" text="document.title"

// 或者使用 evaluate 类操作
// 注意: AgentBot 的 browser_control 可能不直接支持 evaluate
// 替代方案: 先用 content 获取页面，再用 shell + curl 配合
```

### 6. 滚动页面

```javascript
// 通过 JS 执行滚动
browser_control action="act" kind="type" selector="body" text="window.scrollTo(0, document.body.scrollHeight)"
```

### 7. 截图

```javascript
browser_control action="screenshot"
```

### 8. 多标签页管理

```javascript
// 列出所有标签页
browser_control action="tabs"

// 切换到指定标签页
browser_control action="focus" targetId="tab-id"

// 关闭标签页
browser_control action="close" targetId="tab-id"
```

### 9. 文件上传

```javascript
browser_control action="upload" selector="input[type=file]" filePath="/path/to/file.png"
```

## 与原 CDP Proxy 的差异

1. **连接方式**：原 CDP Proxy 需要用户 Chrome 开启远程调试端口；AgentBot 使用内置 Playwright，无需额外配置

2. **API 风格**：原 CDP Proxy 提供 HTTP REST API；AgentBot 使用结构化 tool calling

3. **Session 管理**：原 CDP Proxy 使用 targetId；AgentBot 使用内置 tab 管理

4. **执行 JS**：原 CDP Proxy 有专门的 `/eval` 端点；AgentBot 需要通过页面交互间接执行

## 高级模式

### 从页面提取数据

```javascript
// 步骤1: 导航到页面
browser_control action="navigate" url="https://example.com/list"

// 步骤2: 获取页面内容
browser_control action="content"

// 步骤3: 分析内容，找到目标元素

// 步骤4: 点击或提取
browser_control action="click" selector="a.item-link"

// 步骤5: 获取详情页内容
browser_control action="content"
```

### 处理动态加载

```javascript
// 滚动触发懒加载
browser_control action="act" kind="type" selector="body" text="window.scrollTo(0, document.body.scrollHeight)"

// 等待加载
// 注意: AgentBot 可能需要手动等待，工具链暂不支持自动等待

// 再次获取内容
browser_control action="content"
```

### 视频帧捕获

```javascript
// 导航到视频页面
browser_control action="navigate" url="https://example.com/video"

// 等待视频加载
// 通过 content 检查 video 元素

// 跳转到指定时间点（通过 JS）
browser_control action="act" kind="type" selector="video" text="document.querySelector('video').currentTime = 30"

// 截图捕获当前帧
browser_control action="screenshot"
```

## 故障排除

| 问题 | 可能原因 | 解决方案 |
|------|---------|---------|
| 页面内容为空 | JS 渲染未完成 | 添加等待时间后重试 |
| 元素点击失败 | 选择器不正确 | 使用 snapshot 查看实际结构 |
| 登录态丢失 | 浏览器重启 | 重新登录 |
| 反爬拦截 | 请求频率过高 | 降低频率，使用 GUI 交互模式 |

## 最佳实践

1. **先快照后操作**：使用 `snapshot` 了解页面结构，再进行点击/输入
2. **验证结果**：每次操作后使用 `content` 或 `snapshot` 验证效果
3. **及时关闭**：任务完成后使用 `stop` 关闭浏览器，释放资源
4. **错误处理**：操作失败后尝试备用策略（如 GUI 交互代替程序化）
