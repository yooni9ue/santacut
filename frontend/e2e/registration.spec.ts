import { expect, test, type Page } from "@playwright/test";

const participant = { id: "participant-test", nickname: "루돌프테스터", regionCode: "KR-11-680" };
const roots = [
  { code: "KR-11", name: "서울", parent_code: null, selectable: false },
  { code: "KR-26", name: "부산", parent_code: null, selectable: false },
  { code: "KR-50", name: "제주", parent_code: null, selectable: true },
];
async function mockApi(page: Page, options: { conflict?: boolean; lostSession?: boolean; meError?: boolean } = {}) {
  const writes: { body: Record<string, string>; csrf?: string }[] = [];
  await page.route("**/v1/**", async route => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname === "/v1/me") {
      if (options.meError) return route.fulfill({ status: 500, json: {} });
      // Inspect the real browser cookie jar; WebKit routing omits Cookie headers.
      const hasSession = (await page.context().cookies(request.url())).some(cookie => cookie.name === "sc_session" && cookie.value === "e2e-session") && !options.lostSession;
      return route.fulfill({ status: hasSession ? 200 : 401, json: hasSession ? { ...participant, ...writes.at(-1)?.body } : {} });
    }
    if (url.pathname === "/v1/regions") {
      const parent = url.searchParams.get("parent");
      return route.fulfill({ json: !parent ? roots : parent === "KR-11" ? [{ code: "KR-11-680", name: "강남구", selectable: true, parent_code: parent }] : [{ code: "KR-26-500", name: "수영구", selectable: true, parent_code: parent }] });
    }
    if (url.pathname === "/v1/nicknames/availability") return route.fulfill({ json: { available: url.searchParams.get("nickname") !== "중복닉네임" } });
    if (url.pathname === "/v1/csrf") return route.fulfill({ json: { headerName: "X-XSRF-TOKEN", token: "masked-token" }, headers: { "set-cookie": "XSRF-TOKEN=raw-cookie; Path=/; SameSite=Lax" } });
    if (url.pathname === "/v1/participants" && request.method() === "POST") {
      writes.push({ body: request.postDataJSON(), csrf: request.headers()["x-xsrf-token"] });
      if (options.conflict) return route.fulfill({ status: 409, json: { code: "NICKNAME_TAKEN" } });
      return route.fulfill({ status: 201, json: { ...participant, ...request.postDataJSON() }, headers: { "set-cookie": "sc_session=e2e-session; Path=/; HttpOnly; SameSite=Lax" } });
    }
    return route.fulfill({ status: 404, json: {} });
  });
  return writes;
}
async function fillForm(page: Page) {
  await page.goto("/join");
  await page.getByRole("textbox", { name: "닉네임" }).fill(participant.nickname);
  await page.getByRole("combobox", { name: "시·도", exact: true }).selectOption("KR-11");
  await page.getByRole("combobox", { name: "시·군·구", exact: true }).selectOption("KR-11-680");
  await expect(page.getByText(/지금 사용할 수 있어요/)).toBeVisible();
}

test("registers using JSON CSRF token and restores the session after reload", async ({ page }) => {
  const writes = await mockApi(page);
  await fillForm(page);
  await page.getByRole("button", { name: "참가 등록하고 다음" }).click();
  await expect(page).toHaveURL(/\/review$/);
  expect(writes).toEqual([{ body: { nickname: participant.nickname, regionCode: participant.regionCode }, csrf: "masked-token" }]);
  await expect(page.getByRole("main").getByText("서울 강남구", { exact: true })).toBeVisible();
  await page.goto("/join");
  await expect(page.getByText(/이미 참가 중이에요/)).toBeVisible();
  await page.reload();
  await expect(page.getByText(participant.nickname, { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "이 정보로 계속" }).click();
  await expect(page).toHaveURL(/\/review$/);
  await expect(page.getByRole("main").getByText("서울 강남구", { exact: true })).toBeVisible();
  expect(writes).toHaveLength(1);
});

test("changing provinces clears the district and supports selectable root regions", async ({ page }) => {
  const writes = await mockApi(page);
  await fillForm(page);
  await page.getByRole("combobox", { name: "시·도", exact: true }).selectOption("KR-26");
  await expect(page.getByRole("combobox", { name: "시·군·구", exact: true })).toHaveValue("");
  await page.getByRole("button", { name: "참가 등록하고 다음" }).click();
  await expect(page.getByRole("main").getByRole("alert")).toContainText("지역을 선택");
  expect(writes).toHaveLength(0);
  await page.getByRole("combobox", { name: "시·도", exact: true }).selectOption("KR-50");
  await expect(page.getByRole("combobox", { name: "시·군·구", exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "참가 등록하고 다음" }).click();
  await expect(page).toHaveURL(/\/review$/);
  expect(writes[0].body.regionCode).toBe("KR-50");
});

test("nickname conflict at registration preserves input and permits correction", async ({ page }) => {
  const writes = await mockApi(page, { conflict: true });
  await fillForm(page);
  await page.getByRole("button", { name: "참가 등록하고 다음" }).click();
  await expect(page.getByRole("main").getByRole("alert")).toContainText("다른 이름");
  await expect(page.getByRole("textbox", { name: "닉네임" })).toHaveValue(participant.nickname);
  await expect(page).toHaveURL(/\/join$/);
  expect(writes).toHaveLength(1);
  await page.getByRole("textbox", { name: "닉네임" }).fill("중복닉네임");
  await expect(page.getByText("이미 사용 중인 닉네임이에요.", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "참가 등록하고 다음" }).click();
  expect(writes).toHaveLength(1);
});

test("does not register twice if the browser fails to preserve the session", async ({ page }) => {
  const writes = await mockApi(page, { lostSession: true });
  await fillForm(page);
  await page.getByRole("button", { name: "참가 등록하고 다음" }).click();
  await expect(page.getByRole("main").getByRole("alert")).toContainText("등록은 완료됐지만 세션");
  await expect(page.getByRole("button", { name: "참가 등록하고 다음" })).toHaveCount(0);
  await page.getByRole("button", { name: "참가 정보 다시 확인" }).click();
  expect(writes).toHaveLength(1);
});

test("session lookup failure blocks registration until retry", async ({ page }) => {
  const writes = await mockApi(page, { meError: true });
  await page.goto("/join");
  await expect(page.getByRole("button", { name: "참가 정보 다시 확인" })).toBeVisible();
  await expect(page.getByRole("textbox", { name: "닉네임" })).toHaveCount(0);
  expect(writes).toHaveLength(0);
});

test("start screen offers real registration without selecting a file", async ({ page }) => {
  const writes = await mockApi(page);
  await page.goto("/start");
  await page.getByRole("link", { name: "참가 정보 먼저 등록·확인하기" }).click();
  await expect(page.getByRole("textbox", { name: "닉네임" })).toBeVisible();
  await expect(page.getByRole("button", { name: "참가 등록하고 다음" })).toBeVisible();
  expect(writes).toHaveLength(0);
});
