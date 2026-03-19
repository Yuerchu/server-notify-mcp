import type { Env } from "./types.js";
import { verifyBearerToken, jsonResponse } from "./auth.js";
import { handleAsk } from "./handlers/ask.js";
import { handleGetAnswer, handlePostAnswer } from "./handlers/answer.js";
import { listQuestions } from "./services/kv.js";
import { handlePage } from "./handlers/page.js";
import {
  handleRegisterDevice,
  handleUnregisterDevice,
} from "./handlers/device.js";

export default {
  async fetch(
    request: Request,
    env: Env,
    _ctx: ExecutionContext
  ): Promise<Response> {
    const url = new URL(request.url);
    const path = url.pathname;
    const method = request.method;
    const workerUrl = `${url.protocol}//${url.host}`;

    // CORS headers for web answer page
    if (method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, POST, DELETE, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type, Authorization",
          "Access-Control-Max-Age": "86400",
        },
      });
    }

    const addCors = (response: Response): Response => {
      const headers = new Headers(response.headers);
      headers.set("Access-Control-Allow-Origin", "*");
      return new Response(response.body, {
        status: response.status,
        statusText: response.statusText,
        headers,
      });
    };

    try {
      // GET /health
      if (path === "/health" && method === "GET") {
        return addCors(jsonResponse({ status: "ok" }));
      }

      // GET /page/:answer_token — no auth required (token in URL)
      const pageMatch = path.match(/^\/page\/([a-f0-9-]+)$/);
      if (pageMatch && method === "GET") {
        return await handlePage(pageMatch[1], env, workerUrl);
      }

      // POST /answer/:id — supports both Bearer token and answer_token in body
      const answerPostMatch = path.match(/^\/answer\/([a-f0-9-]+)$/);
      if (answerPostMatch && method === "POST") {
        const authError = verifyBearerToken(request, env);
        const isAuthenticated = authError === null;
        // Allow unauthenticated requests — they must provide answer_token in body
        return addCors(
          await handlePostAnswer(
            answerPostMatch[1],
            request,
            env,
            isAuthenticated
          )
        );
      }

      // --- All routes below require Bearer token ---
      const authError = verifyBearerToken(request, env);
      if (authError) {
        return addCors(authError);
      }

      // GET /questions — list all questions
      if (path === "/questions" && method === "GET") {
        const questions = await listQuestions(env);
        return addCors(jsonResponse({ questions }));
      }

      // POST /ask
      if (path === "/ask" && method === "POST") {
        return addCors(await handleAsk(request, env, workerUrl));
      }

      // GET /answer/:id
      const answerGetMatch = path.match(/^\/answer\/([a-f0-9-]+)$/);
      if (answerGetMatch && method === "GET") {
        return addCors(await handleGetAnswer(answerGetMatch[1], env));
      }

      // POST /register-device
      if (path === "/register-device" && method === "POST") {
        return addCors(await handleRegisterDevice(request, env));
      }

      // DELETE /register-device
      if (path === "/register-device" && method === "DELETE") {
        return addCors(await handleUnregisterDevice(request, env));
      }

      return addCors(jsonResponse({ error: "not_found" }, 404));
    } catch (err) {
      console.error("Worker error:", err);
      return addCors(
        jsonResponse(
          {
            error: "internal_error",
            message: err instanceof Error ? err.message : String(err),
          },
          500
        )
      );
    }
  },
} satisfies ExportedHandler<Env>;
