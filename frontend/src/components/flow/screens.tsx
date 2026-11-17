"use client";

import Link from "next/link";
import { UploadDraftScreen } from "./upload-draft-screen";
import { RegistrationScreen } from "./registration-screen";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { FlowFrame, InfoNote, NextLink } from "./frame";
import { sourceName, usePreview, type Source } from "./preview-context";
import { analysisError, analysisFailure, getAnalysis, getLatestAnalysis, isFinished, uploadAnalysis, watchAnalysis, type AnalysisStatus } from "@/lib/analyses";
import { getParticipant } from "@/lib/participants";
import { createShare, formatKoreanDate, formatScore, getMyResult, resultError } from "@/lib/results";
import { getRankings, rankingError, type RankingRow } from "@/lib/rankings";

export function StartScreen() {
  const { update } = usePreview();
  const router = useRouter();
  function choose(source: Source) {
    update({ source, fileName: "", file: null, speaker: "", speakers: [], uploadKey: "", analysisId: null, sample: false });
    router.push("/prepare");
  }
  return <FlowFrame step={0} title={<>산타한테 뭘<br />보여줄 건데?</>} description="말로만 착했다고 하면 산타도 곤란해.">
    <div className="flow-source-list">
      <button className="flow-source" onClick={() => choose("KAKAO")}><span className="flow-source-icon">K</span><span><b>카카오톡 대화</b><small>내 말투와 대화 속 행동으로 심사받기</small><em>TXT · JSON</em></span><i>↗</i></button>
      <button className="flow-source" onClick={() => choose("INSTAGRAM")}><span className="flow-source-icon flow-instagram">◎</span><span><b>인스타그램 활동</b><small>내보낸 활동 기록으로 심사받기</small><em>JSON</em></span><i>↗</i></button>
    </div>
    <InfoNote>파일 전송과 분석은 아직 예시예요. 참가 정보는 실제로 등록하고 확인할 수 있어요.</InfoNote>
    <Link className="flow-text-link" href="/join" onClick={() => update({ sample: false })}>참가 정보 먼저 등록·확인하기 ↗</Link>
    <div className="flow-side-note"><span aria-hidden="true">( •_• )</span><p>하나만 내도 돼.<br />두 개 낸다고 선물 두 개 아님.</p></div>
  </FlowFrame>;
}

export function PrepareScreen() {
  const { data } = usePreview();
  const isKakao = data.source === "KAKAO";
  const instructions = isKakao ? [
    ["심사받을 대화를 골라요", "내가 참여한 대화방의 기록을 준비해 주세요."],
    ["대화 내용을 내보내요", "기기별 내보내기 메뉴에서 대화 파일을 저장해요. 자세한 안내는 출시 전에 제공할게요."],
    ["파일을 가져와요", "미리보기에서는 TXT 또는 JSON 파일의 이름과 크기만 확인해요."],
  ] : [
    ["내 정보 내보내기를 찾아요", "인스타그램 계정 센터의 내 정보 내보내기를 이용해 주세요."],
    ["JSON 형식으로 준비해요", "파일 생성까지 기다려야 할 수 있어요. 평가 가능한 활동 범위는 검증 후 안내할게요."],
    ["준비된 파일을 가져와요", "현재 화면에서는 파일을 전송하거나 내용을 분석하지 않아요."],
  ];
  return <FlowFrame back="/start" step={0} title={<>기록 좀<br />꺼내볼까요.</>} description={`${sourceName(data.source)} 파일을 준비해 주세요.`} action={<NextLink href="/upload">파일 준비됐어요</NextLink>}>
    <div className="flow-guide-top"><span>{isKakao ? "K" : "◎"}</span><div><b>{sourceName(data.source)}</b><p>파일을 이미 갖고 있다면 바로 넘어가도 돼요.</p></div></div>
    <ol className="flow-guide-list">{instructions.map(([title, detail], index) => <li key={title}><b>{String(index + 1).padStart(2, "0")}</b><div><h2>{title}</h2><p>{detail}</p></div></li>)}</ol>
    <InfoNote>파일은 최대 20MB까지 선택할 수 있어요. 이 미리보기에서는 파일이 서버로 전송되지 않아요.</InfoNote>
    <Link className="flow-text-link" href="/start">다른 기록으로 바꾸기 ↗</Link>
  </FlowFrame>;
}

