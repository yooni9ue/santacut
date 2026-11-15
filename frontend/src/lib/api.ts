"use client";

import axios from "axios";

interface CsrfResponse {
  headerName: string;
  token: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  path: string;
}

// Browser requests stay on this origin; Next.js proxies /v1 to the backend.
// Server Components must use a separate request-scoped client with explicit cookies.
export const api = axios.create({
  baseURL: "/v1",
  timeout: 15_000,
  withCredentials: true,
  // Spring returns a masked token in JSON; do not replace it with the raw cookie.
  withXSRFToken: false,
  headers: { Accept: "application/json" },
});

api.interceptors.request.use(async (config) => {
  const method = config.method?.toUpperCase() ?? "GET";
  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    const { data } = await api.get<CsrfResponse>("/csrf", {
      signal: config.signal,
    });
    config.headers.set(data.headerName, data.token);
  }
  return config;
});

export function getApiErrorMessage(error: unknown): string {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) {
    return "문제가 발생했어요. 잠시 후 다시 시도해 주세요.";
  }
  if (error.code === "ECONNABORTED" || error.code === "ETIMEDOUT") {
    return "응답이 늦어지고 있어요. 잠시 후 다시 시도해 주세요.";
  }
  if (!error.response) {
    return "서버에 연결할 수 없어요. 네트워크 상태를 확인해 주세요.";
  }
  switch (error.response.status) {
    case 401:
      return "참가 정보가 없거나 세션이 만료됐어요.";
    case 403:
      return "요청을 확인할 수 없어요. 새로고침 후 다시 시도해 주세요.";
    case 409:
      return "현재 상태에서는 요청을 처리할 수 없어요.";
    case 429:
      return "요청이 너무 많아요. 잠시 후 다시 시도해 주세요.";
    default:
      return "요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.";
  }
}
