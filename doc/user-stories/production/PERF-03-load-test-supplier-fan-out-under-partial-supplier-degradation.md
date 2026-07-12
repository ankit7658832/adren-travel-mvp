---
id: PERF-03
epic: Performance/Load Testing
phase: production
status: not-started
story_points: 5
dependencies: ["SUP-13"]
labels: ["backend", "performance", "phase2"]
prd_references: ["§24.2"]
modules_or_screens: ["Infra (performance test)"]
testing_tiers: ["integration (Testcontainers)"]
---

# PERF-03: Load-test supplier fan-out under partial supplier degradation

## Summary (business)
This story simulates one travel supplier's system going down or slowing to a crawl while lots of customers are searching, to confirm that problem doesn't drag down results from all the other, healthy suppliers. It protects the overall booking experience from being hurt by a single partner's outage.

## User Story
**As a** platform reliability owner, **I want** validate that one supplier's simulated downtime doesn't degrade search latency for the others under real concurrent load, **so that** PRD §24.2's circuit-breaker isolation NFR holds under load, not just SUP-13's tuned configuration in isolation.

## Acceptance Criteria
- Given one supplier is simulated as degraded/down under concurrent search load, when the load test runs, then the other suppliers' search latency remains within SLA, and the degraded supplier's circuit breaker trips per SUP-13's tuned thresholds without affecting the others.

## Developer Notes
- **PRD reference(s):** §24.2 NFR Supplier Integration
- **Module(s)/Screen(s):** Infra (performance test)
- **Story points:** 5 — Load-test scenario specifically validating SUP-13's per-supplier isolation under concurrency.
- **Dependencies:** SUP-13
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: supplier-degradation load-test scenario (one supplier simulated down under load)
- [NEW] Backend: assertion — other suppliers' latency unaffected, degraded supplier's breaker trips independently
