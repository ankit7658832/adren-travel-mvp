---
id: DMC-04
epic: Local DMC + BYOS
phase: mock
status: not-started
story_points: 5
dependencies: ["DMC-02", "BOK-16"]
labels: ["backend", "dmc", "supplier", "phase1"]
prd_references: ["§10.3", "§20.14"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# DMC-04: Track Local DMC quality signal — cancellation rate and complaint count

## Summary (business)
This tracks how often bookings with a given local partner (DMC) get cancelled and how many customer complaints they receive, giving the business an ongoing, up-to-date quality score for each supplier. It's the early-warning data that feeds into automatically flagging underperforming partners before they damage customer trust.

## User Story
**As a** Super Admin/Consultant, **I want** see a Local DMC's rolling cancellation rate and complaint count, **so that** PRD §10.3 step 5's ongoing quality signal is visible, feeding the flagging rule in DMC-05.

## Acceptance Criteria
- Given a booking against a Local DMC product is cancelled, when the quality signal updates, then `cancellation_rate` recalculates as a rolling figure on the DMC record.

## Developer Notes
- **PRD reference(s):** §10.3 Local DMC Onboarding step 5; §20.14 Local DMC Record
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Rolling-metric calculation triggered by booking/cancellation events from `booking`.
- **Dependencies:** DMC-02, BOK-16
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `recalculateQualitySignal` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — event-driven on cancellation)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
