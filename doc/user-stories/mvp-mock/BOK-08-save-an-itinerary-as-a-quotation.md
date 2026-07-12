---
id: BOK-08
epic: Booking Core
phase: mock
status: not-started
story_points: 3
dependencies: ["BOK-01", "BOK-03"]
labels: ["backend", "booking", "phase1"]
prd_references: ["§9.1", "§22.3", "§20.9"]
modules_or_screens: ["booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# BOK-08: Save an itinerary as a Quotation

## Summary (business)
Once a trip itinerary is finalized, a Consultant can save it as a formal Quotation that becomes locked from casual edits. This creates a clear, stable price and itinerary to share with a customer, marking the point where a rough plan becomes a firm offer.

## User Story
**As a** Consultant/User, **I want** save a finalized itinerary as a read-only Quotation, **so that** the itinerary lifecycle progresses from Draft to Quotation per PRD §9.1 Flow A step 8.

## Acceptance Criteria
- Given an itinerary has at least one line item per required category, when 'Save as Quotation' is clicked, then the itinerary status transitions Draft → Quotation and becomes read-only except via explicit edit (T4).

## Developer Notes
- **PRD reference(s):** §9.1 Flow A step 8; §22.3 T4; §20.9 Quotation
- **Module(s)/Screen(s):** booking
- **Story points:** 3 — Extends the existing `ItineraryController`/`saveAsQuotation` reference endpoint with the full status-machine rule and Quotation entity linkage.
- **Dependencies:** BOK-01, BOK-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `Itinerary.markAsQuotation()` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/itineraries/{id}/quotation`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
