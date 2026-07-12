---
id: DMC-05
epic: Local DMC + BYOS
phase: mock
status: not-started
story_points: 3
dependencies: ["DMC-04"]
labels: ["backend", "frontend", "dmc", "supplier", "phase1"]
prd_references: ["§22.5"]
modules_or_screens: ["supplier", "Super Admin Console (21.6)"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# DMC-05: Flag a Local DMC to both the onboarding Consultant and Super Admin on threshold breach

## Summary (business)
This automatically flags a local partner (DMC) on their record when their cancellations or complaints rise above an acceptable level, alerting both the consultant who onboarded them and senior management. It ensures poor-performing suppliers are caught and addressed quickly rather than discovered only after customers complain.

## User Story
**As a** Super Admin/Consultant, **I want** see a visible flag on a Local DMC's record once its cancellation rate or complaint count exceeds a defined threshold, **so that** PRD §22.5's second acceptance criterion is met.

## Acceptance Criteria
- Given a Local DMC's cancellation rate exceeds a defined threshold, when the quality signal updates, then both the onboarding Consultant and Super Admin see a flag on that DMC's record.

## Developer Notes
- **PRD reference(s):** §22.5 Local DMC Onboarding (threshold flag)
- **Module(s)/Screen(s):** supplier, Super Admin Console (21.6)
- **Story points:** 3 — Threshold check + visibility rule on top of DMC-04's rolling metric.
- **Dependencies:** DMC-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [EXTEND] Backend: threshold check on quality-signal recalculation, sets a `flagged` boolean
- [EXTEND] Frontend: flag badge on the Local DMC record in both Consultant and Super Admin views
- [NEW] Backend: unit test
- [NEW] Frontend: component test
