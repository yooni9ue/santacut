import { expect, test } from "@playwright/test";

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
