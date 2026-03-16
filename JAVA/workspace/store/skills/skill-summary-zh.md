# AgentBot Store 技能目录总览

> 本文档汇总了 `workspace/store/skills` 目录下的所有技能，按功能分类整理。
> 
> **统计时间**: 2026年3月  
> **技能总数**: 约 1100+ 个

---

## 📊 目录结构

```
workspace/store/skills/
├── 核心系统技能 (Core System Skills)
├── 第三方服务自动化 (Third-party Automation)
├── 文档处理技能 (Document Processing)
├── 浏览器自动化 (Browser Automation)
├── 桌面自动化 (Desktop Automation)
├── 内容创作技能 (Content Creation)
├── 营销与销售 (Marketing & Sales)
├── 开发与运维 (DevOps & Development)
├── 项目管理 (Project Management)
├── 通讯协作 (Communication & Collaboration)
└── 其他工具技能 (Other Utilities)
```

---

## 1️⃣ 核心系统技能 (Core System Skills)

这些技能是 AgentBot 的核心功能组件，用于系统管理和技能管理。

| 技能名称 | 描述 | 触发条件 |
|---------|------|---------|
| `skill-creator` | 技能创建指南，指导如何创建、打包和发布新技能 | 用户想要创建或更新技能时 |
| `clawdhub` | ClawdHub CLI 工具，用于搜索、安装、更新和发布技能 | 需要从 clawdhub.com 管理技能时 |
| `find-skills` | 查找可用技能 | 需要搜索技能时 |
| `list-skills` | 列出已安装技能 | 查看当前技能列表 |
| `load-skill` | 加载指定技能 | 需要加载特定技能 |
| `install-skill` | 安装新技能 | 需要安装技能时 |
| `reload-skill` | 重新加载技能 | 技能更新后需要刷新 |
| `get-skill-info` | 获取技能信息 | 查看技能详情 |
| `get-skill-reference` | 获取技能参考文档 | 需要查看技能参考资料 |

**内存与状态管理技能:**
- `add-memory` - 添加长期记忆
- `search-memory` - 搜索记忆
- `get-memory-stats` - 获取内存统计
- `get-session-logs` - 获取会话日志
- `get-chat-history` - 获取聊天历史
- `get-user-profile` - 获取用户资料
- `update-user-profile` - 更新用户资料

**计划任务管理:**
- `create-plan` - 创建执行计划
- `complete-plan` - 完成计划步骤
- `update-plan-step` - 更新计划步骤
- `get-plan-status` - 获取计划状态
- `schedule-task` - 调度任务
- `list-scheduled-tasks` - 列出已调度任务
- `cancel-scheduled-task` - 取消调度任务
- `trigger-scheduled-task` - 触发调度任务
- `update-scheduled-task` - 更新调度任务
- `set-task-timeout` - 设置任务超时

**MCP (Model Context Protocol) 相关:**
- `call-mcp-tool` - 调用 MCP 工具
- `list-mcp-servers` - 列出 MCP 服务器
- `get-mcp-instructions` - 获取 MCP 指令

**系统功能:**
- `run-shell` - 运行 shell 命令
- `read-file` - 读取文件
- `write-file` - 写入文件
- `list-directory` - 列出目录
- `deliver-artifacts` - 交付产物
- `artifacts-builder` - 产物构建器
- `generate-image` - 生成图像
- `get-image-file` - 获取图像文件
- `get-voice-file` - 获取语音文件
- `send-sticker` - 发送贴纸
- `enable-thinking` - 启用思考模式
- `switch-persona` - 切换角色
- `toggle-proactive` - 切换主动模式
- `skip-profile-question` - 跳过个人资料问题

---

## 2️⃣ 文档处理技能 (Document Processing)

用于处理各类办公文档的技能集合。

### 办公文档三件套

| 技能名称 | 描述 | 文件格式 |
|---------|------|---------|
| `docx` | Word 文档创建、编辑和分析，支持追踪修订、批注、格式保留 | .docx |
| `pptx` | 演示文稿创建、编辑和分析，支持幻灯片布局和演讲者备注 | .pptx |
| `xlsx` | 电子表格创建、编辑和数据分析，支持公式和可视化 | .xlsx, .xlsm, .csv |
| `pdf` | PDF 处理工具包，支持文本/表格提取、创建、合并/拆分、表单填写 | .pdf |

### 文档处理详细功能

