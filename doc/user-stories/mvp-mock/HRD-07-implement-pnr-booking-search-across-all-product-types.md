---
id: HRD-07
epic: Hardening
phase: mock
status: not-started
story_points: 5
dependencies: ["BOK-19", "FND-23"]
labels: ["backend", "booking", "phase1"]
prd_references: ["§16", "§22.8"]
modules_or_screens: ["booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# HRD-07: Implement PNR/Booking Search across all product types

## Summary (business)
Staff will be able to look up any booking using its reference number and find it immediately, no matter whether it's a flight, hotel, transfer, cruise, or activity. This saves time and avoids having to search different systems depending on what type of booking it is.

## User Story
**As a** User, **I want** search by PNR/internal booking reference and get a result regardless of underlying product type, **so that** PRD §16 and §22.8's T12 requirement are met.

## Acceptance Criteria
- Given a booking reference is entered in PNR search, when the search runs, then it returns results regardless of whether the underlying product is a flight, hotel, transfer, cruise, or activity (T12).

## Developer Notes
- **PRD reference(s):** §16 PNR Search; §22.8 T12
- **Module(s)/Screen(s):** booking
- **Story points:** 5 — Single search endpoint over BOK-19's `pnrSearchableRef`, paginated per FND-23's convention.
- **Dependencies:** BOK-19, FND-23
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `GET /api/v1/bookings/search?ref=` endpoint (paginated) across all product-type line items
- [NEW] Backend: unit test — one case per product type
- [NEW] Backend: module test
