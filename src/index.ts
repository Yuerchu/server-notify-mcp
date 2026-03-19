#!/usr/bin/env node

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

const SERVERCHAN_API_KEY = process.env.SERVERCHAN_API_KEY;
const WORKER_URL = process.env.WORKER_URL;
const WORKER_AUTH_TOKEN = process.env.WORKER_AUTH_TOKEN;

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

// --- Remote Ask helpers ---

function requireWorkerConfig(): void {
  if (!WORKER_URL || !WORKER_AUTH_TOKEN) {
    throw new Error(
      "WORKER_URL and WORKER_AUTH_TOKEN environment variables are required for remote ask. " +
        "Set them in your MCP server configuration."
    );
  }
}

async function workerFetch(
  path: string,
  options: RequestInit = {}
): Promise<Response> {
  const url = `${WORKER_URL}${path}`;
  const headers = {
    Authorization: `Bearer ${WORKER_AUTH_TOKEN}`,
    "Content-Type": "application/json",
    ...((options.headers as Record<string, string>) || {}),
  };
  return fetch(url, { ...options, headers });
}

async function postAsk(
  question: string,
  options?: string[],
  source?: string
): Promise<{ question_id: string; page_url: string }> {
  const response = await workerFetch("/ask", {
    method: "POST",
    body: JSON.stringify({ question, options, source }),
  });
  if (!response.ok) {
    const err = (await response.json()) as { error?: string };
    throw new Error(`Worker /ask failed: ${err.error || response.statusText}`);
  }
  return (await response.json()) as {
    question_id: string;
    page_url: string;
  };
}

async function getAnswer(
  questionId: string
): Promise<{ status: string; answer?: string; answered_at?: string }> {
  const response = await workerFetch(`/answer/${questionId}`);
  if (!response.ok) {
    const err = (await response.json()) as { error?: string };
    throw new Error(
      `Worker /answer failed: ${err.error || response.statusText}`
    );
  }
  return (await response.json()) as {
    status: string;
    answer?: string;
    answered_at?: string;
  };
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function pollForAnswer(
  questionId: string,
  timeoutMs: number
): Promise<{ status: string; answer?: string }> {
  const start = Date.now();
  const interval = 3000;

  while (Date.now() - start < timeoutMs) {
    const result = await getAnswer(questionId);
    if (result.status === "answered") {
      return result;
    }
    const remaining = timeoutMs - (Date.now() - start);
    if (remaining <= 0) break;
    await sleep(Math.min(interval, remaining));
  }

  return { status: "timeout" };
}

// --- Remote Ask tools ---

server.tool(
  "ask_user_remote",
  "Send a question to the user's phone and optionally wait for their answer. " +
    "Use this when you need user input or a decision and the user may be away from the computer. " +
    "The question is pushed to the user's phone via notification (ServerChan + FCM). " +
    "GUIDELINES: " +
    "(1) Only use when you genuinely need user input and cannot proceed without it. " +
    "(2) Provide options when possible to minimize the user's effort. " +
    "(3) Do NOT send more than one remote question per task. " +
    "(4) If wait=true and it times out, do NOT resend the same question — use check_remote_answer later. " +
    "(5) Sub-agents should NOT use this tool; only the top-level agent should.",
  {
    question: z.string().describe("The question to ask the user"),
    options: z
      .array(z.string())
      .optional()
      .describe("Optional predefined answer choices for the user to pick from"),
    wait: z
      .boolean()
      .default(false)
      .describe(
        "If true, block and poll until the user answers or timeout is reached. Default: false"
      ),
    timeout: z
      .number()
      .default(300)
      .describe(
        "Max seconds to wait when wait=true. Default: 300 (5 min). Max: 600 (10 min)"
      ),
  },
  async ({ question, options, wait, timeout }) => {
    try {
      requireWorkerConfig();

      const effectiveTimeout = Math.min(Math.max(timeout, 10), 600);
      const result = await postAsk(question, options, "claude-code");

      if (!wait) {
        return {
          content: [
            {
              type: "text" as const,
              text:
                `Question sent to user's phone.\n` +
                `question_id: ${result.question_id}\n` +
                `Use check_remote_answer with this question_id to check for the user's response later.`,
            },
          ],
        };
      }

      // Blocking mode: poll for answer
      const answer = await pollForAnswer(
        result.question_id,
        effectiveTimeout * 1000
      );

      if (answer.status === "answered") {
        return {
          content: [
            {
              type: "text" as const,
              text: `User answered: ${answer.answer}`,
            },
          ],
        };
      }

      return {
        content: [
          {
            type: "text" as const,
            text:
              `User has not answered yet (timed out after ${effectiveTimeout}s).\n` +
              `question_id: ${result.question_id}\n` +
              `Use check_remote_answer later to check if they've responded. Do NOT resend the same question.`,
          },
        ],
      };
    } catch (error) {
      return {
        content: [
          {
            type: "text" as const,
            text: `Failed to send remote question: ${error instanceof Error ? error.message : String(error)}`,
          },
        ],
        isError: true,
      };
    }
  }
);

server.tool(
  "check_remote_answer",
  "Check if the user has answered a previously sent remote question. " +
    "Use the question_id returned by ask_user_remote.",
  {
    question_id: z
      .string()
      .describe("The question_id returned by ask_user_remote"),
  },
  async ({ question_id }) => {
    try {
      requireWorkerConfig();

      const result = await getAnswer(question_id);

      if (result.status === "answered") {
        return {
          content: [
            {
              type: "text" as const,
              text: `User answered: ${result.answer}`,
            },
          ],
        };
      }

      return {
        content: [
          {
            type: "text" as const,
            text: `User has not answered yet. You can check again later.`,
          },
        ],
      };
    } catch (error) {
      return {
        content: [
          {
            type: "text" as const,
            text: `Failed to check answer: ${error instanceof Error ? error.message : String(error)}`,
          },
        ],
        isError: true,
      };
    }
  }
);

const transport = new StdioServerTransport();
await server.connect(transport);
