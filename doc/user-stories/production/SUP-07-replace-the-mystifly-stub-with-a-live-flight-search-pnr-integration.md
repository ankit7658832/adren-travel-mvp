---
id: SUP-07
epic: Supplier Live Integrations
phase: production
status: not-started
story_points: 8
dependencies: ["FND-11", "DMC-07"]
labels: ["backend", "supplier", "phase2"]
prd_references: ["§10.2.4"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "integration (Testcontainers, sandbox)"]
---

# SUP-07: Replace the Mystifly stub with a live flight-search/PNR integration

## Summary (business)
This connects our platform to Mystifly's real flight search and ticketing system, with a fast double-check of the fare immediately before payment is charged. Since flight prices can change within minutes, this protects customers from being unexpectedly charged a different amount than what they saw when they searched.

## User Story
**As a** backend engineer, **I want** have a `MystiflyClient` issue real flight searches and PNR issuance with fast fare-expiry re-validation, **so that** PRD §10.2.4's fare-expiry-sensitive booking flow is implemented against a real sandbox.

## Acceptance Criteria
- Given a Mystifly fare expires between search and payment capture, when payment is attempted, then the price is re-validated immediately pre-payment and a 'price changed, please confirm' prompt is shown rather than charging a stale amount (T20).

## Developer Notes
- **PRD reference(s):** §10.2.4 Mystifly
- **Module(s)/Screen(s):** supplier
- **Story points:** 8 — Flight fares expire in minutes, not hours — the re-validation-immediately-pre-payment requirement is the highest-stakes correctness rule in the supplier set.
- **Dependencies:** FND-11, DMC-07
- **Testing tier(s):** unit, integration (Testcontainers, sandbox)

## Sub-tasks
- [NEW] Backend: `internal.mystifly.MystiflyClient` in its own sub-package (live, per backend-best-practices §6)
- [EXTEND] Backend: response mapping into normalized `SupplierSearchResult` shape (pattern from `HotelbedsClient`)
- [NEW] Backend: Mystifly-specific exception types + user-facing message mapping (PRD §10.2 table)
- [EXTEND] Backend: rate limiter + circuit breaker wiring, independently configured per supplier
- [NEW] Backend: unit test (mapping + error handling)
- [NEW] Backend: module/integration test, wired into `SupplierAggregationService` parallel fan-out
