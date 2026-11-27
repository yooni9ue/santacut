import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { AxiosError, AxiosHeaders, type AxiosAdapter } from "axios";
import { afterEach, expect, test, vi } from "vitest";
import { api } from "@/lib/api";
import { RECAP_POLL_MS, recapRefetchInterval, type RecapCard, type RecapResponse } from "@/lib/results";
import { ResultExtras } from "./result-extras";

const original = api.defaults.adapter;
afterEach(() => { api.defaults.adapter = original; });

const top = { width: 1080, height: 1920, score_text: "72.40", rank: 48213, top_percent: 38 };
const bands = [["PREMIUM", "굴뚝 프리패스", "90+"], ["GIFT_DEPARTED", "썰매 탑승 완료", "75~89"], ["SCRAPED_THROUGH", "굴뚝에 걸쳐서 통과", "65~74"], ["ONE_STEP_SHORT", "굴뚝 앞 유턴", "55~64"], ["OFF_LIST", "명단에 없음", "50~54"], ["COAL", "석탄 당첨", "≤49"]]
  .map(([code, label, range]) => ({ code, label, range, current: code === "SCRAPED_THROUGH" }));
const cards: RecapCard[] = [
  { ...top, index: 0, kind: "VERDICT", verdict: "착한 아이", passed: true, score: 72.4, cut: 65, cut_distance: "+7.40", cut_distance_value: 7.4, grade: "굴뚝에 걸쳐서 통과", grade_code: "SCRAPED_THROUGH", verdict_line: "나쁜 아이냐고? 아뇨.", delivery_eta: "2026-12-24T15:30:00Z" },
  { ...top, index: 1, kind: "CUTLINE", total: 126000, grade: "굴뚝에 걸쳐서 통과", grade_code: "SCRAPED_THROUGH", band_detail: "바지 걸렸는데 통과", band_position: 2, bands },
  { ...top, index: 2, kind: "REASON", ordinal: 1, quote: "○○이는 언제 가능?", chip_text: "'ㄱ?' 발사 횟수 19번", label: "약속발사상", line: "판은 늘 여기서 열렸다.", dim_tag: null },
  { ...top, index: 3, kind: "CUT", ordinal: 1, quote: null, chip_text: "'고마워' 0번", label: "감사품절상", line: "고맙다는 말이 품절됐다.", dim_tag: "존중 −" },
  { ...top, index: 4, kind: "CONCLUSION", verdict: "착한 아이", passed: true, grade: "굴뚝에 걸쳐서 통과", grade_code: "SCRAPED_THROUGH", band_tag: "걸쳐서통과", band_position: 2, title: "약속 발사대", archetype_code: "CAROL_LEADER", archetype_name: "캐럴 떼창 리더", type_percent: 12, hero_quote: "○○이는 언제 가능?", hero_chip_text: "'ㄱ?' 발사 횟수 19번", final_line: "약속은 네가 쏘고 선물은 산타가.", cut_distance: "+7.40", friend_call: "우리 방에서 나보다 착한 애 나와" },
];
const recap: RecapResponse = { assessment_id: "a", skin: "GIFT", score: 72.4, score_text: "72.40", share_text: "서버 공유 문구", rank: 48213, top_percent: 38, ranking_pending: false, cards };

function renderDeck(data: RecapResponse) {
  api.defaults.adapter = vi.fn<AxiosAdapter>(async config => ({ config, status: 200, statusText: "OK", headers: {}, data }));
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(<QueryClientProvider client={client}><ResultExtras recapAvailable /></QueryClientProvider>);
}

