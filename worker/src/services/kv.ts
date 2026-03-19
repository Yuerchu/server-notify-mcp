import type { Env, QuestionRecord, DeviceRecord } from "../types.js";

const QUESTION_TTL = 86400; // 24 hours

export async function putQuestion(
  env: Env,
  record: QuestionRecord
): Promise<void> {
  await env.QUESTIONS.put(`q:${record.id}`, JSON.stringify(record), {
    expirationTtl: QUESTION_TTL,
  });
}

export async function getQuestion(
  env: Env,
  questionId: string
): Promise<QuestionRecord | null> {
  const data = await env.QUESTIONS.get(`q:${questionId}`);
  if (!data) return null;
  return JSON.parse(data) as QuestionRecord;
}

export async function putAnswerToken(
  env: Env,
  answerToken: string,
  questionId: string
): Promise<void> {
  await env.QUESTIONS.put(`token:${answerToken}`, questionId, {
    expirationTtl: QUESTION_TTL,
  });
}

export async function getQuestionIdByToken(
  env: Env,
  answerToken: string
): Promise<string | null> {
  return await env.QUESTIONS.get(`token:${answerToken}`);
}

export async function deleteAnswerToken(
  env: Env,
  answerToken: string
): Promise<void> {
  await env.QUESTIONS.delete(`token:${answerToken}`);
}

export async function listQuestions(env: Env): Promise<QuestionRecord[]> {
  const keys = await env.QUESTIONS.list({ prefix: "q:" });
  const questions: QuestionRecord[] = [];
  for (const key of keys.keys) {
    const data = await env.QUESTIONS.get(key.name);
    if (data) {
      questions.push(JSON.parse(data) as QuestionRecord);
    }
  }
  // Sort by createdAt descending
  questions.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  return questions;
}

export async function getDevices(env: Env): Promise<DeviceRecord[]> {
  const data = await env.QUESTIONS.get("devices");
  if (!data) return [];
  return JSON.parse(data) as DeviceRecord[];
}

export async function addDevice(
  env: Env,
  device: DeviceRecord
): Promise<void> {
  const devices = await getDevices(env);
  const existing = devices.findIndex((d) => d.fcmToken === device.fcmToken);
  if (existing >= 0) {
    devices[existing] = device;
  } else {
    devices.push(device);
  }
  await env.QUESTIONS.put("devices", JSON.stringify(devices));
}

export async function removeDevice(
  env: Env,
  fcmToken: string
): Promise<boolean> {
  const devices = await getDevices(env);
  const filtered = devices.filter((d) => d.fcmToken !== fcmToken);
  if (filtered.length === devices.length) return false;
  await env.QUESTIONS.put("devices", JSON.stringify(filtered));
  return true;
}
