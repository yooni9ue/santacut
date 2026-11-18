"use client";

import { useState } from "react";
import "./santa-dance.css";

function Dancer({ rudolph = false }: { rudolph?: boolean }) {
  const coat = rudolph ? "#bc8c65" : "#d94636";
  return <svg viewBox="0 0 180 255" fill="none" aria-hidden="true" className={rudolph ? "dance-character is-rudolph" : "dance-character is-santa"}>
    <ellipse cx="90" cy="239" rx="52" ry="6" fill="#242424" opacity=".08" />
    <g stroke="#303030" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
      <g className="dance-step dance-step-left"><path d="m73 190-13 35-16 4q-6 9 9 10h22l10-44" fill="#303030" /></g>
      <g className="dance-step dance-step-right"><path d="m100 193 8 35 19 2q9 9-7 10h-23l-12-44" fill="#303030" /></g>
      <g className="dance-upper">
        <path d="M64 121q-20 20-16 68 41 22 83 0 2-45-18-68Z" fill={coat} />
        {rudolph ? <><ellipse cx="90" cy="163" rx="22" ry="32" fill="#edcfaa" /><path d="m61 130 27 12 27-12-5 20H65Z" fill="#d94636" /><path d="m78 143 2 18 14-15" fill="#d94636" /></> : <><path d="M89 130v58" stroke="#fff" strokeWidth="12" /><path d="M50 175h78v15H50Z" fill="#303030" /><rect x="78" y="173" width="23" height="20" rx="2" fill="#deded9" /><path d="M85 180h9v7h-9Z" fill="#303030" stroke="none" /></>}
        {/* Head stays attached to shoulders throughout the dance. */}
        {rudolph ? <>
          <path d="m70 65-9-28m4 13-16-7m14 1 6-13m40 34 9-28m-4 13 16-7m-14 1-6-13" stroke="#79573e" strokeWidth="6" />
          <path d="M66 81Q39 80 44 64q24-1 28 18m38-1q27-1 22-17-24-1-28 18" fill="#bc8c65" />
          <path d="M65 70q24-13 48 0l8 40q-7 31-32 30-25 0-33-30Z" fill="#bc8c65" />
          <ellipse cx="89" cy="118" rx="25" ry="20" fill="#edcfaa" />
          <path d="m70 91 9-3m21 0 9 3M75 99v5m29-5v5" />
          <ellipse cx="89" cy="111" rx="13" ry="10" fill="#d94636" /><path d="m84 127 5 3 5-3" />
        </> : <>
          <path d="M57 91q-5 41 32 53 36-12 33-53" fill="#fff" />
          <rect x="58" y="66" width="63" height="55" rx="24" fill="#f3cdb3" />
          <path d="m68 87 9-3m26 3 9 1M75 94v5m30-5v5" />
          <path d="M89 115q-14-15-24 2 14 9 24-2Zm0 0q14-15 24 2-14 9-24-2Z" fill="#fff" strokeWidth="2" />
          <ellipse cx="89" cy="106" rx="9" ry="7" fill="#d95a48" />
          <path d="M59 67q5-40 35-37 23 1 36 28l-17 9-15-18 10 24" fill="#d94636" />
          <circle className="dance-pom" cx="132" cy="59" r="10" fill="#fff" />
          <path d="M58 66q32-6 63 0l1 13q-32-5-65 0Z" fill="#fff" />
        </>}
        <g className="dance-arm dance-arm-left"><path d="m62 131-18 26" stroke={coat} strokeWidth="20" /><g className="dance-forearm dance-forearm-left"><path d="m44 157-1 25" stroke={coat} strokeWidth="18" /><circle cx="43" cy="186" r="9" fill={rudolph ? "#79573e" : "#fff"} /></g></g>
        <g className="dance-arm dance-arm-right"><path d="m116 132 19 25" stroke={coat} strokeWidth="20" /><g className="dance-forearm dance-forearm-right"><path d="m135 157 1 25" stroke={coat} strokeWidth="18" /><circle cx="136" cy="186" r="9" fill={rudolph ? "#79573e" : "#fff"} /></g></g>
      </g>
    </g>
  </svg>;
}

export function SantaDance({ active = true }: { active?: boolean }) {
  const [paused, setPaused] = useState(false);
  const [wild, setWild] = useState(false);
  return <section className={`santa-dance ${active && !paused ? "is-dancing" : "is-still"} ${wild ? "is-wild" : ""}`} aria-label="산타와 루돌프의 니욘 앤 비욘 댄스">
    <div className="dance-caption" aria-hidden="true"><span>니욘</span><span>앤 비욘</span></div>
    <div className="dance-duo"><Dancer /><Dancer rudolph /></div>
    <p className="dance-note">{active ? "심사는 맡겨놓고 둘이 이러는 중." : "크흠. 아무 일도 없었습니다."}</p>
    {active && <div className="dance-controls"><button type="button" aria-pressed={wild} onClick={() => setWild(value => !value)}>{wild ? "진정해 얘들아" : "더 격하게"}</button><button type="button" aria-pressed={paused} onClick={() => setPaused(value => !value)}>{paused ? "다시 춰" : "춤 잠깐 멈춰"}</button></div>}
  </section>;
}
