---
id: FND-02
epic: Foundation
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-01"]
labels: ["backend", "foundation", "security", "phase1"]
prd_references: ["§5.1", "§6"]
modules_or_screens: ["booking", "supplier", "shared"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FND-02: Enforce PRD §6 role matrix via method-level @PreAuthorize on module Api interfaces

## Summary (business)
This story makes sure permission checks (who is allowed to do what) are enforced consistently everywhere in the system, not just on the screens people see. That way a staff member without the right permissions can never sneak past a check by using a different route into the system, such as an automated task.

## User Story
**As a** Super Admin, **I want** have role checks enforced on the Api interface itself, not just at the controller, **so that** every caller (a future scheduled job, another module, a controller) inherits the same authorization guarantee per RULES.md §5.1.

## Acceptance Criteria
- Given a USER principal calls a method reserved for CONSULTANT/SUPER_ADMIN (e.g. onboarding a Local DMC), when the call reaches the Api layer, then it is rejected with 403 regardless of which controller/listener invoked it.
- Given a capability is marked 'No (unless granted)' for USER in §6 (e.g. create package), when the per-Consultant grant flag is false, then the call is rejected; when the flag is true, it succeeds.

## Developer Notes
- **PRD reference(s):** §5.1 (RULES.md); §6 Roles & Permissions Matrix
- **Module(s)/Screen(s):** booking, supplier, shared
- **Story points:** 5 — Well-understood pattern (@PreAuthorize expressions) but must be threaded across every existing Api method.
- **Dependencies:** FND-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `@PreAuthorize` expressions on `BookingApi`/`SupplierSearchApi` methods
- [NEW] Backend: per-Consultant capability-grant flag (data-driven, not a role switch) for 'unless granted' cases
- [NEW] Backend: unit test per role/capability combination
- [NEW] Backend: module test asserting a listener/job path also gets the check