export function UploadScreen() {
  const { data } = usePreview();
  return <UploadDraftScreen key={data.source} />;
}

const districts: Record<string, string[]> = { 서울: ["강남구", "마포구", "서초구", "송파구", "종로구"], 경기: ["성남시", "수원시", "고양시"], 부산: ["해운대구", "수영구", "동래구"] };

export function JoinScreen() {
  const { data } = usePreview();
  return data.sample ? <SampleJoinScreen /> : <RegistrationScreen />;
}

function SampleJoinScreen() {
  const { data, update } = usePreview();
  const [error, setError] = useState("");
  const router = useRouter();
  function submit(event: React.FormEvent) {
    event.preventDefault();
    const nickname = data.nickname.normalize("NFKC").trim();
    if (!/^[가-힣a-zA-Z0-9_]{2,16}$/.test(nickname)) { setError("닉네임은 한글·영문·숫자·밑줄로 2~16자 입력해 주세요."); return; }
    update({ nickname }); router.push("/review");
  }
  return <FlowFrame back="/upload" step={1} title={<>선물 받을 분,<br />성함이?</>} description="실명 말고 닉네임. 산타도 사생활은 지켜.">
    <form id="participant-form" onSubmit={submit}>
      <div className="flow-field"><label htmlFor="nickname">닉네임</label><input id="nickname" value={data.nickname} maxLength={32} placeholder="예: 루돌프인턴" autoComplete="nickname" aria-invalid={Boolean(error)} aria-describedby={error ? "nickname-error" : "nickname-help"} onChange={(event) => { update({ nickname: event.target.value }); setError(""); }} />{error ? <p className="flow-error" id="nickname-error" role="alert">{error}</p> : <p id="nickname-help">2~16자 · 한글, 영문, 숫자, 밑줄. 중복 확인은 실제 등록 시 제공돼요.</p>}</div>
      <div className="flow-field"><label htmlFor="province">사는 지역 <span>선택 화면 예시</span></label><div className="flow-select-row"><select id="province" aria-label="시·도" value={data.province} onChange={(event) => update({ province: event.target.value, district: districts[event.target.value][0] })}>{Object.keys(districts).map((province) => <option key={province}>{province}</option>)}</select><select aria-label="시·군·구" value={data.district} onChange={(event) => update({ district: event.target.value })}>{districts[data.province].map((district) => <option key={district}>{district}</option>)}</select></div><p>상세 주소는 받지 않아요. 지금은 일부 지역을 예시로 보여드려요.</p></div>
      <InfoNote>닉네임은 결과·랭킹에 표시되고, 지역은 도착일 계산에 사용돼요. 미리보기 입력은 서버에 등록되지 않아요.</InfoNote>
      <div className="flow-action"><button className="flow-primary" type="submit">다음 <span aria-hidden="true">↗</span></button></div>
    </form>
  </FlowFrame>;
}

export function ReviewScreen() {
  const { data } = usePreview();
  return data.sample ? <SampleReviewScreen /> : <LiveReviewScreen />;
}

function SampleReviewScreen() {
  const { data } = usePreview();
  const [agreed, setAgreed] = useState(false);
  return <FlowFrame back="/join" step={2} title={<>마지막 확인.<br />변명은 지금 해.</>} description="산타에게 보여줄 내용을 확인해 주세요." action={<NextLinkGuard enabled={agreed} href="/analysis">심사 화면 보기</NextLinkGuard>}>
    <dl className="flow-summary"><div><dt>참가자</dt><dd>{data.nickname || "루돌프인턴"}<Link href="/join">수정</Link></dd></div><div><dt>거주 지역</dt><dd>{data.province} {data.district}</dd></div><div><dt>기록 출처</dt><dd>{sourceName(data.source)}</dd></div><div><dt>파일</dt><dd className="flow-filename">{data.fileName || "대화_예시.txt"}<Link href="/upload">변경</Link></dd></div>{data.source === "KAKAO" && <div><dt>본인 화자</dt><dd>{data.speaker}</dd></div>}</dl>
    <div className="flow-privacy"><h2>산타도 지킬 건 지켜요.</h2><ul><li>대화 원문과 상대방 이름은 결과에 공개하지 않아요.</li><li>닉네임·점수·순위·판정이 공개되는 구성이에요.</li><li>재미용 판정이며 실제 선물을 배송하지 않아요.</li></ul></div>
    <label className="flow-checkbox"><input type="checkbox" checked={agreed} onChange={(event) => setAgreed(event.target.checked)} /><span>미리보기임을 확인하고 예시 심사 화면을 볼게요.</span></label>
    <p className="flow-fine-print">실제 분석 동의나 파일 전송은 발생하지 않아요. 운영용 분석·보관 정책은 기능 출시 시 별도로 안내합니다.</p>
  </FlowFrame>;
}

