---
id: FIN-18
epic: Financial Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["FIN-05", "FIN-17"]
labels: ["backend", "financial", "payments", "compliance", "phase1"]
prd_references: ["§12.1", "§17.2"]
modules_or_screens: ["payments", "compliance"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FIN-18: Implement UK TOMS VAT calculation layer

## Summary (business)
For UK-based consultants, VAT (Value Added Tax) under the special TOMS scheme (Tour Operators' Margin Scheme, a UK tax rule for travel packages) is correctly applied only to the consultant's profit margin, not the full price the customer pays. This ensures the business stays compliant with UK tax law and avoids overcharging VAT, which would otherwise inflate prices or create tax liabilities.

## User Story
**As a** Consultant, **I want** see UK TOMS VAT applied correctly to a package's margin component, not the full sale price, **so that** PRD §12.1 Worked Example D's requirement — TOMS must not be approximated as a flat percentage of total sale price — is implemented.

## Acceptance Criteria
- Given a UK Consultant's package cost (margin only, per the TOMS mechanism) is GBP 200, when VAT is calculated, then it applies to the margin component only, matching Worked Example D's shape, never to the full package price.

## Developer Notes
- **PRD reference(s):** §12.1 Worked Example D; §17.2 Platform Enforcement
- **Module(s)/Screen(s):** payments, compliance
- **Story points:** 5 — Calculation layer's shape only — exact rate requires UK tax-counsel sign-off per PRD §19.
- **Dependencies:** FIN-05, FIN-17
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

> ⚠️ **NEEDS CLARIFICATION:** PRD §19/§12.1 Example D: exact TOMS VAT rate and mechanics require UK tax-counsel sign-off before implementation — this story implements the calculation layer's shape (margin-only base) using the PRD's illustrative rate only, gated behind a config flag until counsel confirms the real figure.

## Sub-tasks
- [NEW] Backend: `calculateUkTomsVat` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — pricing pipeline step)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
