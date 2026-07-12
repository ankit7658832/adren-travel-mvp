---
id: FND-19
epic: Foundation
phase: mock
status: not-started
story_points: 2
dependencies: []
labels: ["frontend", "foundation", "tooling", "phase1"]
prd_references: ["§7.3"]
modules_or_screens: ["Frontend shell (tooling)"]
testing_tiers: ["module (lint run in CI)"]
---

# FND-19: Add ESLint flat config with jsx-a11y

## Summary (business)
This story turns on automated code-quality and accessibility checks that run every time new code is submitted, including checks that ensure the site remains usable by people with disabilities. Today these checks don't actually run despite being set up, so accessibility issues could slip through unnoticed.

## User Story
**As a** frontend engineer, **I want** have `npm run lint` actually run something, including accessibility linting, **so that** the accessibility baseline in RULES.md §7.3 is enforced in CI rather than just hoped for — currently there is no ESLint config at all despite the dependencies being installed.

## Acceptance Criteria
- Given `npm run lint` is run, when the command executes, then it lints the codebase using `eslint-plugin-jsx-a11y`, `eslint-plugin-react-hooks`, and `eslint-plugin-react-refresh` and exits non-zero on a violation.

## Developer Notes
- **PRD reference(s):** §7.3 Accessibility baseline (RULES.md, reconciliation item #5)
- **Module(s)/Screen(s):** Frontend shell (tooling)
- **Story points:** 2 — Config-only change; `eslint-plugin-jsx-a11y` needs adding as a new devDependency.
- **Dependencies:** None
- **Testing tier(s):** module (lint run in CI)

## Sub-tasks
- [NEW] Infra: `eslint.config.js` flat config with jsx-a11y + react-hooks + react-refresh plugins
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
