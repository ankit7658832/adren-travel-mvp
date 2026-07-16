---
id: BOK-05
epic: Booking Core
phase: mock
status: not-started
story_points: 5
dependencies: ["BOK-03", "BOK-23"]
labels: ["backend", "booking", "phase1"]
prd_references: ["§20.4"]
modules_or_screens: ["booking", "supplier"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# BOK-05: Add Transfer line items to an itinerary (Transferz)

## Summary (business)
Consultants can add a ground transfer (e.g., airport pickup) to a trip itinerary, including the vehicle type and exact pickup/drop-off locations. This lets the business package transport bookings from its transfer partner into a traveler's itinerary.

## User Story
**As a** Consultant/User, **I want** add a transfer line item with vehicle type and pickup/dropoff points, **so that** transfer products from Transferz can be represented on an itinerary per PRD §20.4.

## Acceptance Criteria
- Given a transfer option is added, when the line item is created, then it stores `vehicleType` and geocoded `pickupPoint`/`dropoffPoint` linked to the itinerary's location entries.

## Developer Notes
- **PRD reference(s):** §20.4 Line Item — Transfer (Transferz)
- **Module(s)/Screen(s):** booking, supplier
- **Story points:** 5 — Same shape as BOK-03, one product-type variant.
- **Dependencies:** BOK-03, BOK-23 (needs `TransferzClient` to source real, if stubbed, results from)
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `TransferLineItem` entity + `TransferLineItemRepository` (package-private, own Flyway migration)
- [EXTEND] Backend: `addTransferLineItem` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [EXTEND] Backend: REST endpoint `POST /api/v1/itineraries/{id}/line-items/transfer` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