**DOCX 技能:**
- 创建新 Word 文档 (使用 docx-js)
- 编辑现有文档 (使用 Document 库)
- 追踪修订工作流 (Redlining)
- 文本提取和转换 (使用 pandoc)
- 批注和评论处理

**PPTX 技能:**
- 创建新演示文稿 (使用 html2pptx)
- 使用模板创建演示文稿
- 编辑现有演示文稿
- 提取演讲者备注和评论
- 幻灯片缩略图生成

**XLSX 技能:**
- 使用 pandas 进行数据分析
- 使用 openpyxl 创建/编辑带公式的电子表格
- 公式重新计算 (recalc.py)
- 金融模型标准支持

**PDF 技能:**
- 使用 pypdf 进行基本操作 (合并、拆分、旋转)
- 使用 pdfplumber 提取文本和表格
- 使用 reportlab 创建 PDF
- OCR 扫描文档处理

---

## 3️⃣ 浏览器自动化 (Browser Automation)

用于控制浏览器和执行网页操作的技能。

| 技能名称 | 描述 |
|---------|------|
| `browser-open` | 打开浏览器 |
| `browser-new-tab` | 新建标签页 |
| `browser-list-tabs` | 列出所有标签页 |
| `browser-switch-tab` | 切换标签页 |
| `browser-close` | 关闭浏览器/标签页 |
| `browser-navigate` | 页面导航 |
| `browser-get-content` | 获取页面内容 |
| `browser-click` | 点击元素 |
| `browser-type` | 输入文本 |
| `browser-screenshot` | 页面截图 |
| `browser-status` | 获取浏览器状态 |
| `browser-task` | 执行浏览器任务 |

**第三方浏览器自动化服务:**
- `browser-tool-automation` - 通用浏览器工具自动化
- `browserbase-tool-automation` - Browserbase 服务
- `browserhub-automation` - BrowserHub 服务
- `browserless-automation` - Browserless 服务
- `anchor-browser-automation` - Anchor Browser
- `hyperbrowser-automation` - Hyperbrowser
- `scrapingbee-automation` - ScrapingBee
- `scrapingant-automation` - ScrapingAnt
- `scrapfly-automation` - Scrapfly
- `scrape-do-automation` - Scrape.do
- `brightdata-automation` - Bright Data

---

## 4️⃣ 桌面自动化 (Desktop Automation)

用于控制本地桌面环境的技能。

| 技能名称 | 描述 |
|---------|------|
| `desktop-click` | 桌面点击操作 |
| `desktop-type` | 桌面文本输入 |
| `desktop-hotkey` | 热键操作 |
| `desktop-screenshot` | 桌面截图 |
| `desktop-find-element` | 查找桌面元素 |
| `desktop-scroll` | 滚动操作 |
| `desktop-wait` | 等待操作 |
| `desktop-window` | 窗口管理 |
| `desktop-inspect` | 桌面检查 |

---

## 5️⃣ 第三方服务自动化 (Third-party Automation)

### 5.1 项目管理与协作

| 技能名称 | 服务 | 功能 |
|---------|------|------|
| `github` | GitHub | 使用 gh CLI 进行 Issue、PR、CI 管理 |
| `github-automation` | GitHub API | GitHub API 自动化 |
| `gitlab-automation` | GitLab | GitLab 自动化 |
| `bitbucket-automation` | Bitbucket | Bitbucket 自动化 |
| `linear-automation` | Linear | Linear 项目管理 (通过 Rube MCP) |
| `jira-automation` | Jira | Jira 问题跟踪 |
| `asana-automation` | Asana | Asana 项目管理 |
| `monday-automation` | Monday.com | Monday 项目管理 |
| `clickup-automation` | ClickUp | ClickUp 项目管理 |
| `notion` / `notion-automation` | Notion | Notion 页面和数据库管理 |
| `trello` / `trello-automation` | Trello | Trello 看板管理 |
| `confluence-automation` | Confluence | Confluence 文档管理 |
| `basecamp-automation` | Basecamp | Basecamp 项目管理 |

### 5.2 通讯与消息

