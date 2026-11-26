"use client";

import { isAxiosError } from "axios";
import { flushSync } from "react-dom";
import { toBlob } from "html-to-image";
import { StuckSanta } from "@/components/stuck-santa";
import { useEffect, useRef, useState } from "react";
import { formatKoreanDate, getMyDossier, getMyRecap, resultError, type DossierResponse, type DossierScene, type RecapCard, type RecapResponse } from "@/lib/results";

import { ResultLinkShare } from "./result-link-share";

export function ResultExtras({ recapAvailable, dossierAvailable, refreshKey, ranked = false }: { recapAvailable?: boolean; dossierAvailable?: boolean; refreshKey?: string; ranked?: boolean }) {
  const [recap, setRecap] = useState<RecapResponse>();
  const [dossier, setDossier] = useState<DossierResponse>();
  const [error, setError] = useState("");
  const [mild, setMild] = useState(false);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const exportLock = useRef(false);
  const exportRef = useRef<HTMLElement | null>(null);
  const [exportPreview, setExportPreview] = useState<{ card: RecapCard; skin: string; mild: boolean; width: number }>();
  const [exportProgress, setExportProgress] = useState("");
  const [activeIndex, setActiveIndex] = useState(0);
  const [showContents, setShowContents] = useState(false);
  const assessmentId = useRef<string | undefined>(undefined);
  const touchStart = useRef<{ x: number; y: number } | undefined>(undefined);
  const cardRefs = useRef<Record<string, HTMLElement | null>>({});

  useEffect(() => {
    const controller = new AbortController();
    async function load() {
      const responses = await Promise.allSettled([
        recapAvailable ? getMyRecap(controller.signal) : Promise.resolve(undefined),
        dossierAvailable ? getMyDossier(controller.signal) : Promise.resolve(undefined),
      ]);
      if (controller.signal.aborted) return;
      if (responses[0].status === "fulfilled") {
        setRecap(responses[0].value);
        if (assessmentId.current !== responses[0].value?.assessment_id) setActiveIndex(0);
        assessmentId.current = responses[0].value?.assessment_id;
      }
      if (responses[1].status === "fulfilled") setDossier(responses[1].value);
      const failure = responses.find(response => response.status === "rejected");
      if (failure?.status === "rejected" && !(isAxiosError(failure.reason) && failure.reason.response?.status === 404)) {
        setError(resultError(failure.reason));
      }
    }
    void load();
    return () => controller.abort();
  }, [recapAvailable, dossierAvailable, refreshKey]);

  async function exportCard(id: string, name: string, text: string, share: boolean) {
    const node = cardRefs.current[id];
    if (!node || exportLock.current) return;
    exportLock.current = true;
    setBusy(true);
    setMessage("");
    try {
      const blob = await renderCard(node);
      const file = new File([blob], name, { type: "image/png" });
      if (share && navigator.share && navigator.canShare?.({ files: [file] })) {
        try {
          await navigator.share({ files: [file], text });
          return;
        } catch (failure) {
          if (failure instanceof DOMException && failure.name === "AbortError") return;
        }
      }
      downloadFile(file, name);
      if (share) setMessage("공유 메뉴를 사용할 수 없어 이미지를 저장했어요.");
    } catch (failure) {
      setMessage(resultError(failure));
    } finally {
      exportLock.current = false;
      setBusy(false);
    }
  }

  async function exportAll() {
    if (!recap?.cards.length || exportLock.current) return;
    exportLock.current = true;
    setBusy(true);
    setMessage("");
    const snapshot = recap;
    const width = cardRefs.current[`recap-${snapshot.cards[activeIndex].index}`]?.getBoundingClientRect().width ?? 340;
    try {
      const { zipSync } = await import("fflate");
      const files: Record<string, Uint8Array> = {};
      for (const [index, card] of snapshot.cards.entries()) {
        flushSync(() => {
          setExportPreview({ card, skin: snapshot.skin, mild, width });
          setExportProgress(`전체 카드 저장 준비 중 · ${index + 1} / ${snapshot.cards.length}`);
        });
        if (!exportRef.current) throw new Error("카드를 준비하지 못했어요. 다시 시도해 주세요.");
        const blob = await renderCard(exportRef.current);
        files[`santacut-recap-${String(index + 1).padStart(2, "0")}.png`] = new Uint8Array(await blob.arrayBuffer());
      }
      const zip = zipSync(files, { level: 0 });
      downloadFile(new Blob([new Uint8Array(zip)], { type: "application/zip" }), "santacut-recap-all.zip");
      setMessage(`카드 ${snapshot.cards.length}장을 ZIP으로 저장했어요. 압축을 풀면 각 PNG 이미지를 볼 수 있어요.`);
    } catch (failure) {
      setMessage(resultError(failure));
    } finally {
      setExportPreview(undefined);
      setExportProgress("");
      exportLock.current = false;
      setBusy(false);
    }
  }

  const activeCard = recap?.cards[activeIndex];
  function finishSwipe(event: React.TouchEvent<HTMLElement>) {
    const start = touchStart.current;
    touchStart.current = undefined;
    if (!start || !recap || busy) return;
    const dx = event.changedTouches[0].clientX - start.x;
    const dy = event.changedTouches[0].clientY - start.y;
    if (Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy)) {
      setActiveIndex(index => Math.max(0, Math.min(recap.cards.length - 1, index + (dx < 0 ? 1 : -1))));
    }
  }
  return <>
    {recap && <section className="flow-recap" aria-label="팩폭 산타 카드">
      <div className="flow-recap-top"><strong aria-live="polite">{activeIndex + 1} / {recap.cards.length}</strong><button type="button" disabled={busy} aria-expanded={showContents} onClick={() => setShowContents(value => !value)}>전체 카드 ⊞</button></div>
      <div className="flow-recap-progress" aria-label="카드 선택">{recap.cards.map((card, index) =>
        <button key={card.index} type="button" disabled={busy} className={index === activeIndex ? "is-active" : ""} aria-label={`${index + 1}번째 카드 ${recapCardName(card.kind)}`} aria-current={index === activeIndex ? "step" : undefined} onClick={() => setActiveIndex(index)} />
      )}</div>
      {showContents && <div className="flow-recap-contents" aria-label="전체 카드 목록">{recap.cards.map((card, index) => <button key={card.index} type="button" disabled={busy} aria-current={index === activeIndex ? "step" : undefined} onClick={() => { setActiveIndex(index); setShowContents(false); }}><span>{String(index + 1).padStart(2, "0")}</span>{recapCardName(card.kind)}</button>)}</div>}
      {activeCard && <>
        <article className={`flow-recap-card is-${activeCard.kind.toLowerCase()} ${recap.skin === "COAL" ? "is-coal" : ""}`}
          onTouchStart={event => { touchStart.current = { x: event.touches[0].clientX, y: event.touches[0].clientY }; }}
          onTouchEnd={finishSwipe}
          ref={node => { cardRefs.current[`recap-${activeCard.index}`] = node; }}>
          <RecapFace card={activeCard} skin={recap.skin} mild={mild} />
        </article>
        <div className="flow-recap-nav"><button type="button" disabled={busy || activeIndex === 0} onClick={() => setActiveIndex(index => index - 1)}>← 이전</button><span>{recapCardName(activeCard.kind)}</span><button type="button" disabled={busy || activeIndex === recap.cards.length - 1} onClick={() => setActiveIndex(index => index + 1)}>다음 →</button></div>
        <div className="flow-extra-actions"><button disabled={busy} onClick={() => void exportCard(`recap-${activeCard.index}`, `santacut-recap-${activeCard.index + 1}.png`, mild ? recap.share_text_mild : recap.share_text, true)}>이 카드 공유</button><button disabled={busy} onClick={() => void exportCard(`recap-${activeCard.index}`, `santacut-recap-${activeCard.index + 1}.png`, "", false)}>현재 카드 저장</button></div>
        <div className="flow-extra-actions flow-recap-save-all"><button disabled={busy} onClick={() => void exportAll()}>전체 {recap.cards.length}장 저장 (ZIP)</button></div>
      </>}
      <ResultLinkShare key={recap.assessment_id} ranked={ranked} />
      <label className="flow-extra-toggle"><input type="checkbox" disabled={busy} checked={mild} onChange={event => setMild(event.target.checked)} /> 농담 빼고 보기</label>
      <p className="flow-extra-share-text">{mild ? recap.share_text_mild : recap.share_text}</p>
    </section>}
    {dossier && <details className="flow-dossier" aria-label="산타 조서"><summary>산타 조서 더 보기 <span>↗</span></summary><section className="flow-extra">
      <div className="flow-extra-heading"><h2>산타 조서</h2><span>{dossier.title}</span></div>
      <div className="flow-extra-deck">{dossier.scenes.map(scene => <div key={scene.id} className="flow-extra-item">
        <article className="flow-extra-card" ref={node => { cardRefs.current[`dossier-${scene.id}`] = node; }}>
          <p className="flow-extra-kicker">SANTA CUT / {scene.speaker}</p><DossierContent scene={scene} />
        </article>
        {scene.shareable && <div className="flow-extra-actions"><button disabled={busy} onClick={() => void exportCard(`dossier-${scene.id}`, `santacut-dossier-${scene.id}.png`, "산타 조서", true)}>이미지 공유</button><button disabled={busy} onClick={() => void exportCard(`dossier-${scene.id}`, `santacut-dossier-${scene.id}.png`, "", false)}>이미지 저장</button></div>}
      </div>)}</div>
    </section></details>}
    {exportPreview && <div className="flow-recap-export" aria-hidden="true" style={{ width: exportPreview.width }}>
      <article ref={exportRef} className={`flow-recap-card is-${exportPreview.card.kind.toLowerCase()} ${exportPreview.skin === "COAL" ? "is-coal" : ""}`}>
        <RecapFace card={exportPreview.card} skin={exportPreview.skin} mild={exportPreview.mild} />
      </article>
    </div>}
    {error && <p className="flow-error" role="alert">새 결과 카드를 불러오지 못했어요. {error}</p>}
    {message && <p role="status">{message}</p>}
    {busy && <p role="status">{exportProgress || "이미지를 만들고 있어요."}</p>}
  </>;
}