function LiveReviewScreen() {
  const { data, update } = usePreview();
  const [agreed, setAgreed] = useState(false);
  const [consent, setConsent] = useState(false);
  const [participant, setParticipant] = useState<{ nickname: string; regionCode: string } | null>(null);
  const [error, setError] = useState("");
  const [progress, setProgress] = useState(0);
  const [sending, setSending] = useState(false);
  const router = useRouter();
  useEffect(() => { let active = true; getParticipant().then(value => { if (active) setParticipant(value); }).catch(() => { if (active) setError("참가 정보를 확인하지 못했어요. 다시 시도해 주세요."); }); return () => { active = false; }; }, []);
  const ready = Boolean(participant && data.file && data.speaker && data.uploadKey && agreed && consent && !sending);
  async function submit() {
    if (!ready || !data.file) return;
    setSending(true); setError(""); setProgress(0);
    try {
      const result = await uploadAnalysis({ file: data.file, source: data.source, speaker: data.speaker, key: data.uploadKey }, { onProgress: setProgress });
      update({ analysisId: result.id, file: null, speakers: [], speaker: "" });
      router.push("/analysis");
    } catch (failure) { setError(analysisError(failure)); setSending(false); }
  }
  return <FlowFrame back="/join" step={2} title={<>마지막 확인.<br />이번엔 진짜 전송해.</>} description="동의한 뒤에만 파일을 심사 서버로 보냅니다." action={<button className="flow-primary" disabled={!ready} onClick={submit}>{sending ? `전송 중 ${progress}%` : "파일 전송하고 심사 시작"}<span aria-hidden="true">↗</span></button>}>
    {!participant && !error && <p role="status">참가 정보를 확인하고 있어요.</p>}
    <dl className="flow-summary"><div><dt>참가자</dt><dd>{participant?.nickname ?? (data.nickname || "확인 중")}</dd></div><div><dt>거주 지역</dt><dd>{data.province} {data.district}</dd></div><div><dt>기록 출처</dt><dd>{sourceName(data.source)}</dd></div><div><dt>파일</dt><dd className="flow-filename">{data.fileName || "파일이 없어요"}</dd></div><div><dt>본인 화자</dt><dd>{data.speaker || "선택하지 않음"}</dd></div></dl>
    <div className="flow-privacy"><h2>전송 전에 꼭 읽어줘요.</h2><ul><li>선택한 본인 화자의 메시지만 분석해요.</li><li>원문과 상대방 이름은 검증 후 최대 24시간 안에 삭제해요.</li><li>닉네임·점수·순위는 공개될 수 있어요.</li></ul></div>
    <label className="flow-checkbox"><input type="checkbox" checked={agreed} onChange={event => setAgreed(event.target.checked)} /><span>이 파일에서 선택한 화자가 나라는 것을 확인했어요.</span></label>
    <label className="flow-checkbox"><input type="checkbox" checked={consent} onChange={event => setConsent(event.target.checked)} /><span>파일을 전송하고 AI 심사를 받는 데 동의해요.</span></label>
    {error && <p className="flow-error" role="alert">{error}</p>}
    {!data.file && <Link className="flow-text-link" href="/upload">파일을 다시 선택하기 ↗</Link>}
  </FlowFrame>;
}

function NextLinkGuard({ enabled, href, children }: { enabled: boolean; href: string; children: React.ReactNode }) {
  return enabled ? <NextLink href={href}>{children}</NextLink> : <button className="flow-primary" disabled>{children}<span aria-hidden="true">↗</span></button>;
}