| 技能名称 | 服务 | 功能 |
|---------|------|------|
| `slack` / `slack-automation` | Slack | Slack 消息、反应、置顶管理 |
| `discord` / `discord-automation` | Discord | Discord 自动化 |
| `telegram-automation` | Telegram | Telegram 自动化 |
| `whatsapp-automation` | WhatsApp | WhatsApp 自动化 |
| `twitter-automation` | Twitter/X | Twitter 自动化 |
| `linkedin-automation` | LinkedIn | LinkedIn 自动化 |
| `instagram-automation` | Instagram | Instagram 自动化 |
| `facebook-automation` | Facebook | Facebook 自动化 |
| `intercom-automation` | Intercom | Intercom 客服 |
| `zendesk-automation` | Zendesk | Zendesk 客服 |
| `freshdesk-automation` | Freshdesk | Freshdesk 客服 |

### 5.3 邮件与营销

| 技能名称 | 服务 | 功能 |
|---------|------|------|
| `gmail-automation` | Gmail | Gmail 邮件管理 |
| `outlook-automation` | Outlook | Outlook 邮件管理 |
| `mailchimp-automation` | Mailchimp | 邮件营销 |
| `sendgrid-automation` | SendGrid | 邮件发送 |
| `resend-automation` | Resend | 邮件发送 |
| `klaviyo-automation` | Klaviyo | 邮件营销自动化 |
| `brevo-automation` | Brevo | 邮件营销 |
| `convertkit-automation` | ConvertKit | 邮件营销 |
| `mailerlite-automation` | MailerLite | 邮件营销 |
| `activecampaign-automation` | ActiveCampaign | 营销自动化 |
| `hubspot-automation` | HubSpot | CRM 和营销 |
| `salesforce-automation` | Salesforce | CRM 管理 |
| `pipedrive-automation` | Pipedrive | 销售管道 |

### 5.4 日历与日程

| 技能名称 | 服务 | 功能 |
|---------|------|------|
| `google-calendar-automation` | Google Calendar | 日历管理 |
| `googlecalendar-automation` | Google Calendar | 日历管理 (API) |
| `outlook-calendar-automation` | Outlook Calendar | 日历管理 |
| `calendly-automation` | Calendly | 预约调度 |
| `cal-com-automation` | Cal.com | 预约调度 |
| `cal-automation` | Cal | 预约调度 |
| `apple-reminders` | Apple Reminders | 提醒事项 |
| `todoist-automation` | Todoist | 任务管理 |
| `ticktick-automation` | TickTick | 任务管理 |
| `things-mac` | Things | 任务管理 (Mac) |

### 5.5 云存储与文件

| 技能名称 | 服务 | 功能 |
|---------|------|------|
| `google-drive-automation` | Google Drive | 云存储管理 |
| `googledrive-automation` | Google Drive | 云存储 (API) |
| `dropbox-automation` | Dropbox | 云存储管理 |
| `one-drive-automation` | OneDrive | 云存储管理 |
| `share-point-automation` | SharePoint | 企业文档管理 |
| `box-automation` | Box | 企业云存储 |

### 5.6 开发与运维工具

| 技能名称 | 服务 | 功能 |
|---------|------|------|
| `docker_hub-automation` | Docker Hub | Docker 镜像管理 |
| `docker-hub-automation` | Docker Hub | Docker 自动化 |
| `vercel-automation` | Vercel | 部署管理 |
| `aws-automation` | AWS | AWS 服务管理 |
| `digital-ocean-automation` | DigitalOcean | 云服务器管理 |
| `datadog-automation` | Datadog | 监控和日志 |
| `new-relic-automation` | New Relic | 性能监控 |
| `sentry-automation` | Sentry | 错误追踪 |
| `circleci-automation` | CircleCI | CI/CD |
| `gitlab-automation` | GitLab CI | CI/CD |
| `github-automation` | GitHub Actions | CI/CD |
| `buildkite-automation` | Buildkite | CI/CD |
| `supabase-automation` | Supabase | 数据库和认证 |
| `neon-automation` | Neon | 数据库 |
| `snowflake-automation` | Snowflake | 数据仓库 |
| `stripe-automation` | Stripe | 支付处理 |

### 5.7 Google 服务套件

| 技能名称 | 服务 |
|---------|------|
| `google-analytics-automation` | Google Analytics |
| `google-ads-automation` | Google Ads |
| `google-docs-automation` | Google Docs |
| `google-sheets-automation` | Google Sheets |
| `google-slides-automation` | Google Slides |
| `google-meet-automation` | Google Meet |
| `google-tasks-automation` | Google Tasks |
| `google-photos-automation` | Google Photos |
| `google-maps-automation` | Google Maps |
| `google-classroom-automation` | Google Classroom |
| `google-admin-automation` | Google Admin |
| `google-search-console-automation` | Search Console |

