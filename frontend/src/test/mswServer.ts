import { setupServer } from "msw/node";

/**
 * TST-04 — the shared MSW server every component test's HTTP-dependent
 * assertions should use instead of `vi.mock("@/shared/api/apiClient")`.
 * Intercepting at the network layer (not mocking the axios client module)
 * means a test exercises the real request/response shape `apiClient`
 * actually produces — a typo'd URL, a wrong HTTP method, or a payload
 * shape mismatch fails the test instead of silently working because the
 * mocked function was never asked to validate anything.
 * <p>
 * No default handlers registered here — each test's own `server.use(...)`
 * call (reset after every test in `src/test/setup.ts`) defines exactly the
 * response it needs, same as the per-test `mockResolvedValue`/
 * `mockRejectedValue` calls this replaces.
 */
export const server = setupServer();
