---
id: FES-07
epic: Frontend Shell
phase: mock
status: not-started
story_points: 8
dependencies: ["FND-01", "FND-02", "FES-01", "FES-02"]
labels: ["frontend", "foundation", "security", "phase1"]
prd_references: ["§6"]
modules_or_screens: ["Frontend shell (all screens)"]
testing_tiers: ["unit", "component test", "e2e"]
---

# FES-07: Add an auth/session context with per-role route guards

## Summary (business)
This ensures that people only ever see the parts of the application appropriate to their role (traveler, consultant, or super admin) and are redirected away before an unauthorized screen even appears, rather than briefly flashing sensitive admin content before blocking it. This protects sensitive administrative tools and reinforces trust that access controls are enforced consistently, not just as an afterthought.

## User Story
**As a** Consultant/User/Super Admin, **I want** only reach routes appropriate to my role, **so that** FND-01/FND-02's backend authorization is mirrored on the frontend so a User never even sees a Super-Admin-only route rendered before a 403.

## Acceptance Criteria
- Given a USER-role session attempts to navigate to the Super Admin Console route, when the route guard evaluates, then navigation is redirected before the Super Admin Console component ever mounts.

## Developer Notes
- **PRD reference(s):** §6 Roles & Permissions Matrix
- **Module(s)/Screen(s):** Frontend shell (all screens)
- **Story points:** 8 — Session context + guard-per-route wiring across all 10 screens; correctness depends on staying in sync with FND-01/02's backend role matrix.
- **Dependencies:** FND-01, FND-02, FES-01, FES-02
- **Testing tier(s):** unit, component test, e2e

## Sub-tasks
- [NEW] Frontend: auth/session context (principal: userId/role/consultantId, mirroring FND-01)
- [NEW] Frontend: per-route guard component wrapping each protected `<Route>`
- [NEW] Frontend: component test — guard redirect per role
- [NEW] Frontend: e2e — unauthorized navigation attempt redirected
