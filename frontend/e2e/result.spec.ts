import { expect, test } from "@playwright/test";
import { readFile } from "node:fs/promises";

test("결과를 PNG로 공유·저장하고 카톡용 링크를 공유한다", async ({ page }) => {
  await page.addInitScript(() => {
    const calls: { url?: string; files?: string[] }[] = [];
    Object.defineProperty(window, "shareCalls", { value: calls });
    Object.defineProperty(navigator, "canShare", { value: () => true });
    Object.defineProperty(navigator, "share", { configurable: true, value: async (data: ShareData) => {
      calls.push({ url: data.url, files: data.files?.map(file => `${file.name}:${file.type}`) });
    } });
  });
  await page.route("**/v1/me/result", route => route.fulfill({ json: { score_hundredths: 8245, verdict: "기록이 꽤 다정해요.", comment: "오늘도 따뜻했어요.", archetype: { code: "HOT_COCOA_COURIER", name: "핫초코 배달부" }, rank: 3, tied: true, eta: "2026-12-26T03:24:00Z", delivery_sequence: 4, computed_at: "2026-11-17T00:00:00Z", ranking_pending: false, stage: "PREVIEW" } }));
  await page.route("**/v1/csrf", route => route.fulfill({ json: { headerName: "X-XSRF-TOKEN", token: "test-token" } }));
  const shareUrl = "http://127.0.0.1:3100/s/test-share";
  let shareRequests = 0;
  await page.route("**/v1/shares", route => {
    shareRequests++;
    expect(route.request().headers()["x-xsrf-token"]).toBe("test-token");
    expect(route.request().postDataJSON()).toEqual({ include_quote: false });
    return route.fulfill({ json: { token: "test-share", url: "https://api-dev.santacut.me/s/test-share", imageUrl: "https://api-dev.santacut.me/s/test-share/image.png" } });
  });
  await page.route("**/s/test-share/image.png", route => route.fulfill({
    contentType: "image/png",
    body: Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/lXQAAAAASUVORK5CYII=", "base64"),
  }));
  await page.route("**/v1/rankings**", route => route.fulfill({ json: { season_year: 2026, participant_count: 1, computed_at: null, stage: "PREVIEW", rows: [{ nickname: "테스터", rank: 3, tied: true, score_hundredths: 8245, eta: null, eta_date: "2026-12-26" }], next_cursor: null } }));
  await page.goto("/result");
  await expect(page.getByRole("main").getByText(/82\.45/)).toBeVisible();
  await expect(page.getByRole("main").getByText("공동 3위", { exact: true })).toBeVisible();
  expect(shareRequests).toBe(0);
  await page.getByRole("button", { name: "공유 카드 링크 만들기" }).click();
  await page.getByRole("button", { name: "인스타 공유" }).click();
  await page.getByRole("button", { name: "카톡 링크 공유" }).click();
  expect(await page.evaluate(() => Reflect.get(window, "shareCalls"))).toEqual([
    { url: undefined, files: ["santacut-result.png:image/png"] },
    { url: shareUrl, files: undefined },
  ]);
  const [download] = await Promise.all([
    page.waitForEvent("download"),
    page.getByRole("link", { name: "이미지 저장" }).click(),
  ]);
  expect(download.suggestedFilename()).toBe("santacut-result.png");
  await page.evaluate(() => {
    Object.defineProperty(navigator, "share", { value: undefined });
    Object.defineProperty(navigator, "clipboard", { value: { writeText: async (url: string) => {
      Object.defineProperty(window, "copiedLink", { value: url });
    } } });
  });
  await page.getByRole("button", { name: "인스타 공유" }).click();
  await expect(page.getByRole("status")).toContainText("이미지를 저장한 뒤");
  await page.getByRole("button", { name: "카톡 링크 공유" }).click();
  await expect(page.getByRole("status")).toContainText("링크를 복사했어요");
  expect(await page.evaluate(() => Reflect.get(window, "copiedLink"))).toBe(shareUrl);
  expect(shareRequests).toBe(1);
});

type Top = { rank: number | null; top_percent: number | null };
const ranked: Top = { rank: 48213, top_percent: 38 };
const bands = [["PREMIUM", "굴뚝 프리패스", "90+"], ["GIFT_DEPARTED", "썰매 탑승 완료", "75~89"], ["SCRAPED_THROUGH", "굴뚝에 걸쳐서 통과", "65~74"], ["ONE_STEP_SHORT", "굴뚝 앞 유턴", "55~64"], ["OFF_LIST", "명단에 없음", "50~54"], ["COAL", "석탄 당첨", "≤49"]]
  .map(([code, label, range]) => ({ code, label, range, current: code === "SCRAPED_THROUGH" }));
