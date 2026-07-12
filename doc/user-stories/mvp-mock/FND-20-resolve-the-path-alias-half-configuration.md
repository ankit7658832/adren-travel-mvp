---
id: FND-20
epic: Foundation
phase: mock
status: not-started
story_points: 1
dependencies: []
labels: ["frontend", "foundation", "tooling", "phase1"]
prd_references: ["§7.5"]
modules_or_screens: ["Frontend shell (tooling)"]
testing_tiers: ["unit"]
---

# FND-20: Resolve the `@/*` path-alias half-configuration

## Summary (business)
This story cleans up an inconsistency in how the frontend codebase's internal shortcuts for importing shared code are set up, ensuring the convention either works everywhere or is removed entirely. This is a technical housekeeping fix that prevents confusing, hard-to-diagnose bugs for the engineering team; it has no direct customer-facing impact.

## User Story
**As a** frontend engineer, **I want** have the `@/*` import alias either fully wired or fully removed, **so that** the codebase doesn't carry a dangling, half-configured convention where `tsconfig.json` declares an alias `vite.config.ts` doesn't resolve, per RULES.md §7.5.

## Acceptance Criteria
- Given a new cross-feature import is written, when the chosen convention is followed, then it either uses `@/shared/...` (alias wired into `vite.config.ts`) or a relative path — never a mix that silently fails to resolve.

## Developer Notes
- **PRD reference(s):** §7.5 Path alias (RULES.md, reconciliation item #7)
- **Module(s)/Screen(s):** Frontend shell (tooling)
- **Story points:** 1 — Single decision + one-line config change (or one deletion) — the smallest story in the catalogue.
- **Dependencies:** None
- **Testing tier(s):** unit

## Sub-tasks
- [NEW] Frontend: wire `resolve.alias` into `vite.config.ts` to match `tsconfig.json`'s `@/*` (or remove the `paths` entry if relative imports are chosen instead)
- [NEW] Frontend: convert at least one existing cross-feature import to prove the chosen convention resolves
