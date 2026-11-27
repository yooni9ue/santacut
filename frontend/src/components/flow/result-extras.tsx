"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { flushSync } from "react-dom";
import { toBlob } from "html-to-image";
import { StuckSanta } from "@/components/stuck-santa";
import { useEffect, useRef, useState } from "react";
import { getMyDossier, getMyRecap, recapRefetchInterval, resultError, type DossierResponse, type DossierScene, type RecapCard } from "@/lib/results";

import { ResultLinkShare } from "./result-link-share";

export function ResultExtras({ recapAvailable, dossierAvailable, refreshKey, ranked = false }: { recapAvailable?: boolean; dossierAvailable?: boolean; refreshKey?: string; ranked?: boolean }) {
  const recapQuery = useQuery({
    queryKey: ["me", "recap", refreshKey ?? ""],
    queryFn: ({ signal }) => getMyRecap(signal),
    enabled: Boolean(recapAvailable),
    staleTime: 0,
    placeholderData: keepPreviousData,
    // Keep asking while the latest result waits for its rank, then stop.
    refetchInterval: query => recapRefetchInterval(query.state.data),
  });
  const recap = recapAvailable ? recapQuery.data ?? undefined : undefined;
  const [dossier, setDossier] = useState<DossierResponse>();
  const [dossierError, setDossierError] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const exportLock = useRef(false);
  const exportRef = useRef<HTMLElement | null>(null);
  const [exportPreview, setExportPreview] = useState<{ card: RecapCard; skin: string; width: number }>();
  const [exportProgress, setExportProgress] = useState("");
  const [activeIndex, setActiveIndex] = useState(0);
  const [showContents, setShowContents] = useState(false);
  const [assessmentId, setAssessmentId] = useState<string>();
  const touchStart = useRef<{ x: number; y: number } | undefined>(undefined);
  const cardRefs = useRef<Record<string, HTMLElement | null>>({});

  // A new assessment starts from the first card; rank refreshes keep the current card.
  if (recap && recap.assessment_id !== assessmentId) {
    setAssessmentId(recap.assessment_id);
    setActiveIndex(0);
  }

  useEffect(() => {
    if (!dossierAvailable) return;
    const controller = new AbortController();
    getMyDossier(controller.signal).then(setDossier).catch(failure => {
      if (controller.signal.aborted || (isAxiosError(failure) && failure.response?.status === 404)) return;
      setDossierError(resultError(failure));
    });
    return () => controller.abort();
  }, [dossierAvailable, refreshKey]);
  const error = dossierError || (recapQuery.isError && !recapQuery.data ? resultError(recapQuery.error) : "");
  const activeCard = recap?.cards[Math.min(activeIndex, recap.cards.length - 1)];

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
    const width = (activeCard && cardRefs.current[`recap-${activeCard.index}`]?.getBoundingClientRect().width) || 340;
    try {
      const images: Blob[] = [];
      for (const [index, card] of snapshot.cards.entries()) {
        flushSync(() => {
          setExportPreview({ card, skin: snapshot.skin, width });
          setExportProgress(`전체 카드 저장 준비 중 · ${index + 1} / ${snapshot.cards.length}`);
        });
        if (!exportRef.current) throw new Error("카드를 준비하지 못했어요. 다시 시도해 주세요.");
        const blob = await renderCard(exportRef.current);
        images.push(blob);
      }
      images.forEach((blob, index) => {
        window.setTimeout(() => downloadFile(blob, `santacut-recap-${String(index + 1).padStart(2, "0")}.png`), index * 350);
      });
      setMessage(`카드 ${snapshot.cards.length}장을 각각 PNG로 저장했어요. 브라우저에서 여러 파일 다운로드를 허용해 주세요.`);
    } catch (failure) {
      setMessage(resultError(failure));
    } finally {
      setExportPreview(undefined);
      setExportProgress("");
      exportLock.current = false;
      setBusy(false);
    }
  }

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
        <button key={card.index} type="button" disabled={busy} className={index === activeIndex ? "is-active" : ""} aria-label={`${index + 1}번째 카드 ${recapCardName(card)}`} aria-current={index === activeIndex ? "step" : undefined} onClick={() => setActiveIndex(index)} />
      )}</div>
      {showContents && <div className="flow-recap-contents" aria-label="전체 카드 목록">{recap.cards.map((card, index) => <button key={card.index} type="button" disabled={busy} aria-current={index === activeIndex ? "step" : undefined} onClick={() => { setActiveIndex(index); setShowContents(false); }}><span>{String(index + 1).padStart(2, "0")}</span>{recapCardName(card)}</button>)}</div>}
      {activeCard && <>
        <article className={`flow-recap-card is-${activeCard.kind.toLowerCase()} ${recap.skin === "COAL" ? "is-coal" : ""}`}
          onTouchStart={event => { touchStart.current = { x: event.touches[0].clientX, y: event.touches[0].clientY }; }}
          onTouchEnd={finishSwipe}
          ref={node => { cardRefs.current[`recap-${activeCard.index}`] = node; }}>
          <RecapFace card={activeCard} skin={recap.skin} />
        </article>
        <div className="flow-recap-nav"><button type="button" disabled={busy || activeIndex === 0} onClick={() => setActiveIndex(index => index - 1)}>← 이전</button><span>{recapCardName(activeCard)}</span><button type="button" disabled={busy || activeIndex === recap.cards.length - 1} onClick={() => setActiveIndex(index => index + 1)}>다음 →</button></div>
        <div className="flow-extra-actions"><button disabled={busy} onClick={() => void exportCard(`recap-${activeCard.index}`, `santacut-recap-${activeCard.index + 1}.png`, recap.share_text, true)}>이 카드 공유</button><button disabled={busy} onClick={() => void exportCard(`recap-${activeCard.index}`, `santacut-recap-${activeCard.index + 1}.png`, "", false)}>현재 카드 저장</button></div>
        <div className="flow-extra-actions flow-recap-save-all"><button disabled={busy} onClick={() => void exportAll()}>전체 {recap.cards.length}장 저장 (ZIP)</button></div>
      </>}
      <ResultLinkShare key={recap.assessment_id} ranked={ranked || recap.rank != null} />
      <p className="flow-extra-share-text">{recap.share_text}</p>
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
        <RecapFace card={exportPreview.card} skin={exportPreview.skin} />
      </article>
    </div>}
    {error && <p className="flow-error" role="alert">새 결과 카드를 불러오지 못했어요. {error}</p>}
    {message && <p role="status">{message}</p>}
    {busy && <p role="status">{exportProgress || "이미지를 만들고 있어요."}</p>}
  </>;
}

