"use client";

import { useEffect, useRef, useState } from "react";
import { createShare, resultError, type ShareResponse } from "@/lib/results";

const filename = "santacut-result.png";

export function ResultShare({ ranked }: { ranked: boolean }) {
  const [card, setCard] = useState<ShareResponse>();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const submitting = useRef(false);

  async function prepare() {
    if (submitting.current) return;
    submitting.current = true;
    setPending(true);
    setError("");
    try {
      setCard(await createShare());
    } catch (failure) {
      setError(resultError(failure));
    } finally {
      submitting.current = false;
      setPending(false);
    }
  }

  if (card) return <ShareActions card={card} />;
  if (!ranked) return <p className="flow-share-note">순위가 나오면 결과 카드를 공유할 수 있어요.</p>;
  return <section className="flow-share-panel" aria-label="결과 공유">
    <p className="flow-share-note">카드를 만들면 링크를 가진 사람이 결과를 볼 수 있어요.</p>
    <button type="button" className="flow-secondary" onClick={prepare} disabled={pending}>
      {pending ? "공유 카드 만드는 중" : "공유 카드 링크 만들기"}<span aria-hidden="true">↗</span>
    </button>
    {error && <p className="flow-error" role="alert">{error}</p>}
  </section>;
}

function ShareActions({ card }: { card: ShareResponse }) {
  const [image, setImage] = useState<{ file: File; url: string }>();
  const [imageFailed, setImageFailed] = useState(false);
  const [message, setMessage] = useState("");
  const linkInput = useRef<HTMLInputElement>(null);
  const imagePath = `/s/${encodeURIComponent(card.token)}/image.png`;

  useEffect(() => {
    const controller = new AbortController();
    let url: string | undefined;
    async function loadImage() {
      try {
        // Use the app's same-origin rewrite so browser CORS rules do not block PNG downloads.
        const response = await fetch(imagePath, { signal: controller.signal, headers: { Accept: "image/png" } });
        if (!response.ok || !response.headers.get("content-type")?.startsWith("image/png")) throw new Error("Invalid card image");
        const blob = await response.blob();
        if (controller.signal.aborted) return;
        const file = new File([blob], filename, { type: "image/png" });
        url = URL.createObjectURL(file);
        setImage({ file, url });
      } catch {
        if (!controller.signal.aborted) setImageFailed(true);
      }
    }
    void loadImage();
    return () => {
      controller.abort();
      if (url) URL.revokeObjectURL(url);
    };
  }, [imagePath]);

  async function copyLink() {
    try {
      await navigator.clipboard.writeText(card.url);
      setMessage("링크를 복사했어요. 카톡 채팅에 붙여넣으세요.");
    } catch {
      linkInput.current?.focus();
      linkInput.current?.select();
      setMessage("선택된 링크를 직접 복사해 주세요.");
    }
  }

  async function shareInstagram() {
    if (!image) return;
    const payload = { files: [image.file] };
    if (navigator.share && navigator.canShare?.(payload)) {
      try {
        await navigator.share(payload);
        return;
      } catch (error) {
        if (error instanceof DOMException && error.name === "AbortError") return;
      }
    }
    setMessage("이미지를 저장한 뒤 인스타그램에서 사진을 골라 올려주세요.");
  }

  async function shareKakao() {
    if (navigator.share) {
      try {
        await navigator.share({ title: "나의 산타컷 결과", url: card.url });
        return;
      } catch (error) {
        if (error instanceof DOMException && error.name === "AbortError") return;
      }
    }
    await copyLink();
  }

  return <section className="flow-share-panel" aria-label="결과 공유">
    <div className="flow-share-heading"><h2>결과 카드 공유</h2><a href={card.url} target="_blank" rel="noreferrer">링크 열기 ↗</a></div>
    <div className="flow-share-actions">
      <button type="button" onClick={shareInstagram} disabled={!image}>인스타 공유</button>
      <button type="button" onClick={shareKakao}>카톡 링크 공유</button>
      {image ? <a href={image.url} download={filename}>이미지 저장</a> :
        <a href={imagePath} target="_blank" rel="noreferrer">이미지 열기</a>}
    </div>
    <p className="flow-share-note">공유 메뉴에서 Instagram 또는 카카오톡을 선택하세요.</p>
    {imageFailed && <p className="flow-error" role="alert">이미지 파일을 준비하지 못했어요. 이미지 열기에서 저장하거나 새로고침 후 다시 시도해 주세요.</p>}
    {!image && !imageFailed && <p className="flow-share-note" role="status">이미지를 준비하고 있어요.</p>}
    <div className="flow-share-copy">
      <input ref={linkInput} aria-label="공개 결과 링크" value={card.url} readOnly />
      <button type="button" onClick={copyLink}>링크 복사</button>
    </div>
    {message && <p className="flow-share-note" role="status">{message}</p>}
  </section>;
}