export function AnalysisScreen() {
  const { data } = usePreview();
  return data.sample || (!data.analysisId && !data.fileName) ? <SampleAnalysisScreen /> : <LiveAnalysisScreen />;
}

function SampleAnalysisScreen() {
  const [status, setStatus] = useState("analyzing");
  const failed = status === "failed" || status === "insufficient";
  const title = status === "queued" ? "산타 앞에 줄 서는 중." : status === "failed" ? "산타가 잠깐 멈췄어요." : status === "insufficient" ? "기록이 조금 더 필요해요." : "산타가 기록을 읽는 중.";
  return <FlowFrame back="/review" step={2} title={<>판정 중에는<br />얌전히 계세요.</>} description="이 화면은 심사 상태를 보여주는 예시예요." action={failed ? <button className="flow-primary" onClick={() => setStatus("analyzing")}>다시 시도하는 화면 보기 <span aria-hidden="true">↻</span></button> : <NextLink href="/result">예시 결과 보기</NextLink>}>
    <div className={`flow-analysis ${failed ? "is-failed" : ""}`}><span className="flow-analysis-face" aria-hidden="true">{failed ? "( •_• )" : "( ◉_◉ )"}</span><span className="flow-status-pill">상태 예시</span><h2 aria-live="polite">{title}</h2><p>{status === "insufficient" ? "근거가 부족하면 낮은 점수를 주는 대신 다른 기록을 요청해요." : status === "failed" ? "일시적인 오류예요. 이전에 받은 정상 결과는 유지돼요." : "진짜 분석은 아직 시작되지 않았어요. 아래에서 결과 화면을 미리 확인해 보세요."}</p></div>
    <div className="flow-state-preview"><p>다른 심사 상태 보기</p><div>{[["queued", "대기"], ["analyzing", "심사 중"], ["failed", "오류"], ["insufficient", "근거 부족"]].map(([value, label]) => <button key={value} aria-pressed={status === value} onClick={() => setStatus(value)}>{label}</button>)}</div></div>
    {failed && <Link href="/upload" className="flow-text-link">다른 파일 준비하기 ↗</Link>}
  </FlowFrame>;
}

function LiveAnalysisScreen() {
  const { data } = usePreview();
  const [status, setStatus] = useState<AnalysisStatus | null>(null);
  const [error, setError] = useState("");
  const [fallback, setFallback] = useState(false);
  const [jobId, setJobId] = useState(data.analysisId);
  useEffect(() => { let active = true; (async () => { try { const latest = jobId ? await getAnalysis(jobId) : await getLatestAnalysis(); if (active) { setStatus(latest); if (latest) setJobId(latest.id); } } catch (failure) { if (active) setError(analysisError(failure)); } })(); return () => { active = false; }; }, [jobId]);
  useEffect(() => { if (!jobId || !status || isFinished(status.status)) return; return watchAnalysis(jobId, next => setStatus(next), () => setFallback(true)); }, [jobId, status?.status]);
  useEffect(() => { if (!fallback || !jobId || status && isFinished(status.status)) return; const timer = window.setInterval(() => { getAnalysis(jobId).then(setStatus).catch(failure => setError(analysisError(failure))); }, 3000); return () => window.clearInterval(timer); }, [fallback, jobId, status]);
  const title = !status ? "심사 상태를 확인하는 중." : status.status === "SUCCEEDED" ? "심사가 끝났어요." : status.status === "FAILED" ? "심사가 멈췄어요." : status.status === "QUEUED" ? "산타 앞에 줄 서는 중." : status.status === "RANKING_PENDING" ? "순위를 정리하는 중." : "산타가 기록을 읽는 중.";
  return <FlowFrame back="/review" step={2} title={<>판정 중에는<br />얌전히 계세요.</>} description="실제 심사 상태를 서버에서 확인하고 있어요."><div className={`flow-analysis ${status?.status === "FAILED" ? "is-failed" : ""}`}><span className="flow-analysis-face" aria-hidden="true">{status?.status === "FAILED" ? "( •_• )" : "( ◉_◉ )"}</span><span className="flow-status-pill">{status?.status ?? "확인 중"}</span><h2 aria-live="polite">{title}</h2><p>{status?.status === "FAILED" ? analysisFailure(status.error_code) : status?.status === "SUCCEEDED" ? "실제 결과 화면을 준비하고 있어요. 이 페이지를 닫지 않아도 돼요." : "새로운 POST 요청 없이 현재 작업만 확인합니다."}</p></div>{error && <p className="flow-error" role="alert">{error}</p>}{status?.status === "FAILED" && <Link href="/upload" className="flow-text-link">다른 파일 준비하기 ↗</Link>}</FlowFrame>;
}

