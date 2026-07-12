---
id: FND-14
epic: Foundation
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-13"]
labels: ["backend", "foundation", "booking", "phase1"]
prd_references: ["§9.2", "§22.2"]
modules_or_screens: ["booking", "supplier"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FND-14: Implement the Default Selection Algorithm for per-location product pre-selection

## Summary (business)
This story adds smart automation that pre-selects a sensible hotel, activity, or other product for each destination in a trip, based on availability, the consultant's preferences, profitability, and customer ratings. This saves consultants significant time by giving them a solid starting itinerary instead of forcing them to compare every option by hand.

## User Story
**As a** Consultant/User, **I want** have the system pre-select one default product per category per location using availability, my configured priority, margin, and rating in that order, **so that** I don't have to manually compare every option to get a reasonable starting itinerary, per PRD §9.2.

## Acceptance Criteria
- Given multiple hotel options are available for a location, when the system auto-selects a default, then the selected option is the highest-margin confirmable option (T2).
- Given the Consultant has configured a preferred supplier, when auto-selection runs, then the preferred supplier's option is selected if available, overriding pure margin ranking (T3).

## Developer Notes
- **PRD reference(s):** §9.2 Default Selection Algorithm; §22.2 Default Selection Algorithm
- **Module(s)/Screen(s):** booking, supplier
- **Story points:** 5 — Well-specified 4-step ranking algorithm; the complexity is in wiring it against live aggregated supplier results.
- **Dependencies:** FND-13
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `DefaultSelectionService` implementing the 4-step ranking
- [EXTEND] Backend: wired into `SupplierAggregationService` result post-processing
- [NEW] Backend: unit test — each of the 4 tie-break steps
- [NEW] Backend: module test — end-to-end against aggregated results
