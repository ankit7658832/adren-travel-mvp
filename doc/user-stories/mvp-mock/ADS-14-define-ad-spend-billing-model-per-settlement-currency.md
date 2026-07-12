---
id: ADS-14
epic: Ads/Campaign Management
phase: mock
status: not-started
story_points: 5
dependencies: ["ADS-11", "FIN-06"]
labels: ["backend", "ads", "payments", "phase1"]
prd_references: ["§1", "§19"]
modules_or_screens: ["ads", "payments"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# ADS-14: Define ad-spend billing model per settlement currency

## Summary (business)
Ad spending is billed to each Consultant in their own local settlement currency, with Adren's management fee for running the advertising built into the calculation. This ensures Consultants are charged fairly and clearly in the currency they operate in, matching the business's advertising revenue model.

## User Story
**As a** Super Admin, **I want** bill ad spend to each Consultant in their settlement currency using a defined managed-service fee model, **so that** PRD §1's 'managed-service fee on ad spend' business model is reflected in the billing calculation.

## Acceptance Criteria
- Given a campaign accrues spend in a Consultant's settlement currency, when billing is calculated, then the managed-service fee is applied per the configured model and reconciled against FIN-06's wallet/ledger.

## Developer Notes
- **PRD reference(s):** §1 Executive Summary (managed-service fee); §19 Open Items
- **Module(s)/Screen(s):** ads, payments
- **Story points:** 5 — Calculation logic is straightforward once the business rule is confirmed; flagged pending business sign-off.
- **Dependencies:** ADS-11, FIN-06
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

> ⚠️ **NEEDS CLARIFICATION:** PRD §19: exact ad-spend billing model per settlement currency is an open item for business confirmation — this story implements a configurable-percentage placeholder pipeline pending the confirmed model.

## Sub-tasks
- [NEW] Backend: `calculateAdSpendBilling` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — billing pipeline step)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
