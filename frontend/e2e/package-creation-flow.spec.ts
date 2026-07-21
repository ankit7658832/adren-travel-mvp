import { test, expect } from "@playwright/test";
import { loginAs } from "./support/devAuth";

// Traces to PRD Section 9.1 Flow B (itinerary -> quotation -> package ->
// publish) and Section 21.3's acceptance criteria (BOK-11). Requires the
// real backend running on :8080 with SPRING_PROFILES_ACTIVE=dev.
//
// Genuinely blocked from chaining end-to-end, discovered while building
// this spec (TST-03) — flagged here, not fixed here, since it's a booking-
// flow gap, not a test-infra one: no REST endpoint anywhere creates a new
// Itinerary row. Every booking endpoint (line-items, save-as-quotation,
// convert-to-package) requires one to already exist
// (`requireOwnedDraftItinerary` in BookingServiceImpl); `new Itinerary(...)`
// is only ever constructed in unit tests, never in production code. The
// frontend's "build itinerary" step only ever writes to a client-side
// Zustand draft (itineraryDraftStore.ts) with a locally-generated UUID —
// it never persists anything server-side. Calling
// `POST /api/v1/itineraries/{id}/quotation` against that client-only ID
// genuinely 400s with "No itinerary: <id>" (confirmed directly against
// the real running backend, not assumed). Until an itinerary-creation
// endpoint exists, Flow B's two halves can only be tested separately:
// this spec covers the real, working "build itinerary" UI leg (and proves
// loginAs actually works, unlike the unmodified itinerary-builder-flow.spec.ts,
// which still fails at search with 0 results since it was never updated to
// log in); package-builder-flow.spec.ts already covers the Package Builder
// screen's own real behavior (form validation, empty state) against a
// directly-navigated quotationId.
test("Consultant can search and build an itinerary with an auto-selected line item (Flow B's first leg)", async ({
  page,
  request,
}) => {
  await loginAs(page, request, "CONSULTANT");

  await page.goto("/search");
  await page.getByLabel(/locations/i).fill("Goa");
  await page.getByRole("button", { name: /search/i }).click();
  await expect(page.getByLabel("search-results").locator("li")).toHaveCount(1);

  await page.getByRole("button", { name: /build itinerary/i }).click();
  await expect(page).toHaveURL(/\/itinerary\/[0-9a-f-]+/);
  await expect(page.getByLabel("itinerary-line-items").locator("li")).toHaveCount(1);
  await expect(page.getByText("Auto-selected: Best available match")).toBeVisible();
});

test("attempting to save the client-only itinerary draft as a Quotation fails, confirming the no-itinerary-creation-endpoint gap this file documents", async ({
  page,
  request,
}) => {
  await loginAs(page, request, "CONSULTANT");

  await page.goto("/search");
  await page.getByLabel(/locations/i).fill("Goa");
  await page.getByRole("button", { name: /search/i }).click();
  await expect(page.getByLabel("search-results").locator("li")).toHaveCount(1);
  await page.getByRole("button", { name: /build itinerary/i }).click();
  await expect(page).toHaveURL(/\/itinerary\/[0-9a-f-]+/);
  await expect(page.getByLabel("itinerary-line-items").locator("li")).toHaveCount(1);

  const itineraryId = new URL(page.url()).pathname.split("/").pop();
  const token = await page.evaluate(() => window.localStorage.getItem("adren_auth_token"));
  const quotationResponse = await request.post(
    `http://localhost:8080/api/v1/itineraries/${itineraryId}/quotation`,
    { headers: { Authorization: `Bearer ${token}` } },
  );

  expect(quotationResponse.status()).toBe(400);
  expect(await quotationResponse.text()).toContain("No itinerary");
});
