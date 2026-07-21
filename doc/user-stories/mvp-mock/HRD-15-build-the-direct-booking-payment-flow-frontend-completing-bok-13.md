---
id: HRD-15
epic: Hardening
phase: mock
status: not-started
story_points: 5
dependencies: ["BOK-13", "BOK-14", "HRD-14"]
labels: ["frontend", "booking", "phase1"]
prd_references: ["§9.1", "§21.4"]
modules_or_screens: ["booking", "payments", "Booking & Payment Flow (21.4)"]
testing_tiers: ["component test", "e2e"]
---

# HRD-15: Build the Direct Booking & Payment flow frontend (completing BOK-13)

## Summary (business)
A User can actually walk through entering traveler details, reviewing the price breakdown, and confirming a booking on-screen, instead of landing on a placeholder screen. BOK-13 built and verified every backend piece of this flow (traveler profile capture, price breakdown, `confirmBooking`/`confirmBookingOnAccount`) but explicitly deferred the frontend; this was found and flagged (not silently left) during the mock-complete Definition of Done walkthrough (`doc/phases.md` §5).

## User Story
**As a** User, **I want** to enter traveler details, review the price breakdown, and confirm my booking on one screen, **so that** PRD §9.1 Flow C's self-service booking journey is actually usable, not just backed by working APIs nobody can reach.

## Acceptance Criteria
- Given a User arrives at `/booking/:packageId` (or an itinerary-derived equivalent) with a real, authenticated session, when the screen loads, then it shows the traveler-details form first (backed by `TravelerController`/`CreateTravelerProfileCommand`, BOK-14).
- Given traveler details are submitted, when the User proceeds, then the price breakdown (collapsible net/markup detail per Consultant visibility settings) is shown before a payment/confirm action, matching BOK-13's own acceptance criteria.
- Given the User confirms, when the booking is placed, then the screen calls the real `BookingApi.confirmBooking`/`confirmBookingOnAccount` endpoint (no direct Stripe path required — the on-account/wallet path is real and simpler than the stubbed `StripeClient`) and shows a success confirmation with the new booking id.
- Given the confirm call fails (e.g. credit-limit breach, ATOL disclosure not yet completed), when the error is returned, then a clear inline error is shown, not a silent failure or a generic crash.

## Developer Notes
- **PRD reference(s):** §9.1 Flow C; §21.4 Booking & Payment Flow
- **Module(s)/Screen(s):** booking, payments, Booking & Payment Flow (21.4) — replaces the `BookingPaymentFlow.tsx` placeholder from BOK-13
- **Story points:** 5 — Backend is entirely already real and tested (BOK-13/BOK-14); this story is frontend-only orchestration across traveler details, price breakdown, and confirmation.
- **Dependencies:** BOK-13 (backend `confirmBooking` already built), BOK-14 (traveler profile capture), HRD-14 (a real session to reach this screen at all — the placeholder was reachable in practice only because no login screen existed either)
- **Testing tier(s):** component test (Testing Library, co-located), e2e (extends `search-flow.spec.ts`'s `loginAs` pattern)

## Sub-tasks
- [NEW] Frontend: `useBookingPaymentFlow` hook (React Query — traveler profile submission, price breakdown fetch, `confirmBooking` mutation)
- [REPLACE] Frontend: `BookingPaymentFlow.tsx` — traveler details → price breakdown → confirm, all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, asserts on role/label per RULES.md §7.3)
- [NEW] Frontend: Playwright e2e spec (logs in via HRD-14's real login, not a dev-only shortcut)
