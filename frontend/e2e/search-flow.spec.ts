import { test, expect } from "@playwright/test";

// Traces to PRD Section 9.1 Flow A (Itinerary Creation) and 22.1
// acceptance criteria. Requires the real backend running on :8080
// (FND-13 replaced the mocked search hook with a real POST /api/v1/search
// call — see vite.config.ts's dev-server proxy).
test("Consultant can search multiple locations and see a result per location", async ({ page }) => {
  await page.goto("/search");

  await page.getByLabel(/locations/i).fill("Goa, Udaipur, Jaipur");
  await page.getByRole("button", { name: /search/i }).click();

  await expect(page.getByLabel("search-results").locator("li")).toHaveCount(3);
});

test("every searched location gets a map pin, including one with no inventory (T1)", async ({ page }) => {
  await page.goto("/search");

  await page.getByLabel(/locations/i).fill("Goa, Udaipur, Jaipur");
  await page.getByRole("button", { name: /search/i }).click();

  await expect(page.getByTestId("map-pin")).toHaveCount(3);
});
