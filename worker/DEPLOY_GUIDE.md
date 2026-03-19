# Cloudflare Worker 部署操作流程

## 前置条件
- 已登录 Cloudflare Dashboard (https://dash.cloudflare.com)
- 本地已安装 wrangler（worker 目录下 `pnpm exec wrangler`）

## 步骤 1：创建 KV Namespace

1. 打开 https://dash.cloudflare.com → 左侧菜单 → **KV**
2. 点击 **Create a namespace**
3. 名称填：`remote-ask-questions`
4. 点击 **Add**
5. 创建完成后，复制该 namespace 的 **ID**（一串 hex 字符串）

## 步骤 2：更新 wrangler.toml

打开 `worker/wrangler.toml`，把 `id = "TODO_FILL_AFTER_CREATE"` 替换为刚才复制的 KV namespace ID。

## 步骤 3：生成 Auth Token

生成一个随机的 64 字符 hex 字符串作为 WORKER_AUTH_TOKEN：
```bash
openssl rand -hex 32
```
记下这个值，后面要用两次（Worker secret + MCP 环境变量）。

## 步骤 4：配置 Worker Secrets

在 `worker/` 目录下执行：

```bash
pnpm exec wrangler secret put WORKER_AUTH_TOKEN
# 粘贴步骤 3 生成的 token

pnpm exec wrangler secret put SERVERCHAN_API_KEY
# 粘贴你的 Server酱 SendKey
```

FCM 可以后续再配：
```bash
pnpm exec wrangler secret put FCM_SERVICE_ACCOUNT_JSON
# 粘贴 Firebase 服务账号 JSON（整个 JSON 一行）
```

## 步骤 5：部署 Worker

```bash
cd worker
pnpm exec wrangler deploy
```

部署成功后会输出 Worker URL，格式类似：
`https://remote-ask-worker.<your-subdomain>.workers.dev`

## 步骤 6：验证部署

```bash
# 健康检查
curl https://remote-ask-worker.<subdomain>.workers.dev/health

# 发送测试问题
curl -X POST https://remote-ask-worker.<subdomain>.workers.dev/ask \
  -H "Authorization: Bearer <你的WORKER_AUTH_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"question":"这是一个测试问题，请选择一个选项","options":["选项A","选项B","选项C"]}'

# 返回的 page_url 可以在浏览器打开测试答题页面
```

## 步骤 7：更新 MCP Server 配置

```bash
claude mcp add-json server-notify '{
  "type": "stdio",
  "command": "node",
  "args": ["C:/Users/Administrator/Documents/Code/server-notify-mcp/dist/index.js"],
  "env": {
    "SERVERCHAN_API_KEY": "<你的SendKey>",
    "WORKER_URL": "https://remote-ask-worker.<subdomain>.workers.dev",
    "WORKER_AUTH_TOKEN": "<步骤3生成的token>"
  }
}' -s user
```

## 步骤 8：验证 MCP 工具

在 Claude Code 中运行 `/mcp`，确认 server-notify 显示 3 个工具：
- `notify`
- `ask_user_remote`
- `check_remote_answer`
