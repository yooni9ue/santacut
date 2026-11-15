import { api, csrfHeader, throwApiError } from '../../shared/api/client';
import type { components } from '../../shared/api/schema';

export type ShareLink = components['schemas']['CreateShareResponse'];

export async function createShare(): Promise<ShareLink> {
  const { data, error, response } = await api.POST('/v1/shares', { params: { header: await csrfHeader() } });
  if (!response.ok) throwApiError(response.status, error);
  if (!data) throw new Error('The server returned an empty share response.');
  return data;
}

export async function revokeShare(token: string): Promise<void> {
  const { error, response } = await api.DELETE('/v1/shares/{token}', {
    params: { path: { token }, header: await csrfHeader() },
  });
  if (!response.ok) throwApiError(response.status, error);
}
