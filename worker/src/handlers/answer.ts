import type { Env, AnswerRequest } from "../types.js";
import { jsonResponse } from "../auth.js";
import {
  getQuestion,
  putQuestion,
  getQuestionIdByToken,
  deleteAnswerToken,
} from "../services/kv.js";

export async function handleGetAnswer(
  questionId: string,
  env: Env
): Promise<Response> {
  const record = await getQuestion(env, questionId);
  if (!record) {
    return jsonResponse({ error: "question_not_found" }, 404);
  }

  if (record.status === "answered") {
    return jsonResponse({
      status: "answered",
      answer: record.answer,
      answered_at: record.answeredAt,
    });
  }

  return jsonResponse({ status: "pending" });
}

export async function handlePostAnswer(
  questionId: string,
  request: Request,
  env: Env,
  isAuthenticated: boolean
): Promise<Response> {
  let body: AnswerRequest;
  try {
    body = (await request.json()) as AnswerRequest;
  } catch {
    return jsonResponse({ error: "invalid_json" }, 400);
  }

  if (!body.answer || typeof body.answer !== "string") {
    return jsonResponse({ error: "answer_required" }, 400);
  }

  // Authenticate via Bearer token or answer_token
  if (!isAuthenticated) {
    if (!body.answer_token) {
      return jsonResponse({ error: "auth_required" }, 401);
    }

    const tokenQuestionId = await getQuestionIdByToken(env, body.answer_token);
    if (!tokenQuestionId || tokenQuestionId !== questionId) {
      return jsonResponse({ error: "invalid_answer_token" }, 403);
    }

    // Consume the one-time token
    await deleteAnswerToken(env, body.answer_token);
  }

  const record = await getQuestion(env, questionId);
  if (!record) {
    return jsonResponse({ error: "question_not_found" }, 404);
  }

  if (record.status === "answered") {
    return jsonResponse({ error: "already_answered" }, 400);
  }

  record.answer = body.answer;
  record.status = "answered";
  record.answeredAt = new Date().toISOString();
  record.tokenUsed = true;

  await putQuestion(env, record);

  return jsonResponse({ status: "ok" });
}
