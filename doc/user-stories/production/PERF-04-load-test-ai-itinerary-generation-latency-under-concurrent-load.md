---
id: PERF-04
epic: Performance/Load Testing
phase: production
status: not-started
story_points: 5
dependencies: ["AI-13", "LLM-05"]
labels: ["backend", "performance", "phase2"]
prd_references: ["§24.3"]
modules_or_screens: ["Infra (performance test)"]
testing_tiers: ["integration (Testcontainers)"]
---

# PERF-04: Load-test AI itinerary generation latency under concurrent load

## Summary (business)
This story tests the AI itinerary-planning feature under heavy simultaneous use to make sure it keeps responding within an acceptable time, or fails in a clear, controlled way rather than freezing indefinitely. It protects the promise of fast, AI-assisted trip planning even when many customers are using it at once.

## User Story
**As a** platform reliability owner, **I want** validate AI-13's bounded per-segment timeout holds under realistic concurrent AI request volume, **so that** PRD §24.3's NFR holds at production concurrency, informing LLM-05's production SLO alerting thresholds.

## Acceptance Criteria
- Given concurrent AI itinerary-completion requests are load-tested, when the test runs, then per-segment latency stays within AI-13's bounded timeout at the tested concurrency level, or the system degrades to AI-05's explicit failure state rather than an unbounded hang.

## Developer Notes
- **PRD reference(s):** §24.3 NFR AI Governance
- **Module(s)/Screen(s):** Infra (performance test)
- **Story points:** 5 — Load-test harness targeting AI-13/LLM-02's already-built timeout mechanism under concurrency.
- **Dependencies:** AI-13, LLM-05
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: concurrent AI-request load-test scenario
- [NEW] Backend: latency/timeout assertion under load
