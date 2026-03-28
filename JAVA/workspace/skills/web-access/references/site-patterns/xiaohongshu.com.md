---
domain: xiaohongshu.com
aliases: [小红书, xhs, 红薯]
updated: 2026-03-19
---

## 平台特征

- **架构**: 现代 React SPA，内容动态渲染
- **反爬级别**: 高
- **登录需求**: 大部分内容需登录查看
- **内容加载方式**: 动态加载，无限滚动

## 有效模式

### URL 模式

- 用户主页: `https://www.xiaohongshu.com/user/profile/{user_id}`
- 笔记详情: `https://www.xiaohongshu.com/explore/{note_id}`
- 搜索结果: `https://www.xiaohongshu.com/search_result?keyword={keyword}`

**注意**: URL 中的 `xsec_token` 参数对访问部分内容是必需的，站点生成的链接已包含，手动构造可能失败。

### 操作策略

1. **首页访问**
   - 使用 `browser_control navigate` 访问
   - 等待 JS 渲染完成（2-3秒）
   - 检查是否需要登录（内容为空或出现登录提示）

2. **搜索流程**
   - 导航到首页
   - 使用 `act type` 在搜索框输入关键词
   - 使用 `act kind="press" key="Enter"` 提交
   - 等待结果加载
   - 使用 `content` 或 `snapshot` 提取结果

3. **内容提取**
   - 笔记标题: `div.title` 或 `h1`
   - 笔记内容: `div.desc` 或 `div.content`
   - 图片: `img` 标签，需提取 `src` 属性
   - 视频: `video` 标签，可使用 `screenshot` 采帧

### 选择器参考

```javascript
// 搜索框
'input[placeholder*="搜索"]'

// 笔记卡片
'div.note-item'

// 笔记标题
'div.title'

// 笔记内容
'div.desc'

// 下一页/加载更多
'button.load-more'
```

## 已知陷阱

### 2026-03-19: xsec_token 机制

部分笔记访问需要 `xsec_token` 参数，这是服务端生成的防爬 token。手动构造 URL 访问会失败。

**解决方案**: 从搜索/列表页获取完整链接，不要手动拼接 URL。

### 2026-03-19: 创作者平台状态校验

访问创作者主页时，可能会遇到状态校验（如是否登录、账号状态）。

**解决方案**: 确保在内置浏览器中完成登录流程。

### 2026-03-19: 暂存草稿流程

发布内容时，平台有自动暂存草稿机制，未完成的内容可能被保存为草稿。

**注意**: 自动化发布功能需谨慎使用，遵循平台规则。

### 2026-03-19: 反爬行为

频繁操作会触发验证码或 IP 限制。

**缓解策略**:
- 控制操作频率
- 使用 GUI 交互模式（`act` 模拟真实用户行为）
- 遇到验证码时提示用户手动处理

## 登录流程

当访问受限内容时：

1. 提示用户: "需要登录小红书才能查看该内容"
2. 用户使用内置浏览器完成登录
3. 登录后继续操作

**注意**: AgentBot 内置浏览器不共享用户 Chrome 的登录态，需单独登录。
