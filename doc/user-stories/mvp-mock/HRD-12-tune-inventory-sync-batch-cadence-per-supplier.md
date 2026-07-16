---
id: HRD-12
epic: Hardening
phase: mock
status: not-started
story_points: 5
dependencies: ["BOK-27"]
labels: ["backend", "supplier", "phase1"]
prd_references: ["§10.5", "§10.2.1"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# HRD-12: Tune inventory sync batch cadence per supplier

## Summary (business)
Different travel suppliers update their content (like hotel photos and descriptions) on different schedules. BOK-27 already runs each supplier's sync job on the cadence the PRD documents (nightly for Hotelbeds, weekly for Widgety, etc.), hard-coded per supplier. This story makes those cadences operator-tunable — via configuration, not a code change — so Super Admin can adjust a supplier's sync frequency (e.g., a supplier changes their content-update pattern, or load needs rebalancing) without redeploying.

## User Story
**As a** platform reliability owner, **I want** each supplier's sync cadence exposed as environment/config-driven values rather than hard-coded intervals, **so that** cadence can be retuned per PRD §10.5's per-supplier guidance without a code change, once BOK-27's baseline scheduled sync exists.

## Acceptance Criteria
- Given BOK-27's per-supplier scheduled sync jobs exist with hard-coded cadences, when this story's config layer is added, then each supplier's interval is overridable via `application.yml`/environment config, defaulting to the PRD-documented cadence (nightly for Hotelbeds/STUBA/TBO/HBActivities, weekly for Widgety) if unset.
- Given a supplier's configured cadence is changed, when the application restarts, then the new interval takes effect without code changes to `SupplierContentSyncService`.

## Developer Notes
- **PRD reference(s):** §10.5 Inventory Sync; §10.2.1/10.2.6/10.2.7 (per-supplier sync frequency)
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Config-driven cadence override on top of BOK-27's existing scheduled jobs, plus validation that overrides don't exceed sane bounds.
- **Dependencies:** BOK-27
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: `SupplierContentSyncService`'s per-supplier `@Scheduled` cadence reads from config (`application.yml`) instead of a hard-coded constant, defaulting to the PRD-documented value
- [NEW] Backend: unit test — config override changes effective cadence; unset config falls back to PRD default
- [NEW] Backend: integrationTest — job execution honors configured cadence
