import type { Env, ServerChanResponse } from "../types.js";

export async function pushViaServerChan(
  env: Env,
  title: string,
  desp: string
): Promise<{ success: boolean; error?: string }> {
  if (!env.SERVERCHAN_API_KEY) {
    return { success: false, error: "SERVERCHAN_API_KEY not configured" };
  }

  try {
    const url = new URL(
      `https://sctapi.ftqq.com/${env.SERVERCHAN_API_KEY}.send`
    );
    url.searchParams.set("title", title);
    url.searchParams.set("desp", desp);

    const response = await fetch(url.toString(), { method: "POST" });
    if (!response.ok) {
      return {
        success: false,
        error: `HTTP ${response.status} ${response.statusText}`,
      };
    }

    const result = (await response.json()) as ServerChanResponse;
    if (result.code === 0) {
      return { success: true };
    }
    return { success: false, error: result.message };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : String(err),
    };
  }
}
