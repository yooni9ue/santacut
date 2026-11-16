import { AxiosError, AxiosHeaders, type AxiosAdapter } from "axios";
import { afterEach, expect, test, vi } from "vitest";
import { api } from "./api";
import { checkNickname, getParticipant, getRegions, isValidNickname, normalizeNickname, participantError } from "./participants";
const originalAdapter = api.defaults.adapter;
afterEach(() => { api.defaults.adapter = originalAdapter; });

function fail(status: number, code?: string) {
  const config = { headers: new AxiosHeaders() };
  return new AxiosError("failure", undefined, config, undefined, {
    status, statusText: "error", data: { code }, headers: {}, config,
  });
}
test.each([401, 404])("%s session is treated as unregistered", async (status) => {
  api.defaults.adapter = vi.fn<AxiosAdapter>().mockRejectedValue(fail(status));
  expect(await getParticipant()).toBeNull();
});
test("server errors do not silently become an anonymous session", async () => {
  api.defaults.adapter = vi.fn<AxiosAdapter>().mockRejectedValue(fail(500));
  await expect(getParticipant()).rejects.toThrow();
});
test("normalizes nicknames before validation and availability checks", async () => {
  const adapter = vi.fn<AxiosAdapter>(async config => ({ config, data: { available: true }, status: 200, statusText: "OK", headers: {} }));
  api.defaults.adapter = adapter;
  expect(normalizeNickname(" Ｓanta ")).toBe("Santa");
  expect(isValidNickname(" Ｓanta ")).toBe(true);
  expect(isValidNickname("산타! ")).toBe(false);
  await checkNickname(" Ｓanta ");
  expect(adapter.mock.calls[0][0].params.nickname).toBe("Santa");
  const signal = new AbortController().signal;
  await getRegions("KR-11", signal);
  expect(adapter.mock.calls[1][0]).toMatchObject({ params: { parent: "KR-11" }, signal });
});
test("registration conflicts explain recovery without server details", () => {
  expect(participantError(fail(409, "NICKNAME_TAKEN"))).toContain("다른 이름");
  expect(participantError(fail(400, "UNSUPPORTED_REGION"))).toContain("지역을 다시");
});
