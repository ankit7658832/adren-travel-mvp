---
id: FES-05
epic: Frontend Shell
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-13"]
labels: ["frontend", "foundation", "phase1"]
prd_references: ["§21.1", "§21.2"]
modules_or_screens: ["shared (frontend)"]
testing_tiers: ["component test"]
---

# FES-05: Build shared MapPanel/ResultsPanel layout primitives

## Summary (business)
This builds a reusable "map on one side, results list on the other" layout that both the search screen and the trip-building screen will share, instead of each screen having its own separate version. This keeps the look and feel consistent for users and reduces the effort and risk of bugs when building or updating either screen.

## User Story
**As a** frontend engineer, **I want** have a reusable split-panel layout pair for the map+results shape shared by Search Dashboard and Itinerary Builder, **so that** PRD §21.1/§21.2's shared split-panel structure isn't duplicated per screen, per frontend-best-practices §5.

## Acceptance Criteria
- Given `MapPanel`/`ResultsPanel` are composed on the Search Dashboard, when they render, then pins/results follow the documented split-panel layout (map left/top on desktop/mobile per §21.1).
- Given the same primitives are composed on the Itinerary Builder, when they render, then the layout matches without re-implementing the split-panel CSS/structure.

## Developer Notes
- **PRD reference(s):** §21.1 Search Dashboard; §21.2 Itinerary Builder
- **Module(s)/Screen(s):** shared (frontend)
- **Story points:** 5 — Two components extracted from FND-13's real implementation, generalized for reuse by FND-16.
- **Dependencies:** FND-13
- **Testing tier(s):** component test

## Sub-tasks
- [NEW] Frontend: `MapPanel` shared component (extracted/generalized from FND-13)
- [NEW] Frontend: `ResultsPanel` shared component
- [NEW] Frontend: component test per primitive
