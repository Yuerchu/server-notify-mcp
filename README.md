# server-notify-mcp

一个 [MCP (Model Context Protocol)](https://modelcontextprotocol.io/) 服务器，让 Code CLI 能通过 [Server酱](https://sct.ftqq.com/) 向你的微信推送通知。

适用场景：Claude Code/Codex/OpenCode 跑完耗时任务、生成了报告、遇到错误需要你介入、或者需要你做决策时，自动给你发一条微信消息。

## 安装

```bash
git clone https://github.com/Yuerchu/server-notify-mcp.git
cd server-notify-mcp
pnpm install
pnpm build
```

## 获取 SendKey

前往 [sct.ftqq.com/sendkey](https://sct.ftqq.com/sendkey) 登录后获取你的 SendKey。

## 配置到 Claude Code

```bash
claude mcp add-json server-notify '{"type":"stdio","command":"node","args":["你的路径/server-notify-mcp/dist/index.js"],"env":{"SERVERCHAN_API_KEY":"你的SendKey"}}' -s user
```

配置完成后在 Claude Code 中运行 `/mcp` 确认状态为 connected。

## 工具说明

注册了一个 `notify` 工具，参数如下：

| 参数      | 类型   | 必填 | 说明                    |
| --------- | ------ | ---- | ----------------------- |
| `title` | string | 是   | 通知标题，最多 32 字符  |
| `desp`  | string | 否   | 通知正文，支持 Markdown |

Claude Code 会在以下场景自动调用：

- 耗时任务完成
- 需要交付报告或总结
- 需要用户执行操作或做出决策
- 发生错误或重要事件需要关注

## 技术栈

- TypeScript + ESM
- [@modelcontextprotocol/sdk](https://github.com/modelcontextprotocol/typescript-sdk)
- stdio 传输

## 许可证

MIT
