---
id: BOK-20
epic: Booking Core
phase: mock
status: not-started
story_points: 8
dependencies: ["FND-14"]
labels: ["backend", "booking", "supplier", "phase1"]
prd_references: ["§9.4", "§22.2"]
modules_or_screens: ["supplier", "booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# BOK-20: Deduplicate the same physical hotel property offered by two suppliers

## Summary (business)
When two different hotel suppliers offer the exact same physical hotel in search results, the system recognizes it's the same property and shows it only once instead of as duplicate listings. This keeps search results clean and ensures the system's logic for picking the best default option works properly instead of getting confused by duplicate entries.

## User Story
**As a** Consultant/User, **I want** see one entry for a hotel even when two suppliers (e.g. Hotelbeds and STUBA) return the same physical property, **so that** search results aren't cluttered with duplicate listings and the Default Selection Algorithm can choose cleanly, per PRD §9.4.

## Acceptance Criteria
- Given two suppliers offer the same physical hotel for a search, when results are aggregated, then the entries are deduplicated via property-matching before the Default Selection Algorithm runs.

## Developer Notes
- **PRD reference(s):** §9.4 Business Rules & Edge Cases; §22.2 (interacts with Default Selection)
- **Module(s)/Screen(s):** supplier, booking
- **Story points:** 8 — Property-matching/dedup across heterogeneous supplier content (names, addresses, no shared ID) is inherently fuzzy-matching work — the highest-uncertainty story in this epic.
- **Dependencies:** FND-14
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: property-matching service (name/geo/address heuristic match across `HotelbedsClient`/`StubaClient`/`TboClient` results)
- [EXTEND] Backend: wired ahead of `DefaultSelectionService` in the aggregation pipeline
- [NEW] Backend: unit test — known duplicate pairs matched, known distinct properties not merged
- [NEW] Backend: module test — end-to-end aggregation with dedup
