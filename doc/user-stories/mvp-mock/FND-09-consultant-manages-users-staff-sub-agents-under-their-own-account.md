---
id: FND-09
epic: Foundation
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-01", "FND-02"]
labels: ["backend", "frontend", "foundation", "phase1"]
prd_references: ["§3.2", "§3.3", "§6"]
modules_or_screens: ["whitelabel"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# FND-09: Consultant manages Users (staff/sub-agents) under their own account

## Summary (business)
This story lets a travel consultant create separate logins for their own staff, instead of everyone sharing one account. Each staff member can then search and book trips under appropriate permissions, without ever having access to the consultant's own login or sensitive settings like pricing markup.

## User Story
**As a** Consultant, **I want** add and manage Users under my account, **so that** my staff can search, build itineraries, and book products without me sharing my own login, per PRD §3.3.

## Acceptance Criteria
- Given a Consultant adds a new User, when the User is created, then that User can log in scoped to the Consultant's consultant_id and cannot change markup, onboard suppliers, or manage branding.
- Given a Consultant grants a User the 'create package' permission, when the grant is saved, then that specific User can create packages while others under the same Consultant cannot.

## Developer Notes
- **PRD reference(s):** §3.2 Consultant persona; §3.3 User persona; §6 Roles & Permissions Matrix
- **Module(s)/Screen(s):** whitelabel
- **Story points:** 5 — Standard CRUD entity plus per-User capability-grant flags reused by FND-02's authorization checks.
- **Dependencies:** FND-01, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [NEW] Backend: `ConsultantUser` entity + `ConsultantUserRepository` (package-private, own Flyway migration)
- [NEW] Backend: `addUser/updateUserGrants` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `POST /api/v1/users` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
- [NEW] Frontend: `useUserManagement` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `UserManagement.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
