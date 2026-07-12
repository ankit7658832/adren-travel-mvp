---
id: FES-08
epic: Frontend Shell
phase: mock
status: not-started
story_points: 5
dependencies: ["FES-04"]
labels: ["frontend", "foundation", "phase1"]
prd_references: ["§21.3", "§21.4", "§21.6"]
modules_or_screens: ["Frontend shell (tooling)"]
testing_tiers: ["component test"]
---

# FES-08: Adopt react-hook-form + zod as the form/validation standard

## Summary (business)
This standardizes how the product collects and validates information typed into forms (such as traveler details, package setup, and onboarding), before three major forms are built. Committing to one consistent approach now avoids inconsistent, harder-to-maintain forms later, which speeds up development and reduces form-related bugs for users.

## User Story
**As a** frontend engineer, **I want** have one consistent form/validation approach before the Traveler Detail form, Package Builder form, and onboarding wizard are built, **so that** frontend-best-practices §4's recommendation is adopted deliberately, avoiding each form reinventing its own `useState`-per-field handling.

## Acceptance Criteria
- Given a new multi-field form is built after this story lands, when it is inspected, then it uses `react-hook-form` + a `zod` schema via `@hookform/resolvers`, not a bespoke per-field `useState` pattern.

## Developer Notes
- **PRD reference(s):** §21.3 Package Builder; §21.4 Booking & Payment Flow; §21.6 Super Admin Console (onboarding wizard)
- **Module(s)/Screen(s):** Frontend shell (tooling)
- **Story points:** 5 — Library adoption + one reference form migration to prove the pattern, ahead of BOK-11/BOK-13/ADS-03/FES-09 all needing it.
- **Dependencies:** FES-04
- **Testing tier(s):** component test

## Sub-tasks
- [NEW] Frontend: `react-hook-form` + `zod` + `@hookform/resolvers` dependency adoption
- [NEW] Frontend: one reference form migrated to prove the pattern (e.g. the Search Dashboard's date/pax fields)
- [NEW] Frontend: component test — validation error surfaces via FES-04's `TextField` aria wiring
