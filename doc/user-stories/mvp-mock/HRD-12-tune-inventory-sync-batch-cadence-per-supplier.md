---
id: HRD-12
epic: Hardening
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-11"]
labels: ["backend", "supplier", "phase1"]
prd_references: ["§10.5", "§10.2.1"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# HRD-12: Tune inventory sync batch cadence per supplier

## Summary (business)
Different travel suppliers update their content (like hotel photos and descriptions) on different schedules. This story makes sure each supplier's information is refreshed at the right frequency (for example nightly for one supplier, weekly for another) rather than using a single one-size-fits-all schedule, keeping content accurate without unnecessary system load.

## User Story
**As a** platform reliability owner, **I want** have each supplier's static-content sync run on its documented cadence (nightly for Hotelbeds, weekly for Widgety, etc.), **so that** PRD §10.5 and §10.2.x's per-supplier sync-frequency notes are implemented as scheduled jobs, not one generic cadence.

## Acceptance Criteria
- Given Hotelbeds' nightly Content API batch job runs, when it completes, then static content (images, descriptions, amenities) refreshes without affecting real-time search/pricing.
- Given Widgety's weekly ship-image/deck-plan sync job runs, when it completes, then it does not run more frequently than weekly, matching the lower change-frequency rationale in §10.2.6.

## Developer Notes
- **PRD reference(s):** §10.5 Inventory Sync; §10.2.1/10.2.6/10.2.7 (per-supplier sync frequency)
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Scheduled-job configuration per supplier, differentiated cadence — mechanically repetitive but must not be collapsed into one job.
- **Dependencies:** FND-11
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: per-supplier scheduled sync job (Hotelbeds nightly, Widgety/HBActivities per their documented cadence)
- [NEW] Backend: unit test — cadence configuration per supplier
- [NEW] Backend: integrationTest — job execution against LocalStack-backed storage
