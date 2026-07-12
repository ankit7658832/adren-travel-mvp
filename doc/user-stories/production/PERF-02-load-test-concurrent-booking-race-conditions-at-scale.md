---
id: PERF-02
epic: Performance/Load Testing
phase: production
status: not-started
story_points: 5
dependencies: ["BOK-16"]
labels: ["backend", "performance", "phase2"]
prd_references: ["§23.1", "§25"]
modules_or_screens: ["Infra (performance test)"]
testing_tiers: ["integration (Testcontainers)"]
---

# PERF-02: Load-test concurrent booking race conditions at scale

## Summary (business)
This story checks what happens when many people try to book the very last available seat, room, or ticket for the same trip at the exact same moment. It makes sure only one booking actually goes through and everyone else is told clearly it's no longer available, so the business never accidentally sells the same inventory twice.

## User Story
**As a** platform reliability owner, **I want** validate BOK-16's optimistic-locking behavior under real concurrent load, not just a two-request unit test, **so that** PRD §23.1 Edge Case #1 holds at production-realistic concurrency levels.

## Acceptance Criteria
- Given N concurrent requests target the last available unit of the same inventory item, when the load test runs, then exactly one succeeds and N-1 fail gracefully with 'no longer available' — no duplicate booking occurs at any tested concurrency level.

## Developer Notes
- **PRD reference(s):** §23.1 Edge Case #1; §25 T21
- **Module(s)/Screen(s):** Infra (performance test)
- **Story points:** 5 — Load-test harness targeting BOK-16's already-built `@Version` mechanism at higher concurrency than its unit/integration test covers.
- **Dependencies:** BOK-16
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: concurrent-booking load-test scenario (N simultaneous requests on one contended unit)
- [NEW] Backend: assertion — exactly one success, N-1 graceful failures, zero duplicates
