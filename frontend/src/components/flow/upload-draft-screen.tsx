"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { extractSpeakers, validateChatFile } from "@/lib/chat-file";
import { FlowFrame, InfoNote, NextLink } from "./frame";
import { usePreview } from "./preview-context";

export function UploadDraftScreen() {
  const { data, update } = usePreview();
  const [error, setError] = useState("");
  const [reading, setReading] = useState(false);
  const version = useRef(0);
  useEffect(() => () => { version.current += 1; }, []);
  const format = data.source === "KAKAO" ? "TXT" : "JSON";
  async function selectFile(file?: File) {
    if (!file) return;
    const current = ++version.current;
    update({ file: null, fileName: "", speakers: [], speaker: "", uploadKey: "", sample: false });
    setError(""); setReading(true);
    try {
      validateChatFile(file, data.source);
      const speakers = extractSpeakers(new Uint8Array(await file.arrayBuffer()), data.source);
      if (current !== version.current) return;
      update({ file, fileName: file.name, speakers, speaker: "", uploadKey: crypto.randomUUID(), analysisId: null });
    } catch (failure) {
      if (current === version.current) setError(failure instanceof Error ? failure.message : "파일을 읽지 못했어요. 다시 선택해 주세요.");
    } finally { if (current === version.current) setReading(false); }
  }
  const ready = data.fileName && data.speaker && (data.sample || data.file) && !reading;
  return <FlowFrame live back="/prepare" step={0} title={<>증거물,<br />여기 놓으세요.</>} description="착한 척은 끝. 이제 기록 차례." action={ready ? <NextLink href="/join">이 파일로 계속</NextLink> : <button className="flow-primary" disabled>{reading ? "파일을 읽고 있어요" : data.fileName ? "본인 화자를 선택해 주세요" : "파일을 먼저 선택해 주세요"}</button>}>
    <label className={`flow-upload ${data.fileName ? "has-file" : ""}`}><input type="file" aria-label="분석 파일 선택" accept={data.source === "KAKAO" ? ".txt" : ".json"} onChange={event => { const file = event.target.files?.[0]; event.target.value = ""; void selectFile(file); }} /><span className="flow-upload-icon" aria-hidden="true">{data.fileName ? "✓" : "↑"}</span><b>{data.fileName || "파일 선택하기"}</b><small>{data.fileName ? "다시 누르면 파일을 바꿀 수 있어요" : `${format} / 최대 20MB`}</small></label>
    {reading && <p role="status">브라우저에서 본인 화자 목록을 확인하고 있어요. 아직 서버로 보내지 않아요.</p>}
    {error && <p className="flow-error" role="alert">{error}</p>}
    <button className="flow-sample-button" onClick={() => { version.current++; setReading(false); setError(""); update({ sample: true, file: null, speaker: "나", speakers: ["나", "루돌프"], uploadKey: "", analysisId: null, fileName: data.source === "KAKAO" ? "산타와의_대화_예시.txt" : "인스타_활동_예시.json" }); }}>파일 없이 예시로 둘러보기 →</button>
    {data.fileName && !reading && <div className="flow-field"><label htmlFor="speaker">이 대화에서 나는? {data.sample && <span>선택 화면 예시</span>}</label><select id="speaker" value={data.speaker} onChange={event => update({ speaker: event.target.value, uploadKey: crypto.randomUUID() })}><option value="">본인 이름 선택</option>{data.speakers.map(speaker => <option key={speaker} value={speaker}>{speaker}</option>)}</select><p>두 플랫폼 모두 대화 속 본인 이름을 직접 골라 주세요. 다른 사람의 메시지는 심사 대상에서 제외돼요.</p></div>}
    <InfoNote>카카오톡은 UTF-8 대화 TXT, 인스타그램은 압축을 푼 message_1.json을 선택해 주세요. 등록과 마지막 전송 동의 전에는 파일을 서버로 보내지 않아요. 새로고침하면 파일을 다시 골라야 해요.</InfoNote>
    <Link className="flow-text-link" href="/analysis">이미 낸 심사 상태 확인하기 ↗</Link>
  </FlowFrame>;
}