const base = (index: number, top: Top) => ({ index, width: 1080, height: 1920, score_text: "72.40", ...top });
const verdictCard = (top: Top) => ({ ...base(0, top), kind: "VERDICT", verdict: "착한 아이", passed: true, score: 72.4, cut: 65, cut_distance: "+7.40", cut_distance_value: 7.4,
  grade: "굴뚝에 걸쳐서 통과", grade_code: "SCRAPED_THROUGH", verdict_line: "나쁜 아이냐고? 아뇨아뇨.", delivery_eta: top.rank == null ? null : "2026-12-24T15:30:00Z" });
const cutlineCard = (top: Top) => ({ ...base(1, top), kind: "CUTLINE", total: top.rank == null ? null : 126000, grade: "굴뚝에 걸쳐서 통과", grade_code: "SCRAPED_THROUGH",
  band_detail: "바지 걸렸는데 통과", band_position: 2, bands });
const evidenceCard = (index: number, top: Top, kind: string, ordinal: number, quote: string | null) => ({ ...base(index, top), kind, ordinal, quote,
  chip_text: quote ? "'ㄱ?' 발사 횟수 19번" : "'고마워' 0번", label: kind === "REASON" ? "약속발사상" : "감사품절상", line: `한 줄 ${index}`, dim_tag: null });
const conclusionCard = (index: number, top: Top) => ({ ...base(index, top), kind: "CONCLUSION", verdict: "착한 아이", passed: true, grade: "굴뚝에 걸쳐서 통과", grade_code: "SCRAPED_THROUGH",
  band_tag: "걸쳐서통과", band_position: 2, title: "약속 발사대", archetype_code: "CAROL_LEADER", archetype_name: "캐럴 떼창 리더", type_percent: 12,
  hero_quote: "○○이는 언제 가능?", hero_chip_text: "'ㄱ?' 발사 횟수 19번", final_line: "약속은 네가 쏘고 선물은 산타가.", cut_distance: "+7.40", friend_call: "우리 방에서 나보다 착한 애 나와" });
function recapDeck(assessment: string, top: Top = ranked) {
  const cards = [verdictCard(top), cutlineCard(top), evidenceCard(2, top, "REASON", 1, "○○이는 언제 가능?"), evidenceCard(3, top, "CUT", 1, null), conclusionCard(4, top)];
  return { assessment_id: assessment, skin: "GIFT", score: 72.4, score_text: "72.40", share_text: "서버 공유 문구", ...top, ranking_pending: top.rank == null, cards };
}

test("새 결과의 v9 리캡과 조서를 표시하고 카드 이미지를 저장한다", async ({ page }) => {
  await page.route("**/v1/me/result", route => route.fulfill({ json: {
    score_hundredths: 7240, verdict: "선물 실었다", comment: "올해도 수고했어.", rank: 48213, tied: false,
    eta: "2026-12-24T15:30:00Z", ranking_pending: false, recap_available: true, dossier_available: true,
  } }));
  await page.route("**/v1/me/recap", route => route.fulfill({ json: recapDeck("test-assessment") }));
  await page.route("**/v1/me/dossier", route => route.fulfill({ json: {
    title: "웃음 과다 야간 근무자", tone: "normal", scenes: [{ id: "hook", kind: "hook", speaker: "elf", headline: "올해의 한마디",
      big_value: "킹받네", lines: [{ id: "public", text: "312번", shareable: true }, { id: "private", text: "화면에서만 보는 말", shareable: false }],
      memo: null, chart: null, shareable: true }],
  } }));
  await page.goto("/result");
  const deck = page.getByRole("region", { name: "팩폭 산타 카드" });
  await expect(deck.getByRole("heading", { name: "착한 아이" })).toBeVisible();
  await expect(deck.getByText("전국 48213등 · 상위 38%")).toBeVisible();
  await expect(page.getByText("농담 빼고 보기")).toHaveCount(0);
  await page.getByText("산타 조서 더 보기").click();
  await expect(page.getByText("킹받네")).toBeVisible();
  await page.getByRole("button", { name: "2번째 카드 산타컷 표" }).click();
  await expect(deck.getByText("선물 컷 65")).toBeVisible();
  await expect(deck.getByText("나 72.40점")).toBeVisible();
  await page.getByRole("button", { name: "3번째 카드 선물 받는 이유 1" }).click();
  await expect(deck.getByText("○○이는 언제 가능?")).toBeVisible();
  await expect(deck.getByRole("heading", { name: "약속발사상" })).toBeVisible();
  await page.getByRole("button", { name: "4번째 카드 점수 깎인 말 1" }).click();
  await expect(deck.getByText("'고마워' 0번")).toBeVisible();
  await page.getByRole("button", { name: "5번째 카드 결론" }).click();
  await expect(deck.getByRole("heading", { name: "굴뚝에 걸쳐서 통과" })).toBeVisible();
  await expect(deck.getByText("약속 발사대 · 캐럴 떼창 리더")).toBeVisible();
  await expect(deck.getByText("#산타컷 #걸쳐서통과")).toBeVisible();
  const [download] = await Promise.all([
    page.waitForEvent("download"),
    deck.getByRole("button", { name: "현재 카드 저장" }).click(),
  ]);
  expect(download.suggestedFilename()).toBe("santacut-recap-5.png");
  const image = await readFile(await download.path());
  expect(image.subarray(1, 4).toString()).toBe("PNG");
  expect(image.readUInt32BE(16)).toBe(1080);
  expect(image.readUInt32BE(20)).toBe(1920);
});

