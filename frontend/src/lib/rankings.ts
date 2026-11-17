"use client";

import { api, getApiErrorMessage } from "./api";

export interface RankingRow { nickname: string; rank: number; tied: boolean; score_hundredths: number; eta: string | null; eta_date: string }
export interface RankingPage { season_year: number; participant_count: number; computed_at: string | null; stage: "PREVIEW" | "FINAL"; rows: RankingRow[]; next_cursor: string | null }
export async function getRankings(cursor?: string, signal?: AbortSignal) {
  return (await api.get<RankingPage>("/rankings", { params: cursor ? { cursor } : undefined, signal })).data;
}
export const rankingError = (error: unknown) => getApiErrorMessage(error);
