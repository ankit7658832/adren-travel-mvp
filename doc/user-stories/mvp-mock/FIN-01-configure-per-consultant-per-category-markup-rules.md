---
id: FIN-01
epic: Financial Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-01"]
labels: ["backend", "financial", "payments", "phase1"]
prd_references: ["§12.1"]
modules_or_screens: ["payments"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FIN-01: Configure per-Consultant, per-category markup rules

## Summary (business)
Travel consultants can set their own profit margin for each type of product they sell (hotels, flights, transfers, cruises, activities), either as a percentage or a fixed fee. This lets each consultant control their earnings consistently, so every booking they make reflects the margin they intended rather than a one-size-fits-all rule.

## User Story
**As a** Consultant, **I want** configure my markup per product category as a percentage or a flat fee, **so that** my margin is applied consistently across every hotel/flight/transfer/cruise/activity line item, per PRD §12.1.

## Acceptance Criteria
- Given a Consultant configures a 15% markup on hotels, when a hotel line item is added, then the sell_rate reflects net_rate × 1.15 (Worked Example A, T6).
- Given a Consultant configures a flat-fee markup on activities, when an activity line item is added, then the flat fee is added to net_rate rather than a percentage.

## Developer Notes
- **PRD reference(s):** §12.1 Markup & Commission Engine
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — New configurable rule entity per Consultant×category; calculation logic is straightforward, configuration surface is the work.
- **Dependencies:** FND-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `MarkupRule` entity + `MarkupRuleRepository` (package-private, own Flyway migration)
- [NEW] Backend: `configureMarkup` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `PUT /api/v1/consultants/{id}/markup-rules` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
