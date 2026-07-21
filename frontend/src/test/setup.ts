import "@testing-library/jest-dom/vitest";
import { afterAll, afterEach, beforeAll } from "vitest";
import { server } from "./mswServer";

// TST-04 — MSW intercepts apiClient's real HTTP calls for every component
// test (see mswServer.ts). onUnhandledRequest: "error" is deliberate: a
// test that forgets to register a handler for a call apiClient actually
// makes should fail loudly, not silently pass through to a real network
// call jsdom can't make anyway.
beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
