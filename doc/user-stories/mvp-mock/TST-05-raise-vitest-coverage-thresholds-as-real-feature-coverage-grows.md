---
id: TST-05
epic: Test Infrastructure
phase: mock
status: not-started
story_points: 2
dependencies: ["TST-04"]
labels: ["frontend", "testing", "foundation", "phase1"]
prd_references: ["testing-strategy skill (Frontend tiers, coverage thresholds)"]
modules_or_screens: ["Infra (test)"]
testing_tiers: ["component test"]
---

# TST-05: Raise Vitest coverage thresholds as real feature coverage grows

## Summary (business)
As more of the product is actually built out, this raises the bar for how much of the code must be verified by automated tests, rather than leaving the original, looser starting requirement in place indefinitely. This ensures our quality safety net keeps pace with the growing product instead of becoming a weak formality.

## User Story
**As a** frontend engineer, **I want** have the coverage gate ratchet upward from the current scaffold-stage floor (70% lines/functions/statements, 60% branches), **so that** the coverage gate stays a meaningful floor rather than a permanently-loose scaffold artifact, per testing-strategy's explicit 'raise as real feature coverage grows' guidance.

## Acceptance Criteria
- Given real feature coverage in `src/features/` exceeds the current threshold by a defined margin, when the threshold-review checkpoint is reached, then `vite.config.ts`'s coverage thresholds are raised to match, not left at the scaffold-stage floor indefinitely.

## Developer Notes
- **PRD reference(s):** testing-strategy skill (Frontend tiers, coverage thresholds)
- **Module(s)/Screen(s):** Infra (test)
- **Story points:** 2 — Small, periodic config change — the story is establishing the review checkpoint/process, not a one-time number bump.
- **Dependencies:** TST-04
- **Testing tier(s):** component test

## Sub-tasks
- [EXTEND] Frontend: `vite.config.ts` coverage thresholds raised
- [NEW] Frontend: coverage-threshold review noted in the release checklist
