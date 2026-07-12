---
id: DMC-07
epic: Local DMC + BYOS
phase: mock
status: not-started
story_points: 8
dependencies: ["DMC-06", "FND-11", "FND-12"]
labels: ["backend", "dmc", "supplier", "phase1"]
prd_references: ["§10.2.9"]
modules_or_screens: ["supplier"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# DMC-07: Make the supplier integration layer credential-source-agnostic

## Summary (business)
This ensures the underlying system that connects to outside suppliers works identically whether the company's own trading account or a consultant's personal one (BYOS, "Bring Your Own Supplier") is being used, avoiding fragile, inconsistent code paths. In practice this means new supplier connections are more reliable and cheaper to maintain over time, with less risk of bugs from special-case handling.

## User Story
**As a** backend engineer, **I want** have the same Hotelbeds integration code path work whether credentials are Adren's own or a Consultant's BYOS credentials, **so that** PRD §10.2.9's requirement is met as a dependency-injection concern, not a branching concern, per backend-best-practices §6.

## Acceptance Criteria
- Given a search request is scoped to a Consultant with BYOS Hotelbeds credentials configured, when `HotelbedsClient` is invoked, then it receives the resolved credential set as a parameter — no `if (isByos)` branch exists inside the client itself.

## Developer Notes
- **PRD reference(s):** §10.2.9 BYOS (Technical integration pattern)
- **Module(s)/Screen(s):** supplier
- **Story points:** 8 — Structural refactor of the credential-resolution path across every supplier client — must be done once, correctly, before more suppliers land (Phase 2 SUP epic depends on this).
- **Dependencies:** DMC-06, FND-11, FND-12
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: credential-set-as-parameter contract across `HotelbedsClient` (and future supplier clients)
- [NEW] Backend: upstream credential-source resolver (Adren vs. BYOS) invoked once per request, injected downstream
- [NEW] Backend: unit test — same client class, both credential sources
- [NEW] Backend: module test
