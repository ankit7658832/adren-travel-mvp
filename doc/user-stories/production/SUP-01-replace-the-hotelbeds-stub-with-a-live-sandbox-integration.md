---
id: SUP-01
epic: Supplier Live Integrations
phase: production
status: not-started
story_points: 8
dependencies: ["FND-11", "DMC-07"]
labels: ["backend", "supplier", "phase2"]
prd_references: ["§10.2.1"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "integration (Testcontainers, sandbox)"]
---

# SUP-01: Replace the Hotelbeds stub with a live sandbox integration

## Summary (business)
This connects our platform to Hotelbeds' real test environment (a safe practice system that mirrors production) instead of using fake sample data, so hotel search and booking can be verified against real supplier behavior before going live. It also ensures that if a hotel rate expires before a customer books it, they're told to search again rather than being silently charged a different price.

## User Story
**As a** backend engineer, **I want** have `HotelbedsClient` call Hotelbeds' real sandbox API using SHA-256-signed requests, **so that** PRD §10.2.1's full authentication, mapping, and error-handling spec is implemented against a real (sandbox) endpoint instead of the MVP mock.

## Acceptance Criteria
- Given a search is issued against Hotelbeds sandbox, when the request is signed, then `X-Signature` is computed as SHA-256(apiKey+secret+UTC timestamp) and IP whitelisting is satisfied per the sandbox account setup.
- Given Hotelbeds returns `RATE_STALE` at booking, when the response is handled, then the user sees 'This rate has expired — please re-search,' forcing a new search rather than a silent re-price.

## Developer Notes
- **PRD reference(s):** §10.2.1 Hotelbeds
- **Module(s)/Screen(s):** supplier
- **Story points:** 8 — Real external API replaces a stub — first live-credential integration, highest first-integration risk in this epic.
- **Dependencies:** FND-11, DMC-07
- **Testing tier(s):** unit, integration (Testcontainers, sandbox)

## Sub-tasks
- [NEW] Backend: `internal.hotelbeds.HotelbedsClient` in its own sub-package (live, per backend-best-practices §6)
- [EXTEND] Backend: response mapping into normalized `SupplierSearchResult` shape (pattern from `HotelbedsClient`)
- [NEW] Backend: Hotelbeds-specific exception types + user-facing message mapping (PRD §10.2 table)
- [EXTEND] Backend: rate limiter + circuit breaker wiring, independently configured per supplier
- [NEW] Backend: unit test (mapping + error handling)
- [NEW] Backend: module/integration test, wired into `SupplierAggregationService` parallel fan-out
