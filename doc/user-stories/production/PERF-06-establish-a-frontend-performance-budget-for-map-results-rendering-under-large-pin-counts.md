---
id: PERF-06
epic: Performance/Load Testing
phase: production
status: not-started
story_points: 5
dependencies: ["FES-05"]
labels: ["frontend", "performance", "phase2"]
prd_references: ["§2"]
modules_or_screens: ["Search Dashboard (21.1)", "Itinerary Builder (21.2)"]
testing_tiers: ["component test"]
---

# PERF-06: Establish a frontend performance budget for map/results rendering under large pin counts

## Summary (business)
This story sets a clear speed target for how smoothly the map and results screen perform when a search returns a large number of location pins, and confirms the screen doesn't slow down or lag when travellers interact with unrelated things like a date picker. It keeps the browsing experience fast and responsive even for large, detailed searches.

## User Story
**As a** frontend engineer, **I want** have a defined performance budget for the map+results split-panel screens as pin/result counts grow, **so that** frontend-best-practices §2's map-rendering guidance (memoized markers once real map integration lands) is verified against a measured budget, not just implemented and assumed sufficient.

## Acceptance Criteria
- Given a search result set with a large number of location pins is rendered, when render performance is measured, then it stays within the defined frame-budget threshold, with `React.memo`'d pin/marker components confirmed not to re-render on unrelated state changes (e.g. a date-picker interaction).

## Developer Notes
- **PRD reference(s):** frontend-best-practices skill §2 (Map rendering)
- **Module(s)/Screen(s):** Search Dashboard (21.1), Itinerary Builder (21.2)
- **Story points:** 5 — Performance-budget definition + measurement harness against FES-05's MapPanel component, informed by frontend-best-practices' explicit prediction of this exact jank risk.
- **Dependencies:** FES-05
- **Testing tier(s):** component test

## Sub-tasks
- [NEW] Frontend: performance budget definition for MapPanel/ResultsPanel rendering
- [NEW] Frontend: render-count/timing measurement harness
- [NEW] Frontend: component test — unrelated state change does not re-render unchanged pins
