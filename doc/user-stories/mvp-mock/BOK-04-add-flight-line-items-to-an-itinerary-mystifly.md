---
id: BOK-04
epic: Booking Core
phase: mock
status: not-started
story_points: 5
dependencies: ["BOK-03"]
labels: ["backend", "booking", "phase1"]
prd_references: ["§20.3"]
modules_or_screens: ["booking", "supplier"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# BOK-04: Add Flight line items to an itinerary (Mystifly)

## Summary (business)
Consultants can add a flight to a trip itinerary, capturing airline, fare type, cabin class, baggage allowance, and (once booked) the airline's booking reference. This enables flights from the platform's flight supplier to be sold and tracked alongside other trip components.

## User Story
**As a** Consultant/User, **I want** add a flight line item with airline, fare basis, cabin class, and PNR fields, **so that** transport products from Mystifly can be represented on an itinerary per PRD §20.3.

## Acceptance Criteria
- Given a flight option is added from search results, when the line item is created, then it stores `airlineCode`, `flightNumber`, `fareBasisCode`, `cabinClass`, `baggageAllowance`, with `pnr` nullable until booked.

## Developer Notes
- **PRD reference(s):** §20.3 Line Item — Flight (Mystifly)
- **Module(s)/Screen(s):** booking, supplier
- **Story points:** 5 — Same shape as BOK-03, one product-type variant.
- **Dependencies:** BOK-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `FlightLineItem` entity + `FlightLineItemRepository` (package-private, own Flyway migration)
- [EXTEND] Backend: `addFlightLineItem` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [EXTEND] Backend: REST endpoint `POST /api/v1/itineraries/{id}/line-items/flight` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
