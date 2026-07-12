---
id: DMC-11
epic: Local DMC + BYOS
phase: mock
status: not-started
story_points: 3
dependencies: ["DMC-10"]
labels: ["backend", "dmc", "supplier", "phase1"]
prd_references: ["§10.5"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# DMC-11: Alert on stale Local DMC inventory beyond a defined threshold

## Summary (business)
This automatically warns senior management when a local partner's manually-maintained product listings haven't been refreshed in a defined amount of time, since these partners don't have an automatic live data feed that could otherwise catch staleness. It prevents customers from being shown outdated prices or availability for these manually-managed suppliers.

## User Story
**As a** Super Admin, **I want** be alerted when a Local DMC's manually-entered inventory hasn't been updated beyond a defined staleness threshold, **so that** PRD §10.5's sync-failure alerting requirement extends to the manual/no-live-API Local DMC path, which has no automatic sync to fail.

## Acceptance Criteria
- Given a Local DMC's inventory calendar has not been updated within the defined staleness threshold, when the scheduled check runs, then Super Admin receives an alert flagging that DMC's inventory as stale.

## Developer Notes
- **PRD reference(s):** §10.5 Inventory Sync
- **Module(s)/Screen(s):** supplier
- **Story points:** 3 — Scheduled job + threshold check over DMC-10's inventory records.
- **Dependencies:** DMC-10
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: scheduled staleness-check job over Local DMC inventory `updated_at`
- [NEW] Backend: alert dispatch to Super Admin on breach
- [NEW] Backend: unit test — threshold boundary
