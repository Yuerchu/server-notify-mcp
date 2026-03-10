#!/usr/bin/env node

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

const SERVERCHAN_API_KEY = process.env.SERVERCHAN_API_KEY;

if (!SERVERCHAN_API_KEY) {
  console.error(
    "Error: SERVERCHAN_API_KEY environment variable is required.\n" +
      "Get your SendKey from https://sct.ftqq.com/sendkey"
  );
  process.exit(1);
}

interface ServerChanResponse {
  code: number;
  message: string;
  data?: {
    pushid: string;
    readkey: string;
    error: string;
    errno: number;
  };
}

async function sendNotification(
  title: string,
  desp?: string
): Promise<ServerChanResponse> {
  const url = new URL(`https://sctapi.ftqq.com/${SERVERCHAN_API_KEY}.send`);
  url.searchParams.set("title", title);
  if (desp) {
    url.searchParams.set("desp", desp);
  }

  const response = await fetch(url.toString(), { method: "POST" });

  if (!response.ok) {
    throw new Error(`HTTP error: ${response.status} ${response.statusText}`);
  }

  return (await response.json()) as ServerChanResponse;
}

const server = new McpServer({
  name: "server-notify-mcp",
  version: "1.0.0",
});

server.tool(
  "notify",
  "Send a push notification to the user's phone via ServerChan. " +
    "IMPORTANT GUIDELINES - Read carefully before using: " +
    "(1) This tool sends a REAL push notification to the user's phone. Use it SPARINGLY. " +
    "(2) Do NOT notify for routine progress updates, minor file changes, or intermediate steps. " +
    "(3) If you are a sub-agent/child agent, do NOT use this tool — only the top-level orchestrating agent should send notifications. " +
    "(4) Only use this tool when the situation matches one of the following: " +
    "A) The ENTIRE task (not a sub-step) has fully completed or critically failed and the user is likely AFK. " +
    "B) You are BLOCKED and need user input/confirmation to continue — e.g., ambiguous requirements, permission needed, or a decision fork — " +
    "and the user has not responded for a while or is likely away. " +
    "In both cases, the notification must provide actionable value (e.g., 'build done, ready for review', 'deploy failed, needs fix', or 'need your input on X before continuing'). " +
    "(5) When in doubt, do NOT notify. One missed notification is far better than ten unwanted ones. " +
    "(6) NEVER send more than one notification per task. Consolidate into a single summary if needed. " +
    "The title should be concise (max 32 chars). " +
    "The desp field supports Markdown.",
  {
    title: z
      .string()
      .max(32)
      .describe("Notification title, concise summary (max 32 chars)"),
    desp: z
      .string()
      .optional()
      .describe(
        "Notification body, supports Markdown. Use for detailed info, reports, or action items."
      ),
  },
  async ({ title, desp }) => {
    try {
      const result = await sendNotification(title, desp);

      if (result.code === 0) {
        return {
          content: [
            {
              type: "text" as const,
              text: `Done`,
            },
          ],
        };
      } else {
        return {
          content: [
            {
              type: "text" as const,
              text: `Notification failed: ${result.message} (code: ${result.code})`,
            },
          ],
          isError: true,
        };
      }
    } catch (error) {
      return {
        content: [
          {
            type: "text" as const,
            text: `Failed to send notification: ${error instanceof Error ? error.message : String(error)}`,
          },
        ],
        isError: true,
      };
    }
  }
);

const transport = new StdioServerTransport();
await server.connect(transport);
