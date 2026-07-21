/// <reference types="vitest/config" />
import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    // Matches tsconfig.json's "@/*" -> "src/*" path mapping.
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // Backend runs on 8080 (see backend/src/main/resources/application.yml)
      "/api": "http://localhost:8080",
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.ts"],
    // e2e/ is Playwright-only (npm run test:e2e) — without this, Vitest's
    // default include glob also picks up e2e/*.spec.ts and fails trying to
    // execute Playwright's test() outside a Playwright runner.
    exclude: ["e2e/**", "node_modules/**"],
    coverage: {
      provider: "v8",
      reporter: ["text", "html"],
      // TST-05 — raised from the original scaffold-stage floor (70/70/60/70)
      // now that real feature coverage has grown well past it (measured
      // 90.68/82.14/89.9/90.68 lines/functions/branches/statements as of
      // this review). Set a few points below the measured figures, not
      // flush against them, so a normal PR has headroom rather than
      // immediately tripping the gate — see RULES.md S8 for the recurring
      // review checkpoint this threshold should keep pace with.
      thresholds: {
        lines: 85,
        functions: 78,
        branches: 85,
        statements: 85,
      },
    },
  },
});
