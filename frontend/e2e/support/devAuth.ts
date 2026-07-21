import type { APIRequestContext, Page } from "@playwright/test";

// Must match src/shared/auth/authTypes.ts's AUTH_TOKEN_STORAGE_KEY — not
// imported directly since e2e/ sits outside tsconfig.json's `include`
// (`["src"]`) and no other spec in this folder imports app internals;
// e2e tests treat the app as a black box, same as a real browser would.
const AUTH_TOKEN_STORAGE_KEY = "adren_auth_token";

const BACKEND_BASE_URL = "http://localhost:8080";

/**
 * TST-03 — logs a page in as a real, validly-signed dev identity via
 * `DevAuthController`'s `SPRING_PROFILES_ACTIVE=dev`-only endpoint
 * (`GET /dev-auth/token`). Every prior e2e spec (search-flow,
 * itinerary-builder-flow, package-builder-flow) documented "no login
 * screen exists yet" as a blocking gap and navigated straight past auth;
 * this closes that gap using the local-dev seed Consultant + token-minting
 * endpoint added since — the backend MUST be running with
 * SPRING_PROFILES_ACTIVE=dev for this to work (see backend/README.md).
 */
export async function loginAs(
  page: Page,
  request: APIRequestContext,
  role: "CONSULTANT" | "SUPER_ADMIN",
): Promise<void> {
  const response = await request.get(`${BACKEND_BASE_URL}/dev-auth/token?role=${role}`);
  const { token } = (await response.json()) as { token: string };

  await page.addInitScript(
    ({ key, value }) => {
      window.localStorage.setItem(key, value);
    },
    { key: AUTH_TOKEN_STORAGE_KEY, value: token },
  );
}
