---
id: DMC-01
epic: Local DMC + BYOS
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-01"]
labels: ["backend", "frontend", "dmc", "supplier", "phase1"]
prd_references: ["§10.3", "§20.14"]
modules_or_screens: ["supplier", "Local DMC Onboarding — NEW feature folder"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# DMC-01: Submit a new Local DMC for onboarding

## Summary (business)
This lets our travel consultants register a new local destination expert (DMC, or Destination Management Company - a local partner who supplies tours, transfers, and other on-the-ground services) by capturing their business details, the types of products they offer, sample prices, and references. Every new partner starts in a "Pending" state so nothing goes live until it has been checked, protecting customers from unvetted suppliers.

## User Story
**As a** Consultant, **I want** submit a Local DMC's business info, product categories, sample rates, and references, **so that** PRD §10.3 step 1's onboarding submission is captured before any vetting can begin.

## Acceptance Criteria
- Given a Consultant submits a new Local DMC, when the submission is saved, then its status is Pending, not Active, until at least one verification step completes (T9).

## Developer Notes
- **PRD reference(s):** §10.3 Local DMC Onboarding step 1; §20.14 Local DMC Record
- **Module(s)/Screen(s):** supplier, Local DMC Onboarding — NEW feature folder
- **Story points:** 5 — New entity + submission form; the Pending-by-default rule is the load-bearing invariant (enforced on the entity, per backend-best-practices §1).
- **Dependencies:** FND-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [NEW] Backend: `LocalDmcRecord` entity + `LocalDmcRecordRepository` (package-private, own Flyway migration)
- [NEW] Backend: `submitLocalDmc` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `POST /api/v1/local-dmc` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
- [NEW] Frontend: `useLocalDmcOnboarding` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `LocalDmcOnboarding.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
