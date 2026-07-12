---
id: FES-01
epic: Frontend Shell
phase: mock
status: not-started
story_points: 5
dependencies: []
labels: ["frontend", "foundation", "phase1"]
prd_references: ["§21"]
modules_or_screens: ["Frontend shell (all screens)"]
testing_tiers: ["component test"]
---

# FES-01: Register all PRD Part 21 screens as code-split routes

## Summary (business)
This sets up the app so each of the ten planned screens (including the separate Super Admin area) loads on its own, only when a user actually visits it, instead of forcing everyone to download the entire application upfront. The payoff is a faster, snappier first load for both consultants and travelers, which matters most on slower connections or mobile devices.

## User Story
**As a** frontend engineer, **I want** have every screen registered in `App.tsx` as its own route, code-split with `React.lazy`, **so that** the app doesn't ship one monolithic bundle as the ten distinct Part 21 screens land, several of which (Super Admin Console) serve a completely different persona from the Consultant/User screens, per frontend-best-practices §2.

## Acceptance Criteria
- Given a route for a screen not yet visited is navigated to, when the bundle loads, then only that screen's chunk is fetched, wrapped in `Suspense` with a loading fallback.

## Developer Notes
- **PRD reference(s):** §21 Screen-by-Screen UI Specification (all subsections)
- **Module(s)/Screen(s):** Frontend shell (all screens)
- **Story points:** 5 — Routing skeleton is mechanical; the discipline is code-splitting every route from the start rather than retrofitting later.
- **Dependencies:** None
- **Testing tier(s):** component test

## Sub-tasks
- [NEW] Frontend: route registration for all 10 PRD Part 21 screens in `App.tsx`
- [NEW] Frontend: `React.lazy` + `Suspense` per route
- [NEW] Frontend: component test — lazy route resolves and renders
