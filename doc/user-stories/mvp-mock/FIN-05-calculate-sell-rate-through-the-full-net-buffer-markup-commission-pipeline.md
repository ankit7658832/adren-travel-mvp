---
id: FIN-05
epic: Financial Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["FIN-01", "FIN-02", "FIN-03", "FIN-04"]
labels: ["backend", "financial", "payments", "phase1"]
prd_references: ["§12.1", "§24.1"]
modules_or_screens: ["payments"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# FIN-05: Calculate sell rate through the full net→buffer→markup→commission pipeline

## Summary (business)
The system calculates the final price a customer pays by combining the supplier's base cost, the currency safety buffer, the consultant's margin, and the company's commission into one accurate, penny-precise figure. This ensures pricing is consistent, trustworthy, and fully traceable for every booking, avoiding rounding errors that could erode profit or overcharge customers.

## User Story
**As a** Consultant, **I want** see a correct sell_rate on every line item combining net rate, currency buffer, markup, and commission, **so that** PRD §12.1's worked examples produce decimal-safe, auditable results end-to-end.

## Acceptance Criteria
- Given a hotel line item is priced per Worked Example B's inputs, when the pipeline runs, then sell_rate matches the worked example to the cent using `Money`'s `BigDecimal`/`HALF_UP` semantics, never `double`.

## Developer Notes
- **PRD reference(s):** §12.1 Worked Examples A & B; §24.1 (decimal-safe arithmetic)
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — Orchestrates FIN-01/02/03/04 into one pipeline — the composition is the risk, not any individual step.
- **Dependencies:** FIN-01, FIN-02, FIN-03, FIN-04
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: `PricingPipeline` composing buffer→markup→commission→FX-snapshot in order
- [NEW] Backend: unit test — both worked examples reproduced exactly
- [NEW] Backend: integrationTest — full pipeline against a real line item persist/read round trip
