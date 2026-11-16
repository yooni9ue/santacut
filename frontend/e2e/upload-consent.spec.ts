import { expect, test } from "@playwright/test";

test("파일 동의 전에는 업로드하지 않고, 동의 후 한 번만 제출한다", async ({ page }) => {
  let uploads = 0;
  await page.route("**/v1/me", route => route.fulfill({ json: { id: "p1", nickname: "테스터", regionCode: "KR-11-680" } }));
  await page.route("**/v1/regions**", route => route.fulfill({ json: [] }));
  await page.route("**/v1/analyses/files", async route => { uploads += 1; await route.fulfill({ status: 202, json: { id: "analysis-1", status: "VALIDATING" } }); });
  await page.route("**/v1/csrf", route => route.fulfill({ json: { headerName: "X-XSRF-TOKEN", token: "test-token" } }));
  await page.goto("/upload");
  await page.getByLabel("분석 파일 선택").setInputFiles({ name: "chat.txt", mimeType: "text/plain", buffer: Buffer.from("[나] [오전 10:00] 안녕\n[친구] [오전 10:01] 반가워") });
  const speaker = page.getByRole("combobox", { name: "이 대화에서 나는?" });
  await expect(speaker).toBeEnabled();
  await speaker.selectOption({ label: "나" });
  await page.getByRole("link", { name: "이 파일로 계속" }).click();
  await expect(page).toHaveURL(/\/join$/);
  await page.getByRole("button", { name: "이 정보로 계속" }).click();
  await expect(page).toHaveURL(/\/review$/);
  expect(uploads).toBe(0);
  await expect(page.getByRole("button", { name: "파일 전송하고 심사 시작" })).toBeDisabled();
  await page.getByRole("checkbox").nth(0).check();
  await page.getByRole("checkbox").nth(1).check();
  await page.getByRole("button", { name: "파일 전송하고 심사 시작" }).click();
  await expect(page).toHaveURL(/\/analysis$/);
  expect(uploads).toBe(1);
});
