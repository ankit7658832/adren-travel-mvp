---
id: ADS-12
epic: Ads/Campaign Management
phase: mock
status: not-started
story_points: 5
dependencies: ["ADS-07", "BOK-12"]
labels: ["backend", "ads", "booking", "phase1"]
prd_references: ["§23.5", "§25"]
modules_or_screens: ["ads", "booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# ADS-12: Auto-pause a campaign when its linked Package price changes

## Summary (business)
If the price of a travel package changes while its ad campaign is still running, the campaign automatically pauses itself until someone re-approves the updated pricing and creative. This prevents customers from seeing outdated prices in ads, which could otherwise create booking disputes or broken trust.

## User Story
**As a** Super Admin/Consultant, **I want** have a Live campaign automatically pause if its linked Package's price changes, **so that** PRD §23.5 Edge Case #11 and T16 are satisfied.

## Acceptance Criteria
- Given a campaign's linked Package is edited (price change) while the campaign is Live, when the change is saved, then the system detects the mismatch and pauses the campaign until creative/pricing is re-approved (T16).

## Developer Notes
- **PRD reference(s):** §23.5 Edge Case #11; §25 T16
- **Module(s)/Screen(s):** ads, booking
- **Story points:** 5 — Event listener on a Package-price-changed event (published by `booking`, consumed by `ads` per RULES.md §2.1's cross-module event pattern).
- **Dependencies:** ADS-07, BOK-12
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `PackagePriceChangedEvent` published by `booking`
- [NEW] Backend: `ads` `@ApplicationModuleListener` pausing linked Live campaigns (idempotent per RULES.md §2.2)
- [NEW] Backend: unit test
- [NEW] Backend: module test — event triggers pause
