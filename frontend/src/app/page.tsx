"use client";

import { useState } from "react";
import Link from "next/link";

function StuckSanta({ freed }: { freed: boolean }) {
  return (
    <svg viewBox="0 0 380 310" fill="none" role="img" aria-label={freed ? "굴뚝에서 상반신을 쑥 빼낸 산타" : "배가 굴뚝에 끼어 양손으로 가장자리를 짚은 산타"}>
      <ellipse cx="187" cy="283" rx="125" ry="10" fill="#242424" opacity=".07" />
      <ellipse cx="180" cy="181" rx="133" ry="96" fill="#f5f5f3" />
      <path d="m57 151 12 3m-8-19 10 8m228 18 13-4m-13 19 15 1" stroke="#a8a8a2" strokeWidth="2" strokeLinecap="round" />

      {/* Head, shoulders and arms share one group, including during rescue. */}
      <g className={freed ? "toonsanta-freed" : "toonsanta-stuck"} stroke="#303030" strokeWidth="2.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M139 144c-18 0-31 14-39 35l-6 19 25 9 14-26m76-37c19 0 33 14 41 35l7 19-25 9-15-26" fill="#d94636" />
        <path d="M143 137c-21 10-27 31-27 64v26h118v-26c0-34-7-54-29-64Z" fill="#df4937" />
        <path d="M173 154v47" stroke="#fff" strokeWidth="11" />
        <path d="M117 202h116v16H117Z" fill="#303030" />
        <rect x="163" y="200" width="24" height="21" rx="3" fill="#dddcd8" />
        <rect x="170" y="206" width="10" height="9" rx="1" fill="#303030" stroke="none" />
        <path d="M133 108c-7 11-5 34 5 46 9 11 23 19 36 22 17-5 31-14 39-29 6-13 7-30-1-41Z" fill="#fff" />
        <ellipse cx="137" cy="106" rx="7" ry="10" fill="#efc1a4" />
        <ellipse cx="211" cy="106" rx="7" ry="10" fill="#efc1a4" />
        <path d="M140 86c12-14 56-13 68 0v28c0 17-15 28-34 28-18 0-34-11-34-28Z" fill="#f3cdb3" />
        <path d="m146 95 11-3m36 3 9 2" strokeWidth="3" />
        <path d="M153 104v5m43-4v5" strokeWidth="3.5" />
        <ellipse cx="148" cy="116" rx="6" ry="4" fill="#e7a28c" stroke="none" />
        <ellipse cx="200" cy="116" rx="6" ry="4" fill="#e7a28c" stroke="none" />
        <path d="M174 126c-8-9-20-5-24 5 8 6 18 4 24-5Zm0 0c8-9 20-5 24 5-8 6-18 4-24-5Z" fill="#fff" strokeWidth="2" />
        <ellipse cx="174" cy="119" rx="10" ry="8" fill="#d95a48" strokeWidth="2" />
        <path d="M169 143q5 3 10 0" strokeWidth="2" />
        <path d="M138 80c4-24 18-46 40-46 21 0 34 15 41 30l-17 7-12-14 12 24Z" fill="#df4937" />
        <circle cx="220" cy="67" r="10" fill="#fff" />
        <path d="M139 78c18-5 50-5 69 0 4 1 6 5 5 9-1 4-5 6-9 5-21-4-42-4-61 0-5 1-9-2-10-6-1-4 2-7 6-8Z" fill="#fff" />
        <path d="m103 185 19 6m107 0 19-6" stroke="#fff" strokeWidth="10" />
        <path d="M105 190c-8-4-15 0-16 7-1 7 4 12 12 12h14c6 0 10-5 8-10-1-4-5-7-9-7l-2-5c-2-4-7-2-7 3Z" fill="#fff" />
        <path d="M244 190c8-4 15 0 16 7 1 7-4 12-12 12h-14c-6 0-10-5-8-10 1-4 5-7 9-7l2-5c2-4 7-2 7 3Z" fill="#fff" />
      </g>

      <path d="M105 218h144v58H105Z" fill="#deded9" stroke="#303030" strokeWidth="2.8" />
      <path d="M106 241h142m-142 22h142m-97-45v23m58-23v23m-86 0v22m58-22v22m-28 0v13m57-13v13" stroke="#81817a" strokeWidth="1.8" />
      <rect x="93" y="209" width="168" height="17" rx="3" fill="#f2f2ef" stroke="#303030" strokeWidth="2.8" />
      <path d="M96 209c12-6 18 2 29-1s17 2 29-1 17 2 29 0 18 3 30-1 18 2 29 0 13 2 16 3" stroke="#fff" strokeWidth="6" strokeLinecap="round" />
      <path d="m304 231 4-9 3 9 9 2-8 5-1 9-6-7-9 1 5-7Z" fill="#fff" stroke="#a8a8a2" strokeWidth="1.5" />
    </svg>
  );
}

const examples = [
  { number: "97.82", name: "산타 친자 의혹", comment: "선물 말고 상속을 기다려라.", badge: "족보 확인 중", face: "◉‿◉", kind: "good" },
  { number: "61.09", name: "굴뚝 앞 배송 보류", comment: "주소는 맞는데… 마음의 준비가 안 됨.", badge: "문 앞에서 고민 중", face: "•_•", kind: "mid" },
  { number: "38.24", name: "새해 복 많이 받아라", comment: "선물이 해를 넘김. 아무튼 선물임.", badge: "내년에 뵙겠습니다", face: "ಠ_ಠ", kind: "late" },
];

