---
id: TST-09
epic: Test Infrastructure
phase: mock
status: not-started
story_points: 3
dependencies: ["AI-07", "TST-01"]
labels: ["backend", "testing", "foundation", "ai", "phase1"]
prd_references: ["§11.2", "§24.3"]
modules_or_screens: ["ai (test infra)"]
testing_tiers: ["module (@ApplicationModuleTest)"]
---

# TST-09: Build an AI governance audit-log test harness asserting the 100%-logged invariant

## Summary (business)
This builds an automated check confirming that every single use of AI within the product is properly recorded in an audit trail, with none missed. This gives the business confidence that our AI usage record-keeping is complete and reliable, which matters for regulatory compliance and being able to explain AI-driven decisions if ever challenged.

## User Story
**As a** backend engineer, **I want** have a reusable test harness that asserts every AI call in a test run produced exactly one audit-log entry, **so that** PRD §11.2/§24.3's 100%-logged, no-sampling requirement is enforced by the AI module's own test suite, not just documented.

## Acceptance Criteria
- Given N AI calls are made within a module/integrationTest run, when the harness's assertion is applied, then exactly N audit-log entries exist — any mismatch fails the test, not just a manual review.

## Developer Notes
- **PRD reference(s):** §11.2 principle 5; §24.3 NFR AI Governance
- **Module(s)/Screen(s):** ai (test infra)
- **Story points:** 3 — Focused assertion helper on top of AI-07's audit table — small but load-bearing for every future AI-module test.
- **Dependencies:** AI-07, TST-01
- **Testing tier(s):** module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `AiAuditCompletenessAssertions` shared test helper
- [NEW] Backend: applied to AI-02/AI-03's existing tests as the reference usage
