"use client";

import { useRef, useState } from "react";
import Link from "next/link";

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

const sampleReports = [
  { score: "97.82", title: "산타 친자 의혹", verdict: "이 정도면 선물 말고 상속을 기다려라.", date: "12월 25일", time: "오전 01:03", stamp: "족보 확인", tag: "호적 한번 떼봐라" },
  { score: "61.09", title: "굴뚝 앞 배송 보류", verdict: "산타가 주소를 다시 확인 중. 모르는 척 아님.", date: "12월 31일", time: "오전 05:42", stamp: "일단 보류", tag: "굴뚝 앞에서 서성이는 중" },
  { score: "38.24", title: "선물 대신 부재중", verdict: "크리스마스 선물인데 새해 복 많이 받아라.", date: "2027년 1월 8일", time: "오전 03:12", stamp: "내년 도착", tag: "선물이 연휴를 즐기는 중" },
];

const steps = [
  ["01", "증거물을 제출한다", "카톡 대화나 지원되는 인스타 활동 파일을 준비. 변명은 파일에 안 들어갑니다."],
  ["02", "산타가 돋보기를 든다", "AI가 기록을 읽고 점수와 판정을 만들어요. 돋보기는 분위기용입니다."],
  ["03", "택배보다 긴장되는 조회", "점수 순서대로 계산되는 선물 도착일. 늦으면 친구에게 같이 웃어달라고 공유해요."],
];

export default function Home() {
  const noticeRef = useRef<HTMLDialogElement>(null);
  const [sampleIndex, setSampleIndex] = useState(0);
  const report = sampleReports[sampleIndex];
  return (
    <div className="site-shell chaos-theme">
      <div className="airmail-border" />
      <header className="site-header">
        <a className="wordmark" href="#" aria-label="산타컷 홈"><span className="brand-star">✳</span> 산타컷<span className="wordmark-en">SANTA CUT</span></a>
        <Link href="/original" className="header-link">순한맛 보기 <span>↗</span></Link>
      </header>

      <main>
        <section className="hero" aria-labelledby="hero-title">
          <div className="hero-copy">
            <div className="eyebrow"><span /> 북극 선물 자격 심사국 <span className="edition">제2026-호</span></div>
            <div className="invitation">[출석요구서] 귀하의 기록이 궁금합니다.</div>
            <h1 id="hero-title">너...<br /><span className="title-red">선물 받을</span><br />자격 <span className="title-outline">있어</span><span className="question-mark">?</span></h1><span className="chaos-scribble">산타: 일단 앉아봐.</span>
            <p className="hero-description">착하게 살았다는 말, 일단 접수.<br />AI 산타가 기록 보고 다시 연락드림.</p>

          </div>

          <div className="hero-art" aria-label="산타 심사 결과 예시"><span className="chaos-evidence">증거물 A-1225</span>
            <span className="art-spark spark-one">✦</span><span className="art-spark spark-two">✧</span>
            <div className="postmark"><span>북극 심사국</span><strong>극비<br />아님</strong><span>친구한테 보여줘도 됨</span></div>
            <div className="envelope"><div className="envelope-lines" /><span className="envelope-label">TO. 본인은 착하다고 주장하는 자</span></div>
            <article className="result-ticket">
              <div className="ticket-top"><span>선물 지급 적격성 검토서</span><span className="example-label">예시 결과</span></div>
              <div className="ticket-heading"><div><p>심사 결과: {report.tag}</p><h2>{report.title}</h2></div><div className="santa-icon"><SantaSeal /></div></div>
              <div className="ticket-score"><span>{report.score}</span><span className="score-unit">/ 100점</span><span className="passed-stamp">담당 산타<br /><b>{report.stamp}</b></span></div>
              <p className="verdict">“{report.verdict}”</p>
              <div className="ticket-perforation"><span /><span /></div>
              <div className="ticket-delivery"><div><span className="small-label">선물 도착 예정</span><strong>{report.date} <span>{report.time}</span></strong></div><span className="gift-icon" aria-hidden="true">✳</span></div>
              <div className="ticket-bottom"><span>수신인 &nbsp; 루돌프인턴 님</span><span className="barcode" aria-hidden="true" /></div>
            </article>
            <button className="art-caption" onClick={() => setSampleIndex((sampleIndex + 1) % sampleReports.length)}><span aria-hidden="true">↖</span> 다른 판정 예시 까보기 ↻</button><p className="chaos-sample-status" aria-live="polite">예시 {sampleIndex + 1} / {sampleReports.length} · 실제 분석 결과가 아닙니다</p>

          </div>
        </section>

        <div className="ticker" aria-hidden="true"><div>산타도 주 52시간 <span>✳</span> 루돌프 노조 협의 완료 <span>✳</span> 착한 척 서류로 제출 <span>✳</span> 산타도 주 52시간 <span>✳</span> 루돌프 노조 협의 완료 <span>✳</span></div></div>

        <section className="how-section" id="how-it-works" aria-labelledby="how-title">
          <div className="section-intro"><p className="section-label">선물 타는 법 (일단 읽어)</p><h2 id="how-title">절차는 제법 멀쩡함.</h2><p>준비물: 내 기록.<br />마음의 준비: 알아서.</p></div>
          <div className="steps">{steps.map(([number, title, description]) => <article className="step" key={number}><span className="step-number">{number}</span><h3>{title}</h3><p>{description}</p></article>)}</div>
        </section>

        <section className="delivery-note"><span className="note-star" aria-hidden="true">✳</span><div><p className="section-label">긴급 공지 / 루돌프 노사 합의</p><h2>크리스마스 선물인데,<br className="mobile-break" /> 새해에 도착할 수도.</h2><p>루돌프가 워라밸을 선언했습니다. 하루 6시간 운행.<br className="desktop-break" /> 점수 높은 순으로 가니까, 선물이 해를 넘길 수도 있어요.</p></div><span className="note-signature">From,<br /><b>Santa.</b></span></section>
      </main>
      <div className="start-dock"><button className="primary-button" onClick={() => noticeRef.current?.showModal()}>산타야 나 파일 좀 봐봐 <span aria-hidden="true">↗</span></button><p className="button-note">AI 심사 준비 중 · 실제 선물은 안 옴 (중요)</p></div>
      <footer className="site-footer"><a className="footer-brand" href="#">✳ 산타컷</a><p>기록으로 웃고, 판정은 가볍게.<br />오락용 AI 해석과 배송 시뮬레이션이며 실제 선물을 배송하지 않습니다.</p><span>© 2026 SANTA CUT</span></footer>
      <dialog ref={noticeRef} className="launch-notice" aria-labelledby="notice-title"><button className="close-button" aria-label="안내 닫기" onClick={() => noticeRef.current?.close()} autoFocus>×</button><span className="notice-star">✳</span><p className="section-label">A LETTER FROM SANTA</p><h2 id="notice-title">산타 지금<br />출근 안 했는데요.</h2><p>지금은 디자인 미리보기예요.<br />파일 업로드와 AI 심사는 준비 중입니다.<br />산타한테 파일은 아직 안 갑니다.</p><button className="primary-button" onClick={() => noticeRef.current?.close()}>출근하면 다시 올게 <span>↗</span></button></dialog>
    </div>
  );
}