test("순위 계산 중에는 리캡을 먼저 보여 주고 순위가 나올 때까지 다시 부른다", async ({ page }) => {
  await page.route("**/v1/me/analysis", route => route.fulfill({ json: { id: "analysis-pending", status: "RANKING_PENDING", error_code: null } }));
  await page.route("**/v1/analyses/analysis-pending", route => route.fulfill({ json: { id: "analysis-pending", status: "RANKING_PENDING", error_code: null } }));
  await page.route("**/v1/analyses/analysis-pending/events", route => route.fulfill({ contentType: "text/event-stream", body: "" }));
  let recapRequests = 0;
  await page.route("**/v1/me/recap", route => {
    recapRequests++;
    return route.fulfill({ json: recapDeck("pending", recapRequests < 2 ? { rank: null, top_percent: null } : ranked) });
  });
  await page.clock.install();
  await page.goto("/analysis");
  const deck = page.getByRole("region", { name: "팩폭 산타 카드" });
  await expect(deck.getByRole("heading", { name: "착한 아이" })).toBeVisible();
  await expect(deck.getByText("순위 계산 중", { exact: true })).toBeVisible();
  await expect(page.getByText("순위 집계 전")).toHaveCount(0);
  await page.clock.runFor(3_100);
  await expect(deck.getByText("전국 48213등 · 상위 38%")).toBeVisible();
  await expect.poll(() => recapRequests).toBe(2);
  // Advance well past several poll intervals; a settled rank must not trigger more requests.
  await page.clock.runFor(10_000);
  await expect.poll(() => recapRequests).toBe(2);
});

test("여덟 장 리캡의 마지막 카드까지 이동할 수 있다", async ({ page }) => {
  await page.route("**/v1/me/result", route => route.fulfill({ json: {
    score_hundredths: 7240, verdict: "판정", comment: "평", rank: 1, tied: false,
    eta: null, ranking_pending: false, recap_available: true, dossier_available: false,
  } }));
  const deck = recapDeck("eight-cards");
  const cards = [deck.cards[0], deck.cards[1],
    ...[1, 2, 3].map(ordinal => evidenceCard(1 + ordinal, ranked, "REASON", ordinal, ordinal === 2 ? null : `이유 말풍선 ${ordinal}`)),
    ...[1, 2].map(ordinal => evidenceCard(4 + ordinal, ranked, "CUT", ordinal, `깎인 말풍선 ${ordinal}`)),
    conclusionCard(7, ranked)];
  await page.route("**/v1/me/recap", route => route.fulfill({ json: { ...deck, cards } }));
  await page.goto("/result");
  await expect(page.getByText("1 / 8")).toBeVisible();
  await page.getByRole("button", { name: "7번째 카드 점수 깎인 말 2" }).click();
  await expect(page.getByRole("region", { name: "팩폭 산타 카드" }).getByText("깎인 말풍선 2")).toBeVisible();
  await page.getByRole("button", { name: "8번째 카드 결론" }).click();
  await expect(page.getByText("8 / 8")).toBeVisible();
  await expect(page.getByRole("region", { name: "팩폭 산타 카드" }).getByText("우리 방에서 나보다 착한 애 나와")).toBeVisible();
  await expect(page.getByRole("button", { name: "다음 →" })).toBeDisabled();
  await page.getByRole("button", { name: "← 이전" }).click();
  await expect(page.getByText("7 / 8", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "전체 8장 개별 저장", exact: true })).toBeEnabled();
});

