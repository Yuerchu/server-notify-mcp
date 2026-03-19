import type { Env, DeviceRecord } from "../types.js";
import { jsonResponse } from "../auth.js";
import { addDevice, removeDevice } from "../services/kv.js";

export async function handleRegisterDevice(
  request: Request,
  env: Env
): Promise<Response> {
  let body: { fcm_token?: string; label?: string };
  try {
    body = (await request.json()) as { fcm_token?: string; label?: string };
  } catch {
    return jsonResponse({ error: "invalid_json" }, 400);
  }

  if (!body.fcm_token || typeof body.fcm_token !== "string") {
    return jsonResponse({ error: "fcm_token_required" }, 400);
  }

  const device: DeviceRecord = {
    fcmToken: body.fcm_token,
    registeredAt: new Date().toISOString(),
    label: body.label,
  };

  await addDevice(env, device);
  return jsonResponse({ status: "registered" });
}

export async function handleUnregisterDevice(
  request: Request,
  env: Env
): Promise<Response> {
  let body: { fcm_token?: string };
  try {
    body = (await request.json()) as { fcm_token?: string };
  } catch {
    return jsonResponse({ error: "invalid_json" }, 400);
  }

  if (!body.fcm_token || typeof body.fcm_token !== "string") {
    return jsonResponse({ error: "fcm_token_required" }, 400);
  }

  const removed = await removeDevice(env, body.fcm_token);
  if (!removed) {
    return jsonResponse({ error: "device_not_found" }, 404);
  }

  return jsonResponse({ status: "unregistered" });
}
