"use client";

import { isAxiosError } from "axios";
import { api, getApiErrorMessage } from "./api";
import type { Source } from "@/components/flow/preview-context";

export const analysisStates = ["VALIDATING", "QUEUED", "ANALYZING", "RANKING_PENDING", "SUCCEEDED", "FAILED"] as const;
export type AnalysisState = typeof analysisStates[number];
export interface AnalysisSubmission { id: string; status: AnalysisState }
export interface AnalysisStatus extends AnalysisSubmission {
  error_code: string | null;
  assessment_id: string | null;
  created_at: string;
  finished_at: string | null;
}
export const isFinished = (status?: AnalysisState) => status === "SUCCEEDED" || status === "FAILED";
export const analysisKey = (id: string) => ["analysis", id] as const;

export async function uploadAnalysis(input: { file: File; source: Source; speaker: string; key: string }, options?: { signal?: AbortSignal; onProgress?: (percent: number) => void }) {
  const body = new FormData();
  body.append("file", input.file);
  body.append("source", input.source);
  body.append("self_speaker", input.speaker);
  return (await api.post<AnalysisSubmission>("/analyses/files", body, {
    headers: { "Idempotency-Key": input.key }, signal: options?.signal, timeout: 120_000,
    onUploadProgress: progress => { if (progress.total) options?.onProgress?.(Math.min(100, Math.floor(progress.loaded / progress.total * 100))); },
  })).data;
}
export async function getAnalysis(id: string, signal?: AbortSignal) {
  return (await api.get<AnalysisStatus>(`/analyses/${encodeURIComponent(id)}`, { signal })).data;
}
export async function getLatestAnalysis(signal?: AbortSignal): Promise<AnalysisStatus | null> {
  try { return (await api.get<AnalysisStatus>("/me/analysis", { signal })).data; }
  catch (error) {
    if (isAxiosError(error) && error.response?.status === 404) return null;
    throw error;
  }
}

export function watchAnalysis(id: string, onStatus: (status: AnalysisStatus) => void, onDisconnect: () => void) {
  let source: EventSource;
  try { source = new EventSource(`/v1/analyses/${encodeURIComponent(id)}/events`, { withCredentials: true }); }
  catch { onDisconnect(); return () => {}; }
  let closed = false;
  function stop() { if (!closed) { closed = true; source.close(); } }
  function disconnect() { if (!closed) { stop(); onDisconnect(); } }
  source.addEventListener("status", event => {
    if (closed) return;
    try {
      const status: AnalysisStatus = JSON.parse((event as MessageEvent).data);
      if (status.id !== id || !analysisStates.includes(status.status)) { disconnect(); return; }
      onStatus(status);
      if (isFinished(status.status)) stop();
    } catch { disconnect(); }
  });
  source.addEventListener("error", disconnect);
  return stop;
}

const failures: Record<string, string> = {
  UNSUPPORTED_FILE_FORMAT: "지원하지 않는 파일 형식이에요. 원본 TXT 또는 message_1.json을 다시 선택해 주세요.",
  SPEAKER_NOT_IDENTIFIED: "고른 화자의 메시지를 찾지 못했어요. 파일과 본인 화자를 다시 확인해 주세요.",
  INSUFFICIENT_EVENTS: "최근 90일 안의 본인 메시지가 20개 이상 있는 대화를 준비해 주세요.",
  INSUFFICIENT_EVIDENCE: "판정할 근거가 부족해요. 짧은 인사만 있는 대화 대신 내용이 충분한 기록을 준비해 주세요.",
  EXPIRED: "대기 시간이 지나 심사가 종료됐어요. 잠시 후 다른 요청으로 다시 시도해 주세요.",
  RETRY_EXHAUSTED: "서버에서 여러 번 확인했지만 처리하지 못했어요. 다른 파일을 준비해 주세요.",
  SEASON_FROZEN: "이번 시즌 심사가 마감됐어요. 새 분석을 시작할 수 없어요.",
  AI_NOT_CONFIGURED: "심사 서버를 준비 중이에요. 잠시 후 다시 시도해 주세요.",
  IDEMPOTENCY_CONFLICT: "이전 요청과 내용이 달라졌어요. 파일과 본인 화자를 다시 선택해 주세요.",
  ANALYSIS_FAILED: "심사를 완료하지 못했어요. 잠시 후 다른 파일로 다시 시도해 주세요.",
};
export const analysisFailure = (code: string | null) => failures[code ?? ""] ?? "심사를 완료하지 못했어요. 다른 파일로 다시 시도해 주세요.";
export function analysisError(error: unknown) {
  if (isAxiosError<{ code?: string; retry_at?: string }>(error)) {
    if (error.response?.status === 413) return "파일이 너무 커요. 20MB 이하 파일을 선택해 주세요. 전송 경로에서 더 작은 크기 제한을 적용할 수도 있어요.";
    if (error.response?.status === 429) {
      const retryAt = error.response.data?.retry_at;
      const date = retryAt ? new Date(retryAt) : null;
      const suffix = date && Number.isFinite(date.getTime()) ? ` ${date.toLocaleString("ko-KR", { timeZone: "Asia/Seoul" })} 이후 다시 시도해 주세요.` : " 잠시 후 다시 시도해 주세요.";
      return `하루 제출 한도에 도달했어요.${suffix}`;
    }
    const code = error.response?.data?.code;
    if (code && failures[code]) return failures[code];
  }
  return getApiErrorMessage(error);
}