export function ResultScreen() {
  const { data } = usePreview();
  return data.sample ? <SampleResultScreen /> : <LiveResultScreen />;
}

function SampleResultScreen() {
  const { data } = usePreview();
  const shareRef = useRef<HTMLDialogElement>(null);
  const shareButton = useRef<HTMLButtonElement>(null);
  const nickname = data.nickname || "루돌프인턴";
  return <FlowFrame back="/analysis" title={<>산타의 판정이<br />도착했습니다.</>} description={`${nickname} 님, 마음의 준비 됐죠?`} action={<button ref={shareButton} className="flow-primary" onClick={() => shareRef.current?.showModal()}>공유 카드 보기 <span aria-hidden="true">↗</span></button>}>
    <div className="flow-result-card"><span className="flow-status-pill">가상 결과</span><span className="flow-result-face" aria-hidden="true">◉‿◉</span><h2>산타 친자 의혹</h2><p className="flow-score">97.82<span>점</span></p><p className="flow-verdict">“이 정도면 선물 말고<br />상속을 기다려라.”</p><div className="flow-result-stats"><div><small>통합 순위</small><b>12<span>위</span></b></div><div><small>선물 도착 예정</small><b className="flow-eta">12월 26일<span>오전 03:24</span></b></div></div><p className="flow-fine-print">점수·순위·도착일 모두 화면 확인용 예시입니다.</p></div>
    <details className="flow-breakdown"><summary>산타가 본 네 가지 항목 <span>＋</span></summary><dl>{[["배려·공감", "98.40"], ["긍정·응원", "97.20"], ["존중하는 소통", "98.00"], ["나눔·협력", "97.50"]].map(([name, score]) => <div key={name}><dt>{name}</dt><dd>{score}<span>점 · 예시</span></dd></div>)}</dl><p>실제 평가가 아닌 화면 확인용 점수입니다.</p></details>
    <NextLink href="/ranking" secondary>전체 랭킹 둘러보기</NextLink><Link className="flow-text-link" href="/start">다른 기록으로 다시 시작 ↗</Link>
    <InfoNote>출발 전에는 참가자와 순위에 따라 도착 예정일이 바뀔 수 있어요. 실제 선물은 오지 않습니다.</InfoNote>
    <dialog ref={shareRef} className="flow-share-dialog" onClose={() => shareButton.current?.focus()} aria-labelledby="share-title"><button className="flow-close" aria-label="공유 카드 닫기" onClick={() => shareRef.current?.close()} autoFocus>×</button><p className="flow-kicker">SANTA CUT / 공유 카드 예시</p><h2 id="share-title">{nickname}의 산타컷</h2><span className="flow-result-face" aria-hidden="true">◉‿◉</span><strong>산타 친자 의혹</strong><p className="flow-score">97.82<span>점</span></p><p>12위 · 12월 26일 도착 예정</p><p className="flow-fine-print">공개 링크 생성·이미지 저장은 준비 중입니다.</p><button className="flow-primary" onClick={() => shareRef.current?.close()}>확인했어요 <span aria-hidden="true">↗</span></button></dialog>
  </FlowFrame>;
}

