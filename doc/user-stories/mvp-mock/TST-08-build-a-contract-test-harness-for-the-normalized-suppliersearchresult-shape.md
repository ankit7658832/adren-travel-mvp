---
id: TST-08
epic: Test Infrastructure
phase: mock
status: not-started
story_points: 5
dependencies: ["TST-01"]
labels: ["backend", "testing", "foundation", "supplier", "phase1"]
prd_references: ["§20.2", "§6"]
modules_or_screens: ["supplier (test infra)"]
testing_tiers: ["unit"]
---

# TST-08: Build a contract-test harness for the normalized SupplierSearchResult shape

## Summary (business)
As we connect to more travel suppliers over time, this creates an automated check ensuring each supplier's data is translated into our own consistent internal format, without any supplier-specific quirks leaking through. This protects the product from subtle data-consistency bugs as we scale to more supplier partnerships.

## User Story
**As a** backend engineer, **I want** have shared assertion helpers that verify every supplier client's output conforms to the normalized result shape, **so that** PRD §20.2–20.6's normalized-field discipline (backend-best-practices §6) is enforced consistently as more supplier clients are added in Phase 2's SUP epic.

## Acceptance Criteria
- Given a new supplier client's mapping is unit-tested, when the shared contract assertion is applied, then it fails the build if a supplier-specific field name (e.g. TBO's `TraceId`) leaks into the public normalized result shape.

## Developer Notes
- **PRD reference(s):** §20.2-20.6 Line Item data dictionary; backend-best-practices skill §6 (normalization discipline)
- **Module(s)/Screen(s):** supplier (test infra)
- **Story points:** 5 — Shared test-helper library; low runtime complexity, high leverage as supplier count grows.
- **Dependencies:** TST-01
- **Testing tier(s):** unit

## Sub-tasks
- [NEW] Backend: `SupplierSearchResultContractAssertions` shared test helper
- [NEW] Backend: applied to `HotelbedsClient`'s existing test as the reference usage
