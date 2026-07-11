import { test, expect } from "@playwright/test";

// Traces to PRD Section 9.1 Flow A (Itinerary Creation) and 22.1
// acceptance criteria. Expand this suite as real backend endpoints replace
// the mocked search hook.
test("Consultant can search multiple locations and see a result per location", async ({ page }) => {
  await page.goto("/search");

  await page.getByLabel(/locations/i).fill("Goa, Udaipur, Jaipur");
  await page.getByRole("button", { name: /search/i }).click();

  await expect(page.getByLabel("search-results").locator("li")).toHaveCount(3);
});