### 5.8 Microsoft 服务套件

| 技能名称 | 服务 |
|---------|------|
| `microsoft-teams-automation` | Microsoft Teams |
| `microsoft-tenant-automation` | Microsoft Tenant |
| `microsoft-clarity-automation` | Microsoft Clarity |
| `share_point-automation` | SharePoint |
| `dynamics365-automation` | Dynamics 365 |

### 5.9 Zoho 服务套件

| 技能名称 | 服务 |
|---------|------|
| `zoho-crm-automation` | Zoho CRM |
| `zoho-desk-automation` | Zoho Desk |
| `zoho-mail-automation` | Zoho Mail |
| `zoho-books-automation` | Zoho Books |
| `zoho-invoice-automation` | Zoho Invoice |
| `zoho-inventory-automation` | Zoho Inventory |
| `zoho-bigin-automation` | Zoho Bigin |

### 5.10 AI/ML 服务

| 技能名称 | 服务 | 功能 |
|---------|------|------|
| `openai-automation` | OpenAI | GPT 和其他模型 |
| `anthropic-automation` | Anthropic | Claude API |
| `gemini-automation` | Google Gemini | Gemini 模型 |
| `mistral-ai-automation` | Mistral AI | Mistral 模型 |
| `groqcloud-automation` | Groq | 高速推理 |
| `perplexityai-automation` | Perplexity | 搜索增强 AI |
| `elevenlabs-automation` | ElevenLabs | 语音合成 |
| `deepgram-automation` | Deepgram | 语音识别 |
| `replicate-automation` | Replicate | 模型部署 |
| `huggingface-automation` | Hugging Face | 模型库 |
| `langbase-automation` | Langbase | LLM 平台 |
| `langsmith-fetch` | LangSmith | LLM 可观测性 |

### 5.11 数据分析与可视化

| 技能名称 | 服务 | 功能 |
|---------|------|------|
| `airtable-automation` | Airtable | 数据库和表格 |
| `googlesheets-automation` | Google Sheets | 电子表格 |
| `excel-automation` | Excel | 电子表格处理 |
| `bigml-automation` | BigML | 机器学习平台 |
| `datarobot-automation` | DataRobot | 自动化 ML |
| `amplitude-automation` | Amplitude | 产品分析 |
| `mixpanel-automation` | Mixpanel | 产品分析 |
| `segment-automation` | Segment | 数据集成 |
| `snowflake-automation` | Snowflake | 数据仓库 |
| `tableau-automation` | Tableau | 数据可视化 |

---

## 6️⃣ 内容创作技能 (Content Creation)

专门用于内容生成和创意工作的技能。

### 6.1 文案写作技能

| 技能名称 | 描述 |
|---------|------|
| `copywriting` | 文案写作 - 创建营销和销售文案 |
| `copy-editing` | 文案编辑 - 改进和润色文本 |
| `content-research-writer` | 内容研究写作 - 研究并撰写内容 |
| `content-strategy` | 内容策略 - 制定内容计划 |
| `blogwatcher` | 博客监控 - 追踪博客动态 |

### 6.2 营销与增长技能

| 技能名称 | 描述 |
|---------|------|
| `launch-strategy` | 产品发布策略 |
| `pricing-strategy` | 定价策略 |
| `free-tool-strategy` | 免费工具营销策略 |
| `referral-program` | 推荐计划设计 |
| `email-sequence` | 邮件序列设计 |
| `cold-email` | 冷邮件撰写 |
| `marketing-ideas` | 营销创意生成 |
| `marketing-psychology` | 营销心理学应用 |
| `seo-audit` | SEO 审计 |
| `programmatic-seo` | 程序化 SEO |
| `schema-markup` | Schema 标记 |
| `competitor-alternatives` | 竞争对手替代方案分析 |
| `competitive-ads-extractor` | 竞争广告提取 |
| `twitter-algorithm-optimizer` | Twitter 算法优化 |
| `domain-name-brainstormer` | 域名头脑风暴 |

### 6.3 CRO (转化率优化) 技能

| 技能名称 | 描述 |
|---------|------|
| `page-cro` | 页面转化率优化 |
| `form-cro` | 表单转化率优化 |
| `popup-cro` | 弹窗转化率优化 |
| `paywall-upgrade-cro` | 付费墙升级优化 |
| `signup-flow-cro` | 注册流程优化 |
| `onboarding-cro` | 用户引导优化 |

### 6.4 品牌与设计

