"use client";

import { useRef } from "react";

function SantaSeal() {
  return (
    <svg viewBox="0 0 160 160" fill="none" aria-hidden="true">
      <path d="M40 65 67 18c5-8 15-8 21 0l30 43" fill="#d43c2e" stroke="#163e34" strokeWidth="3" />
      <path d="m76 20 33 7 11 30" stroke="#163e34" strokeWidth="3" strokeLinecap="round" />
      <circle cx="121" cy="59" r="11" fill="#fff9ec" stroke="#163e34" strokeWidth="3" />
      <path d="M37 74c-8 14-6 41 7 53 10 11 25 20 36 24 19-8 39-22 42-44 2-12 0-24-5-33" fill="#fff9ec" stroke="#163e34" strokeWidth="3" />
      <rect x="36" y="57" width="83" height="25" rx="12" fill="#fff9ec" stroke="#163e34" strokeWidth="3" />
      <path d="M54 83v19c0 17 50 17 50 0V83" fill="#f4c4a0" />
      <path d="M62 91h5m23 0h5" stroke="#163e34" strokeWidth="4" strokeLinecap="round" />
      <ellipse cx="79" cy="101" rx="9" ry="7" fill="#df6b50" />
      <path d="M79 108c-10-11-24-7-25 5 9 6 19 4 25-5Zm0 0c10-11 24-7 25 5-9 6-19 4-25-5Z" fill="#fff9ec" stroke="#163e34" strokeWidth="2" />
      <path d="M72 127c5 4 10 4 15 0" stroke="#163e34" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

const steps = [
  ["01", "올해의 기록을 꺼내요", "카톡 대화 또는 지원되는 인스타 활동 파일을 준비해 주세요."],
  ["02", "산타의 심사를 기다려요", "AI 산타가 기록을 읽고 나만의 점수와 유쾌한 판정을 만들어요."],
  ["03", "내 선물은 언제 올까요?", "통합 순위로 달라지는 선물 도착 예정일을 확인하고 공유해요."],
];

export default function Home() {
  const noticeRef = useRef<HTMLDialogElement>(null);
  return (
    <div className="site-shell">
      <div className="airmail-border" />
      <header className="site-header">
        <a className="wordmark" href="#" aria-label="산타컷 홈"><span className="brand-star">✳</span> 산타컷<span className="wordmark-en">SANTA CUT</span></a>
        <a href="#how-it-works" className="header-link">산타컷 이용 방법 <span>↗</span></a>
      </header>

      <main>
        <section className="hero" aria-labelledby="hero-title">
          <div className="hero-copy">
            <div className="eyebrow"><span /> NORTH POLE POST OFFICE <span className="edition">2026 EDITION</span></div>
            <div className="invitation">산타 심사 초대장이 도착했어요</div>
            <h1 id="hero-title">너 올해<br /><span className="title-red">산타컷</span> <span className="title-outline">통과</span><br />가능<span className="question-mark">?</span><span className="title-spark">✳</span></h1>
            <p className="hero-description">내 기록으로 받는 산타 점수,<br />그리고 나의 선물 도착 예정일.</p>

          </div>

          <div className="hero-art" aria-label="산타 심사 결과 예시: 97.82점, 산타 친자 의혹">
            <span className="art-spark spark-one">✦</span><span className="art-spark spark-two">✧</span>
            <div className="postmark"><span>SANTA CLAUS</span><strong>DEC<br />25</strong><span>SPECIAL DELIVERY</span></div>
            <div className="envelope"><div className="envelope-lines" /><span className="envelope-label">TO. 올해도 열심히 산 너에게</span></div>
            <article className="result-ticket">
              <div className="ticket-top"><span>SANTA’S OFFICIAL REPORT</span><span className="example-label">예시 결과</span></div>
              <div className="ticket-heading"><div><p>2026 산타 자격 심사</p><h2>산타 친자 의혹</h2></div><div className="santa-icon"><SantaSeal /></div></div>
              <div className="ticket-score"><span>97.82</span><span className="score-unit">/ 100</span><span className="passed-stamp">산타컷<br /><b>PASS</b></span></div>
              <p className="verdict">“이 정도면 선물 말고 상속을 기다려라.”</p>
              <div className="ticket-perforation"><span /><span /></div>
              <div className="ticket-delivery"><div><span className="small-label">선물 도착 예정</span><strong>12월 25일 <span>오전 01:03</span></strong></div><span className="gift-icon" aria-hidden="true">✳</span></div>
              <div className="ticket-bottom"><span>수신인 &nbsp; 루돌프인턴 님</span><span className="barcode" aria-hidden="true" /></div>
            </article>
            <div className="art-caption"><span>↖</span> 올해의 나는 어느 쪽일까?</div>
            <span className="north-pole-tag">CHECKED TWICE. JUDGED ONCE.</span>
          </div>
        </section>

        <div className="ticker" aria-hidden="true"><div>기록은 거짓말을 안 하니까 <span>✳</span> 산타는 지금 심사 중 <span>✳</span> 선물에도 순서가 있다 <span>✳</span> 기록은 거짓말을 안 하니까 <span>✳</span> 산타는 지금 심사 중 <span>✳</span></div></div>

        <section className="how-section" id="how-it-works" aria-labelledby="how-title">
          <div className="section-intro"><p className="section-label">HOW TO GET YOUR GIFT</p><h2 id="how-title">선물까지, 딱 세 걸음.</h2><p>기록은 네가 준비해.<br />판정은 산타가 할게.</p></div>
          <div className="steps">{steps.map(([number, title, description]) => <article className="step" key={number}><span className="step-number">{number}</span><h3>{title}</h3><p>{description}</p></article>)}</div>
        </section>

        <section className="delivery-note"><span className="note-star" aria-hidden="true">✳</span><div><p className="section-label">A LITTLE NOTE FROM SANTA</p><h2>크리스마스 선물인데,<br className="mobile-break" /> 새해에 도착할 수도.</h2><p>산타도 하루 6시간만 일하거든요. 점수가 높은 순서대로 출발하니까,<br className="desktop-break" /> 내 순위에 따라 선물 도착 예정일도 달라져요.</p></div><span className="note-signature">From,<br /><b>Santa.</b></span></section>
      </main>
      <div className="start-dock"><button className="primary-button" onClick={() => noticeRef.current?.showModal()}>내 산타 점수 알아보기 <span aria-hidden="true">↗</span></button><p className="button-note">카톡 대화 · 인스타 활동 기록으로 참여해요</p></div>
      <footer className="site-footer"><a className="footer-brand" href="#">✳ 산타컷</a><p>기록으로 웃고, 판정은 가볍게.<br />오락용 AI 해석과 배송 시뮬레이션이며 실제 선물을 배송하지 않습니다.</p><span>© 2026 SANTA CUT</span></footer>
      <dialog ref={noticeRef} className="launch-notice" aria-labelledby="notice-title"><button className="close-button" aria-label="안내 닫기" onClick={() => noticeRef.current?.close()} autoFocus>×</button><span className="notice-star">✳</span><p className="section-label">A LETTER FROM SANTA</p><h2 id="notice-title">산타가 심사실을<br />준비하고 있어요.</h2><p>아직 심사는 시작되지 않았어요.<br />데이터 업로드와 AI 심사 기능은 준비 중입니다.</p><button className="primary-button" onClick={() => noticeRef.current?.close()}>알겠어요 <span>↗</span></button></dialog>
    </div>
  );
}
