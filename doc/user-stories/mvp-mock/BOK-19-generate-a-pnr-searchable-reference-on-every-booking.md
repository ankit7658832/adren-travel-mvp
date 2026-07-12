---
id: BOK-19
epic: Booking Core
phase: mock
status: not-started
story_points: 2
dependencies: ["BOK-13"]
labels: ["backend", "booking", "phase1"]
prd_references: ["§20.8"]
modules_or_screens: ["booking"]
testing_tiers: ["unit"]
---

# BOK-19: Generate a PNR-searchable reference on every Booking

## Summary (business)
Every booking, no matter what type of travel product it contains, gets its own unique Adren reference number that customer service can search by. This means support staff can always look up a booking even when it doesn't involve an airline ticket or when supplier reference numbers aren't readily available.

## User Story
**As a** User, **I want** have every booking carry Adren's own searchable reference regardless of product type, **so that** PNR search (HRD-07) can look up a booking without depending on airline PNRs or supplier booking IDs, per PRD §20.8.

## Acceptance Criteria
- Given a booking is confirmed, when the Booking entity is inspected, then it carries a `pnrSearchableRef` distinct from any airline PNR or supplier booking reference.

## Developer Notes
- **PRD reference(s):** §20.8 Booking (pnr_searchable_ref)
- **Module(s)/Screen(s):** booking
- **Story points:** 2 — Small addition to the Booking entity's confirmation path.
- **Dependencies:** BOK-13
- **Testing tier(s):** unit

## Sub-tasks
- [EXTEND] Backend: `Booking.pnrSearchableRef` generation on confirm
- [NEW] Backend: unit test — uniqueness and format
