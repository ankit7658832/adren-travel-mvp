---
id: FES-04
epic: Frontend Shell
phase: mock
status: not-started
story_points: 8
dependencies: ["FND-19"]
labels: ["frontend", "foundation", "phase1"]
prd_references: ["§21"]
modules_or_screens: ["shared (frontend)"]
testing_tiers: ["component test"]
---

# FES-04: Build shared UI primitives — Button, Card, TextField, Select

## Summary (business)
This creates a shared set of reusable building blocks (buttons, cards, text fields, dropdowns) that every part of the application will use, so they look and behave consistently and automatically work well with screen readers and other accessibility tools. This saves development time on every future screen and ensures the product is usable by people with disabilities without extra effort each time.

## User Story
**As a** frontend engineer, **I want** have generic, accessible primitives in `shared/components` instead of each feature reinventing form controls, **so that** every consumer gets correct `label`/`htmlFor`/`aria-invalid`/`aria-describedby` wiring for free, per frontend-best-practices §5.

## Acceptance Criteria
- Given a `TextField` primitive is rendered with a validation error, when it is inspected, then `aria-invalid` and `aria-describedby` are wired to the error message automatically, with no per-consumer a11y code required.

## Developer Notes
- **PRD reference(s):** §21 (cross-screen UI consistency)
- **Module(s)/Screen(s):** shared (frontend)
- **Story points:** 8 — First entries in an empty `shared/components` — four primitives, each needing correct accessibility wiring per RULES.md §7.3, is genuinely the size of a small design-system slice.
- **Dependencies:** FND-19
- **Testing tier(s):** component test

## Sub-tasks
- [NEW] Frontend: `Button` component
- [NEW] Frontend: `Card` component
- [NEW] Frontend: `TextField` component (label/aria wiring built-in)
- [NEW] Frontend: `Select` component
- [NEW] Frontend: component test per primitive
