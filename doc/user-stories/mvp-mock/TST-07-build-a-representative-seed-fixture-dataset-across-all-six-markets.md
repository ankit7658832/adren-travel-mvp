---
id: TST-07
epic: Test Infrastructure
phase: mock
status: not-started
story_points: 5
dependencies: ["TST-01", "FND-04"]
labels: ["backend", "testing", "foundation", "phase1"]
prd_references: ["§13.1", "§17"]
modules_or_screens: ["Infra (test)"]
testing_tiers: ["integration (Testcontainers)"]
---

# TST-07: Build a representative seed/fixture dataset across all six markets

## Summary (business)
This creates a ready-made, realistic set of sample customer, itinerary, and booking data covering all six markets we operate in (India, Australia, UK, USA, Dubai/UAE, and Denmark). Having this shared, realistic data on hand makes it much easier to verify that country-specific rules (like identity checks, taxes, travel protection requirements, and currency handling) work correctly, reducing the chance of costly compliance mistakes.

## User Story
**As a** QA/backend engineer, **I want** have a representative set of Consultants, Itineraries, and Bookings across all six markets (India, Australia, UK, USA, Dubai/UAE, Denmark) available for module/integration tests, **so that** market-dependent logic (KYC, GST/TCS, ATOL, currency) can be tested against realistic data shapes instead of every test author inventing their own fixtures.

## Acceptance Criteria
- Given a module/integrationTest needs a UK Consultant with a dynamic flight+hotel package, when it requests the seed fixture, then one is available pre-built, matching the ATOL-relevant shape BOK-11 tests need.

## Developer Notes
- **PRD reference(s):** §13.1 Consultant Onboarding (per-market); §17 Regional Compliance
- **Module(s)/Screen(s):** Infra (test)
- **Story points:** 5 — Fixture-data authoring across 6 markets × multiple entity types — breadth work, not complexity.
- **Dependencies:** TST-01, FND-04
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: seed-data builder per market (Consultant + representative Itinerary/Booking)
- [NEW] Backend: fixture usable from both `test` and `integrationTest` source sets
