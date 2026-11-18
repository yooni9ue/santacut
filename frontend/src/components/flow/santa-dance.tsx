"use client";

import { useEffect, useRef, useState } from "react";
import motion from "./neon-motion.json";
import centerMotion from "./center-motion.json";
import "./santa-dance.css";

type Point = [number, number];
// Motion tracks are sampled at 30fps and store x/y pairs in the joint order below.
const sourceFps = 30;
const rudolphEntranceFrame = 33;
const dances = {
  neon: { label: "니욘앤비욘", motion },
  center: { label: "체포하겠어", motion: [centerMotion, centerMotion] },
};
type Dance = keyof typeof dances;
const middle = (a: Point, b: Point): Point => [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2];

function Face({ rudolph }: { rudolph: boolean }) {
  return rudolph ? <>
    <path d="m-19-30-9-28m4 13-16-7m14 1 6-13m39 34 9-28m-4 13 16-7m-14 1-6-13" stroke="#79573e" strokeWidth="6" />
    <path d="M-23-14Q-50-15-45-31q24-1 28 18m38-1q27-1 22-17-24-1-28 18" fill="#bc8c65" />
    <path d="M-24-25q24-13 48 0l8 40q-7 31-32 30-25 0-33-30Z" fill="#bc8c65" />
    <ellipse cy="23" rx="25" ry="20" fill="#edcfaa" />
    <path d="m-19-4 9-3m21 0 9 3M-14 4v5m29-5v5" />
    <ellipse cy="16" rx="13" ry="10" fill="#d94636" /><path d="m-5 32 5 3 5-3" />
  </> : <>
    <path d="M-32-4q-5 41 32 53 36-12 33-53" fill="#fff" />
    <rect x="-31" y="-29" width="63" height="55" rx="24" fill="#f3cdb3" />
    <path d="m-21-8 9-3m26 3 9 1M-14-1v5m30-5v5" />
    <path d="M0 20q-14-15-24 2 14 9 24-2Zm0 0q14-15 24 2-14 9-24-2Z" fill="#fff" strokeWidth="2" />
    <ellipse cy="11" rx="9" ry="7" fill="#d95a48" />
    <path d="M-30-28q5-40 35-37 23 1 36 28l-17 9-15-18 10 24" fill="#d94636" />
    <circle cx="43" cy="-36" r="10" fill="#fff" />
    <path d="M-31-29q32-6 63 0l1 13q-32-5-65 0Z" fill="#fff" />
  </>;
}

function Limb({ points, color, width }: { points: Point[]; color: string; width: number }) {
  const line = points.map(point => point.join(",")).join(" ");
  return <>
    <polyline points={line} stroke="#303030" strokeWidth={width + 5} />
    <polyline points={line} stroke={color} strokeWidth={width} />
  </>;
}

function Whistle({ wrist, elbow }: { wrist: Point; elbow: Point }) {
  const angle = Math.atan2(wrist[1] - elbow[1], wrist[0] - elbow[0]) * 180 / Math.PI;
  return <g className="dance-whistle" transform={`translate(${wrist}) rotate(${angle})`}>
    <path d="M1-9h13c4-5 13-5 18 0 5 5 4 13-2 17-5 4-12 3-17-1H1Z" fill="#dce8f2" />
    <path d="M9-4h7" stroke="#52677d" />
    <circle cx="24" cy="-1" r="4" fill="#52677d" />
  </g>;
}

