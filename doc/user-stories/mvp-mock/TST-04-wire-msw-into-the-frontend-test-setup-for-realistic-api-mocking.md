---
id: TST-04
epic: Test Infrastructure
phase: mock
status: not-started
story_points: 5
dependencies: ["FES-08"]
labels: ["frontend", "testing", "foundation", "phase1"]
prd_references: ["§7.1"]
modules_or_screens: ["Infra (test)"]
testing_tiers: ["component test"]
---

# TST-04: Wire MSW into the frontend test setup for realistic API mocking

## Summary (business)
This improves how the frontend team tests the app by having tests simulate real network calls to the backend rather than faking results internally. This makes automated tests far more trustworthy indicators of whether the app will actually work correctly for customers, catching integration problems earlier and more cheaply.

## User Story
**As a** frontend engineer, **I want** have `src/test/setup.ts` intercept `apiClient` calls via MSW instead of mocking hook functions directly, **so that** tests exercise the real request/response shape once features start making real `apiClient` calls, per testing-strategy's explicit note that `msw` is installed but unwired.

## Acceptance Criteria
- Given a component test needs a server response, when it runs, then MSW intercepts the `apiClient` HTTP call and returns the configured response — no `apiClient` function itself is mocked.

## Developer Notes
- **PRD reference(s):** testing-strategy skill (Frontend tiers, MSW note); §7.1 (RULES.md, React Query reconciliation)
- **Module(s)/Screen(s):** Infra (test)
- **Story points:** 5 — Wires an already-installed dependency; the risk is in migrating existing mocked-hook tests to the new pattern without regressing coverage.
- **Dependencies:** FES-08
- **Testing tier(s):** component test

## Sub-tasks
- [NEW] Test infra: MSW server registered in `src/test/setup.ts`, existing search-dashboard tests migrated to MSW handlers
- [NEW] Test infra: sample test exercising the new harness
- [NEW] Test infra: CI wiring / gradle-or-npm script update
