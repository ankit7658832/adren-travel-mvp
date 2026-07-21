import { test, expect } from "@playwright/test";
import { loginAs } from "./support/devAuth";

// Traces to PRD Section 9.1 Flow A steps 5-6 and Section 21.2 acceptance
// criteria. Requires the real backend running on :8080, same as
// search-flow.spec.ts.
//
// TST-03 — fixed together with search-flow.spec.ts: both used to navigate
// straight past auth ("no login screen exists yet"), which is now closed
// via loginAs (the dev-only token-minting endpoint).
test("Consultant can search, build an itinerary, and swap a location's auto-selected default for an alternate", async ({
  page,
  request,
}) => {
  await loginAs(page, request, "CONSULTANT");
  await page.goto("/search");

  await page.getByLabel(/locations/i).fill("Goa");
  await page.getByRole("button", { name: /search/i }).click();
  await expect(page.getByLabel("search-results").locator("li")).toHaveCount(1);

  await page.getByRole("button", { name: /build itinerary/i }).click();
  await expect(page).toHaveURL(/\/itinerary\//);
  await expect(page.getByLabel("itinerary-line-items").locator("li")).toHaveCount(1);
  await expect(page.getByText("Auto-selected: Best available match")).toBeVisible();

  await page.getByRole("button", { name: /change/i }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await expect(page.getByLabel("alternate-options").locator("li").first()).toBeVisible();

  await page.getByLabel("alternate-options").locator("li").first().getByRole("button", { name: /select/i }).click();

  await expect(page.getByRole("dialog")).not.toBeVisible();
  await expect(page.getByText("Auto-selected: Best available match")).not.toBeVisible();
});

// AI-10, PRD S21.2's persistent AI-assist entry point / S11.2 principle 2.
// Runs against the real backend, same as the test above — including its
// real (dummy-keyed, per Stage 4's AI Layer epic) GROQ_API_KEY, so a
// generation request genuinely reaches the real Groq API and genuinely
// fails authentication. This proves the entry point and its explicit
// error state (never a silently fabricated suggestion) rather than a
// happy path this environment cannot actually produce.
test("Consultant can open the Complete with AI entry point and submit a request", async ({ page, request }) => {
  await loginAs(page, request, "CONSULTANT");
  await page.goto("/search");
  await page.getByLabel(/locations/i).fill("Goa");
  await page.getByRole("button", { name: /search/i }).click();
  await expect(page.getByLabel("search-results").locator("li")).toHaveCount(1);
  await page.getByRole("button", { name: /build itinerary/i }).click();
  await expect(page).toHaveURL(/\/itinerary\//);

  await page.getByRole("button", { name: /complete with ai/i }).click();
  // TST-03 — getByLabel(/location/i) is ambiguous here (the itinerary
  // MapPanel's own "Map showing N itinerary location(s)" aria-label also
  // matches); never caught before since this step was never reached
  // against a real, auth-passing backend until loginAs existed.
  await expect(page.getByRole("textbox", { name: /location/i })).toBeVisible();

  await page.getByRole("textbox", { name: /location/i }).fill("Goa");
  await page.getByLabel(/what is the traveler looking for/i).fill("A relaxing beach trip");
  await page.getByRole("button", { name: /generate suggestions/i }).click();

  // Real Groq call, real (dummy-keyed) 401 — an explicit error, never a
  // silently inserted/fabricated suggestion (AI-05/backend-best-practices).
  await expect(page.getByRole("alert")).toContainText(/could not generate ai suggestions/i);
  await expect(page.getByRole("button", { name: /retry/i })).toBeVisible();
  await expect(page.getByLabel("itinerary-line-items").locator("li")).toHaveCount(1);
});