function Dancer({ pose, rudolph = false, opacity = 1, offsetX = 0, whistle = false }: {
  pose: number[]; rudolph?: boolean; opacity?: number; offsetX?: number; whistle?: boolean;
}) {
  const [nose, leftShoulder, rightShoulder, leftElbow, rightElbow, leftWrist, rightWrist, leftHip, rightHip, leftKnee, rightKnee, leftAnkle, rightAnkle] =
    Array.from({ length: 13 }, (_, index): Point => [pose[index * 2], pose[index * 2 + 1]]);
  const shoulder = middle(leftShoulder, rightShoulder);
  const hip = middle(leftHip, rightHip);
  const lean = Math.atan2(shoulder[0] - hip[0], hip[1] - shoulder[1]) * 180 / Math.PI;
  const headX = shoulder[0] + Math.max(-27, Math.min(27, nose[0] - shoulder[0]));
  const headY = shoulder[1] - Math.max(16, Math.min(55, shoulder[1] - nose[1]));
  const coat = rudolph ? "#bc8c65" : "#d94636";
  return <g className="dance-character" opacity={opacity} transform={`translate(${offsetX} 0)`}>
    <ellipse cx={hip[0]} cy="236" rx="55" ry="7" fill="#242424" opacity=".08" />
    <g stroke="#303030" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
      <Limb points={[leftHip, leftKnee, leftAnkle]} color={coat} width={21} />
      <Limb points={[rightHip, rightKnee, rightAnkle]} color={coat} width={21} />
      <ellipse cx={leftAnkle[0]} cy={leftAnkle[1]} rx="16" ry="9" fill="#303030" />
      <ellipse cx={rightAnkle[0]} cy={rightAnkle[1]} rx="16" ry="9" fill="#303030" />
      <Limb points={[shoulder, hip]} color={coat} width={rudolph ? 66 : 78} />
      {rudolph ? <path d={`M${shoulder.join(",")} L${hip.join(",")}`} stroke="#edcfaa" strokeWidth="28" /> : <>
        <path d={`M${shoulder.join(",")} L${hip.join(",")}`} stroke="#fff" strokeWidth="11" />
        <g transform={`translate(${hip}) rotate(${lean})`}><rect x="-38" y="-10" width="76" height="17" rx="7" fill="#303030" /><rect x="-9" y="-11" width="18" height="19" rx="3" fill="#deded9" /></g>
      </>}
      <g transform={`translate(${headX},${headY}) rotate(${Math.max(-35, Math.min(35, lean))})`}><Face rudolph={rudolph} /></g>
      <Limb points={[leftShoulder, leftElbow, leftWrist]} color={coat} width={18} />
      <Limb points={[rightShoulder, rightElbow, rightWrist]} color={coat} width={18} />
      <circle cx={leftWrist[0]} cy={leftWrist[1]} r="10" fill={rudolph ? "#79573e" : "#fff"} />
      {whistle && <Whistle wrist={rightWrist} elbow={rightElbow} />}
      <circle cx={rightWrist[0]} cy={rightWrist[1]} r="10" fill={rudolph ? "#79573e" : "#fff"} />
    </g>
  </g>;
}

export function SantaDance({ active = true }: { active?: boolean }) {
  const [dance, setDance] = useState<Dance>("neon");
  const [paused, setPaused] = useState(false);
  const [wild, setWild] = useState(false);
  const [frame, setFrame] = useState(0);
  const playhead = useRef(0);
  const track = dances[dance].motion;

  function selectDance(next: Dance) {
    if (dance === next) return;
    playhead.current = 0;
    setFrame(0);
    setDance(next);
  }

  useEffect(() => {
    if (!active || paused) return;
    const preference = window.matchMedia("(prefers-reduced-motion: reduce)");
    let request = 0;
    let last: number | undefined;
    function tick(now: number) {
      if (last !== undefined) playhead.current += Math.min(now - last, 100) * (wild ? 1.33 : 1);
      last = now;
      setFrame(Math.floor(playhead.current * sourceFps / 1000) % track[0].length);
      request = requestAnimationFrame(tick);
    }
    function updatePreference() {
      cancelAnimationFrame(request);
      last = undefined;
      if (!preference.matches) request = requestAnimationFrame(tick);
    }
    preference.addEventListener("change", updatePreference);
    updatePreference();
    return () => {
      preference.removeEventListener("change", updatePreference);
      cancelAnimationFrame(request);
    };
  }, [active, paused, track, wild]);

  return <section className="santa-dance" aria-label="산타와 루돌프의 댄스">
    {active && <div className="dance-selector" role="group" aria-label="춤 선택">
      <button type="button" aria-pressed={dance === "neon"} onClick={() => selectDance("neon")}>{dances.neon.label}</button>
      <button type="button" aria-pressed={dance === "center"} onClick={() => selectDance("center")}>{dances.center.label}</button>
    </div>}
    <svg className="dance-duo" viewBox="0 -188 570 474" fill="none" aria-hidden="true">
      {dance === "center" ? <g transform="translate(40 25) scale(.86)">
        <Dancer pose={track[0][frame]} offsetX={-139} whistle />
        <Dancer pose={track[0][frame]} rudolph offsetX={139} whistle />
      </g> : <>
        <Dancer pose={track[1][frame]} rudolph opacity={Math.max(0, Math.min(1, (frame - rudolphEntranceFrame) / 6))} />
        <Dancer pose={track[0][frame]} />
      </>}
    </svg>
    <p className="dance-note">{active ? "심사는 맡겨놓고 둘이 이러는 중." : "크흠. 아무 일도 없었습니다."}</p>
    {active && <div className="dance-controls">
      <button type="button" aria-pressed={wild} onClick={() => setWild(value => !value)}>{wild ? "진정해 얘들아" : "더 격하게"}</button>
      <button type="button" aria-pressed={paused} onClick={() => setPaused(value => !value)}>{paused ? "다시 춰" : "춤 잠깐 멈춰"}</button>
    </div>}
  </section>;
}
