import { defineConfig, devices } from "@playwright/test";

/**
 * End-to-end tier: real browser, real (or LocalStack-backed) backend.
 * Reserve these for the highest-value user journeys (PRD Flow A/B/C in
 * Section 9.1) rather than duplicating every Vitest component test here —
 * see the `testing-strategy` skill for the test-pyramid guidance.
 */
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  retries: process.env.CI ? 2 : 0,
  reporter: "html",
  use: {
    baseURL: "http://localhost:5173",
    trace: "on-first-retry",
  },
  webServer: {
    command: "npm run dev",
    url: "http://localhost:5173",
    reuseExistingServer: !process.env.CI,
  },
  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
  ],
});
