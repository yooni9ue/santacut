import { AxiosError, AxiosHeaders, type AxiosAdapter } from "axios";
import { afterEach, expect, test, vi } from "vitest";
import { api } from "./api";
import { analysisError, getLatestAnalysis, isFinished, uploadAnalysis, watchAnalysis } from "./analyses";
const original = api.defaults.adapter;
afterEach(() => { api.defaults.adapter = original; vi.unstubAllGlobals(); });

test("multipart upload preserves intent key and uses the CSRF response token", async () => {
  const adapter = vi.fn<AxiosAdapter>(async config => ({ config, status: 200, statusText: "OK", headers: {}, data: config.url === "/csrf" ? { headerName: "X-XSRF-TOKEN", token: "masked" } : { id: "job", status: "VALIDATING" } }));
  api.defaults.adapter = adapter;
  const input = { file: new File(["[나] [오전 9:00] 안녕"], "대화.txt"), source: "KAKAO" as const, speaker: "나", key: "same-key-for-retry" };
  await uploadAnalysis(input);
  await uploadAnalysis(input);
  const submissions = adapter.mock.calls.map(([config]) => config).filter(config => config.url === "/analyses/files");
  expect(submissions).toHaveLength(2);
  for (const config of submissions) {
    expect(config.headers.get("Idempotency-Key")).toBe(input.key);
    expect(config.headers.get("X-XSRF-TOKEN")).toBe("masked");
    const body = config.data as FormData;
    expect(body.get("source")).toBe("KAKAO");
    expect(body.get("self_speaker")).toBe("나");
    expect((body.get("file") as File).name).toBe("대화.txt");
  }
});
test("latest analysis treats 404 as empty, but does not swallow missing sessions", async () => {
  const config = { headers: new AxiosHeaders() };
  const fail = (status: number) => new AxiosError("failed", undefined, config, undefined, { status, statusText: "", headers: {}, data: {}, config });
  api.defaults.adapter = vi.fn<AxiosAdapter>().mockRejectedValue(fail(404));
  expect(await getLatestAnalysis()).toBeNull();
  api.defaults.adapter = vi.fn<AxiosAdapter>().mockRejectedValue(fail(401));
  await expect(getLatestAnalysis()).rejects.toThrow();
});
class Stream extends EventTarget {
  static instance: Stream;
  close = vi.fn();
  constructor(public url: string, public options: EventSourceInit) { super(); Stream.instance = this; }
}
test("SSE sends credentials, accepts queue regressions, and closes on completion", () => {
  vi.stubGlobal("EventSource", Stream);
  const status = vi.fn(); const fallback = vi.fn();
  watchAnalysis("job", status, fallback);
  const stream = Stream.instance;
  expect(stream.options).toEqual({ withCredentials: true });
  for (const value of ["ANALYZING", "QUEUED", "SUCCEEDED", "FAILED"]) stream.dispatchEvent(new MessageEvent("status", { data: JSON.stringify({ id: "job", status: value }) }));
  expect(status.mock.calls.map(([event]) => event.status)).toEqual(["ANALYZING", "QUEUED", "SUCCEEDED"]);
  expect(stream.close).toHaveBeenCalledTimes(1);
  expect(fallback).not.toHaveBeenCalled();
  expect(isFinished("FAILED")).toBe(true);
});
test("broken SSE reports disconnection once and cleanup prevents later updates", () => {
  vi.stubGlobal("EventSource", Stream);
  const fallback = vi.fn(); const update = vi.fn();
  const stop = watchAnalysis("job", update, fallback);
  Stream.instance.dispatchEvent(new MessageEvent("status", { data: "bad JSON" }));
  Stream.instance.dispatchEvent(new Event("error"));
  expect(fallback).toHaveBeenCalledTimes(1);
  stop();
  Stream.instance.dispatchEvent(new MessageEvent("status", { data: JSON.stringify({ id: "job", status: "SUCCEEDED" }) }));
  expect(update).not.toHaveBeenCalled();
});
test("proxy 413 works even without an API error code", () => {
  const config = { headers: new AxiosHeaders() };
  expect(analysisError(new AxiosError("failed", undefined, config, undefined, { status: 413, statusText: "", headers: {}, data: {}, config }))).toContain("파일이 너무 커요");
});
