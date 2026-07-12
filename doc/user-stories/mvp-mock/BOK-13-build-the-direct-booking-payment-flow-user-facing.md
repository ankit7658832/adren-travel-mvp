---
id: BOK-13
epic: Booking Core
phase: mock
status: not-started
story_points: 8
dependencies: ["BOK-12", "FIN-06", "FIN-08", "BOK-14"]
labels: ["backend", "frontend", "booking", "phase1"]
prd_references: ["§9.1", "§21.4"]
modules_or_screens: ["booking", "payments", "Booking & Payment Flow (21.4) — NEW feature folder"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test", "e2e"]
---

# BOK-13: Build the Direct Booking & Payment flow (User-facing)

## Summary (business)
Customers can search available trip packages or build a custom itinerary, enter traveler information, see a clear price breakdown, and choose how to pay, completing the entire booking process themselves. This is the core self-service booking journey that lets end customers purchase trips directly rather than requiring a Consultant to do it for them.

## User Story
**As a** User, **I want** search available Packages or a custom itinerary, enter traveler details, review the price breakdown, and choose a payment method, **so that** a traveler-facing booking can be completed end-to-end per PRD §9.1 Flow C and §21.4.

## Acceptance Criteria
- Given a User selects traveler(s) and enters details, when they proceed, then the price breakdown (collapsible net/markup detail per Consultant visibility settings) is shown before payment method selection.
- Given a line item flags `passenger_documents_required` or the itinerary includes international travel, when the User reaches the traveler form, then document fields are required inline before proceeding.

## Developer Notes
- **PRD reference(s):** §9.1 Flow C; §21.4 Booking & Payment Flow
- **Module(s)/Screen(s):** booking, payments, Booking & Payment Flow (21.4) — NEW feature folder
- **Story points:** 8 — New end-to-end screen orchestrating booking, traveler profile, and payment method selection — the largest single frontend surface in Booking Core.
- **Dependencies:** BOK-12, FIN-06, FIN-08, BOK-14
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test, e2e

## Sub-tasks
- [EXTEND] Backend: `confirmBooking` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/bookings`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [NEW] Frontend: `useBookingPaymentFlow` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `BookingPaymentFlow.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Frontend: Playwright e2e spec (extends `search-flow.spec.ts` pattern, PRD §9.1 flow)
