---
id: SUP-04
epic: Supplier Live Integrations
phase: production
status: not-started
story_points: 5
dependencies: ["SUP-03"]
labels: ["backend", "supplier", "phase2"]
prd_references: ["§10.2.2"]
modules_or_screens: ["supplier"]
testing_tiers: ["integration (Testcontainers)"]
---

# SUP-04: Cut STUBA over to production with independent throttling and confirmed content-sync cadence

## Summary (business)
This moves our STUBA hotel connection to production with its own separate capacity limit, so heavy usage from one hotel supplier can never slow down or block bookings from another. It protects the reliability of hotel search and booking as real customer traffic scales up.

## User Story
**As a** Super Admin, **I want** have STUBA run against production with its own throttle bucket, independent of Hotelbeds, **so that** PRD §10.2.2's explicit 'lower default concurrency, throttle independently' requirement is met in production.

## Acceptance Criteria
- Given STUBA and Hotelbeds both receive concurrent production traffic, when rate limiting is evaluated, then STUBA's throttle bucket is entirely independent of Hotelbeds' — one supplier's headroom never borrows from the other's budget.

## Developer Notes
- **PRD reference(s):** §10.2.2 STUBA (Rate limits, Sync frequency)
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Narrower cutover than SUP-02 since STUBA's static-content sync cadence was flagged as an open item pending confirmation during technical due diligence.
- **Dependencies:** SUP-03
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: independent production throttle bucket for STUBA
- [NEW] Backend: content-sync cadence confirmed with STUBA and configured
- [NEW] Backend: production-fixture-shaped integrationTest
