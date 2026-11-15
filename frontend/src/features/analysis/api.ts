import { api, csrfHeader, throwApiError } from '../../shared/api/client';
import type { components } from '../../shared/api/schema';

export type AnalysisInput = components['schemas']['AnalysisInput'];
export type AnalysisStatus = components['schemas']['AnalysisStatusResponse'];
export type AnalysisResult = components['schemas']['AnalysisResultResponse'];

export async function submitAnalysis(input: AnalysisInput): Promise<components['schemas']['AnalysisSubmissionResponse']> {
  const { data, error, response } = await api.POST('/v1/analyses', {
    params: { header: { 'Idempotency-Key': crypto.randomUUID(), ...await csrfHeader() } },
    body: input,
  });
  if (!response.ok) throwApiError(response.status, error);
  if (!data) throw new Error('The server returned an empty analysis response.');
  return data;
}

export interface AnalysisEventHandlers {
  onStatus: (status: AnalysisStatus) => void;
  onResult: (result: AnalysisResult) => void;
  onFailure: (status: AnalysisStatus) => void;
  onDisconnect: () => void;
}

export function watchAnalysis(id: string, handlers: AnalysisEventHandlers): () => void {
  const source = new EventSource(`/v1/analyses/${encodeURIComponent(id)}/events`, { withCredentials: true });
  const parse = <T,>(event: Event): T => JSON.parse((event as MessageEvent<string>).data) as T;

  source.addEventListener('analysis', (event) => handlers.onStatus(parse<AnalysisStatus>(event)));
  source.addEventListener('result', (event) => {
    handlers.onResult(parse<AnalysisResult>(event));
    source.close();
  });
  source.addEventListener('error', (event) => {
    if (event instanceof MessageEvent && event.data) handlers.onFailure(parse<AnalysisStatus>(event));
    source.close();
  });
  source.onerror = () => {
    handlers.onDisconnect();
    source.close();
  };

  return () => source.close();
}
