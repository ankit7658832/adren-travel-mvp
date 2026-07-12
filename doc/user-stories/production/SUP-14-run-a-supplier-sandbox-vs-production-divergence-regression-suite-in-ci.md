---
id: SUP-14
epic: Supplier Live Integrations
phase: production
status: not-started
story_points: 5
dependencies: ["TST-06", "SUP-02", "SUP-04", "SUP-06"]
labels: ["backend", "testing", "supplier", "phase2"]
prd_references: ["§23.2", "§25"]
modules_or_screens: ["supplier"]
testing_tiers: ["integration (Testcontainers)"]
---

# SUP-14: Run a supplier sandbox-vs-production divergence regression suite in CI

## Summary (business)
This adds an automated ongoing check that compares supplier connections against both test and realistic production-like conditions every time a change is made, flagging any unexpected differences immediately. It catches problems with supplier integrations early and continuously, rather than relying on a one-time check when the connection was first built.

## User Story
**As a** QA engineer, **I want** have every supplier client change automatically run against both sandbox and production-like fixtures with divergence flagged, **so that** PRD §23.2 Edge Case #4 is enforced continuously at production scale, not just verified once during initial integration.

## Acceptance Criteria
- Given a supplier client's mapping logic changes, when CI runs, then both the sandbox-fixture and production-fixture-shaped test suites (TST-06) execute, and any divergence in mapped output is flagged as a distinct CI failure, not silently passed.

## Developer Notes
- **PRD reference(s):** §23.2 Edge Case #4; §25 T19/T20
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Extends TST-06's MVP-stage separation with real production fixture data now available post-cutover.
- **Dependencies:** TST-06, SUP-02, SUP-04, SUP-06
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Test infra: sandbox-vs-production divergence regression suite using real post-cutover production fixtures
- [NEW] Test infra: sample test exercising the new harness
- [NEW] Test infra: CI wiring / gradle-or-npm script update
