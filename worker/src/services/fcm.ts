import type { Env, DeviceRecord } from "../types.js";
import { getDevices } from "./kv.js";

interface FCMServiceAccount {
  client_email: string;
  private_key: string;
  project_id: string;
}

async function getAccessToken(
  serviceAccount: FCMServiceAccount
): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const payload = {
    iss: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  };

  const enc = (obj: unknown) =>
    btoa(JSON.stringify(obj))
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/, "");

  const unsignedToken = `${enc(header)}.${enc(payload)}`;

  const pemContents = serviceAccount.private_key
    .replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "")
    .replace(/\s/g, "");

  const keyData = Uint8Array.from(atob(pemContents), (c) => c.charCodeAt(0));

  const key = await crypto.subtle.importKey(
    "pkcs8",
    keyData,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(unsignedToken)
  );

  const signatureB64 = btoa(String.fromCharCode(...new Uint8Array(signature)))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");

  const jwt = `${unsignedToken}.${signatureB64}`;

  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
  });

  const tokenData = (await tokenResponse.json()) as { access_token: string };
  return tokenData.access_token;
}

export async function pushViaFCM(
  env: Env,
  questionId: string,
  question: string,
  options?: string[]
): Promise<{ success: boolean; sent: number; failed: number }> {
  if (!env.FCM_SERVICE_ACCOUNT_JSON) {
    return { success: false, sent: 0, failed: 0 };
  }

  const devices = await getDevices(env);
  if (devices.length === 0) {
    return { success: true, sent: 0, failed: 0 };
  }

  let serviceAccount: FCMServiceAccount;
  try {
    serviceAccount = JSON.parse(
      env.FCM_SERVICE_ACCOUNT_JSON
    ) as FCMServiceAccount;
  } catch {
    return { success: false, sent: 0, failed: 0 };
  }

  const accessToken = await getAccessToken(serviceAccount);
  const url = `https://fcm.googleapis.com/v1/projects/${serviceAccount.project_id}/messages:send`;

  let sent = 0;
  let failed = 0;

  for (const device of devices) {
    try {
      const response = await fetch(url, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token: device.fcmToken,
            notification: {
              title: "Claude 需要你的输入",
              body:
                question.length > 100
                  ? question.slice(0, 97) + "..."
                  : question,
            },
            data: {
              question_id: questionId,
              question: question,
              options: options ? JSON.stringify(options) : "",
            },
            android: {
              priority: "high",
              notification: {
                channel_id: "remote_questions",
              },
            },
          },
        }),
      });

      if (response.ok) {
        sent++;
      } else {
        failed++;
      }
    } catch {
      failed++;
    }
  }

  return { success: true, sent, failed };
}
