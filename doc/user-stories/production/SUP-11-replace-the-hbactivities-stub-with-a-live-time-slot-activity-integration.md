---
id: SUP-11
epic: Supplier Live Integrations
phase: production
status: not-started
story_points: 5
dependencies: ["FND-11", "DMC-07"]
labels: ["backend", "supplier", "phase2"]
prd_references: ["§10.2.7"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "integration (Testcontainers, sandbox)"]
---

# SUP-11: Replace the HBActivities stub with a live time-slot activity integration

## Summary (business)
This connects our platform to HBActivities' real system for booking tours and experiences that run at specific times with limited spots. It ensures that when one time slot is fully booked, customers are told to try a different time rather than being shown a generic "nothing available" message, reducing confusion and lost bookings.

## User Story
**As a** backend engineer, **I want** have an `HbActivitiesClient` handle time-slot-specific sellouts and fixed-headcount booking constraints, **so that** PRD §10.2.7's slot-specific error messaging is implemented against a real sandbox.

## Acceptance Criteria
- Given a specific tour departure time is full while others on the same day are open, when a search is run, then the empty state communicates 'this time slot is full, try another time,' not a blanket unavailability message (T23).

## Developer Notes
- **PRD reference(s):** §10.2.7 HBActivities
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Standard REST integration with one specific slot-vs-day distinction.
- **Dependencies:** FND-11, DMC-07
- **Testing tier(s):** unit, integration (Testcontainers, sandbox)

## Sub-tasks
- [NEW] Backend: `internal.hbactivities.HbActivitiesClient` in its own sub-package (live, per backend-best-practices §6)
- [EXTEND] Backend: response mapping into normalized `SupplierSearchResult` shape (pattern from `HotelbedsClient`)
- [NEW] Backend: HbActivities-specific exception types + user-facing message mapping (PRD §10.2 table)
- [EXTEND] Backend: rate limiter + circuit breaker wiring, independently configured per supplier
- [NEW] Backend: unit test (mapping + error handling)
- [NEW] Backend: module/integration test, wired into `SupplierAggregationService` parallel fan-out
