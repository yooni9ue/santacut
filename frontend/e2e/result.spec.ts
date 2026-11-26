import { expect, test } from "@playwright/test";
import { unzipSync } from "fflate";
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
  const shareUrl = "https://santacut.me/s/test-share";
  let shareRequests = 0;
  await page.route("**/v1/shares", route => {
    shareRequests++;
    expect(route.request().headers()["x-xsrf-token"]).toBe("test-token");
    expect(route.request().postDataJSON()).toEqual({ include_quote: false });
    return route.fulfill({ json: { token: "test-share", url: shareUrl, imageUrl: `${shareUrl}/image.png` } });
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

test("새 결과의 리캡과 조서를 표시하고 카드 이미지를 저장한다", async ({ page }) => {
  await page.route("**/v1/me/result", route => route.fulfill({ json: {
    score_hundredths: 7055, verdict: "선물 실었다", comment: "올해도 수고했어.", rank: 1, tied: false,
    eta: "2026-12-24T18:30:02Z", ranking_pending: false, recap_available: true, dossier_available: true,
  } }));
  await page.route("**/v1/me/recap", route => route.fulfill({ json: {
    assessment_id: "test-assessment", skin: "GIFT", score_text: "70.55", share_text: "서버 공유 문구", share_text_mild: "담백한 서버 공유 문구",
    cards: [{ index: 0, kind: "VERDICT", width: 1080, height: 1920, score_text: "70.55", cut_distance: "+5.55", passed: true,
      stamp: "선물 실었다", stamp_mild: "선물 실었다", grade: "썰매 탑승 완료", rank: 1, ranking_pending: false,
      delivery_eta: "2026-12-24T18:30:02Z", title: "단톡방 출석왕", verdict: "산타가 봤다.", verdict_mild: "선물 실어 뒀다." }],
  } }));
  await page.route("**/v1/me/dossier", route => route.fulfill({ json: {
    title: "웃음 과다 야간 근무자", tone: "normal", scenes: [{ id: "hook", kind: "hook", speaker: "elf", headline: "올해의 한마디",
      big_value: "킹받네", lines: [{ id: "public", text: "312번", shareable: true }, { id: "private", text: "화면에서만 보는 말", shareable: false }],
      memo: null, chart: null, shareable: true }],
  } }));
  await page.goto("/result");
  await expect(page.getByRole("region", { name: "팩폭 산타 카드" }).getByText("단톡방 출석왕")).toBeVisible();
  await page.getByText("산타 조서 더 보기").click();
  await expect(page.getByText("킹받네")).toBeVisible();
  await page.getByRole("checkbox", { name: "농담 빼고 보기" }).check();
  await expect(page.getByRole("region", { name: "팩폭 산타 카드" }).getByText("선물 실어 뒀다.")).toBeVisible();
  const [download] = await Promise.all([
    page.waitForEvent("download"),
    page.getByRole("region", { name: "팩폭 산타 카드" }).getByRole("button", { name: "현재 카드 저장" }).click(),
  ]);
  expect(download.suggestedFilename()).toBe("santacut-recap-1.png");
  const image = await readFile(await download.path());
  expect(image.subarray(1, 4).toString()).toBe("PNG");
  expect(image.readUInt32BE(16)).toBe(1080);
  expect(image.readUInt32BE(20)).toBe(1920);
});

test("순위 계산 중에는 새 리캡을 먼저 보여 준다", async ({ page }) => {
  await page.route("**/v1/me/analysis", route => route.fulfill({ json: { id: "analysis-pending", status: "RANKING_PENDING", error_code: null } }));
  await page.route("**/v1/analyses/analysis-pending", route => route.fulfill({ json: { id: "analysis-pending", status: "RANKING_PENDING", error_code: null } }));
  await page.route("**/v1/analyses/analysis-pending/events", route => route.fulfill({ contentType: "text/event-stream", body: "" }));
  await page.route("**/v1/me/recap", route => route.fulfill({ json: {
    assessment_id: "pending", skin: "GIFT", score_text: "70.55", share_text: "서버 공유 문구", share_text_mild: "담백한 문구",
    cards: [{ index: 0, kind: "VERDICT", width: 1080, height: 1920, score_text: "70.55", cut_distance: "+5.55", passed: true,
      stamp: "선물 실었다", stamp_mild: "선물 실었다", grade: "썰매 탑승 완료", rank: null, ranking_pending: true,
      delivery_eta: null, title: "출석왕", verdict: "산타가 봤다.", verdict_mild: "선물 실어 뒀다." }],
  } }));
  await page.goto("/analysis");
  await expect(page.getByRole("region", { name: "팩폭 산타 카드" }).getByText("출석왕")).toBeVisible();
  await expect(page.getByText("순위 계산 중", { exact: true })).toBeVisible();
});

test("여덟 장 리캡의 마지막 카드까지 이동할 수 있다", async ({ page }) => {
  await page.route("**/v1/me/result", route => route.fulfill({ json: {
    score_hundredths: 7055, verdict: "판정", comment: "평", rank: 1, tied: false,
    eta: null, ranking_pending: false, recap_available: true, dossier_available: false,
  } }));
  await page.route("**/v1/me/recap", route => route.fulfill({ json: {
    assessment_id: "eight-cards", skin: "GIFT", score_text: "70.55", share_text: "공유 문구", share_text_mild: "담백 문구",
    cards: Array.from({ length: 8 }, (_, index) => ({ index, kind: "ROAST", width: 1080, height: 1920,
      score_text: "70.55", chip: null, roast: `카드 ${index + 1}`, flip: "근데 괜찮아." })),
  } }));
  await page.goto("/result");
  await expect(page.getByText("1 / 8")).toBeVisible();
  await page.getByRole("button", { name: "8번째 카드 산타의 팩폭" }).click();
  await expect(page.getByText("8 / 8")).toBeVisible();
  await expect(page.getByRole("region", { name: "팩폭 산타 카드" }).getByText("카드 8")).toBeVisible();
  await expect(page.getByRole("button", { name: "다음 →" })).toBeDisabled();
  await page.getByRole("button", { name: "← 이전" }).click();
  await expect(page.getByText("7 / 8")).toBeVisible();
  const [download] = await Promise.all([
    page.waitForEvent("download"),
    page.getByRole("button", { name: "전체 8장 저장 (ZIP)", exact: true }).click(),
  ]);
  expect(download.suggestedFilename()).toBe("santacut-recap-all.zip");
  const files = unzipSync(await readFile(await download.path()));
  expect(Object.keys(files)).toEqual(Array.from({ length: 8 }, (_, i) => `santacut-recap-${String(i + 1).padStart(2, "0")}.png`));
  for (const bytes of Object.values(files)) {
    const png = Buffer.from(bytes);
    expect(png.subarray(1, 4).toString()).toBe("PNG");
    expect(png.readUInt32BE(16)).toBe(1080);
    expect(png.readUInt32BE(20)).toBe(1920);
  }
  expect(new Set(Object.values(files).map(bytes => Buffer.from(bytes).toString("base64"))).size).toBe(8);
  await expect(page.getByText("7 / 8", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "전체 8장 저장 (ZIP)", exact: true })).toBeEnabled();

});

test("좁은 화면에서도 긴 편지와 일러스트가 카드 안에 들어간다", async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 740 });
  const letter = "올해도 네가 먼저 안부를 물어서 이 방에 웃을 일이 많았어. 어색한 침묵마다 먼저 말을 꺼내고 친구의 기쁜 일에도 제일 크게 웃었지. 북극에서도 그 마음을 다 읽었어. 내년에도 지금처럼 다정하게 지내라.";
  await page.route("**/v1/me/result", route => route.fulfill({ json: {
    score_hundredths: 7055, verdict: "판정", rank: 1, recap_available: true, dossier_available: false,
  } }));
  await page.route("**/v1/me/recap", route => route.fulfill({ json: {
    assessment_id: "long-letter", skin: "GIFT", score_text: "70.55", share_text: "공유 문구", share_text_mild: "담백 문구",
    cards: [{ index: 0, kind: "LETTER", width: 1080, height: 1920, score_text: "70.55", letter }],
  } }));
  await page.goto("/result");
  await expect(page.getByText(letter)).toBeVisible();
  await page.evaluate(() => document.fonts.ready);
  const measurements = await page.locator(".flow-recap-card").evaluate(card => {
    const text = card.querySelector(".flow-recap-copy")!.getBoundingClientRect();
    const artwork = card.querySelector(".flow-recap-art")!.getBoundingClientRect();
    return { contentHeight: card.scrollHeight, height: card.clientHeight, textBottom: text.bottom, artworkTop: artwork.top };
  });
  expect(measurements.contentHeight).toBeLessThanOrEqual(measurements.height + 1);
  expect(measurements.textBottom).toBeLessThanOrEqual(measurements.artworkTop + 1);
});

test("리캡 링크는 클릭할 때 생성하고 복사 실패 시 직접 복사할 수 있다", async ({ page }) => {
  await page.route("**/v1/me/result", route => route.fulfill({ json: {
    score_hundredths: 7055, verdict: "판정", rank: 1, recap_available: true,
  } }));
  await page.route("**/v1/me/recap", route => route.fulfill({ json: {
    assessment_id: "link-test", skin: "GIFT", score_text: "70.55", share_text: "공유 문구", share_text_mild: "공유 문구",
    cards: [{ index: 0, kind: "LETTER", score_text: "70.55", letter: "올해도 고마웠어." }],
  } }));
  await page.route("**/v1/csrf", route => route.fulfill({ json: { headerName: "X-XSRF-TOKEN", token: "test-token" } }));
  let requests = 0;
  const url = "https://santacut.me/s/recap-test";
  await page.route("**/v1/shares", route => {
    requests++;
    expect(route.request().postDataJSON()).toEqual({ include_quote: false });
    expect(route.request().headers()["x-xsrf-token"]).toBe("test-token");
    return route.fulfill({ json: { token: "recap-test", url, imageUrl: `${url}/image.png` } });
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