function LiveResultScreen() {
  const { data } = usePreview();
  const [result, setResult] = useState<Awaited<ReturnType<typeof getMyResult>>>(null);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState("");
  const [shareUrl, setShareUrl] = useState("");
  const [shareImageUrl, setShareImageUrl] = useState("");
  const [sharing, setSharing] = useState(false);
  useEffect(() => { const controller = new AbortController(); getMyResult(controller.signal).then(setResult).catch(failure => { if (!controller.signal.aborted) setError(resultError(failure)); }).finally(() => { if (!controller.signal.aborted) setLoaded(true); }); return () => controller.abort(); }, []);
  if (error) return <FlowFrame back="/analysis" step={2} title={<>결과를<br />불러오지 못했어요.</>} description={error} action={<Link className="flow-primary" href="/analysis">심사 상태 다시 보기 ↗</Link>}><span /></FlowFrame>;
  if (!loaded) return <FlowFrame back="/analysis" step={2} title={<>결과를<br />확인하는 중.</>} description="점수와 순위를 안전하게 불러오고 있어요."><span /></FlowFrame>;
  if (!result) return <FlowFrame back="/analysis" step={2} title={<>아직 결과가<br />없어요.</>} description="분석이 끝나면 점수와 판정을 확인할 수 있어요." action={<Link className="flow-primary" href="/analysis">심사 상태 확인하기 ↗</Link>}><InfoNote>결과가 준비되기 전에는 새로운 파일을 자동으로 제출하지 않아요.</InfoNote></FlowFrame>;
  const rank = result.rank == null ? "계산 중" : `${result.tied ? "공동 " : ""}${result.rank}위`;
  async function share() { setSharing(true); setError(""); try { const response = await createShare(); setShareUrl(response.url); setShareImageUrl(response.imageUrl); } catch (failure) { setError(resultError(failure)); } finally { setSharing(false); } }
  return <FlowFrame back="/analysis" title={<>산타의 판정이<br />도착했습니다.</>} description={`${data.nickname || "참가자"} 님, 마음의 준비 됐죠?`} action={<NextLink href="/ranking">전체 랭킹 보기</NextLink>}>
    <div className="flow-result-card"><span className="flow-status-pill">{result.stage === "FINAL" ? "시즌 확정" : "실제 결과"}</span><span className="flow-result-face" aria-hidden="true">◉‿◉</span><h2>{result.archetype?.name || "산타의 판정"}</h2><p className="flow-score">{formatScore(result.score_hundredths)}<span>점</span></p><p className="flow-verdict">“{result.comment || result.verdict}”</p><div className="flow-result-stats"><div><small>{result.ranking_pending ? "순위 반영" : "통합 순위"}</small><b>{rank}</b></div><div><small>선물 도착 예정</small><b className="flow-eta">{formatKoreanDate(result.eta)}</b></div></div>{result.ranking_pending && <p className="flow-fine-print">새 심사의 점수는 확정됐고 순위는 반영 중이에요.</p>}</div>
    {result.dimension_scores && result.dimension_scores.length > 0 && <details className="flow-breakdown"><summary>산타가 본 네 가지 항목 <span>＋</span></summary><dl>{result.dimension_scores.map(dimension => <div key={dimension.key}><dt>{dimension.label}{result.weakest_dimension?.key === dimension.key && <small> 근거가 적어요</small>}</dt><dd>{dimension.score.toFixed(2)}<span>점</span></dd></div>)}</dl>{result.top_percent != null && <p>전체 참가자 중 상위 {result.top_percent}%예요.</p>}</details>}
    <button className="flow-secondary" onClick={share} disabled={sharing}>{sharing ? "공유 카드 만드는 중" : "공유 카드 링크 만들기"}<span aria-hidden="true">↗</span></button>
    {shareUrl && <div className="flow-info" role="status"><span>✓</span><span>공유 링크가 준비됐어요. <a href={shareUrl} target="_blank" rel="noreferrer">새 창에서 열기</a>{shareImageUrl && <> · <a href={shareImageUrl} target="_blank" rel="noreferrer">이미지 보기</a></>}</span></div>}
    <Link className="flow-text-link" href="/start">다른 기록으로 다시 시작 ↗</Link>
  </FlowFrame>;
}

export function RankingScreen() {
  const { data } = usePreview();
  return data.sample ? <SampleRankingScreen /> : <LiveRankingScreen />;
}