| 技能名称 | 描述 |
|---------|------|
| `brand-guidelines` | 品牌指南创建 |
| `logo-dev-automation` | Logo 开发 |
| `canvas-design` | Canva 设计 |
| `figma-automation` | Figma 自动化 |
| `image-enhancer` | 图像增强 |

### 6.5 特定平台内容

| 技能名称 | 描述 |
|---------|------|
| `social-content` | 社交媒体内容 |
| `paid-ads` | 付费广告内容 |
| `internal-comms` | 内部通讯内容 |
| `product-marketing-context` | 产品营销上下文 |
| `developer-growth-analysis` | 开发者增长分析 |

---

## 7️⃣ 特定场景技能 (Domain-Specific)

### 7.1 招聘与人力资源

| 技能名称 | 描述 |
|---------|------|
| `tailored-resume-generator` | 定制简历生成 |
| `lead-research-assistant` | 潜在客户研究助理 |
| `meeting-insights-analyzer` | 会议洞察分析 |
| `async-interview-automation` | 异步面试自动化 |

### 7.2 文档与合规

| 技能名称 | 描述 |
|---------|------|
| `document-skills` | 文档技能综合 |
| `invoice-organizer` | 发票整理 |
| `file-organizer` | 文件整理 |
| `changelog-generator` | 变更日志生成 |

### 7.3 媒体与娱乐

| 技能名称 | 描述 |
|---------|------|
| `video-downloader` | 视频下载 |
| `video-frames` | 视频帧提取 |
| `gifgrep` | GIF 搜索 |
| `spotify-player` | Spotify 播放器 |
| `spotify-automation` | Spotify 自动化 |
| `songsee` | 歌曲相关 |
| `himalaya` | 喜马拉雅音频 |
| `youtube-automation` | YouTube 自动化 |
| `tiktok-automation` | TikTok 自动化 |

### 7.4 实用工具

| 技能名称 | 描述 |
|---------|------|
| `weather` | 天气查询 |
| `goplaces` | 地点搜索 |
| `local-places` | 本地地点 |
| `news-search` | 新闻搜索 |
| `web-search` | 网络搜索 |
| `summarize` | 文本摘要 |
| `camsnap` | 相机快照 |
| `peekaboo` | 窥视工具 |
| `sag` | SAG 工具 |
| `ordercli` | 订单 CLI |
| `blucli` | Blu CLI |
| `wacli` | WA CLI |
| `eightctl` | Eight 控制器 |
| `sonoscli` | Sonos CLI |
| `tmux` | Tmux 终端 |
| `imsg` | iMessage |
| `bluebubbles` | Blue Bubbles |
| `food-order` | 点餐系统 |
| `raffle-winner-picker` | 抽奖工具 |
| `oracle` | 神谕工具 |

---

## 8️⃣ 中文专用技能 (Chinese-Specific)

### 8.1 Baoyu 系列 (中文内容创作)

| 技能名称 | 描述 |
|---------|------|
| `baoyu-article-illustrator` | 文章插图生成 |
| `baoyu-comic` | 漫画生成 |
| `baoyu-compress-image` | 图片压缩 |
| `baoyu-cover-image` | 封面图生成 |
| `baoyu-format-markdown` | Markdown 格式化 |
| `baoyu-image-gen` | 图片生成 |
| `baoyu-infographic` | 信息图生成 |
| `baoyu-markdown-to-html` | Markdown 转 HTML |
| `baoyu-post-to-wechat` | 发布到微信 |
| `baoyu-post-to-x` | 发布到 X/Twitter |
| `baoyu-slide-deck` | 幻灯片生成 |
| `baoyu-url-to-markdown` | URL 转 Markdown |
| `baoyu-xhs-images` | 小红书图片生成 |
| `baoyu-danger-gemini-web` | Gemini Web 工具 |
| `baoyu-danger-x-to-markdown` | X/Twitter 转 Markdown |

### 8.2 中文路径处理

| 技能名称 | 描述 |
|---------|------|
| `chinese-file-lister` | 中文文件列表 - 处理中文目录路径下的文件列表 |
| `chinese-path-handler` | 中文路径处理 - 处理含中文字符的文件路径 |

**功能特点:**
- 支持 GBK/UTF-8 编码
- 跨平台兼容 (Windows/Linux/macOS)
- 解决中文乱码问题

---

## 9️⃣ NanoBanana 系列技能

