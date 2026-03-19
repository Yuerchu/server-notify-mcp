import type { Env, AskRequest, QuestionRecord } from "../types.js";
import { jsonResponse } from "../auth.js";
import { putQuestion, putAnswerToken } from "../services/kv.js";
import { pushViaServerChan } from "../services/serverchan.js";
import { pushViaFCM } from "../services/fcm.js";

export async function handleAsk(
  request: Request,
  env: Env,
  workerUrl: string
): Promise<Response> {
  let body: AskRequest;
  try {
    body = (await request.json()) as AskRequest;
  } catch {
    return jsonResponse({ error: "invalid_json" }, 400);
  }

  if (!body.question || typeof body.question !== "string") {
    return jsonResponse({ error: "question_required" }, 400);
  }

  const questionId = crypto.randomUUID();
  const answerToken = crypto.randomUUID();

  const record: QuestionRecord = {
    id: questionId,
    question: body.question,
    options: body.options,
    status: "pending",
    answerToken,
    tokenUsed: false,
    createdAt: new Date().toISOString(),
    source: body.source,
  };

  await putQuestion(env, record);
  await putAnswerToken(env, answerToken, questionId);

  const pageUrl = `${workerUrl}/page/${answerToken}`;

  // Push notifications asynchronously (don't block response)
  const pushPromises: Promise<unknown>[] = [];

  // Server酱 push
  const title = "Claude 需要你的输入";
  let desp = `## 问题\n\n${body.question}\n\n`;
  if (body.options && body.options.length > 0) {
    desp += `## 选项\n\n${body.options.map((o, i) => `${i + 1}. ${o}`).join("\n")}\n\n`;
  }
  desp += `---\n\n[点击这里回答](${pageUrl})`;
  pushPromises.push(pushViaServerChan(env, title, desp));

  // FCM push
  pushPromises.push(
    pushViaFCM(env, questionId, body.question, body.options)
  );

  // Wait for push notifications but don't fail the request
  const pushResults = await Promise.allSettled(pushPromises);

  const serverChanResult = pushResults[0];
  const fcmResult = pushResults[1];

  const serverChanOk = serverChanResult.status === "fulfilled" &&
    serverChanResult.value && typeof serverChanResult.value === "object" &&
    "success" in serverChanResult.value && (serverChanResult.value as { success: boolean }).success;

  const fcmValue = fcmResult.status === "fulfilled" && fcmResult.value &&
    typeof fcmResult.value === "object" ? fcmResult.value as { success: boolean; sent: number; failed: number } : null;

  return jsonResponse({
    question_id: questionId,
    answer_url: `${workerUrl}/answer/${questionId}`,
    page_url: pageUrl,
    serverchan_sent: !!serverChanOk,
    fcm_sent: fcmValue ? fcmValue.sent : 0,
    fcm_failed: fcmValue ? fcmValue.failed : 0,
  });
}
