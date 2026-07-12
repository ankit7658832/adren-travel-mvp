---
id: DMC-09
epic: Local DMC + BYOS
phase: mock
status: not-started
story_points: 5
dependencies: ["DMC-08", "FND-03"]
labels: ["backend", "dmc", "supplier", "security", "phase1"]
prd_references: ["§22.6", "§5.2", "§5.3"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# DMC-09: Scope BYOS inventory and credentials strictly to the owning Consultant

## Summary (business)
This guarantees that a consultant's personal supplier credentials and the inventory they unlock (via BYOS, "Bring Your Own Supplier") are completely private to that consultant and can never be seen or used by another consultant. This protects sensitive business relationships and pricing arrangements from being accessed by competitors within the same platform.

## User Story
**As a** Consultant, **I want** be certain another Consultant can never see or use my BYOS credentials or the inventory they unlock, **so that** PRD §22.6's tenant-scoping half of T10 and RULES.md §5.2/§5.3 are both satisfied.

## Acceptance Criteria
- Given Consultant B's search runs, when BYOS credentials are resolved, then only Consultant B's own BYOS credentials are ever loaded — Consultant A's are neither visible nor usable, verified via the same tenant-isolation check as FND-03.

## Developer Notes
- **PRD reference(s):** §22.6 T10 (scoping); §5.2/§5.3 (RULES.md)
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Security-critical scoping test on top of DMC-08 — the IDOR risk RULES.md §5.3 explicitly calls out as worse than an itinerary leak.
- **Dependencies:** DMC-08, FND-03
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: BYOS credential resolver enforces tenant scope on every read
- [NEW] Backend: unit test — cross-tenant BYOS read attempt
- [NEW] Backend: integrationTest — cross-tenant BYOS read attempt end-to-end
