---
id: FND-15
epic: Foundation
phase: mock
status: not-started
story_points: 2
dependencies: ["FND-14", "FND-16"]
labels: ["frontend", "foundation", "phase1"]
prd_references: ["§9.2", "§22.2"]
modules_or_screens: ["Itinerary Builder (21.2)"]
testing_tiers: ["component test"]
---

# FND-15: Surface 'Auto-selected: Best available match' label on defaulted line items

## Summary (business)
This story adds a clear on-screen label whenever the system has automatically chosen a product on the consultant's behalf, so it's never confused with something the consultant deliberately picked. This transparency helps consultants catch and adjust auto-picked items before finalizing a trip for a customer.

## User Story
**As a** Consultant/User, **I want** see a visible label whenever the system has auto-selected a product for me, **so that** auto-selection is never mistaken for a deliberate manual choice, per PRD §9.2's explicit surfacing requirement.

## Acceptance Criteria
- Given the UI displays an auto-selected item, when the Consultant views it, then a visible 'Auto-selected: Best available match' label is present.

## Developer Notes
- **PRD reference(s):** §9.2 Default Selection Algorithm; §22.2 T2/T3
- **Module(s)/Screen(s):** Itinerary Builder (21.2)
- **Story points:** 2 — Small, purely presentational addition once FND-14's flag exists on the line item.
- **Dependencies:** FND-14, FND-16
- **Testing tier(s):** component test

## Sub-tasks
- [NEW] Frontend: auto-selected badge on the per-location line-item card
- [NEW] Frontend: component test asserting the label renders when `autoSelected=true`
