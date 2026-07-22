import { test, expect } from "@playwright/test";
import { loginAs } from "./support/devAuth";

// Traces to PRD Section 9.1 Flow C (search/select package -> traveler
// details -> payment -> confirmation) and Section 21.4's acceptance
// criteria (BOK-13, completed by HRD-15). Requires the real backend
// running on :8080 with SPRING_PROFILES_ACTIVE=dev.
//
// HRD-15 built the real BookingPaymentFlow.tsx (traveler details -> price
// breakdown -> payment method -> confirm), replacing the ScreenPlaceholder
// this file used to assert against. A genuine end-to-end Flow C run (real
// published Package all the way through) is still blocked by the same gap
// package-creation-flow.spec.ts documents: no REST endpoint anywhere
// creates a new Itinerary row, so no test — this one included — can create
// a fresh real Package to book without seeding one out-of-band. What IS
// real and verifiable end-to-end here: HRD-14's actual login screen (not
// the dev-only token shortcut) working all the way through, and the real
// screen's error state (not a placeholder) rendering for an unknown
// packageId.
test("Consultant logs in via the real login screen and reaches the real Booking & Payment Flow screen (not a placeholder)", async ({
  page,
}) => {
  await page.goto("/login");
  await page.getByLabel(/email/i).fill("dev-consultant@adren.travel");
  await page.getByLabel(/password/i).fill("DevPassword1!");
  await page.getByRole("button", { name: /sign in/i }).click();

  await expect(page).toHaveURL(/\/dashboard/);

  await page.goto("/booking/00000000-0000-0000-0000-000000000000");

  await expect(page.getByRole("alert")).toContainText(/could not load this package/i);
  await expect(page.getByText("21.4 Booking & Payment Flow")).not.toBeVisible();
});

test("an incorrect password on the real login screen shows a clear error, not a silent failure", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel(/email/i).fill("dev-consultant@adren.travel");
  await page.getByLabel(/password/i).fill("wrong-password");
  await page.getByRole("button", { name: /sign in/i }).click();

  await expect(page.getByRole("alert")).toContainText(/invalid email or password/i);
  await expect(page).toHaveURL(/\/login/);
});

test("logging in as the seeded dev Super Admin also works end-to-end via the real login screen", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel(/email/i).fill("dev-super-admin@adren.travel");
  await page.getByLabel(/password/i).fill("SuperAdminPassword1!");
  await page.getByRole("button", { name: /sign in/i }).click();

  await expect(page).toHaveURL(/\/admin/);
});

// Kept alongside the real-login coverage above: proves the dev-only
// token shortcut (used by every other e2e spec's loginAs helper) still
// reaches the exact same real screen, not a placeholder.
test("the Booking & Payment Flow route renders the real screen for a logged-in session via the dev-auth shortcut", async ({
  page,
  request,
}) => {
  await loginAs(page, request, "CONSULTANT");

  await page.goto("/booking/00000000-0000-0000-0000-000000000000");

  await expect(page.getByRole("alert")).toContainText(/could not load this package/i);
});
