---
id: TST-03
epic: Test Infrastructure
phase: mock
status: not-started
story_points: 5
dependencies: ["BOK-11", "BOK-13"]
labels: ["frontend", "testing", "foundation", "phase1"]
prd_references: ["§9.1"]
modules_or_screens: ["Infra (test)"]
testing_tiers: ["e2e"]
---

# TST-03: Scaffold Playwright e2e specs for Flow B and Flow C journeys

## Summary (business)
This adds automated end-to-end checks (tests that simulate a real customer or agent clicking through the app start to finish) for two more major customer journeys: building a travel package and booking directly. Today only the search journey is checked this way, so this closes a gap and reduces the risk of broken experiences reaching customers in these two important flows.

## User Story
**As a** QA/frontend engineer, **I want** have e2e coverage for Package creation (Flow B) and Direct Booking (Flow C), not just the existing Flow A search spec, **so that** PRD §9.1's three flows are all covered per testing-strategy's 'reserve e2e for journeys' guidance — currently only `search-flow.spec.ts` exists.

## Acceptance Criteria
- Given `npm run test:e2e` is run, when the suite executes, then it includes a Flow B spec (itinerary→quotation→package→publish) and a Flow C spec (search/select package→traveler details→payment→confirmation) alongside the existing Flow A spec.

## Developer Notes
- **PRD reference(s):** §9.1 Flow B; §9.1 Flow C; testing-strategy skill (Frontend e2e tier)
- **Module(s)/Screen(s):** Infra (test)
- **Story points:** 5 — Two new Playwright specs following `search-flow.spec.ts`'s established pattern.
- **Dependencies:** BOK-11, BOK-13
- **Testing tier(s):** e2e

## Sub-tasks
- [NEW] Test infra: `e2e/package-creation-flow.spec.ts` and `e2e/direct-booking-flow.spec.ts`
- [NEW] Test infra: sample test exercising the new harness
- [NEW] Test infra: CI wiring / gradle-or-npm script update
