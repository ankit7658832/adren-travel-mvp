---
id: AI-09
epic: AI Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["AI-02", "BOK-13"]
labels: ["backend", "ai", "phase1"]
prd_references: ["§11.3"]
modules_or_screens: ["ai", "supplier"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# AI-09: Re-validate AI-suggested pricing at booking time if it has gone stale

## Summary (business)
If prices from travel suppliers change between when the AI first suggested a trip and when the customer is ready to book, the system automatically double-checks the current price before finalizing the booking. This prevents customers from being charged incorrect prices and protects the business from honoring outdated, inaccurate quotes.

## User Story
**As a** Consultant/User, **I want** have any AI-approved itinerary re-validated against live supplier pricing before booking confirms, **so that** PRD §11.3's re-validation acceptance criterion is met — stale pricing discovered post-approval triggers re-validation at booking time.

## Acceptance Criteria
- Given stale pricing is discovered post-approval on an AI-generated line item, when booking is attempted, then the system re-validates against the live supplier before confirming, following the same 'price changed, please confirm' pattern as §10.2.4's Mystifly fare-expiry rule.

## Developer Notes
- **PRD reference(s):** §11.3 Acceptance Criteria (stale pricing re-validation)
- **Module(s)/Screen(s):** ai, supplier
- **Story points:** 5 — Reuses the same re-validation pattern already required for Mystifly (§10.2.4) — applied specifically to AI-sourced line items at booking time.
- **Dependencies:** AI-02, BOK-13
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `revalidateAiPricingAtBooking` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — invoked during confirmBooking for ai_generated itineraries)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
