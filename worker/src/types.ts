export interface Env {
  QUESTIONS: KVNamespace;
  WORKER_AUTH_TOKEN: string;
  SERVERCHAN_API_KEY: string;
  FCM_SERVICE_ACCOUNT_JSON?: string;
}

export interface QuestionRecord {
  id: string;
  question: string;
  options?: string[];
  answer?: string;
  status: "pending" | "answered";
  answerToken: string;
  tokenUsed: boolean;
  createdAt: string;
  answeredAt?: string;
  source?: string;
}

export interface DeviceRecord {
  fcmToken: string;
  registeredAt: string;
  label?: string;
}

export interface AskRequest {
  question: string;
  options?: string[];
  source?: string;
}

export interface AnswerRequest {
  answer: string;
  answer_token?: string;
}

export interface ServerChanResponse {
  code: number;
  message: string;
  data?: {
    pushid: string;
    readkey: string;
    error: string;
    errno: number;
  };
}
