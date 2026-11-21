"use client";

import { isAxiosError } from "axios";
import { toBlob } from "html-to-image";
import { useEffect, useRef, useState } from "react";
import { formatKoreanDate, getMyDossier, getMyRecap, resultError, type DossierResponse, type DossierScene, type RecapCard, type RecapResponse } from "@/lib/results";

export function ResultExtras({ recapAvailable, dossierAvailable }: { recapAvailable?: boolean; dossierAvailable?: boolean }) {
  const [recap, setRecap] = useState<RecapResponse>();
  const [dossier, setDossier] = useState<DossierResponse>();
  const [error, setError] = useState("");
  const [mild, setMild] = useState(false);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const cardRefs = useRef<Record<string, HTMLElement | null>>({});

  useEffect(() => {
    const controller = new AbortController();
    async function load() {
      const responses = await Promise.allSettled([
        recapAvailable ? getMyRecap(controller.signal) : Promise.resolve(undefined),
        dossierAvailable ? getMyDossier(controller.signal) : Promise.resolve(undefined),
      ]);
      if (controller.signal.aborted) return;
      if (responses[0].status === "fulfilled") setRecap(responses[0].value);
      if (responses[1].status === "fulfilled") setDossier(responses[1].value);
      const failure = responses.find(response => response.status === "rejected");
      if (failure?.status === "rejected" && !(isAxiosError(failure.reason) && failure.reason.response?.status === 404)) {
        setError(resultError(failure.reason));
      }
    }
    void load();
    return () => controller.abort();
  }, [recapAvailable, dossierAvailable]);

  async function exportCard(id: string, name: string, text: string, share: boolean) {
    const node = cardRefs.current[id];
    if (!node || busy) return;
    setBusy(true);
    setMessage("");
    try {
      await document.fonts.ready;
      const blob = await toBlob(node, {
        backgroundColor: getComputedStyle(node).backgroundColor,
        pixelRatio: 1080 / node.offsetWidth,
        filter: element => !(element instanceof HTMLElement && element.dataset.private === "true"),
      });
      if (!blob) throw new Error("이미지를 만들지 못했어요.");
      const file = new File([blob], name, { type: "image/png" });
      if (share && navigator.share && navigator.canShare?.({ files: [file] })) {
        try {
          await navigator.share({ files: [file], text });
          return;
        } catch (failure) {
          if (failure instanceof DOMException && failure.name === "AbortError") return;
        }
      }
      const url = URL.createObjectURL(file);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = name;
      anchor.click();
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
      if (share) setMessage("공유 메뉴를 사용할 수 없어 이미지를 저장했어요.");
    } catch (failure) {
      setMessage(resultError(failure));
    } finally {
      setBusy(false);
    }
  }

  return <>
    {recap && <section className="flow-extra" aria-label="팩폭 산타 카드">
      <div className="flow-extra-heading"><h2>팩폭 산타 카드</h2><span>{recap.cards.length}장</span></div>
      <label className="flow-extra-toggle"><input type="checkbox" checked={mild} onChange={event => setMild(event.target.checked)} /> 석탄 농담 끄기</label>
      <div className="flow-extra-deck">{recap.cards.map(card => <div key={card.index} className="flow-extra-item">
        <article className={`flow-extra-card ${recap.skin === "COAL" ? "is-coal" : ""}`} ref={node => { cardRefs.current[`recap-${card.index}`] = node; }}>
          <p className="flow-extra-kicker">SANTA CUT / {card.kind}</p><strong className="flow-extra-score">{card.score_text}</strong>
          <RecapContent card={card} mild={mild} />
        </article>
        <div className="flow-extra-actions"><button disabled={busy} onClick={() => void exportCard(`recap-${card.index}`, `santacut-recap-${card.index + 1}.png`, mild ? recap.share_text_mild : recap.share_text, true)}>이미지 공유</button><button disabled={busy} onClick={() => void exportCard(`recap-${card.index}`, `santacut-recap-${card.index + 1}.png`, "", false)}>이미지 저장</button></div>
      </div>)}</div>
      <p className="flow-extra-share-text">{mild ? recap.share_text_mild : recap.share_text}</p>
    </section>}
    {dossier && <section className="flow-extra" aria-label="산타 조서">
      <div className="flow-extra-heading"><h2>산타 조서</h2><span>{dossier.title}</span></div>
      <div className="flow-extra-deck">{dossier.scenes.map(scene => <div key={scene.id} className="flow-extra-item">
        <article className="flow-extra-card" ref={node => { cardRefs.current[`dossier-${scene.id}`] = node; }}>
          <p className="flow-extra-kicker">SANTA CUT / {scene.speaker}</p><DossierContent scene={scene} />
        </article>
        {scene.shareable && <div className="flow-extra-actions"><button disabled={busy} onClick={() => void exportCard(`dossier-${scene.id}`, `santacut-dossier-${scene.id}.png`, "산타 조서", true)}>이미지 공유</button><button disabled={busy} onClick={() => void exportCard(`dossier-${scene.id}`, `santacut-dossier-${scene.id}.png`, "", false)}>이미지 저장</button></div>}
      </div>)}</div>
    </section>}
    {error && <p className="flow-error" role="alert">새 결과 카드를 불러오지 못했어요. {error}</p>}
    {message && <p role="status">{message}</p>}
  </>;
}

function RecapContent({ card, mild }: { card: RecapCard; mild: boolean }) {
  switch (card.kind) {
    case "VERDICT": return <><h3>{card.title}</h3><b className="flow-extra-stamp">{mild ? card.stamp_mild : card.stamp}</b>{(card.passed || !mild) && <p>{card.grade}</p>}<p>{mild ? card.verdict_mild : card.verdict}</p><p>선물 컷 65 · {card.cut_distance}</p><p>{card.ranking_pending ? "순위 계산 중" : card.rank == null ? "순위 집계 전" : `순위 ${card.rank}위`}</p>{card.delivery_eta && <p>{formatKoreanDate(card.delivery_eta)}</p>}</>;
    case "ROAST": return <>{card.chip && <p className="flow-extra-chip">{card.chip.label} · {card.chip.value}</p>}<h3>{card.roast}</h3><p>{card.flip}</p></>;
    case "LETTER": return <><h3>산타가 남긴 쪽지</h3><p>{card.letter}</p></>;
    case "STORY": return <><h3>올해 너, 세 줄 요약</h3>{card.acts.map(act => <p key={act.act}><b>{act.months}</b> {act.line}</p>)}</>;
    case "ARCHETYPE": return <><h3>북극 인사팀 배치 결과</h3><p>{card.archetype.name}</p></>;
    case "DIMENSIONS": return <><h3>산타 채점표</h3>{card.bars.map(bar => <p key={bar.key}>{bar.label} <b>{bar.score.toFixed(2)}</b></p>)}</>;
    default: return null;
  }
}

function DossierContent({ scene }: { scene: DossierScene }) {
  return <>{scene.headline && <h3>{scene.headline}</h3>}{scene.big_value && <strong className="flow-extra-value">{scene.big_value}</strong>}{scene.lines.map(line => <p key={line.id} data-private={!line.shareable}>{line.text}</p>)}{scene.memo && <p>{scene.memo}</p>}{scene.chart && <div className="flow-extra-chart" aria-label={`${scene.chart.type} 그래프`}>{scene.chart.values.map((value, index) => <i key={index} style={{ height: `${Math.max(2, Math.min(100, value))}%` }} title={String(value)} />)}</div>}</>;
}
