---
id: BOK-26
epic: Booking Core
phase: mock
status: not-started
story_points: 5
dependencies: ["BOK-21", "BOK-22", "BOK-23", "BOK-24", "BOK-25"]
labels: ["backend", "booking", "supplier", "resilience", "phase1"]
prd_references: ["§24.2"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# BOK-26: Isolate each supplier integration behind a circuit breaker

## Summary (business)
Today, if one hotel or activity supplier is slow or down, that slowness can drag down search for every other supplier too, because the aggregation logic just tries each supplier in turn with a bare try/catch. This story ensures a single struggling supplier is automatically and temporarily set aside so the rest of search stays fast, then automatically brought back once it recovers — without an engineer having to intervene.

## User Story
**As a** backend engineer, **I want** each supplier client call wrapped in its own circuit breaker (Resilience4j), **so that** one supplier's downtime or latency spike doesn't degrade search latency for the others, per PRD §24.2 and the gap already named in `doc/architecture.md` ("No circuit breakers exist yet") and `RULES.md` §9 item 9.

## Acceptance Criteria
- Given a supplier client (`HotelbedsClient`, `StubaClient`, `TboClient`, `TransferzClient`, `WidgetyClient`, `HbActivitiesClient`) is called from `SupplierAggregationService`, when the call is made, then it goes through a per-supplier-named Resilience4j circuit breaker instance, not a shared global one.
- Given a supplier's failure rate crosses its configured threshold within the rolling window, when subsequent calls to that supplier are made, then the breaker opens and short-circuits immediately (no network call attempted) until the configured wait duration elapses.
- Given a breaker is open for one supplier, when a search fans out to all suppliers concurrently, then the other suppliers' calls complete unaffected — the open breaker for the failing supplier does not block or slow the others.
- Given a breaker transitions to half-open after its wait duration, when a subsequent call to that supplier succeeds, then the breaker closes again and normal calls resume.
- Given a circuit breaker is added for a new supplier client that ships later (e.g., Mystifly in BOK-04), when that client is wired into `SupplierAggregationService`, then it reuses the same generic wrapping mechanism this story builds rather than requiring a new dedicated story.

## Developer Notes
- **PRD reference(s):** §24.2 Supplier Integration NFR (per-supplier circuit-breaker isolation); referenced by `doc/architecture.md` (line noting `SupplierAggregationService` "currently relies on bare try/catch") and `RULES.md` §9 item 9.
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Adds `resilience4j-spring-boot3` as a build dependency, wraps six existing client calls generically (one config-driven wrapper, not six bespoke ones), plus a fallback path per supplier (return empty results, log, and alert per FND-10/11's existing "disable this supplier's results until resolved" error-handling pattern from §10.2.1).
- **Dependencies:** BOK-21, BOK-22, BOK-23, BOK-24, BOK-25 (wraps the new clients; `HotelbedsClient` already exists from the skeleton and is wrapped as part of this story too)
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: add `resilience4j-spring-boot3` dependency; per-supplier named `CircuitBreaker` configuration (failure-rate threshold, wait duration, sliding-window size) in `application.yml`
- [EXTEND] Backend: `SupplierAggregationService` wraps each supplier client call through its named circuit breaker instead of bare try/catch
- [NEW] Backend: fallback behavior on open breaker — exclude that supplier from the result set, log at WARN, matching §10.2.1's "showing results from other suppliers" pattern
- [NEW] Backend: unit test — simulated failing supplier opens its breaker without affecting a simulated healthy supplier's calls
- [NEW] Backend: `@ApplicationModuleTest` — full aggregation fan-out with one supplier forced to fail, verifying overall search latency/result set is unaffected by the failing supplier
