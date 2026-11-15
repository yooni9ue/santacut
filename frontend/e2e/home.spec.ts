import { expect, test } from "@playwright/test";

test.beforeEach(async ({ page }) => { await page.goto("/"); });

test("mobile landing stays within the viewport and Santa can be rescued", async ({ page }) => {
  await expect(page.getByRole("heading", { level: 1 })).toContainText("착한 척");
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  const rescue = page.getByRole("button", { name: "산타 꺼내주기" });
  await expect(rescue).toHaveAttribute("aria-pressed", "false");
  await rescue.click();
  await expect(page.getByRole("button", { name: "다시 끼우기" })).toHaveAttribute("aria-pressed", "true");
  await expect(page.getByText("아무 일도 없었다.")).toBeVisible();
});

test("sample verdicts cycle without submitting an analysis", async ({ page }) => {
  await expect(page.getByRole("heading", { name: "산타 친자 의혹" })).toBeVisible();
  await page.getByRole("button", { name: "다음 헛소리 듣기" }).click();
  await expect(page.getByRole("heading", { name: "굴뚝 앞 배송 보류" })).toBeVisible();
  await expect(page.getByText("판정 예시 2 / 3")).toBeVisible();
});

test("start displays the preview notice and Escape closes it", async ({ page }) => {
  const start = page.getByRole("button", { name: "그래서 나 선물 받냐?" });
  await start.click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  await expect(dialog).toContainText("지금은 디자인 미리보기예요.");
  await page.keyboard.press("Escape");
  await expect(dialog).not.toBeVisible();
  await expect(start).toBeFocused();
});
