import { afterEach, expect, test, vi } from "vitest";
import nextConfig from "../../next.config";

afterEach(() => vi.unstubAllEnvs());

function configure(vercel: boolean, origin?: string) {
  vi.stubEnv("VERCEL", vercel ? "1" : undefined);
  vi.stubEnv("BACKEND_ORIGIN", origin);
}

test("local preview proxies to the local backend by default", async () => {
  configure(false);
  expect(await nextConfig.rewrites!()).toContainEqual({
    source: "/v1/:path*", destination: "http://127.0.0.1:8080/v1/:path*",
  });
});

test("Vercel proxies API and shares to the configured AWS HTTPS origin", async () => {
  configure(true, "https://api.example.com/");
  expect(await nextConfig.rewrites!()).toEqual(expect.arrayContaining([
    { source: "/v1/:path*", destination: "https://api.example.com/v1/:path*" },
    { source: "/s/:path*", destination: "https://api.example.com/s/:path*" },
  ]));
});

test("Vercel cannot silently fall back to localhost", async () => {
  configure(true);
  await expect(nextConfig.rewrites!()).rejects.toThrow("BACKEND_ORIGIN");
});

test.each([
  "http://api.example.com",
  "https://api.example.com/v1",
  "https://user:password@api.example.com",
  "https://api.example.com?token=example",
])("Vercel rejects an invalid backend origin: %s", async (origin) => {
  configure(true, origin);
  await expect(nextConfig.rewrites!()).rejects.toThrow();
});
