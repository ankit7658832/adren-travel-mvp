---
id: FND-05
epic: Foundation
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-04"]
labels: ["backend", "frontend", "foundation", "phase1"]
prd_references: ["§3.1", "§21.6"]
modules_or_screens: ["whitelabel", "Super Admin Console (21.6)"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# FND-05: Super Admin manages Consultant lifecycle (view, suspend, reinstate)

## Summary (business)
This story gives administrators a dashboard to see every onboarded consultant and to pause or restore their access. This lets the business maintain oversight and quickly shut down a problematic account without needing developer help.

## User Story
**As a** Super Admin, **I want** view all onboarded Consultants and change their status, **so that** I retain oversight of the Consultant base per PRD §3.1's Super Admin persona description.

## Acceptance Criteria
- Given Super Admin opens the Consultants list, when the page loads, then every Consultant is shown with status, home market, and onboarding date, paginated per RULES.md §3.4.
- Given Super Admin suspends a Consultant, when the action is confirmed, then that Consultant's Users can no longer search/book until reinstated.

## Developer Notes
- **PRD reference(s):** §3.1 Super Admin persona; §21.6 Super Admin Console
- **Module(s)/Screen(s):** whitelabel, Super Admin Console (21.6)
- **Story points:** 5 — Standard CRUD + status transition on top of FND-04's entity.
- **Dependencies:** FND-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [EXTEND] Backend: `suspendConsultant/reinstateConsultant` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `PATCH /api/v1/consultants/{id}/status`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [NEW] Frontend: `useConsultantList` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `ConsultantList.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
