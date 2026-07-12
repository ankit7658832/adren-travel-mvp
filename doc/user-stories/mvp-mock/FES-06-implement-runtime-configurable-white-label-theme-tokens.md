---
id: FES-06
epic: Frontend Shell
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-06", "FES-02"]
labels: ["frontend", "foundation", "whitelabel", "phase1"]
prd_references: ["§13.2"]
modules_or_screens: ["shared (frontend)"]
testing_tiers: ["unit", "component test"]
---

# FES-06: Implement runtime-configurable white-label theme tokens

## Summary (business)
This allows each travel consultant's own branding (logo, colors) to appear on their customer-facing pages instantly, without needing a new software release every time branding changes. This is important for the white-label (reselling the platform under another company's own brand) business model, letting consultants update their look quickly and independently.

## User Story
**As a** Consultant, **I want** see my branding (logo, colors) applied at runtime without a build-time deploy, **so that** PRD §13.2's per-tenant, runtime-applied branding is supported via CSS custom properties, per frontend-best-practices §5.

## Acceptance Criteria
- Given a Consultant's branding profile (FND-06) specifies primary/secondary colors, when their storefront loads, then CSS custom properties (`--adren-primary`, etc.) are set at runtime from that profile — no build-time-baked Tailwind config values.

## Developer Notes
- **PRD reference(s):** §13.2 Branding Configuration
- **Module(s)/Screen(s):** shared (frontend)
- **Story points:** 5 — Design-system-level runtime theming mechanism; must be chosen before styled components proliferate, per frontend-best-practices §5's explicit warning.
- **Dependencies:** FND-06, FES-02
- **Testing tier(s):** unit, component test

## Sub-tasks
- [NEW] Frontend: CSS custom-property theme-token contract
- [NEW] Frontend: runtime theme provider reading FND-06's branding profile
- [NEW] Frontend: component test — token values reflect the loaded profile