test("every v9 card shows the rank bar, its kicker and server text without a mild toggle", async () => {
  renderDeck(recap);
  const deck = await screen.findByRole("region", { name: "팩폭 산타 카드" });
  const expectations = [
    ["최종 판정", "착한 아이"],
    ["산타컷 표", "선물 컷 65"],
    ["선물 받는 이유 1", "○○이는 언제 가능?"],
    ["점수 깎인 말 1", "'고마워' 0번"],
    ["결론", "#산타컷 #걸쳐서통과"],
  ];
  for (const [kicker, text] of expectations) {
    expect(within(deck).getByText("전국 48213등 · 상위 38%")).toBeInTheDocument();
    expect(within(deck).getAllByText(kicker).length).toBeGreaterThan(0);
    expect(within(deck).getByText(text)).toBeInTheDocument();
    if (kicker !== "결론") fireEvent.click(screen.getByRole("button", { name: "다음 →" }));
  }
  expect(screen.queryByText("농담 빼고 보기")).not.toBeInTheDocument();
  expect(screen.getByText("서버 공유 문구")).toBeInTheDocument();
});

test("a small ranked population without top_percent shows only the national rank", async () => {
  const small = { ...recap, top_percent: null, cards: cards.map(({ top_percent: _omit, ...card }) => card) } as RecapResponse;
  renderDeck(small);
  const deck = await screen.findByRole("region", { name: "팩폭 산타 카드" });
  expect(within(deck).getByText("전국 48213등")).toBeInTheDocument();
  expect(within(deck).queryByText(/상위 undefined/)).not.toBeInTheDocument();
});

test("the cutline lights only the current band and evidence without a quote shows a giant chip", async () => {
  renderDeck(recap);
  await screen.findByRole("region", { name: "팩폭 산타 카드" });
  fireEvent.click(screen.getByRole("button", { name: "2번째 카드 산타컷 표" }));
  const current = screen.getByRole("list", { name: "산타컷 구간표" }).querySelectorAll("[aria-current=true]");
  expect(current).toHaveLength(1);
  expect(current[0]).toHaveTextContent("굴뚝에 걸쳐서 통과");
  expect(current[0]).toHaveTextContent("나 72.40점");
  fireEvent.click(screen.getByRole("button", { name: "4번째 카드 점수 깎인 말 1" }));
  expect(screen.getByText("'고마워' 0번")).toHaveClass("is-giant");
  expect(screen.getByText("존중 −")).toBeInTheDocument();
});

test("a failed rank refresh stops polling and shows the error next to the last deck", async () => {
  const pending = { ...recap, rank: null, top_percent: null, ranking_pending: true, cards: cards.map(card => ({ ...card, rank: null, top_percent: null })) };
  const config = { headers: new AxiosHeaders() };
  const failure = new AxiosError("failed", undefined, config, undefined, { status: 500, statusText: "", headers: {}, data: {}, config });
  api.defaults.adapter = vi.fn<AxiosAdapter>()
    .mockImplementationOnce(async request => ({ config: request, status: 200, statusText: "OK", headers: {}, data: pending }))
    .mockRejectedValue(failure);
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(<QueryClientProvider client={client}><ResultExtras recapAvailable /></QueryClientProvider>);
  expect(await screen.findByText("순위 계산 중")).toBeInTheDocument();
  const query = client.getQueryCache().find<RecapResponse | null>({ queryKey: ["me", "recap", ""] })!;
  expect(recapRefetchInterval(query.state)).toBe(RECAP_POLL_MS);
  await act(() => client.refetchQueries({ queryKey: ["me", "recap"] }));
  expect(await screen.findByRole("alert")).toHaveTextContent("새 결과 카드를 불러오지 못했어요.");
  expect(screen.getByRole("region", { name: "팩폭 산타 카드" })).toBeInTheDocument();
  expect(recapRefetchInterval(query.state)).toBe(false);
});

test("a pending rank shows 순위 계산 중 and never 순위 집계 전", async () => {
  const pending = { ...recap, rank: null, top_percent: null, ranking_pending: true, cards: cards.map(card => ({ ...card, rank: null, top_percent: null })) };
  renderDeck(pending);
  expect(await screen.findByText("순위 계산 중")).toBeInTheDocument();
  expect(screen.queryByText(/순위 집계 전/)).not.toBeInTheDocument();
  expect(screen.queryByText(/전국 .*등/)).not.toBeInTheDocument();
});
