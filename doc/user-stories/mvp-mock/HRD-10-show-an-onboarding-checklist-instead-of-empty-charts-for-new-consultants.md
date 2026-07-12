---
id: HRD-10
epic: Hardening
phase: mock
status: not-started
story_points: 3
dependencies: ["HRD-09"]
labels: ["frontend", "phase1"]
prd_references: ["§21.5"]
modules_or_screens: ["Consultant Dashboard (21.5)"]
testing_tiers: ["component test"]
---

# HRD-10: Show an onboarding checklist instead of empty charts for new Consultants

## Summary (business)
New consultants who haven't made any bookings yet will see a helpful getting-started checklist instead of a dashboard full of empty, zeroed-out charts, making their first experience with the platform more welcoming and actionable.

## User Story
**As a** new Consultant, **I want** see an onboarding checklist instead of empty dashboard charts before I have any bookings, **so that** PRD §21.5's empty-state requirement is met.

## Acceptance Criteria
- Given a new Consultant with zero bookings opens their dashboard, when the page loads, then an onboarding checklist is shown in place of empty charts, not a blank/zeroed-out dashboard.

## Developer Notes
- **PRD reference(s):** §21.5 Consultant Dashboard (empty states)
- **Module(s)/Screen(s):** Consultant Dashboard (21.5)
- **Story points:** 3 — Conditional presentational branch on top of HRD-09's dashboard.
- **Dependencies:** HRD-09
- **Testing tier(s):** component test

## Sub-tasks
- [EXTEND] Frontend: empty-state branch on `ConsultantDashboard` when zero bookings exist
- [NEW] Frontend: component test — empty-state renders the checklist, not zeroed charts
