---
id: PERF-08
epic: Performance/Load Testing
phase: production
status: not-started
story_points: 5
dependencies: ["PINF-05", "PINF-07"]
labels: ["backend", "performance", "phase2"]
prd_references: ["§2"]
modules_or_screens: ["Infra (performance test)"]
testing_tiers: ["integration (Testcontainers)"]
---

# PERF-08: Validate capacity planning and autoscaling policy against projected GMV growth

## Summary (business)
This story tests whether the platform's infrastructure can automatically add capacity fast enough to keep up as booking volumes grow toward future business targets, not just handle today's traffic. It gives confidence that the platform will stay fast and reliable (with strong uptime) as the business scales, rather than breaking down under future growth.

## User Story
**As a** platform reliability owner, **I want** confirm PINF-05's autoscaling policy actually scales ahead of projected GMV/booking-volume growth, not just current load, **so that** the platform's production topology is validated against a growth projection, not just today's load.

## Acceptance Criteria
- Given load is ramped to match a projected future GMV/booking-volume milestone, when autoscaling is observed, then PINF-05's topology scales out ahead of saturation, and the 99.5%+ uptime target holds throughout the ramp.

## Developer Notes
- **PRD reference(s):** §2 Goals & Success Metrics (Revenue/GMV)
- **Module(s)/Screen(s):** Infra (performance test)
- **Story points:** 5 — Capacity-planning validation exercise against PINF-05's already-defined topology and PINF-07's deployment pipeline.
- **Dependencies:** PINF-05, PINF-07
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: capacity-ramp load-test scenario against a projected GMV milestone
- [NEW] Backend: autoscaling-behavior and uptime assertion during the ramp
