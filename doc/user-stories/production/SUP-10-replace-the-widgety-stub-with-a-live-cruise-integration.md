---
id: SUP-10
epic: Supplier Live Integrations
phase: production
status: not-started
story_points: 8
dependencies: ["FND-11", "DMC-07", "BOK-14"]
labels: ["backend", "supplier", "phase2"]
prd_references: ["§10.2.6"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "integration (Testcontainers, sandbox)"]
---

# SUP-10: Replace the Widgety stub with a live cruise integration

## Summary (business)
This connects our platform to Widgety's real cruise booking system and converts complex multi-stop cruise itineraries into a single, easy-to-understand line item for customers, while still capturing all required port details. It also ensures that when a cabin type sells out, customers see a clear message about that specific cabin being unavailable, and that required travel documents like passport details are collected upfront rather than left until the last minute.

## User Story
**As a** backend engineer, **I want** have a `WidgetyClient` flatten multi-port cruise itineraries into Adren's single-line-item model with port metadata, **so that** PRD §10.2.6's port-flattening and passenger-documentation-capture requirements are implemented against a real sandbox.

## Acceptance Criteria
- Given a sailing's cabin category sells out, when a booking is attempted, then a distinct, clearly-labeled 'cabin category sold out' failure state is shown, not a generic rate-expired message.
- Given a cruise booking requires passport details, when booking proceeds, then they are captured in the Traveler Profile before confirmation, not deferred to check-in.

## Developer Notes
- **PRD reference(s):** §10.2.6 Widgety
- **Module(s)/Screen(s):** supplier
- **Story points:** 8 — Multi-port metadata flattening plus a partner-tier access model make this the most structurally distinct remaining supplier integration.
- **Dependencies:** FND-11, DMC-07, BOK-14
- **Testing tier(s):** unit, integration (Testcontainers, sandbox)

## Sub-tasks
- [NEW] Backend: `internal.widgety.WidgetyClient` in its own sub-package (live, per backend-best-practices §6)
- [EXTEND] Backend: response mapping into normalized `SupplierSearchResult` shape (pattern from `HotelbedsClient`)
- [NEW] Backend: Widgety-specific exception types + user-facing message mapping (PRD §10.2 table)
- [EXTEND] Backend: rate limiter + circuit breaker wiring, independently configured per supplier
- [NEW] Backend: unit test (mapping + error handling)
- [NEW] Backend: module/integration test, wired into `SupplierAggregationService` parallel fan-out
