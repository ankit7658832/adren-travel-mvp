---
id: DMC-02
epic: Local DMC + BYOS
phase: mock
status: not-started
story_points: 5
dependencies: ["DMC-01", "FND-02"]
labels: ["backend", "dmc", "supplier", "phase1"]
prd_references: ["§10.3", "§22.5"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# DMC-02: Run the Local DMC Pending → Active vetting workflow

## Summary (business)
This creates a clear, required approval process for turning a newly submitted local partner (DMC) from "Pending" into "Active" so it can start selling. A reviewer must complete at least one verification check before a partner can go live, and the system blocks any attempt to skip that check, ensuring only vetted suppliers reach customers.

## User Story
**As a** Super Admin (or delegated Consultant-level reviewer), **I want** review a submitted Local DMC for basic legitimacy and mark it Active only after at least one verification step, **so that** PRD §10.3 steps 2–3 are implemented as an explicit workflow, not an implicit status flip.

## Acceptance Criteria
- Given a Local DMC is Pending, when a reviewer completes at least one verification step, then status transitions Pending → Active.
- Given no verification step has been completed, when a reviewer attempts to mark the record Active directly, then the transition is rejected — the entity throws rather than silently allowing it, per backend-best-practices §1.

## Developer Notes
- **PRD reference(s):** §10.3 Local DMC Onboarding steps 2-3; §22.5 T9
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — State-machine method on DMC-01's entity plus a reviewer-facing action.
- **Dependencies:** DMC-01, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `LocalDmcRecord.activate()` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/local-dmc/{id}/activate`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