export default function Home() {
  const [freed, setFreed] = useState(false);
  const [sampleIndex, setSampleIndex] = useState(0);
  const result = examples[sampleIndex];

  return (
    <div className="toon-app">
      <header className="toon-header"><a href="#" className="toon-logo" aria-label="산타컷 홈">산타<span>컷</span><i aria-hidden="true">✳</i></a><span className="toon-season">CHRISTMAS, 2026</span><Link href="#toon-guide-title" className="toon-version">이용 방법 ↗</Link></header>
      <main>
        <section className="toon-hero" aria-labelledby="toon-title">
          <div className="toon-pretitle"><span aria-hidden="true">●</span> 산타의 아주 주관적인 크리스마스</div>
          <h1 id="toon-title">올해도<br /><span className="toon-title-red">착한 척</span> 했니<span className="toon-question">?</span></h1>
          <p className="toon-subtitle">괜찮아. 산타도 일하는 척해.</p>
          <div className="toon-scene">
            <span className="toon-scene-label">SANTA IS NOT READY.</span>
            <div className="toon-speech" aria-live="polite">{freed ? <>휴…<br />아무 일도 없었다.</> : <>너 심사하기 전에<br /><b>나 좀 꺼내줘.</b></>}</div>
            <StuckSanta freed={freed} />
            <span className="toon-sound" aria-hidden="true">{freed ? "쑤욱!" : "버둥 버둥"}</span>
            <button className="toon-rescue" onClick={() => setFreed(!freed)} aria-pressed={freed}>{freed ? "다시 끼우기 ↶" : "산타 꺼내주기 ↑"}</button>
          </div>
          <div className="toon-hero-foot"><p>카톡·인스타 기록으로 받는<br /><strong>산타 점수 & 선물 도착 예정일</strong></p><span className="toon-tiny-note">산타 꺼내줘도<br />가산점 없음 ↗</span></div>
        </section>

        <div className="toon-marquee" aria-hidden="true"><span>울면 안 돼</span><span>✳ 웃기면 됨</span><span>선물은 성적순</span></div>

        <section className="toon-results" aria-labelledby="toon-result-title">
          <div className="toon-section-kicker">나한테는 뭐라고 할까?</div>
          <h2 id="toon-result-title">산타 입에서 나오는<br /><em>뜻밖의 소리들.</em></h2>
          <div className={`toon-example toon-example-${result.kind}`}>
            <div className="toon-example-top"><span>이런 결과가 나올 수 있어요</span><span className="toon-sample-badge">예시</span></div>
            <div className="toon-example-face" aria-hidden="true">{result.face}</div>
            <span className="toon-result-tag">{result.badge}</span>
            <h3>{result.name}</h3>
            <p className="toon-result-score">{result.number}<span>점</span></p>
            <p className="toon-result-comment">“{result.comment}”</p>
            <div className="toon-example-bottom"><span aria-live="polite">판정 예시 {sampleIndex + 1} / 3</span><button onClick={() => setSampleIndex((sampleIndex + 1) % examples.length)}>다음 헛소리 듣기 <span aria-hidden="true">↻</span></button></div>
          </div>
          <p className="toon-example-disclaimer">예시입니다. 실제 점수와 판정은 기록 분석 후 정해져요.</p>
        </section>

        <section className="toon-guide" aria-labelledby="toon-guide-title"><div className="toon-section-kicker">방법은 의외로 정상</div><h2 id="toon-guide-title">너는 파일만.<br />산타는 <span>판정만.</span></h2><ol><li><span>1</span><div><h3>올해의 기록 가져오기</h3><p>카톡 대화나 지원되는 인스타 활동 파일.<br />입으로 착했다고 하는 건 안 쳐줌.</p></div><i aria-hidden="true">↗</i></li><li><span>2</span><div><h3>AI 산타의 한마디 듣기</h3><p>기록을 읽고 점수와 판정을 만들어요.<br />친구랑 웃자고 하는 거니까, 가볍게.</p></div><i aria-hidden="true">✳</i></li><li><span>3</span><div><h3>선물 언제 오나 보기</h3><p>점수 순서대로 계산되는 도착 예정일.<br />루돌프가 퇴근하면 다음 날 다시 감.</p></div><i aria-hidden="true">→</i></li></ol></section>

        <aside className="toon-rudolph"><span className="toon-rudolph-face" aria-hidden="true">( •̀ ᴥ •́ )</span><p>“하루 6시간 넘으면<br /><strong>안 뜁니다.”</strong></p><span>— 루돌프, 워라밸 지킴이</span><div>산타는 하루 6시간, 점수 순서대로 이동해요.<br />도착 예정일이 내년이어도 계산 오류는 아닙니다.</div></aside>
      </main>
      <footer className="toon-footer"><strong>산타컷 ✳</strong><p>진지함은 내려놓고, 판정은 가볍게.<br />오락용 AI 해석과 배송 시뮬레이션입니다.<br />실제 선물은 안 옵니다. 이건 진짜임.</p><nav aria-label="이전 디자인"><Link href="/classic">우체국 버전</Link><Link href="/original">손그림 버전</Link><Link href="/post-office">심사국 버전</Link></nav><span>© 2026 SANTA CUT</span></footer>
      <div className="toon-dock"><Link className="toon-start" href="/start">그래서 나 선물 받냐?<span aria-hidden="true">↗</span></Link><p>화면 미리보기 · 파일 전송과 AI 심사는 준비 중</p></div>
    </div>
  );
}
