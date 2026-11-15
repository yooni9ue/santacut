import { useEffect, useState } from 'react';
import { AnalysisPanel } from '../features/analysis/AnalysisPanel';
import { RegistrationForm } from '../features/identity/RegistrationForm';
import { loadCurrentParticipant, loadRegions, type Participant, type Region } from '../features/identity/api';
import { RankingPanel } from '../features/ranking/RankingPanel';
import { ShareButton } from '../features/sharing/ShareButton';

export function App() {
  const [participant, setParticipant] = useState<Participant | null>(null);
  const [regions, setRegions] = useState<Region[]>([]);
  const [refreshKey, setRefreshKey] = useState(0);
  const [booting, setBooting] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let alive = true;
    Promise.all([loadCurrentParticipant(), loadRegions()])
      .then(([currentParticipant, supportedRegions]) => {
        if (!alive) return;
        setParticipant(currentParticipant);
        setRegions(supportedRegions);
      })
      .catch((cause: unknown) => {
        if (alive) setError(cause instanceof Error ? cause.message : '산타컷을 불러오지 못했습니다.');
      })
      .finally(() => { if (alive) setBooting(false); });
    return () => { alive = false; };
  }, []);

  return (
    <main className="app-shell">
      <header className="topbar">
        <a aria-label="산타컷 홈" className="brand" href="/">
          <span className="brand-mark" aria-hidden="true">S</span>
          <span>산타컷</span>
        </a>
        <nav aria-label="주요 메뉴" className="top-nav">
          <a href="#how-it-works">이용 방법</a>
          <a href="/swagger-ui.html" rel="noreferrer" target="_blank">API 문서</a>
        </nav>
        {participant && <span className="profile-pill">🎅 {participant.nickname}</span>}
      </header>

      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">CHRISTMAS GOODNESS INDEX</p>
          <h1>올해 선물 받을 수<br />있을까<span>?</span></h1>
          <p className="hero-description">
            대화 속 다정함을 산타가 심사하고,<br className="desktop-break" /> 선물이 도착할 순서까지 계산해요.
          </p>
          {!participant && <a className="button button-light" href="#register">내 점수 확인하기 <span aria-hidden="true">↓</span></a>}
          <p className="hero-note">점수와 배송 예정 시각은 재미를 위한 시뮬레이션입니다.</p>
        </div>
        <div className="hero-art" aria-hidden="true">
          <div className="orbit orbit-one" />
          <div className="orbit orbit-two" />
          <div className="moon">✦</div>
          <div className="gift gift-one">🎁</div>
          <div className="gift gift-two">🎄</div>
          <div className="santa">🎅</div>
          <span className="sparkle sparkle-one">✦</span>
          <span className="sparkle sparkle-two">✧</span>
        </div>
      </section>

      <section className="how-it-works" id="how-it-works">
        <div><span>01</span><strong>닉네임과 지역</strong><small>고유한 산타 프로필 만들기</small></div>
        <div><span>02</span><strong>대화 데이터</strong><small>정규화한 JSON으로 AI 심사</small></div>
        <div><span>03</span><strong>점수와 배송</strong><small>랭킹과 도착 예정 시각 확인</small></div>
      </section>

      {error && <div className="global-error" role="alert">{error}</div>}
      {booting ? <div className="loading-panel">산타의 작업실 문을 여는 중…</div> : participant ? (
        <div className="workspace-grid">
          <div className="workspace-main">
            <AnalysisPanel onCompleted={() => setRefreshKey((value) => value + 1)} />
            <RankingPanel refreshKey={refreshKey} />
          </div>
          <aside className="workspace-aside">
            <section className="panel share-panel">
              <p className="eyebrow">SHOW YOUR SCORE</p>
              <h2>결과 공유하기</h2>
              <p className="muted">내 점수와 배송 순번을 카드로 만들어 친구에게 보내 보세요.</p>
              <ShareButton />
            </section>
            <section className="panel privacy-panel">
              <span aria-hidden="true">🔒</span>
              <h3>대화는 점수만 남기고 삭제</h3>
              <p>분석 원문은 결과를 계산한 뒤 작업 큐에서 지워져요. 점수 기록에는 대화 문장을 저장하지 않습니다.</p>
            </section>
          </aside>
        </div>
      ) : (
        <section className="registration-section" id="register">
          <div className="registration-intro">
            <p className="eyebrow">START YOUR SANTA PROFILE</p>
            <h2>먼저 산타 명단에<br />이름을 올려 주세요.</h2>
            <p className="muted">닉네임은 랭킹과 공유 카드에 표시됩니다. 배송지는 구 단위까지만 받아요.</p>
          </div>
          <div className="panel registration-panel">
            <div className="section-heading"><h2>참가자 등록</h2><span className="step-badge">01</span></div>
            <RegistrationForm regions={regions} onRegistered={setParticipant} />
          </div>
        </section>
      )}

      <footer className="footer">
        <span>산타컷 · 착한 척도 데이터가 됩니다.</span>
        <span>AI 점수와 배송 일정은 엔터테인먼트 목적입니다.</span>
      </footer>
    </main>
  );
}
