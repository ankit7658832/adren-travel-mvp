import { test, expect } from "@playwright/test";

// Traces to PRD §9.5/§21.5 (HRD-09). Requires the real backend running on
// :8080, same as the other e2e specs — and inherits the identical known
// gap they already document: no login/token-issuance screen exists yet
// anywhere in the mvp-mock story catalogue, so this spec cannot drive a
// real authenticated session through the browser. Rather than fabricate a
// client-side JWT the real backend would reject (JwtTokenService verifies
// a real signature, unlike the unit-test-only makeFakeToken helper), this
// proves the one genuinely verifiable, auth-independent behavior: an
// unauthenticated visitor hitting the Consultant Dashboard is bounced by
// FES-07's route guard before the real dashboard (or any of its data)
// ever mounts — not silently shown a blank/broken screen.
test("An unauthenticated visitor hitting the Consultant Dashboard is redirected before it mounts", async ({ page }) => {
  await page.goto("/dashboard");

  await expect(page).toHaveURL("/");
  await expect(page.getByText(/21\.5 Consultant Dashboard/i)).not.toBeVisible();
});
