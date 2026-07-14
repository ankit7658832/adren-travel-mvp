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
      // Keep an explicit floor so coverage can't silently regress —
      // raise these as real feature coverage grows past the scaffold stage.
      thresholds: {
        lines: 70,
        functions: 70,
        branches: 60,
        statements: 70,
      },
    },
  },
});
