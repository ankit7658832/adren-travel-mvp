---
id: SUP-12
epic: Supplier Live Integrations
phase: production
status: not-started
story_points: 8
dependencies: ["BOK-20", "SUP-02", "SUP-04", "SUP-06"]
labels: ["backend", "supplier", "booking", "phase2"]
prd_references: ["§9.4"]
modules_or_screens: ["supplier", "booking"]
testing_tiers: ["integration (Testcontainers)"]
---

# SUP-12: Tune property-matching/deduplication against real supplier data volumes

## Summary (business)
This fine-tunes the logic that detects when two suppliers (like Hotelbeds and STUBA) are actually offering the same physical hotel, using real supplier data instead of test samples. Accurate matching prevents customers from seeing duplicate listings for the same hotel or missing the best price comparison across suppliers, which matters once real inventory is live.

## User Story
**As a** Consultant/User, **I want** see accurate deduplication once real Hotelbeds/STUBA/TBO data (not synthetic MVP fixtures) is flowing through search, **so that** PRD §9.4's deduplication requirement holds up under real-world naming/address inconsistencies across suppliers, which BOK-20's MVP heuristic was only validated against synthetic data.

## Acceptance Criteria
- Given real Hotelbeds and STUBA production data for the same physical property is compared, when the matcher runs, then the false-positive and false-negative rate is measured and tuned against a labeled sample of real properties, not just the MVP's synthetic fixture set.

## Developer Notes
- **PRD reference(s):** §9.4 Business Rules & Edge Cases
- **Module(s)/Screen(s):** supplier, booking
- **Story points:** 8 — Fuzzy-matching tuning against real data is inherently iterative and highest-uncertainty — carries over BOK-20's risk profile at production scale.
- **Dependencies:** BOK-20, SUP-02, SUP-04, SUP-06
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: property-matching heuristic tuned against a labeled real-property sample set
- [NEW] Backend: false-positive/false-negative rate measurement harness
- [NEW] Backend: integrationTest — regression suite of known-tricky real property pairs
