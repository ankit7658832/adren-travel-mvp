---
id: SUP-09
epic: Supplier Live Integrations
phase: production
status: not-started
story_points: 5
dependencies: ["FND-11", "DMC-07"]
labels: ["backend", "supplier", "phase2"]
prd_references: ["§10.2.5"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "integration (Testcontainers, sandbox)"]
---

# SUP-09: Replace the Transferz stub with a live transfer integration

## Summary (business)
This connects our platform to Transferz's real airport/ground transfer booking system and ensures customers get a clear, accurate message when the issue is that Transferz simply doesn't operate a route, versus when the route is covered but temporarily has no available transfers. This avoids confusing or misleading error messages for customers booking transportation.

## User Story
**As a** backend engineer, **I want** have a `TransferzClient` distinguish no-coverage-at-location from no-availability, **so that** PRD §10.2.5's two distinct failure messages are implemented against a real sandbox.

## Acceptance Criteria
- Given Transferz does not service a given pickup/dropoff pair at all, when a search is run, then the user sees 'Transfers not available for this route,' distinct from 'No transfer options available right now'.

## Developer Notes
- **PRD reference(s):** §10.2.5 Transferz
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Standard REST integration with one specific two-message distinction — lower complexity than the session/TraceId-based suppliers.
- **Dependencies:** FND-11, DMC-07
- **Testing tier(s):** unit, integration (Testcontainers, sandbox)

## Sub-tasks
- [NEW] Backend: `internal.transferz.TransferzClient` in its own sub-package (live, per backend-best-practices §6)
- [EXTEND] Backend: response mapping into normalized `SupplierSearchResult` shape (pattern from `HotelbedsClient`)
- [NEW] Backend: Transferz-specific exception types + user-facing message mapping (PRD §10.2 table)
- [EXTEND] Backend: rate limiter + circuit breaker wiring, independently configured per supplier
- [NEW] Backend: unit test (mapping + error handling)
- [NEW] Backend: module/integration test, wired into `SupplierAggregationService` parallel fan-out