test("좁은 화면에서도 결론과 근거 카드가 카드 안에 들어간다", async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 740 });
  await page.route("**/v1/me/result", route => route.fulfill({ json: {
    score_hundredths: 7240, verdict: "판정", rank: 1, recap_available: true, dossier_available: false,
  } }));
  // Worst case from the contract limits: 20-char quotes (+ "…"), 22-char final line, 8-char title, longest grade.
  const longQuote = "○○이는 언제 가능한지 빨리 말해 줘…";
  const deck = recapDeck("narrow");
  const cards = deck.cards.map(card => card.kind === "CONCLUSION"
    ? { ...card, title: "약속발사대장님아", archetype_name: "갑자기 약속 잡는 애 원탑", hero_quote: longQuote, final_line: "약속은 네가 쏘고 선물은 산타가 들고 가" }
    : card.kind === "REASON" ? { ...card, quote: longQuote, line: "판은 늘 여기서 열렸고 너는 늘 먼저 왔다" } : card);
  await page.route("**/v1/me/recap", route => route.fulfill({ json: { ...deck, cards } }));
  await page.goto("/result");
  await page.evaluate(() => document.fonts.ready);
  const fits = () => page.locator(".flow-recap-card").evaluate(card => {
    const text = card.querySelector(".flow-recap-copy")!.getBoundingClientRect();
    const artwork = card.querySelector(".flow-recap-art")?.getBoundingClientRect();
    return { contentHeight: card.scrollHeight, height: card.clientHeight, textBottom: text.bottom, artworkTop: artwork?.top ?? text.bottom };
  });
  for (const name of ["2번째 카드 산타컷 표", "3번째 카드 선물 받는 이유 1", "5번째 카드 결론"]) {
    await page.getByRole("button", { name }).click();
    const measurements = await fits();
    expect(measurements.contentHeight).toBeLessThanOrEqual(measurements.height + 1);
    expect(measurements.textBottom).toBeLessThanOrEqual(measurements.artworkTop + 1);
  }
  const conclusion = await page.locator(".flow-recap-card").evaluate(card => {
    const footer = card.querySelector(".flow-recap-card-bottom")!.getBoundingClientRect().top;
    return ["flow-recap-friend", "flow-recap-hashtags"].map(name => card.querySelector(`.${name}`)!.getBoundingClientRect().bottom <= footer + 1);
  });
  expect(conclusion).toEqual([true, true]);
  await expect(page.getByText("#산타컷 #걸쳐서통과")).toBeVisible();
});

test("리캡 링크는 클릭할 때 생성하고 복사 실패 시 직접 복사할 수 있다", async ({ page }) => {
  await page.route("**/v1/me/result", route => route.fulfill({ json: {
    score_hundredths: 7240, verdict: "판정", rank: 1, recap_available: true,
  } }));
  await page.route("**/v1/me/recap", route => route.fulfill({ json: recapDeck("link-test") }));
  await page.route("**/v1/csrf", route => route.fulfill({ json: { headerName: "X-XSRF-TOKEN", token: "test-token" } }));
  let requests = 0;
  const url = "http://127.0.0.1:3100/s/recap-test";
  await page.route("**/v1/shares", route => {
    requests++;
    expect(route.request().postDataJSON()).toEqual({ include_quote: false });
    expect(route.request().headers()["x-xsrf-token"]).toBe("test-token");
    return route.fulfill({ json: { token: "recap-test", url: "https://api-dev.santacut.me/s/recap-test", imageUrl: "https://api-dev.santacut.me/s/recap-test/image.png" } });
  });
  await page.addInitScript(() => {
    Object.defineProperty(navigator, "share", { configurable: true, value: undefined });
    Object.defineProperty(navigator, "clipboard", { configurable: true, value: { writeText: async (value: string) => {
      Reflect.set(window, "copiedRecapLink", value);
    } } });
  });
  await page.goto("/result");
  const button = page.getByRole("button", { name: "링크 공유하기", exact: true });
  await expect(button).toBeEnabled();
  await expect(page.getByText("링크를 받은 사람은 누구나 리캡 카드를 모두 볼 수 있어요.")).toBeVisible();
  expect(requests).toBe(0);
  await button.click();
  await expect(page.getByRole("status")).toContainText("링크를 복사했어요");
  expect(await page.evaluate(() => Reflect.get(window, "copiedRecapLink"))).toBe(url);
  await page.evaluate(() => {
    Object.defineProperty(navigator, "clipboard", { value: { writeText: async () => { throw new Error("Denied"); } } });
  });
  await button.click();
  await expect(page.getByRole("status")).toContainText("직접 복사");
  await expect(page.getByRole("textbox", { name: "공유할 결과 링크" })).toHaveValue(url);
  expect(requests).toBe(1);
});
