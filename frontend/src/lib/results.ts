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
export interface RecapBand { code: string; label: string; range: string; current: boolean }
export type RecapEvidenceKind = "REASON" | "CUT" | "KEEP";
export type RecapCard = ({ index: number; score_text: string; width: number; height: number; rank: number | null; top_percent: number | null } & (
  | { kind: "VERDICT"; verdict: string; passed: boolean; score: number; cut: number; cut_distance: string; cut_distance_value: number; grade: string; grade_code: string; verdict_line: string; delivery_eta: string | null }
  | { kind: "CUTLINE"; total: number | null; grade: string; grade_code: string; band_detail: string; band_position: number; bands: RecapBand[] }
  | { kind: RecapEvidenceKind; ordinal: number; quote: string | null; chip_text: string; label: string; line: string; dim_tag: string | null }
  | { kind: "CONCLUSION"; verdict: string; passed: boolean; grade: string; grade_code: string; band_tag: string; band_position: number; title: string; archetype_code: string; archetype_name: string; type_percent: number | null; hero_quote: string | null; hero_chip_text: string | null; final_line: string; cut_distance: string; friend_call: string }
));
export interface RecapResponse {
  assessment_id: string;
  skin: "GIFT" | "COAL";
  score: number;
  score_text: string;
  share_text: string;
  rank: number | null;
  top_percent: number | null;
  ranking_pending: boolean;
  cards: RecapCard[];
}
const recapKinds = new Set<string>(["VERDICT", "CUTLINE", "REASON", "CUT", "KEEP", "CONCLUSION"]);
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
  try {
    const recap = (await api.get<RecapResponse>("/me/recap", { signal })).data;
    // Skip card kinds this build does not know so a newer server deck still renders.
    return { ...recap, cards: recap.cards.filter(card => recapKinds.has(card.kind)) };
  } catch (error) {
    if (isAxiosError(error) && error.response?.status === 404) return null;
    throw error;
  }
}
export const RECAP_POLL_MS = 3_000;
export const recapRefetchInterval = (recap: RecapResponse | null | undefined) => recap?.ranking_pending ? RECAP_POLL_MS : false;
export async function getMyDossier(signal?: AbortSignal) {
  return (await api.get<DossierResponse>("/me/dossier", { signal })).data;
}
export async function createShare() {
  const share = (await api.post<ShareResponse>("/shares", { include_quote: false })).data;
  const path = `/s/${encodeURIComponent(share.token)}`;
  return { ...share, url: new URL(path, window.location.origin).href, imageUrl: new URL(`${path}/image.png`, window.location.origin).href };
}

export function resultError(error: unknown) { return getApiErrorMessage(error); }
export const formatScore = (value: number) => (value / 100).toFixed(2);
export const formatKoreanDate = (value: string | null) => value ? new Date(value).toLocaleString("ko-KR", { timeZone: "Asia/Seoul", month: "long", day: "numeric", hour: "numeric", minute: "2-digit" }) : "계산 중";
