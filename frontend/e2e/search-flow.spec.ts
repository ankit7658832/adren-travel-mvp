import { test, expect } from "@playwright/test";
import { loginAs } from "./support/devAuth";

// Traces to PRD Section 9.1 Flow A (Itinerary Creation) and 22.1
// acceptance criteria. Requires the real backend running on :8080
// (FND-13 replaced the mocked search hook with a real POST /api/v1/search
// call — see vite.config.ts's dev-server proxy).
//
// TST-03 — this and every other pre-existing e2e spec used to navigate
// straight past auth ("no login screen exists yet"), which meant it never
// actually passed against a real, security-enforcing backend (confirmed:
// the real /api/v1/search call 401s with no token attached — the /search
// route itself isn't access-gated, only the API call underneath it is).
// Fixed together with package-creation-flow.spec.ts's loginAs helper,
// which finally closes that gap via the dev-only token-minting endpoint.
test("Consultant can search multiple locations and see a result per location", async ({ page, request }) => {
  await loginAs(page, request, "CONSULTANT");
  await page.goto("/search");

  await page.getByLabel(/locations/i).fill("Goa, Udaipur, Jaipur");
  await page.getByRole("button", { name: /search/i }).click();

  await expect(page.getByLabel("search-results").locator("li")).toHaveCount(3);
});

test("every searched location gets a map pin, including one with no inventory (T1)", async ({ page, request }) => {
  await loginAs(page, request, "CONSULTANT");
  await page.goto("/search");

  await page.getByLabel(/locations/i).fill("Goa, Udaipur, Jaipur");
  await page.getByRole("button", { name: /search/i }).click();

  await expect(page.getByTestId("map-pin")).toHaveCount(3);
});
