import type { Env } from "./types.js";

export function verifyBearerToken(
  request: Request,
  env: Env
): Response | null {
  const authHeader = request.headers.get("Authorization");
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return new Response(JSON.stringify({ error: "missing_auth" }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    });
  }

  const token = authHeader.slice(7);
  if (token !== env.WORKER_AUTH_TOKEN) {
    return new Response(JSON.stringify({ error: "invalid_token" }), {
      status: 403,
      headers: { "Content-Type": "application/json" },
    });
  }

  return null;
}

export function jsonResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

export function htmlResponse(html: string, status = 200): Response {
  return new Response(html, {
    status,
    headers: { "Content-Type": "text/html; charset=utf-8" },
  });
}