| 技能名称 | 描述 |
|---------|------|
| `nano-banana-pro` | NanoBanana Pro 工具 |
| `nano-pdf` | Nano PDF 处理 |
| `nano-nets-automation` | Nano Nets 自动化 |
| `NanoBanana-PPT-Skills` | NanoBanana PPT 技能 |

---

## 🔟 其他技能包 (Other Skill Collections)

### 10.1 微短剧技能

| 技能名称 | 描述 |
|---------|------|
| `Micro-Drama-Skills-main` | 微短剧技能包 |

### 10.2 PPT OOXML 工具

| 技能名称 | 描述 |
|---------|------|
| `ppt-ooxml-tool` | PPT OOXML 处理工具 |

---

## 📈 技能使用统计

### 按类别分布

| 类别 | 数量估计 | 占比 |
|------|---------|------|
| 第三方服务自动化 (xxx-automation) | ~900+ | ~82% |
| 核心系统技能 | ~50 | ~4.5% |
| 文档处理技能 | ~10 | ~1% |
| 浏览器自动化 | ~15 | ~1.4% |
| 桌面自动化 | ~10 | ~1% |
| 内容创作与营销 | ~30 | ~2.7% |
| Baoyu 中文技能 | ~15 | ~1.4% |
| 其他特定技能 | ~70 | ~6% |

### 热门服务类别

1. **SaaS 应用自动化** - CRM、项目管理、邮件营销等
2. **AI/ML 服务集成** - OpenAI、Anthropic、Google AI 等
3. **社交媒体管理** - Twitter、LinkedIn、Instagram 等
4. **开发与运维工具** - CI/CD、监控、数据库等
5. **通讯协作** - Slack、Discord、Teams 等

---

## 🎯 技能命名规范

### 命名模式

1. **标准自动化技能**: `{service-name}-automation`
   - 示例: `slack-automation`, `github-automation`

2. **核心系统技能**: 使用动词或功能描述
   - 示例: `run-shell`, `read-file`, `find-skills`

3. **特定功能技能**: 使用功能描述
   - 示例: `web-search`, `summarize`, `weather`

4. **中文技能**: 
   - Baoyu 系列: `baoyu-{function}`
   - 中文处理: `chinese-{function}`

5. **套件技能**: 使用品牌前缀
   - 示例: `google-calendar-automation`, `zoho-crm-automation`

---

## 📝 技能文件结构

每个技能目录的标准结构:

```
skill-name/
├── SKILL.md           # 必需 - 技能说明和使用指南
├── config.json        # 可选 - 技能配置
├── scripts/           # 可选 - 可执行脚本
├── references/        # 可选 - 参考文档
├── assets/            # 可选 - 资源文件
└── ...
```

### SKILL.md 标准格式

```yaml
---
name: skill-name
description: 技能描述，说明功能和使用场景
metadata:                     # 可选 - 元数据
  openclaw:
    emoji: "🐙"               # 图标
    requires:
      bins: [gh]              # 必需的二进制文件
      env: [API_KEY]          # 必需的环境变量
---

# 技能标题

## 概述
技能功能和用途说明

## 使用方法
具体使用指南和示例代码
```

---

## 🔧 技能管理命令

### 使用 ClawdHub CLI

```bash
# 安装 ClawdHub CLI
npm i -g clawdhub

# 搜索技能
clawdhub search "关键词"

# 安装技能
clawdhub install skill-name
clawdhub install skill-name --version 1.2.3

# 更新技能
clawdhub update skill-name
clawdhub update --all

# 列出已安装技能
clawdhub list

# 发布技能
clawdhub publish ./my-skill --slug my-skill --name "My Skill" --version 1.0.0
```

---

## 🚀 如何贡献新技能

1. **使用 skill-creator 技能** 获取创建指南
2. **准备技能内容**: SKILL.md + 可选的 scripts/references/assets
3. **使用 package_skill.py 打包**
4. **通过 ClawdHub 发布** 或直接安装

---

## 📚 相关文档

- [SKILL.md 规范](skill-creator/SKILL.md) - 技能创建完整指南
- [ClawdHub CLI 文档](clawdhub/SKILL.md) - 技能管理 CLI
- [GitHub Skill](github/SKILL.md) - GitHub 集成示例

---

*本文档由 AgentBot 自动生成，用于帮助用户了解和管理技能目录。*

**生成时间**: 2026-03-10  
**技能总数**: 1100+  
**最后更新**: 请查看各技能的 SKILL.md 获取最新信息
