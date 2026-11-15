import { api, csrfHeader, throwApiError } from '../../shared/api/client';
import type { components } from '../../shared/api/schema';

export type Participant = components['schemas']['CurrentParticipantResponse'];
export type Region = components['schemas']['RegionResponse'];

export async function loadCurrentParticipant(): Promise<Participant | null> {
  const { data, error, response } = await api.GET('/v1/me');
  if (response.status === 401) return null;
  if (!response.ok) throwApiError(response.status, error);
  if (!data) throw new Error('The server returned an empty participant response.');
  return data;
}

export async function loadRegions(): Promise<Region[]> {
  const { data, error, response } = await api.GET('/v1/regions');
  if (!response.ok) throwApiError(response.status, error);
  if (!data) throw new Error('The server returned an empty regions response.');
  return data;
}

export async function registerParticipant(input: components['schemas']['ParticipantRequest']): Promise<Participant> {
  const { error, response } = await api.POST('/v1/participants', {
    params: { header: await csrfHeader() },
    body: input,
  });
  if (!response.ok) throwApiError(response.status, error);
  return loadCurrentParticipant().then((participant) => {
    if (!participant) throw new Error('Registration succeeded but the session cookie was not accepted.');
    return participant;
  });
}
