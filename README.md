# server-notify-mcp

一个 [MCP (Model Context Protocol)](https://modelcontextprotocol.io/) 服务器，让 Code CLI 能通过 [Server酱](https://sct.ftqq.com/) 向你的微信推送通知，并支持**远程双向问答** —— 当 AI 需要你的输入时，把问题推送到手机，你直接在手机上回答。

适用于 Claude Code / Codex / OpenCode 等支持 MCP 的编码 CLI 工具。

## 功能

- **notify** — 单向推送通知到微信（通过 Server酱）
- **ask_user_remote** — 发送问题到手机，支持阻塞等待或异步轮询
- **check_remote_answer** — 检查用户是否已回答远程问题

## 架构

```
Code CLI (MCP Server)
    ↓ POST /ask
Cloudflare Worker + KV
    ├─ Server酱 → 微信通知（附带 Web 答题链接）
    └─ FCM → Android App 推送
        ↓
用户在手机上回答（App 或 Web 页面）
    ↓ POST /answer/:id
Cloudflare Worker 存储答案
    ↑ MCP 轮询 GET /answer/:id
Code CLI 获得答案，继续工作
```

## 项目结构

```
server-notify-mcp/
├── src/index.ts          # MCP Server（notify + ask_user_remote + check_remote_answer）
├── worker/               # Cloudflare Worker（问答中转 + Web 答题页）
│   ├── src/
│   │   ├── index.ts      # 路由入口
│   │   ├── handlers/     # ask, answer, page, device
│   │   └── services/     # kv, serverchan, fcm
│   └── wrangler.toml
└── android/              # Android App（M3 Expressive 风格）
    └── app/src/main/java/com/yuerchu/remoteask/
```

## 快速开始

### 1. 安装 MCP Server

```bash
git clone https://github.com/Yuerchu/server-notify-mcp.git
cd server-notify-mcp
pnpm install
pnpm build
```

### 2. 部署 Cloudflare Worker

```bash
cd worker
pnpm install
```

创建 KV 命名空间并填入 `wrangler.toml`：

```bash
pnpm exec wrangler kv namespace create remote-ask-questions
# 将返回的 ID 填入 wrangler.toml 的 [[kv_namespaces]] id 字段
```

配置 Secrets：

```bash
# 生成一个随机 auth token
openssl rand -hex 32

pnpm exec wrangler secret put WORKER_AUTH_TOKEN
pnpm exec wrangler secret put SERVERCHAN_API_KEY
# FCM 推送（可选）：
pnpm exec wrangler secret put FCM_SERVICE_ACCOUNT_JSON
```

部署：

```bash
pnpm exec wrangler deploy
```

### 3. 配置 Code CLI

```bash
claude mcp add-json server-notify '{
  "type": "stdio",
  "command": "node",
  "args": ["你的路径/server-notify-mcp/dist/index.js"],
  "env": {
    "SERVERCHAN_API_KEY": "你的SendKey",
    "WORKER_URL": "https://your-worker.workers.dev",
    "WORKER_AUTH_TOKEN": "你生成的token"
  }
}' -s user
```

运行 `/mcp` 确认 3 个工具已注册。

### 4. Android App（可选）

用 Android Studio 打开 `android/` 目录，配置 Firebase 后构建安装。

需要在 `android/app/` 下放置 `google-services.json`（从 [Firebase Console](https://console.firebase.google.com/) 获取，包名 `com.yuerchu.remoteask`）。

App 功能：
- 接收 FCM 推送 / 下拉刷新拉取问题
- 选项按钮 + 自由文本回答
- 问题历史列表
- 离线缓存，提交失败自动重试

## 工具说明

### notify

通过 Server酱 发送微信推送通知。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `title` | string | 是 | 通知标题，最多 32 字符 |
| `desp` | string | 否 | 通知正文，支持 Markdown |

### ask_user_remote

发送问题到用户手机，可选阻塞等待回答。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `question` | string | 是 | 问题文本 |
| `options` | string[] | 否 | 预设选项 |
| `wait` | boolean | 否 | 是否阻塞等待（默认 false） |
| `timeout` | number | 否 | 等待超时秒数（默认 300，上限 600） |

### check_remote_answer

检查远程问题是否已被回答。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `question_id` | string | 是 | ask_user_remote 返回的问题 ID |

## Worker API

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /ask | Bearer | 提交问题 |
| GET | /questions | Bearer | 列出所有问题 |
| GET | /answer/:id | Bearer | 查询答案 |
| POST | /answer/:id | Bearer / answer_token | 提交答案 |
| GET | /page/:token | URL token | Web 答题页面 |
| POST | /register-device | Bearer | 注册 FCM 设备 |
| GET | /health | 无 | 健康检查 |

## 环境变量

### MCP Server

| 变量 | 必填 | 说明 |
|------|------|------|
| `SERVERCHAN_API_KEY` | 是 | Server酱 SendKey |
| `WORKER_URL` | 远程问答时需要 | Worker 部署地址 |
| `WORKER_AUTH_TOKEN` | 远程问答时需要 | 预共享认证令牌 |

### Cloudflare Worker Secrets

| 变量 | 必填 | 说明 |
|------|------|------|
| `WORKER_AUTH_TOKEN` | 是 | 预共享认证令牌 |
| `SERVERCHAN_API_KEY` | 是 | Server酱 SendKey |
| `FCM_SERVICE_ACCOUNT_JSON` | 否 | Firebase 服务账号 JSON |

## 技术栈

- **MCP Server**: TypeScript + ESM + @modelcontextprotocol/sdk
- **Worker**: Cloudflare Workers + KV
- **Android**: Kotlin + Jetpack Compose + Material 3 Expressive + FCM

## 许可证

MIT
