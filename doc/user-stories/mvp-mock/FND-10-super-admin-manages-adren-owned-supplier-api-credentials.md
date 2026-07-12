---
id: FND-10
epic: Foundation
phase: mock
status: not-started
story_points: 8
dependencies: ["FND-01", "FND-02"]
labels: ["backend", "frontend", "foundation", "supplier", "security", "phase1"]
prd_references: ["§21.6", "§10.2"]
modules_or_screens: ["supplier", "Super Admin Console (21.6)"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# FND-10: Super Admin manages Adren-owned supplier API credentials

## Summary (business)
This story gives administrators a secure screen to manage the login credentials the platform uses to connect to travel suppliers (hotels, flights, transfers, activities), with sensitive values hidden from view and a record of who last changed them. This protects valuable supplier partnerships from accidental leaks and gives a clear audit trail if something goes wrong.

## User Story
**As a** Super Admin, **I want** add and update Hotelbeds/STUBA/TBO/Mystifly/Transferz/Widgety/HBActivities credentials with masked fields and an audit trail of who last changed them, **so that** supplier access can be rotated and reviewed without exposing raw secrets in the UI, per PRD §21.6.

## Acceptance Criteria
- Given Super Admin opens the credential screen for Hotelbeds, when the credential set is displayed, then the secret value is masked and only the last-modified user/timestamp is shown.
- Given Super Admin updates a supplier's credentials, when the change is saved, then an audit log entry records who changed it and when.

## Developer Notes
- **PRD reference(s):** §21.6 Super Admin Console — Supplier credential management; §10.2 Per-Supplier Integration Requirements
- **Module(s)/Screen(s):** supplier, Super Admin Console (21.6)
- **Story points:** 8 — New entity + audit trail + masked-field UI, feeding 7 supplier integrations — foundational for the whole supplier module.
- **Dependencies:** FND-01, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [NEW] Backend: `SupplierCredential` entity + `SupplierCredentialRepository` (package-private, own Flyway migration)
- [NEW] Backend: `updateSupplierCredential` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `PUT /api/v1/suppliers/{supplierId}/credentials` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
- [NEW] Frontend: `useSupplierCredentials` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `SupplierCredentialManagement.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Backend: credential-change audit log (who/when, never the secret value)