function SampleRankingScreen() {
  const { data } = usePreview();
  const [expanded, setExpanded] = useState(false);
  const entries = [["1", "산타아들아님", "99.91"], ["2", "착한말수집가", "99.20"], ["2", "루돌프주주", "99.20"], ["4", "굴뚝관리인", "98.74"], ["5", "선물은못참지", "98.60"], ["6", "북극인턴", "98.45"], ["7", "산타연차반려", "98.33"], ["8", "귤두개드림", "98.25"]];
  return <FlowFrame back="/result" title={<>착한 사람들,<br />왜 이렇게 많아.</>} description="2026 산타컷 통합 랭킹" action={<NextLink href="/result">내 결과 보기</NextLink>}>
    <section className="flow-ranking-me" aria-label="나의 순위"><span>나의 예시 순위</span><div><strong>12<small>위</small></strong><p>{data.nickname || "루돌프인턴"}<b>97.82점</b></p><i>↗</i></div></section>
    <div className="flow-ranking-caption"><b>전체 랭킹</b><span>가상 참가자 · 예시 데이터</span></div>
    <ol className="flow-ranking-list">{entries.slice(0, expanded ? undefined : 5).map(([rank, name, score], index) => <li key={name}><span className={index === 0 ? "is-first" : ""}>{rank === "2" ? "공동 2" : rank}</span><b>{name}</b><strong>{score}<small>점</small></strong></li>)}</ol>
    {!expanded && <button className="flow-load-more" onClick={() => setExpanded(true)}>예시 랭킹 더 보기 ＋</button>}
    <InfoNote>점수가 같으면 공동 순위가 될 수 있어요. 공동 순위여도 배송 순서에 따라 도착 시간이 다를 수 있습니다.</InfoNote>
  </FlowFrame>;
}

function LiveRankingScreen() {
  const { data } = usePreview();
  const [mine, setMine] = useState<Awaited<ReturnType<typeof getMyResult>>>(null);
  const [rows, setRows] = useState<RankingRow[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState(false);
  useEffect(() => { const controller = new AbortController(); getRankings(undefined, controller.signal).then(page => { setRows(page.rows); setCursor(page.next_cursor); }).catch(failure => { if (!controller.signal.aborted) setError(rankingError(failure)); }).finally(() => { if (!controller.signal.aborted) setLoading(false); }); getMyResult(controller.signal).then(setMine).catch(() => undefined); return () => controller.abort(); }, []);
  async function loadMore() { if (!cursor) return; try { const page = await getRankings(cursor); setRows(previous => [...previous, ...page.rows]); setCursor(page.next_cursor); } catch (failure) { setError(rankingError(failure)); } }
  return <FlowFrame back="/result" title={<>착한 사람들,<br />왜 이렇게 많아.</>} description="실제 참가자 기준 산타컷 통합 랭킹" action={<NextLink href="/result">내 결과 보기</NextLink>}>
    <section className="flow-ranking-me" aria-label="나의 순위"><span>{mine?.rank == null ? "내 순위 계산 중" : "나의 순위"}</span><div><strong>{mine?.rank == null ? "—" : `${mine.tied ? "공동 " : ""}${mine.rank}`}<small>{mine?.rank == null ? "" : "위"}</small></strong><p>{data.nickname || "참가자"}{mine && <b>{formatScore(mine.score_hundredths)}점</b>}</p><i>↗</i></div></section>
    {loading && <p role="status">랭킹을 불러오는 중이에요.</p>}
    {error && <p className="flow-error" role="alert">{error}</p>}
    {!loading && !error && rows.length === 0 && <InfoNote>아직 계산된 랭킹이 없어요. 첫 순위 계산을 기다려 주세요.</InfoNote>}
    {rows.length > 0 && <><div className="flow-ranking-caption"><b>전체 랭킹</b><span>실제 참가자 · 서버 계산</span></div><ol className="flow-ranking-list">{rows.slice(0, expanded ? undefined : 5).map((entry, index) => <li key={`${entry.rank}-${entry.nickname}-${index}`}><span className={index === 0 ? "is-first" : ""}>{entry.tied ? `공동 ${entry.rank}` : entry.rank}</span><b>{entry.nickname}</b><strong>{formatScore(entry.score_hundredths)}<small>점</small></strong></li>)}</ol>{!expanded && (rows.length > 5 || cursor) && <button className="flow-load-more" onClick={() => { setExpanded(true); if (rows.length <= 5) void loadMore(); }}>랭킹 더 보기 ＋</button>}{expanded && cursor && <button className="flow-load-more" onClick={() => void loadMore()}>다음 랭킹 불러오기 ＋</button>}</>}
  </FlowFrame>;
}
