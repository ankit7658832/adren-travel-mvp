---
id: SUP-05
epic: Supplier Live Integrations
phase: production
status: not-started
story_points: 8
dependencies: ["FND-11", "DMC-07"]
labels: ["backend", "supplier", "phase2"]
prd_references: ["§10.2.3"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "integration (Testcontainers, sandbox)"]
---

# SUP-05: Replace the TBO stub with a live sandbox integration

## Summary (business)
This connects our platform to TBO's real test environment for hotel bookings, correctly managing the temporary session reference TBO uses to track a booking in progress. If that session times out while a customer is building their trip, they're prompted to start a fresh search instead of the booking silently failing.

## User Story
**As a** backend engineer, **I want** have a `TboClient` call TBO's real API with correct TraceId session handling, **so that** PRD §10.2.3's TraceId-scoped-to-itinerary-draft requirement is implemented against a real sandbox.

## Acceptance Criteria
- Given a TraceId expires mid-itinerary-build, when booking is attempted, then the system detects the expiry and prompts a full re-search rather than a partial retry or silent failure (T19).

## Developer Notes
- **PRD reference(s):** §10.2.3 TBO
- **Module(s)/Screen(s):** supplier
- **Story points:** 8 — TraceId session-state persistence tied to the itinerary draft (not request-scoped) is the most architecturally distinct integration in the supplier set.
- **Dependencies:** FND-11, DMC-07
- **Testing tier(s):** unit, integration (Testcontainers, sandbox)

## Sub-tasks
- [NEW] Backend: `internal.tbo.TboClient` in its own sub-package (live, per backend-best-practices §6)
- [EXTEND] Backend: response mapping into normalized `SupplierSearchResult` shape (pattern from `HotelbedsClient`)
- [NEW] Backend: Tbo-specific exception types + user-facing message mapping (PRD §10.2 table)
- [EXTEND] Backend: rate limiter + circuit breaker wiring, independently configured per supplier
- [NEW] Backend: unit test (mapping + error handling)
- [NEW] Backend: module/integration test, wired into `SupplierAggregationService` parallel fan-out
