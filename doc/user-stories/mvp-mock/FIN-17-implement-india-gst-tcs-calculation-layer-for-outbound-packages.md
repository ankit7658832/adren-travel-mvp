---
id: FIN-17
epic: Financial Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["FIN-05"]
labels: ["backend", "financial", "payments", "compliance", "phase1"]
prd_references: ["§12.1", "§17.2", "§25"]
modules_or_screens: ["payments", "compliance"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FIN-17: Implement India GST/TCS calculation layer for outbound packages

## Summary (business)
For international trips sold to customers in India, the system automatically applies the correct Indian taxes — GST (Goods and Services Tax, a tax on the service/margin portion) and TCS (Tax Collected at Source, a tax collected upfront on the total package value) — in line with current Indian tax law. This keeps the business compliant with Indian tax regulations and avoids costly manual tax calculation errors.

## User Story
**As a** Consultant, **I want** see GST and TCS applied to an outbound package sold to an India-based traveler per current tax rules, **so that** PRD §12.1 Worked Example C and §17.2's India tax-layer requirement are implemented, distinct from UK TOMS logic.

## Acceptance Criteria
- Given an outbound package is sold to an India-based Consultant's traveler, when the sale completes, then the tax-calculation layer applies GST to the margin/service component and TCS to the outbound package value per Section 12.1 Example C (T24).

## Developer Notes
- **PRD reference(s):** §12.1 Worked Example C; §17.2 Platform Enforcement; §25 T24
- **Module(s)/Screen(s):** payments, compliance
- **Story points:** 5 — Calculation layer's shape only — exact rates are an explicit open item (PRD §19) pending tax-counsel sign-off.
- **Dependencies:** FIN-05
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

> ⚠️ **NEEDS CLARIFICATION:** PRD §19/§12.1 Example C: exact GST/TCS rates and mechanics require tax-counsel sign-off before implementation — this story implements the calculation layer's shape using the PRD's illustrative rates only, gated behind a config flag until counsel confirms the real figures.

## Sub-tasks
- [NEW] Backend: `calculateIndiaGstTcs` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — pricing pipeline step)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
