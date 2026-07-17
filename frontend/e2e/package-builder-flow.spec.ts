import { test, expect } from "@playwright/test";

// Traces to PRD Section 21.3 acceptance criteria and Section 17.2's UK
// ATOL auto-enforcement (BOK-11). Requires the real backend running on
// :8080, same as search-flow.spec.ts — and inherits the same known gaps:
// no login/token-issuance screen exists yet (see itinerary-builder-flow.spec.ts's
// comment), AND no "Quotations list" screen exists yet either (BOK-08's
// frontend half isn't built), so this spec can't chain from a real UI
// flow all the way from search through "save as quotation" the way
// itinerary-builder-flow.spec.ts chains from search. Navigates directly
// to a quotationId instead — kept in the same shape as the other specs'
// documented gaps rather than inventing missing upstream UI ad hoc; all
// three specs need fixing together once auth and the Quotation screen land.
test("Consultant is blocked from continuing past the Package Builder form until required fields are filled", async ({
  page,
}) => {
  await page.goto("/packages/new?quotationId=00000000-0000-0000-0000-000000000000");

  await page.getByRole("button", { name: /continue/i }).click();

  await expect(page.getByRole("alert")).toContainText(/required fields must be filled in/i);
});

test("Package Builder shows the empty state when no quotation is selected", async ({ page }) => {
  await page.goto("/packages/new");

  await expect(page.getByText(/no quotation selected/i)).toBeVisible();
});
