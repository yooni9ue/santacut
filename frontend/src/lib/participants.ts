"use client";

import { isAxiosError } from "axios";
import { api, getApiErrorMessage, type ApiErrorResponse } from "./api";

export interface Region {
  code: string;
  name: string;
  parent_code: string | null;
  selectable: boolean;
}
export interface Participant {
  id: string;
  nickname: string;
  regionCode: string;
}
export const participantKey = ["participant", "me"] as const;
export const normalizeNickname = (value: string) => value.normalize("NFKC").trim();
export const isValidNickname = (value: string) => /^[가-힣a-zA-Z0-9_]{2,16}$/.test(normalizeNickname(value));

export async function getParticipant(signal?: AbortSignal): Promise<Participant | null> {
  try {
    return (await api.get<Participant>("/me", { signal, headers: { "Cache-Control": "no-cache" } })).data;
  } catch (error) {
    if (isAxiosError(error) && [401, 404].includes(error.response?.status ?? 0)) return null;
    throw error;
  }
}
export async function getRegions(parent?: string, signal?: AbortSignal) {
  return (await api.get<Region[]>("/regions", { params: parent ? { parent } : undefined, signal })).data;
}
export async function checkNickname(nickname: string, signal?: AbortSignal) {
  return (await api.get<{ available: boolean }>("/nicknames/availability", {
    params: { nickname: normalizeNickname(nickname) }, signal,
  })).data;
}
export async function registerParticipant(input: { nickname: string; regionCode: string }) {
  return (await api.post<Participant>("/participants", { ...input, nickname: normalizeNickname(input.nickname) })).data;
}
export function participantError(error: unknown) {
  if (isAxiosError<ApiErrorResponse & { code?: string }>(error)) {
    switch (error.response?.data?.code) {
      case "NICKNAME_TAKEN": return "이미 사용 중인 닉네임이에요. 다른 이름을 골라 주세요.";
      case "INVALID_NICKNAME": return "닉네임은 한글·영문·숫자·밑줄로 2~16자 입력해 주세요.";
      case "UNSUPPORTED_REGION": return "등록할 수 없는 지역이에요. 지역을 다시 선택해 주세요.";
    }
  }
  return getApiErrorMessage(error);
}
