---
id: HRD-04
epic: Hardening
phase: mock
status: not-started
story_points: 5
dependencies: ["HRD-01"]
labels: ["backend", "frontend", "notification", "phase1"]
prd_references: ["§21.10", "§15"]
modules_or_screens: ["notification", "Notification Preferences Screen (21.10) — NEW feature folder"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# HRD-04: Build the Notification Preferences screen

## Summary (business)
Consultants get a settings screen where they can see and change how they receive their secondary notifications (for example, switching from text message to WhatsApp), with a sensible default already chosen based on their region so most people don't need to change anything.

## User Story
**As a** Consultant, **I want** toggle my secondary notification channel with a regional default pre-selected but overridable, **so that** PRD §21.10's layout is implemented.

## Acceptance Criteria
- Given a Consultant opens Notification Preferences, when the screen loads, then the regional default secondary channel is pre-selected, and the Consultant can override it.
- Given a Consultant overrides their default, when they save, then HRD-01's routing logic uses the override on all subsequent notifications.

## Developer Notes
- **PRD reference(s):** §21.10 Notification Preferences Screen; §15 Notifications
- **Module(s)/Screen(s):** notification, Notification Preferences Screen (21.10) — NEW feature folder
- **Story points:** 5 — Small screen over HRD-01's per-Consultant preference field.
- **Dependencies:** HRD-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [EXTEND] Backend: `updateNotificationPreference` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `PUT /api/v1/consultants/{id}/notification-preference`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [NEW] Frontend: `useNotificationPreferences` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `NotificationPreferences.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
