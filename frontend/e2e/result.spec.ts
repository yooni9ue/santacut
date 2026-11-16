import { expect, test } from "@playwright/test";

test("실제 결과와 랭킹은 서버 응답을 표시하고 공유 링크를 만든다", async ({ page }) => {
  await page.route("**/v1/me/result", route => route.fulfill({ json: { nickname: "테스터", score_hundredths: 8245, verdict: "기록이 꽤 다정해요.", title: "산타의 관찰 결과", rank: 3, tied: true, eta: "2026-12-26T03:24:00Z", computed_at: "2026-11-17T00:00:00Z", ranking_pending: false, stage: "PREVIEW" } }));
  await page.route("**/v1/csrf", route => route.fulfill({ json: { headerName: "X-XSRF-TOKEN", token: "test-token" } }));
  await page.route("**/v1/shares", route => route.fulfill({ json: { html_url: "https://santacut.me/s/test-share", image_url: "https://santacut.me/s/test-share.png" } }));
  await page.route("**/v1/rankings**", route => route.fulfill({ json: { season_year: 2026, participant_count: 1, computed_at: null, stage: "PREVIEW", rows: [{ nickname: "테스터", rank: 3, tied: true, score_hundredths: 8245, eta: null }], next_cursor: null } }));
  await page.goto("/result");
  await expect(page.getByRole("main").getByText(/82\.45/)).toBeVisible();
  await expect(page.getByRole("main").getByText("공동 3위", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "공유 카드 링크 만들기" }).click();
  await expect(page.getByRole("status")).toContainText("새 창에서 열기");
});
