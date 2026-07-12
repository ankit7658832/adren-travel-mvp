---
id: TST-06
epic: Test Infrastructure
phase: mock
status: not-started
story_points: 5
dependencies: ["TST-01"]
labels: ["backend", "testing", "foundation", "supplier", "phase1"]
prd_references: ["§23.2", "§25"]
modules_or_screens: ["supplier (test infra)"]
testing_tiers: ["integration (Testcontainers)"]
---

# TST-06: Separate supplier sandbox-vs-production fixtures in CI

## Summary (business)
Our travel suppliers (the external companies providing hotel and flight inventory, such as Hotelbeds and TBO) behave differently in their test environments versus their live, real-money environments. This work makes sure we test against both separately so differences don't slip through unnoticed, reducing the risk of booking failures or errors affecting real customers and revenue.

## User Story
**As a** QA/backend engineer, **I want** have supplier integration tests run against both sandbox and production-shaped fixtures, flagged separately, **so that** PRD §23.2 Edge Case #4 and T19/T20 are protected against regressions — sandbox and production supplier environments are documented to behave differently (Hotelbeds/TBO specifically).

## Acceptance Criteria
- Given a supplier client's `integrationTest` suite runs in CI, when results are reported, then sandbox-fixture and production-fixture-shaped test runs are reported as two distinct, separately-flagged CI jobs, not assumed equivalent.

## Developer Notes
- **PRD reference(s):** §23.2 Edge Case #4; §25 T19/T20; testing-strategy skill (Supplier integration tests)
- **Module(s)/Screen(s):** supplier (test infra)
- **Story points:** 5 — Test-fixture separation + CI job structure; the actual production fixtures are stubbed/synthetic in MVP (real production access is Phase 2's SUP epic).
- **Dependencies:** TST-01
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: sandbox-shaped and production-shaped fixture sets per supplier client
- [NEW] Backend: CI job split — sandbox-fixture run vs. production-fixture-shaped run, separately flagged
