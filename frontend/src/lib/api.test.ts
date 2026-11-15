import { AxiosError, AxiosHeaders, type AxiosAdapter } from "axios";
import { afterEach, describe, expect, test, vi } from "vitest";
import { api, getApiErrorMessage } from "./api";

const originalAdapter = api.defaults.adapter;
afterEach(() => { api.defaults.adapter = originalAdapter; });

function mockAdapter() {
  const adapter = vi.fn<AxiosAdapter>(async (config) => ({
    config,
    data: config.url === "/csrf" ? { headerName: "X-CSRF-TEST", token: "masked-token" } : { ok: true },
    status: 200,
    statusText: "OK",
    headers: {},
  }));
  api.defaults.adapter = adapter;
  return adapter;
}

test("GET does not request CSRF and preserves session credentials", async () => {
  const adapter = mockAdapter();
  await api.get("/me");
  expect(adapter).toHaveBeenCalledTimes(1);
  expect(adapter.mock.calls[0][0]).toMatchObject({ url: "/me", baseURL: "/v1", withCredentials: true });
});

test.each(["post", "put", "patch", "delete"] as const)("%s fetches CSRF before sending the mutation", async (method) => {
  const adapter = mockAdapter();
  await api.request({ method, url: "/participants", data: { nickname: "루돌프" } });
  expect(adapter.mock.calls.map(([config]) => config.url)).toEqual(["/csrf", "/participants"]);
  expect(adapter.mock.calls[1][0].headers.get("X-CSRF-TEST")).toBe("masked-token");
  expect(adapter.mock.calls[1][0].withXSRFToken).toBe(false);
});

test("a CSRF fetch failure prevents the write request", async () => {
  const adapter = vi.fn<AxiosAdapter>().mockRejectedValue(new AxiosError("offline"));
  api.defaults.adapter = adapter;
  await expect(api.post("/participants", {})).rejects.toThrow("offline");
  expect(adapter).toHaveBeenCalledTimes(1);
  expect(adapter.mock.calls[0][0].url).toBe("/csrf");
});

describe("error messages", () => {
  test("distinguishes expired sessions from network errors", () => {
    const config = { headers: new AxiosHeaders() };
    const unauthorized = new AxiosError("unauthorized", undefined, config, undefined, {
      status: 401, statusText: "Unauthorized", data: {}, headers: {}, config,
    });
    expect(getApiErrorMessage(unauthorized)).toContain("세션이 만료");
    expect(getApiErrorMessage(new AxiosError("offline"))).toContain("네트워크");
  });
  test("does not expose server details", () => {
    expect(getApiErrorMessage(new Error("private server details"))).not.toContain("private server details");
  });
});