function recapCardName(kind: RecapCard["kind"]) {
  return { VERDICT: "최종 판정", ROAST: "산타의 팩폭", LETTER: "산타의 쪽지", STORY: "올해의 기록", ARCHETYPE: "산타 유형", DIMENSIONS: "채점표" }[kind];
}

function RecapContent({ card, mild }: { card: RecapCard; mild: boolean }) {
  switch (card.kind) {
    case "VERDICT": return <><h3>{card.title}</h3><div className="flow-recap-verdict"><b className="flow-extra-stamp">{mild ? card.stamp_mild : card.stamp}</b>{(card.passed || !mild) && <span>{card.grade}</span>}</div><p>{mild ? card.verdict_mild : card.verdict}</p><small className="flow-recap-cut">선물 컷 65 · {card.cut_distance}</small><div className="flow-recap-facts"><p><small>통합 순위</small><span>{card.ranking_pending ? "순위 계산 중" : card.rank == null ? "순위 집계 전" : `${card.rank}위`}</span></p>{card.delivery_eta && <p><small>선물 도착 예정</small>{formatKoreanDate(card.delivery_eta)}</p>}</div></>;
    case "ROAST": return <>{card.chip && <p className="flow-extra-chip">{card.chip.label}<strong>{card.chip.value}</strong></p>}<h3>{card.roast}</h3><p className="flow-recap-message">{card.flip}</p></>;
    case "LETTER": return <><h3>놀려서 미안,<br />근데 진심이다.</h3><p className="flow-recap-message">{card.letter}</p><small>— 북극에서, 야근 중인 산타</small></>;
    case "STORY": return <><h3>올해 너, 세 줄 요약</h3>{card.acts.map(act => <p key={act.act}><b>{act.months}</b> {act.line}</p>)}</>;
    case "ARCHETYPE": return <><h3>북극 인사팀 배치 결과</h3><p>{card.archetype.name}</p></>;
    case "DIMENSIONS": return <><h3>산타 채점표</h3>{card.bars.map(bar => <p key={bar.key}>{bar.label} <b>{bar.score.toFixed(2)}</b></p>)}</>;
    default: return null;
  }
}

