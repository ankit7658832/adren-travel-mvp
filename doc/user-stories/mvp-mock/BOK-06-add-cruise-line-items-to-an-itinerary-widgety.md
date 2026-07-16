---
id: BOK-06
epic: Booking Core
phase: mock
status: not-started
story_points: 5
dependencies: ["BOK-03", "BOK-24"]
labels: ["backend", "booking", "phase1"]
prd_references: ["§20.5", "§10.2.6"]
modules_or_screens: ["booking", "supplier"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# BOK-06: Add Cruise line items to an itinerary (Widgety)

## Summary (business)
Consultants can add a cruise to a trip itinerary, including the sailing, cabin category, and ports of call, and the system automatically flags when passengers will need to provide travel documents (like a passport) for that cruise. This ensures cruise bookings capture all necessary details and that document requirements aren't missed later in the booking process.

## User Story
**As a** Consultant/User, **I want** add a cruise line item with sailing, cabin category, and multi-port metadata, **so that** cruise products from Widgety can be represented on an itinerary per PRD §20.5, including the passenger-documentation flag.

## Acceptance Criteria
- Given a cruise option is added, when the line item is created, then it stores `sailingId`, `cruiseLine`, `cabinCategory`, and `ports[]` as itinerary metadata rather than separate line items.
- Given the selected sailing requires passenger documentation, when the line item is created, then `passengerDocumentsRequired=true` is set, driving the Traveler Profile passport requirement (BOK-14).

## Developer Notes
- **PRD reference(s):** §20.5 Line Item — Cruise (Widgety); §10.2.6 Widgety
- **Module(s)/Screen(s):** booking, supplier
- **Story points:** 5 — Same shape as BOK-03 plus the port-flattening and documentation-flag rules called out in §10.2.6.
- **Dependencies:** BOK-03, BOK-24 (needs `WidgetyClient` to source real, if stubbed, results from)
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `CruiseLineItem` entity + `CruiseLineItemRepository` (package-private, own Flyway migration)
- [EXTEND] Backend: `addCruiseLineItem` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [EXTEND] Backend: REST endpoint `POST /api/v1/itineraries/{id}/line-items/cruise` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