function recapCardName(card: RecapCard) {
  switch (card.kind) {
    case "VERDICT": return "최종 판정";
    case "CUTLINE": return "산타컷 표";
    case "REASON": return `선물 받는 이유 ${card.ordinal}`;
    case "CUT": return `점수 깎인 말 ${card.ordinal}`;
    case "KEEP": return "살아남은 말";
    case "CONCLUSION": return "결론";
  }
}

// The six bands are fixed; the fourth band (55~64) is the first one below the gift cut of 65.
const CUT_BAND_POSITION = 3;
const formatKoreanDay = (value: string) => new Date(value).toLocaleDateString("ko-KR", { timeZone: "Asia/Seoul", month: "long", day: "numeric" });

function RecapQuote({ quote, chip }: { quote: string | null; chip: string | null }) {
  if (!quote) return chip ? <p className="flow-recap-chip is-giant">{chip}</p> : null;
  return <><p className="flow-recap-bubble">{quote}</p>{chip && <p className="flow-recap-chip">{chip}</p>}</>;
}

function RecapContent({ card }: { card: RecapCard }) {
  switch (card.kind) {
    case "VERDICT": return <>
      <span className="flow-recap-badge">{card.passed ? "GIFT" : "COAL"}</span>
      <h3 className="flow-recap-hero">{card.verdict}</h3>
      <p className="flow-recap-grade">{card.grade}</p>
      <p className="flow-recap-line">{card.verdict_line}</p>
      <div className="flow-recap-chips"><span>{card.score_text}점</span><span>컷 {card.cut_distance}</span>{card.delivery_eta && <span>선물 도착 {formatKoreanDay(card.delivery_eta)}</span>}</div>
    </>;
    case "CUTLINE": return <>
      <ol className="flow-recap-bands" aria-label="산타컷 구간표">{card.bands.map((band, index) => <li key={band.code} className={`${band.current ? "is-current" : ""} ${index === CUT_BAND_POSITION ? "is-below-cut" : ""}`} aria-current={band.current ? "true" : undefined}>
        {index === CUT_BAND_POSITION && <small className="flow-recap-cutline">선물 컷 65</small>}
        <span>{band.label}</span><b>{band.range}</b>{band.current && <em>나 {card.score_text}점</em>}
      </li>)}</ol>
      <p className="flow-recap-line">{card.band_detail}</p>
    </>;
    case "REASON":
    case "CUT":
    case "KEEP": return <>
      {card.dim_tag && <span className="flow-recap-tag">{card.dim_tag}</span>}
      <RecapQuote quote={card.quote} chip={card.chip_text} />
      <h3 className="flow-recap-label">{card.label}</h3>
      <p className="flow-recap-line">{card.line}</p>
    </>;
    case "CONCLUSION": return <>
      <span className="flow-recap-badge">{card.verdict}</span>
      <h3 className="flow-recap-band">{card.grade}</h3>
      <p className="flow-recap-title">{card.title} · {card.archetype_name}</p>
      <RecapQuote quote={card.hero_quote} chip={card.hero_chip_text} />
      <p className="flow-recap-line">{card.final_line}</p>
      <div className="flow-recap-chips"><span>{card.score_text}점</span><span>컷 {card.cut_distance}</span>{card.top_percent != null && <span>상위 {card.top_percent}%</span>}{card.type_percent != null && <span>이 유형 {card.type_percent}%</span>}</div>
      <div className="flow-recap-strip" aria-label="내 구간 위치">{Array.from({ length: 6 }, (_, index) => <i key={index} className={`${index === card.band_position ? "is-current" : ""} ${index === CUT_BAND_POSITION ? "is-below-cut" : ""}`} />)}</div>
      <p className="flow-recap-friend">{card.friend_call}</p>
      <p className="flow-recap-hashtags">#산타컷 #{card.band_tag}</p>
    </>;
    default: return null;
  }
}

