---
id: PERF-07
epic: Performance/Load Testing
phase: production
status: not-started
story_points: 5
dependencies: ["PERF-01", "PERF-03", "PERF-04"]
labels: ["performance", "phase2"]
prd_references: ["§2", "§9.6"]
modules_or_screens: ["Infra (performance test)"]
testing_tiers: ["e2e"]
---

# PERF-07: Validate the 10-minute itinerary target end-to-end under realistic network conditions

## Summary (business)
This story measures, under real-world internet speeds (not just fast office or lab connections), whether a customer can go from searching to having a completed, ready-to-send itinerary within the target of 10 minutes. It confirms the platform's core promise of fast trip planning actually holds up for real users on real networks.

## User Story
**As a** product owner, **I want** confirm the median search-to-complete-itinerary time stays within PRD §2's ≤10-minute target under realistic (not just local-dev) network latency, **so that** PRD §2's Goals & Success Metrics target and §9.6's NFR are validated holistically, not just per-component (search SLA, AI latency, etc. individually).

## Acceptance Criteria
- Given a realistic end-to-end itinerary-build session (search → default selection → AI completion → save as Quotation) is run under simulated realistic network latency, when timing is measured, then the median time stays at or below 10 minutes.

## Developer Notes
- **PRD reference(s):** §2 Goals & Success Metrics; §9.6 NFR (10-minute target)
- **Module(s)/Screen(s):** Infra (performance test)
- **Story points:** 5 — Composite end-to-end timing validation across PERF-01/03/04's individually-validated component SLAs.
- **Dependencies:** PERF-01, PERF-03, PERF-04
- **Testing tier(s):** e2e

## Sub-tasks
- [NEW] Backend/Frontend: end-to-end timed Playwright scenario simulating realistic network latency
- [NEW] Backend/Frontend: median-time assertion against the 10-minute target
