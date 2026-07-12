---
id: SUP-03
epic: Supplier Live Integrations
phase: production
status: not-started
story_points: 8
dependencies: ["FND-11", "DMC-07"]
labels: ["backend", "supplier", "phase2"]
prd_references: ["§10.2.2"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "integration (Testcontainers, sandbox)"]
---

# SUP-03: Replace the STUBA stub with a live sandbox integration

## Summary (business)
This connects our platform to STUBA's real test environment for hotel inventory, including the behind-the-scenes math needed to work out our true cost when STUBA's contract only gives us the customer-facing price. It also makes the system automatically retry once if a session times out mid-search, so customers see a smooth experience instead of a random error.

## User Story
**As a** backend engineer, **I want** have a `StubaClient` call STUBA's real XML session-token API, including reverse-markup net-rate derivation where needed, **so that** PRD §10.2.2's authentication and mapping spec is implemented against a real sandbox.

## Acceptance Criteria
- Given a STUBA session token expires mid-search, when the error is handled, then automatic re-authentication is attempted with a single retry before surfacing 'temporarily unavailable'.
- Given STUBA's contract returns sell price directly instead of net, when the mapping runs, then a reverse-markup calculation derives the true net rate per the confirmed contract terms.

## Developer Notes
- **PRD reference(s):** §10.2.2 STUBA
- **Module(s)/Screen(s):** supplier
- **Story points:** 8 — XML/session-token auth model is materially different from Hotelbeds' signed-hash model — genuinely new integration shape, not a copy-paste.
- **Dependencies:** FND-11, DMC-07
- **Testing tier(s):** unit, integration (Testcontainers, sandbox)

## Sub-tasks
- [NEW] Backend: `internal.stuba.StubaClient` in its own sub-package (live, per backend-best-practices §6)
- [EXTEND] Backend: response mapping into normalized `SupplierSearchResult` shape (pattern from `HotelbedsClient`)
- [NEW] Backend: Stuba-specific exception types + user-facing message mapping (PRD §10.2 table)
- [EXTEND] Backend: rate limiter + circuit breaker wiring, independently configured per supplier
- [NEW] Backend: unit test (mapping + error handling)
- [NEW] Backend: module/integration test, wired into `SupplierAggregationService` parallel fan-out
