import { useState } from 'react';
import { createShare } from './api';

export function ShareButton() {
  const [url, setUrl] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function shareResult() {
    setBusy(true);
    setError('');
    try {
      const share = await createShare();
      setUrl(share.url);
      await navigator.clipboard.writeText(share.url);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '공유 링크를 만들지 못했습니다.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="share-actions">
      <button className="button button-secondary" disabled={busy} onClick={() => void shareResult()} type="button">
        {busy ? '링크 만드는 중…' : '결과 링크 복사'}
      </button>
      {url && <a className="share-link" href={url} rel="noreferrer" target="_blank">공유 페이지 열기 ↗</a>}
      {error && <p className="form-error" role="alert">{error}</p>}
    </div>
  );
}
