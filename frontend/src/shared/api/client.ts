import createClient from 'openapi-fetch';
import type { paths } from './schema';

export const api = createClient<paths>({ baseUrl: '', credentials: 'include' });

export async function csrfHeader(): Promise<{ 'X-XSRF-TOKEN': string }> {
  const response = await fetch('/v1/csrf', { credentials: 'include', cache: 'no-store' });
  if (!response.ok) throw new Error(`Could not obtain a CSRF token (${response.status})`);

  const csrf = (await response.json()) as { headerName: string; token: string };
  if (csrf.headerName !== 'X-XSRF-TOKEN') throw new Error('The server returned an unsupported CSRF header.');
  return { 'X-XSRF-TOKEN': csrf.token };
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly body: unknown,
  ) {
    super(errorMessage(body, status));
    this.name = 'ApiError';
  }
}

export function throwApiError(status: number, body: unknown): never {
  throw new ApiError(status, body);
}

function errorMessage(body: unknown, status: number): string {
  if (body && typeof body === 'object' && 'error' in body) {
    return String(body.error);
  }
  return `요청을 처리하지 못했습니다. (HTTP ${status})`;
}
