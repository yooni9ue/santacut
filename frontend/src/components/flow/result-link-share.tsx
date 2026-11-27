"use client";

import { useRef, useState } from "react";
import { createShare, resultError } from "@/lib/results";

export function ResultLinkShare({ ranked }: { ranked: boolean }) {
  const [url, setUrl] = useState("");
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const submitting = useRef(false);
  const input = useRef<HTMLInputElement>(null);

  async function shareLink() {
    if (!ranked || submitting.current) return;
    submitting.current = true;
    setPending(true);
    setMessage("");
    setError("");
    try {
      const link = url || (await createShare()).url;
      setUrl(link);
      if (navigator.share) {
        try {
          await navigator.share({ title: "나의 산타컷 결과", url: link });
          return;
        } catch (failure) {
          if (failure instanceof DOMException && failure.name === "AbortError") return;
        }
      }
      try {
        await navigator.clipboard.writeText(link);
        setMessage("링크를 복사했어요. 원하는 곳에 붙여넣어 주세요.");
      } catch {
        setMessage("아래 링크를 길게 누르거나 선택해서 직접 복사해 주세요.");
      }
    } catch (failure) {
      setError(resultError(failure));
    } finally {
      submitting.current = false;
      setPending(false);
    }
  }

  return <div className="flow-recap-link-share">
    <button type="button" className="flow-secondary" disabled={!ranked || pending} onClick={() => void shareLink()}>
      {pending ? "링크 만드는 중…" : "링크 공유하기"}<span aria-hidden="true">↗</span>
    </button>
    <p className="flow-share-note">{ranked ? "링크를 받은 사람은 누구나 리캡 카드를 모두 볼 수 있어요." : "순위가 나오면 링크를 공유할 수 있어요."}</p>
    {url && <input ref={input} aria-label="공유할 결과 링크" value={url} readOnly onFocus={() => input.current?.select()} />}
    {message && <p className="flow-share-note" role="status">{message}</p>}
    {error && <p className="flow-error" role="alert">{error}</p>}
  </div>;
}
