import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: process.env.CI ? [["github"], ["html", { open: "never" }]] : "list",
  use: { baseURL: "http://127.0.0.1:3100", trace: "retain-on-failure" },
  projects: [
    { name: "mobile-chromium", use: { ...devices["Pixel 7"] } },
    { name: "mobile-webkit", use: { ...devices["iPhone 13"] } },
  ],
  webServer: {
    command: process.env.CI
      ? "pnpm start --hostname 127.0.0.1 --port 3100"
      : "pnpm dev --hostname 127.0.0.1 --port 3100",
    env: { HOSTNAME: "127.0.0.1", PORT: "3100" },
    url: "http://127.0.0.1:3100",
    reuseExistingServer: !process.env.CI,
  },
});
