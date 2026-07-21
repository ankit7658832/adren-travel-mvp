import { test, expect } from "@playwright/test";
import { loginAs } from "./support/devAuth";

// Traces to PRD Section 9.1 Flow C (search/select package -> traveler
// details -> payment -> confirmation) and Section 21.4's acceptance
// criteria (BOK-13). Requires the real backend running on :8080.
//
// BOK-13's frontend half was deliberately deferred (backend/README.md's
// module table, PROGRESS.md: "backend confirmBooking scaffold only —
// frontend screen deferred") and never followed up on —
// BookingPaymentFlow.tsx is still a bare ScreenPlaceholder, not the real
// traveler-details/payment/confirmation screen PRD S21.4 describes. There
// is no real Flow C UI to write a meaningful e2e spec against yet. Per
// this codebase's established pattern (search-flow/itinerary-builder-flow/
// package-builder-flow.spec.ts all document real gaps rather than faking
// coverage over them), this spec asserts what's actually there today —
// replace it with the real traveler-details -> payment -> confirmation
// journey once BOK-13's frontend is actually built.
test("the Booking & Payment Flow route renders its known placeholder, not yet the real Flow C journey", async ({
  page,
  request,
}) => {
  await loginAs(page, request, "CONSULTANT");

  await page.goto("/booking/00000000-0000-0000-0000-000000000000");

  await expect(page.getByText("21.4 Booking & Payment Flow")).toBeVisible();
  await expect(page.getByText(/BOK-13/i)).toBeVisible();
});