function DossierContent({ scene }: { scene: DossierScene }) {
  return <>{scene.headline && <h3>{scene.headline}</h3>}{scene.big_value && <strong className="flow-extra-value">{scene.big_value}</strong>}{scene.lines.map(line => <p key={line.id} data-private={!line.shareable}>{line.text}</p>)}{scene.memo && <p>{scene.memo}</p>}{scene.chart && <div className="flow-extra-chart" aria-label={`${scene.chart.type} 그래프`}>{scene.chart.values.map((value, index) => <i key={index} style={{ height: `${Math.max(2, Math.min(100, value))}%` }} title={String(value)} />)}</div>}</>;
}

function RecapFace({ card, skin }: { card: RecapCard; skin: string }) {
  const withArt = card.kind !== "CUTLINE" && card.kind !== "CONCLUSION";
  return <>
    <div className="flow-recap-card-top"><span>산타컷</span><span className={card.rank == null ? "is-pending" : undefined}>{card.rank == null ? "순위 계산 중" : `전국 ${card.rank}등 · 상위 ${card.top_percent}%`}</span></div>
    <p className="flow-recap-kicker">{recapCardName(card)}</p>
    <div className="flow-recap-copy"><RecapContent card={card} /></div>
    {withArt && <div className="flow-recap-art" aria-hidden="true"><StuckSanta freed={skin === "GIFT"} /></div>}
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
