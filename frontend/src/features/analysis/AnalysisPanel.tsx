import { useState } from 'react';
import { submitAnalysis, watchAnalysis, type AnalysisInput, type AnalysisResult, type AnalysisStatus } from './api';

interface AnalysisPanelProps {
  onCompleted: () => void;
}

const example = `{
  "source": "KAKAO",
  "events": [
    {
      "occurredAt": "2026-11-01T08:30:00Z",
      "text": "친구가 힘들어해서 먼저 연락하고 이야기를 들어줬어."
    }
  ]
}`;

const statusLabels: Record<AnalysisStatus['status'], string> = {
  QUEUED: '산타 심사 대기 중',
  RUNNING: '산타가 대화를 읽는 중',
  SUCCEEDED: '심사 완료',
  FAILED: '심사 중 문제가 생겼어요',
  INSUFFICIENT_DATA: '착한 마음 근거가 조금 부족해요',
};

export function AnalysisPanel({ onCompleted }: AnalysisPanelProps) {
  const [json, setJson] = useState(example);
  const [status, setStatus] = useState<AnalysisStatus['status'] | null>(null);
  const [result, setResult] = useState<AnalysisResult | null>(null);
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  async function startAnalysis() {
    setBusy(true);
    setMessage('');
    setResult(null);
    try {
      const input = JSON.parse(json) as AnalysisInput;
      const job = await submitAnalysis(input);
      setStatus(job.status);
      setMessage('제출됐어요. 완료될 때까지 이 화면을 열어 두세요.');

      const stopWatching = watchAnalysis(job.id, {
        onStatus: (update) => setStatus(update.status),
        onResult: (analysisResult) => {
          setResult(analysisResult);
          setStatus('SUCCEEDED');
          setBusy(false);
          setMessage('결과가 저장됐어요. 아래에서 공유할 수 있어요.');
          onCompleted();
        },
        onFailure: (failure) => {
          setStatus(failure.status);
          setBusy(false);
          setMessage(failure.error_code ?? '분석에 실패했습니다. 입력 데이터를 확인해 주세요.');
        },
        onDisconnect: () => {
          setBusy(false);
          setMessage('실시간 연결이 끊겼어요. 잠시 후 다시 제출해 주세요.');
        },
      });
      window.setTimeout(stopWatching, 155_000);
    } catch (cause) {
      setBusy(false);
      setMessage(cause instanceof SyntaxError ? 'JSON 형식이 올바른지 확인해 주세요.' : cause instanceof Error ? cause.message : '요청을 처리하지 못했습니다.');
    }
  }

  return (
    <section className="panel analysis-panel" aria-labelledby="analysis-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">AI SANTA REVIEW</p>
          <h2 id="analysis-title">올해의 착함 심사받기</h2>
        </div>
        <span className="step-badge">02</span>
      </div>
      <p className="muted">
        플랫폼에서 내보낸 원본 파일을 직접 올리는 기능은 준비 중이에요. 현재는 아래 형식의 정규화 이벤트 JSON을 받습니다.
      </p>
      <label className="json-label" htmlFor="analysis-json">분석 요청 JSON</label>
      <textarea
        autoCapitalize="off"
        autoComplete="off"
        className="json-input"
        id="analysis-json"
        onChange={(event) => setJson(event.target.value)}
        spellCheck={false}
        value={json}
      />
      <div className="analysis-footer">
        <p className="form-hint">이벤트 20–2,000개 · 최근 90일 · 메시지 원문은 분석 후 삭제</p>
        <button className="button button-primary" disabled={busy} onClick={startAnalysis} type="button">
          {busy ? '산타 심사 중…' : '심사 시작하기'}
        </button>
      </div>
      {(status || message) && (
        <div aria-live="polite" className="status-box" role="status">
          {status && <strong>{statusLabels[status]}</strong>}
          {message && <span>{message}</span>}
        </div>
      )}
      {result && (
        <div className="result-card">
          <div className="result-score">
            <span>산타 점수</span>
            <strong>{(result.score_hundredths / 100).toFixed(2)}<small>점</small></strong>
          </div>
          <div className="result-copy">
            <span className="verdict">{result.verdict}</span>
            <p>{result.comment}</p>
            <p>통합 {result.rank}위 · 배송 순번 {result.delivery_sequence}번 · {formatEta(result.eta)}</p>
          </div>
        </div>
      )}
    </section>
  );
}

function formatEta(value: string): string {
  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}
