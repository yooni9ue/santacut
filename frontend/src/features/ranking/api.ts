import { api, throwApiError } from '../../shared/api/client';
import type { components } from '../../shared/api/schema';

export type RankingRow = components['schemas']['RankingResponse'];
export type MyResult = components['schemas']['MyResultResponse'];

export async function loadRankings(offset = 0): Promise<RankingRow[]> {
  const { data, error, response } = await api.GET('/v1/rankings', { params: { query: { offset } } });
  if (!response.ok) throwApiError(response.status, error);
  if (!data) throw new Error('The server returned an empty ranking response.');
  return data;
}

export async function loadMyResult(): Promise<MyResult | null> {
  const { data, error, response } = await api.GET('/v1/me/result');
  if (response.status === 404) return null;
  if (!response.ok) throwApiError(response.status, error);
  if (!data) return null;
  return data;
}
