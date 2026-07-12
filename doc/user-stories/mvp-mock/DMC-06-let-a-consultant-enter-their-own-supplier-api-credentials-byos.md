---
id: DMC-06
epic: Local DMC + BYOS
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-12"]
labels: ["backend", "frontend", "dmc", "supplier", "phase1"]
prd_references: ["§10.4"]
modules_or_screens: ["supplier", "BYOS credential entry — NEW feature folder"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# DMC-06: Let a Consultant enter their own supplier API credentials (BYOS)

## Summary (business)
This lets a consultant connect their own accounts with external inventory providers (known as BYOS, "Bring Your Own Supplier") by entering their own login credentials, so that supplier's inventory becomes available only to that consultant. Credentials are stored securely and encrypted so they can't be exposed or misused.

## User Story
**As a** Consultant, **I want** enter my own Hotelbeds/STUBA/etc. API credentials so BYOS inventory is scoped to my account, **so that** PRD §10.4's BYOS entry flow is available, feeding FND-12's row-level encrypted storage.

## Acceptance Criteria
- Given a Consultant enters their own Hotelbeds credentials, when they save the form, then the credentials are stored via FND-12's row-level encryption, scoped only to that Consultant.

## Developer Notes
- **PRD reference(s):** §10.4 BYOS
- **Module(s)/Screen(s):** supplier, BYOS credential entry — NEW feature folder
- **Story points:** 5 — UI + endpoint over FND-12's already-built encrypted storage mechanism.
- **Dependencies:** FND-12
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [EXTEND] Backend: `saveByosCredential` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/consultants/{id}/byos-credentials`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [NEW] Frontend: `useByosCredentialEntry` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `ByosCredentialEntry.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
