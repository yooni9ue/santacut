import { expect, test } from "@playwright/test";

test("sample Kakao flow keeps input through review, result and ranking", async ({ page }) => {
  let registrationRequests = 0;
  page.on("request", request => { if (request.method() === "POST" && new URL(request.url()).pathname === "/v1/participants") registrationRequests++; });
  await page.goto("/start");
  await page.getByRole("button", { name: /카카오톡 대화/ }).click();
  await page.getByRole("link", { name: "파일 준비됐어요" }).click();
  await page.getByRole("button", { name: "파일 없이 예시로 둘러보기" }).click();
  await page.getByRole("link", { name: "이 파일로 계속" }).click();
  await page.getByRole("textbox", { name: "닉네임" }).fill("루돌프테스터");
  await page.getByRole("combobox", { name: "시·도", exact: true }).selectOption("부산");
  await page.getByRole("combobox", { name: "시·군·구", exact: true }).selectOption("수영구");
  await page.getByRole("button", { name: "다음", exact: true }).click();
  await expect(page.getByText("부산 수영구", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "심사 화면 보기" })).toBeDisabled();
  await page.getByRole("checkbox").check();
  await page.getByRole("link", { name: "심사 화면 보기" }).click();
  await page.getByRole("link", { name: "예시 결과 보기" }).click();
  await expect(page.getByText("루돌프테스터 님, 마음의 준비 됐죠?")).toBeVisible();
  await page.getByRole("button", { name: "공유 카드 보기" }).click();
  await expect(page.getByRole("dialog")).toContainText("루돌프테스터의 산타컷");
  await page.keyboard.press("Escape");
  await expect(page.getByRole("dialog")).not.toBeVisible();
  await page.getByRole("link", { name: "전체 랭킹 둘러보기" }).click();
  await expect(page.getByRole("region", { name: "나의 순위" })).toContainText("루돌프테스터");
  await page.getByRole("button", { name: "예시 랭킹 더 보기" }).click();
  await expect(page.getByText("귤두개드림", { exact: true })).toBeVisible();
  expect(registrationRequests).toBe(0);
});

test("invalid files and nicknames show actionable errors", async ({ page }) => {
  await page.goto("/upload");
  await page.getByLabel("분석 파일 선택").setInputFiles({ name: "photo.png", mimeType: "image/png", buffer: Buffer.from("example") });
  await expect(page.getByRole("main").getByRole("alert")).toContainText("TXT");
  await expect(page.getByRole("button", { name: "파일을 먼저 선택해 주세요" })).toBeDisabled();
  await page.getByRole("button", { name: "파일 없이 예시로 둘러보기" }).click();
  await page.getByRole("link", { name: "이 파일로 계속" }).click();
  await page.getByRole("textbox", { name: "닉네임" }).fill("!");
  await page.getByRole("button", { name: "다음", exact: true }).click();
  await expect(page.getByRole("main").getByRole("alert")).toContainText("2~16자");
  await expect(page).toHaveURL(/\/join$/);
});

test("analysis error states preserve a clear retry path", async ({ page }) => {
  await page.goto("/upload");
  await page.getByRole("button", { name: "파일 없이 예시로 둘러보기" }).click();
  await page.getByRole("link", { name: "이 파일로 계속" }).click();
  await page.getByRole("textbox", { name: "닉네임" }).fill("루돌프테스터");
  await page.getByRole("button", { name: "다음", exact: true }).click();
  await page.getByRole("checkbox").check();
  await page.getByRole("link", { name: "심사 화면 보기" }).click();
  await page.getByRole("button", { name: "오류", exact: true }).click();
  await expect(page.getByRole("heading", { name: "산타가 잠깐 멈췄어요." })).toBeVisible();
  await page.getByRole("button", { name: "다시 시도하는 화면 보기" }).click();
  await expect(page.getByRole("heading", { name: "산타가 기록을 읽는 중." })).toBeVisible();
  await page.getByRole("button", { name: "근거 부족", exact: true }).click();
  await expect(page.getByRole("heading", { name: "기록이 조금 더 필요해요." })).toBeVisible();
});

test("all mobile preview screens fit narrow viewports", async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 740 });
  for (const route of ["start", "prepare", "upload", "join", "review", "analysis", "result", "ranking"]) {
    await page.goto(`/${route}`);
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), route).toBe(true);
  }
});
