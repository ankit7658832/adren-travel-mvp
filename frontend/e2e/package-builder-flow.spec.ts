import { test, expect } from "@playwright/test";
import { loginAs } from "./support/devAuth";

// Traces to PRD Section 21.3 acceptance criteria and Section 17.2's UK
// ATOL auto-enforcement (BOK-11). Requires the real backend running on
// :8080, same as search-flow.spec.ts.
//
// TST-03 — the login gap is fixed (loginAs). "No Quotations list screen
// exists yet" (BOK-08's frontend half) is still real — see
// package-creation-flow.spec.ts's own comment for why this still navigates
// directly to a quotationId rather than chaining from a real UI flow.
test("Consultant is blocked from continuing past the Package Builder form until required fields are filled", async ({
  page,
  request,
}) => {
  await loginAs(page, request, "CONSULTANT");
  await page.goto("/packages/new?quotationId=00000000-0000-0000-0000-000000000000");

  await page.getByRole("button", { name: /continue/i }).click();

  await expect(page.getByRole("alert")).toContainText(/required fields must be filled in/i);
});

test("Package Builder shows the empty state when no quotation is selected", async ({ page, request }) => {
  await loginAs(page, request, "CONSULTANT");
  await page.goto("/packages/new");

  await expect(page.getByText(/no quotation selected/i)).toBeVisible();
});
