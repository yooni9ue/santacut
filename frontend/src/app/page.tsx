"use client";

import { useState } from "react";
import Link from "next/link";
import { StuckSanta } from "@/components/stuck-santa";


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
