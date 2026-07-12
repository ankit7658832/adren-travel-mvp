---
id: DMC-10
epic: Local DMC + BYOS
phase: mock
status: not-started
story_points: 5
dependencies: ["DMC-03"]
labels: ["backend", "frontend", "dmc", "supplier", "phase1"]
prd_references: ["§10.2.8"]
modules_or_screens: ["supplier", "Local DMC Onboarding — NEW feature folder"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# DMC-10: Manage Local DMC inventory items after onboarding (CRUD)

## Summary (business)
This allows consultants to edit or remove individual items from a local partner's product catalogue after the initial bulk upload, so prices and offerings can be kept accurate and up to date over time. Any price change is immediately reflected in what customers see when they search.

## User Story
**As a** Consultant, **I want** edit or remove individual Local DMC inventory items after initial bulk upload, **so that** PRD §10.2.8's CRUD scope extends beyond the initial bulk-upload tool.

## Acceptance Criteria
- Given a Consultant edits a Local DMC inventory item's rate, when they save the change, then the updated rate is reflected in subsequent search results for that DMC's inventory.

## Developer Notes
- **PRD reference(s):** §10.2.8 Local DMC — Manual Integration
- **Module(s)/Screen(s):** supplier, Local DMC Onboarding — NEW feature folder
- **Story points:** 5 — Standard CRUD over DMC-03's bulk-uploaded records.
- **Dependencies:** DMC-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [EXTEND] Backend: `updateLocalDmcInventoryItem` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `PATCH /api/v1/local-dmc/{id}/inventory/{itemId}`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [NEW] Frontend: `useLocalDmcInventory` hook (React Query for server data per RULES.md §7.1)
- [EXTEND] Frontend: `LocalDmcInventoryManagement.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
