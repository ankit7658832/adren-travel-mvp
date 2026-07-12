---
id: PERF-01
epic: Performance/Load Testing
phase: production
status: not-started
story_points: 5
dependencies: ["FND-13", "SUP-02", "SUP-04", "SUP-06"]
labels: ["backend", "performance", "phase2"]
prd_references: ["§24.1"]
modules_or_screens: ["Infra (performance test)"]
testing_tiers: ["integration (Testcontainers)"]
---

# PERF-01: Build a load-test harness for the search aggregation SLA

## Summary (business)
This story tests how the search feature holds up when many customers search at the same time, not just when one person tries it. It ensures search results keep coming back quickly (within a few seconds) even during busy periods, so travellers don't get frustrated and abandon the site.

## User Story
**As a** platform reliability owner, **I want** load-test search aggregation against PRD §24.1's low-single-digit-second SLA for cached/normalized inventory, **so that** the search path holds its NFR under realistic concurrent load, not just functional-test conditions.

## Acceptance Criteria
- Given a k6/Gatling scenario simulates realistic concurrent search volume, when the test runs, then p95 latency for cached/normalized inventory categories stays within the low-single-digit-second SLA.

## Developer Notes
- **PRD reference(s):** §24.1 NFR Booking Engine
- **Module(s)/Screen(s):** Infra (performance test)
- **Story points:** 5 — New load-test harness (k6 or Gatling) targeting an already-built endpoint (FND-13's search).
- **Dependencies:** FND-13, SUP-02, SUP-04, SUP-06
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: k6/Gatling search-load scenario
- [NEW] Backend: p95/p99 latency assertion against the §24.1 SLA
- [NEW] Backend: CI-runnable load-test job (non-blocking, reported)
