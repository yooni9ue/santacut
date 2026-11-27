import { AxiosError, AxiosHeaders, type AxiosAdapter } from "axios";
import { afterEach, expect, test, vi } from "vitest";
import { api } from "./api";
import { getMyRecap, RECAP_POLL_MS, recapRefetchInterval, type RecapResponse } from "./results";

const original = api.defaults.adapter;
afterEach(() => { api.defaults.adapter = original; });

const top = { width: 1080, height: 1920, score_text: "72.40", rank: null, top_percent: null };
const recap: RecapResponse = {
  assessment_id: "a", skin: "GIFT", score: 72.4, score_text: "72.40", share_text: "공유 문구", rank: null, top_percent: null, ranking_pending: true,
  cards: [{ ...top, index: 0, kind: "REASON", ordinal: 1, quote: null, chip_text: "'ㄱ?' 19번", label: "약속발사상", line: "판은 늘 여기서 열렸다.", dim_tag: null }],
};

test("recap skips unknown card kinds and keeps the rest of the deck", async () => {
  const cards = [...recap.cards, { ...top, index: 1, kind: "FUTURE_CARD" }];
  api.defaults.adapter = vi.fn<AxiosAdapter>(async config => ({ config, status: 200, statusText: "OK", headers: {}, data: { ...recap, cards } }));
  expect((await getMyRecap())?.cards.map(card => card.kind)).toEqual(["REASON"]);
});

test("recap treats 404 as no deck, but does not swallow other failures", async () => {
  const config = { headers: new AxiosHeaders() };
  const fail = (status: number) => new AxiosError("failed", undefined, config, undefined, { status, statusText: "", headers: {}, data: {}, config });
  api.defaults.adapter = vi.fn<AxiosAdapter>().mockRejectedValue(fail(404));
  expect(await getMyRecap()).toBeNull();
  api.defaults.adapter = vi.fn<AxiosAdapter>().mockRejectedValue(fail(401));
  await expect(getMyRecap()).rejects.toThrow();
});

test("recap polls only while ranking is pending", () => {
  expect(recapRefetchInterval(recap)).toBe(RECAP_POLL_MS);
  expect(recapRefetchInterval({ ...recap, ranking_pending: false, rank: 3, top_percent: 1 })).toBe(false);
  expect(recapRefetchInterval(null)).toBe(false);
  expect(recapRefetchInterval(undefined)).toBe(false);
});
