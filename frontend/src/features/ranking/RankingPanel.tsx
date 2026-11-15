import { useCallback, useEffect, useState } from 'react';
import { loadMyResult, loadRankings, type MyResult, type RankingRow } from './api';

interface RankingPanelProps {
  refreshKey: number;
}

export function RankingPanel({ refreshKey }: RankingPanelProps) {
  const [rows, setRows] = useState<RankingRow[]>([]);
  const [myResult, setMyResult] = useState<MyResult | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [ranking, result] = await Promise.all([loadRankings(), loadMyResult()]);
      setRows(ranking);
      setMyResult(result);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '랭킹을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void refresh(); }, [refresh, refreshKey]);

  return (
    <section className="panel ranking-panel" aria-labelledby="ranking-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">SANTA DELIVERY LIST</p>
          <h2 id="ranking-title">선물 배송 랭킹</h2>
        </div>
        <button className="button button-secondary button-small" disabled={loading} onClick={() => void refresh()} type="button">
          새로고침
        </button>
      </div>

      {myResult && (
        <div className="my-rank-card">
          <span>내 현재 순위</span>
          <strong>{myResult.rank}<small>위</small></strong>
          <span>{(myResult.score_hundredths / 100).toFixed(2)}점 · {formatEta(myResult.eta)}</span>
        </div>
      )}

      {error && <p className="form-error" role="alert">{error}</p>}
      {loading ? <p className="muted">랭킹을 불러오는 중…</p> : rows.length === 0 ? <p className="empty-state">아직 랭킹이 없어요. 첫 심사를 시작해 보세요.</p> : (
        <div className="table-scroll">
          <table>
            <thead><tr><th scope="col">순위</th><th scope="col">닉네임</th><th scope="col">점수</th><th scope="col">예상 도착</th></tr></thead>
            <tbody>
              {rows.map((row, index) => (
                <tr key={`${row.nickname}-${row.computed_at}-${index}`}>
                  <td><span className={index < 3 ? 'rank rank-top' : 'rank'}>{row.rank}</span></td>
                  <td>{row.nickname}</td>
                  <td>{(row.score_hundredths / 100).toFixed(2)}</td>
                  <td>{formatEta(row.eta)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <p className="form-hint">예상 시각은 서울 시간 기준의 게임 시뮬레이션이에요.</p>
    </section>
  );
}

function formatEta(value: string): string {
  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: 'Asia/Seoul',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}