function DossierContent({ scene }: { scene: DossierScene }) {
  return <>{scene.headline && <h3>{scene.headline}</h3>}{scene.big_value && <strong className="flow-extra-value">{scene.big_value}</strong>}{scene.lines.map(line => <p key={line.id} data-private={!line.shareable}>{line.text}</p>)}{scene.memo && <p>{scene.memo}</p>}{scene.chart && <div className="flow-extra-chart" aria-label={`${scene.chart.type} 그래프`}>{scene.chart.values.map((value, index) => <i key={index} style={{ height: `${Math.max(2, Math.min(100, value))}%` }} title={String(value)} />)}</div>}</>;
}

function RecapFace({ card, skin, mild }: { card: RecapCard; skin: string; mild: boolean }) {
  return <>
    <div className="flow-recap-card-top"><span>산타컷</span><span>{recapCardName(card.kind)}</span></div>
    <strong className="flow-recap-score">{card.score_text}<small>점</small></strong>
    <div className="flow-recap-copy"><RecapContent card={card} mild={mild} /></div>
    <div className="flow-recap-art" aria-hidden="true"><StuckSanta freed={skin === "GIFT"} /></div>
    <p className="flow-recap-card-bottom">SANTA CUT · CHRISTMAS 2026</p>
  </>;
}

async function renderCard(node: HTMLElement) {
  await document.fonts.ready;
  await Promise.all(Array.from(node.querySelectorAll("img")).map(image => image.decode()));
  const blob = await toBlob(node, {
    backgroundColor: getComputedStyle(node).backgroundColor,
    canvasWidth: 1080,
    canvasHeight: 1920,
    pixelRatio: 1,
    filter: element => !(element instanceof HTMLElement && element.dataset.private === "true"),
  });
  if (!blob) throw new Error("이미지를 만들지 못했어요.");
  return blob;
}

function downloadFile(file: Blob, name: string) {
  const url = URL.createObjectURL(file);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = name;
  anchor.click();
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}
