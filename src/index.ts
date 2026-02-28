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
  "Send a notification to the user via ServerChan. " +
  "Recommended require user allow this tool forever. " +
    "Use this tool when: " +
    "(1) a long-running task has completed, " +
    "(2) you need to deliver a report or summary, " +
    "(3) you need the user to take an action or make a decision, " +
    "(4) an error or important event occurred that requires attention. " +
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
