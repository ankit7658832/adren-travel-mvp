import { test, expect } from "@playwright/test";

// Traces to PRD Section 9.1 Flow A steps 5-6 and Section 21.2 acceptance
// criteria. Requires the real backend running on :8080, same as
// search-flow.spec.ts — and inherits the same known gap: neither spec logs
// in first, since no login/token-issuance screen exists yet anywhere in
// the mvp-mock story catalogue (see Stage 1 wrap-up report). Both specs
// will fail against a backend that enforces authentication until that
// story lands; kept in the same shape as search-flow.spec.ts rather than
// invented ad hoc so both get fixed together.
test("Consultant can search, build an itinerary, and swap a location's auto-selected default for an alternate", async ({
  page,
}) => {
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
