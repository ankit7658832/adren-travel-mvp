---
id: HRD-13
epic: Hardening
phase: mock
status: not-started
story_points: 3
dependencies: ["HRD-12"]
labels: ["backend", "supplier", "phase1"]
prd_references: ["§10.5"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# HRD-13: Alert Super Admin on inventory sync staleness beyond threshold

## Summary (business)
If a supplier's content stops updating properly or goes too long without refreshing, administrators will now be automatically alerted and told which supplier is affected, so stale or outdated information (like old prices or descriptions) can be caught and fixed quickly rather than going unnoticed.

## User Story
**As a** Super Admin, **I want** be alerted if a supplier's synced content becomes stale beyond a defined threshold, **so that** PRD §10.5's sync-failure alerting requirement is implemented for the live-API suppliers (complementing DMC-11's manual-DMC equivalent).

## Acceptance Criteria
- Given a supplier's static content sync job fails or its last-successful-run exceeds the staleness threshold, when the check runs, then Super Admin receives an alert naming the affected supplier.

## Developer Notes
- **PRD reference(s):** §10.5 Inventory Sync
- **Module(s)/Screen(s):** supplier
- **Story points:** 3 — Threshold check + alert dispatch over HRD-12's scheduled jobs.
- **Dependencies:** HRD-12
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: staleness-threshold check per supplier sync job
- [NEW] Backend: alert dispatch to Super Admin on breach
- [NEW] Backend: unit test — threshold boundary
