"use client";

import { isAxiosError } from "axios";
import { api, getApiErrorMessage } from "./api";

export interface AnalysisResult {
  score_hundredths: number;
  verdict: string;
  comment: string;
  archetype?: { code: string; name: string } | null;
  rank: number | null;
  tied: boolean;
  eta: string | null;
  delivery_sequence: number | null;
  computed_at: string | null;
  ranking_pending: boolean;
  stage?: "PREVIEW" | "FINAL";
  dimension_scores?: Array<{ key: string; label: string; score: number }>;
  weakest_dimension?: { key: string; label: string; score: number } | null;
  top_percent?: number | null;
  recap_available?: boolean;
  dossier_available?: boolean;
  low_confidence_dimensions?: string[];
}

export async function getMyResult(signal?: AbortSignal) {
  try { return (await api.get<AnalysisResult>("/me/result", { signal })).data; }
  catch (error) {
    if (isAxiosError(error) && error.response?.status === 404) return null;
    throw error;
  }
}
export interface ShareResponse { token: string; url: string; imageUrl: string }
export type RecapCard = ({ index: number; score_text: string; width: number; height: number } & (
  | { kind: "VERDICT"; cut_distance: string; passed: boolean; stamp: string; stamp_mild: string; grade: string; rank: number | null; ranking_pending: boolean; delivery_eta: string | null; title: string; verdict: string; verdict_mild: string }
  | { kind: "ROAST"; chip: { label: string; value: string } | null; roast: string; flip: string }
  | { kind: "LETTER"; letter: string }
  | { kind: "STORY"; acts: Array<{ act: number; months: string; line: string }> }
  | { kind: "ARCHETYPE"; archetype: { code: string; name: string } }
  | { kind: "DIMENSIONS"; bars: Array<{ key: string; label: string; score: number }> }
));
export interface RecapResponse {
  assessment_id: string;
  skin: "GIFT" | "COAL";
  score_text: string;
  share_text: string;
  share_text_mild: string;
  cards: RecapCard[];
}
export interface DossierScene {
  id: string;
  kind: string;
  speaker: "elf" | "rudolph" | "lawyer" | "santa";
  headline: string | null;
  big_value: string | null;
  lines: Array<{ id: string; text: string; shareable: boolean }>;
  memo: string | null;
  chart: { type: string; values: number[] } | null;
  shareable: boolean;
}
export interface DossierResponse { title: string; tone: "normal" | "mild"; scenes: DossierScene[] }
export async function getMyRecap(signal?: AbortSignal) {
  return (await api.get<RecapResponse>("/me/recap", { signal })).data;
}
export async function getMyDossier(signal?: AbortSignal) {
  return (await api.get<DossierResponse>("/me/dossier", { signal })).data;
}
export async function createShare() {
  return (await api.post<ShareResponse>("/shares", { include_quote: false })).data;
}

export function resultError(error: unknown) { return getApiErrorMessage(error); }
export const formatScore = (value: number) => (value / 100).toFixed(2);
export const formatKoreanDate = (value: string | null) => value ? new Date(value).toLocaleString("ko-KR", { timeZone: "Asia/Seoul", month: "long", day: "numeric", hour: "numeric", minute: "2-digit" }) : "계산 중";
