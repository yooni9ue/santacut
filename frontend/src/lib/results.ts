"use client";

import { isAxiosError } from "axios";
import { api, getApiErrorMessage } from "./api";

export interface AnalysisResult {
  nickname: string;
  score_hundredths: number;
  verdict: string;
  title?: string | null;
  rank: number | null;
  tied: boolean;
  eta: string | null;
  delivery_sequence?: number | null;
  computed_at: string | null;
  ranking_pending: boolean;
  stage?: "PREVIEW" | "FINAL";
  breakdown?: Array<{ name: string; score_hundredths: number }>;
}

export async function getMyResult(signal?: AbortSignal) {
  try { return (await api.get<AnalysisResult>("/me/result", { signal })).data; }
  catch (error) {
    if (isAxiosError(error) && error.response?.status === 404) return null;
    throw error;
  }
}

export function resultError(error: unknown) { return getApiErrorMessage(error); }
export const formatScore = (value: number) => (value / 100).toFixed(2);
export const formatKoreanDate = (value: string | null) => value ? new Date(value).toLocaleString("ko-KR", { timeZone: "Asia/Seoul", month: "long", day: "numeric", hour: "numeric", minute: "2-digit" }) : "계산 중";
