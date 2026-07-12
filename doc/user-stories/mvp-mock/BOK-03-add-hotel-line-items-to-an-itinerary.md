---
id: BOK-03
epic: Booking Core
phase: mock
status: not-started
story_points: 5
dependencies: ["BOK-01", "FIN-04", "FIN-05"]
labels: ["backend", "booking", "phase1"]
prd_references: ["§20.2", "§9.3"]
modules_or_screens: ["booking", "supplier"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# BOK-03: Add Hotel line items to an itinerary

## Summary (business)
Consultants can add a hotel stay to a trip itinerary, capturing the property, room type, meal plan, cancellation deadline, and full pricing details. This lets the business sell hotel rooms sourced from any of its several hotel partners within one consistent itinerary experience.

## User Story
**As a** Consultant/User, **I want** add a hotel line item to an itinerary with supplier, rate, and pricing fields, **so that** hotel products from Hotelbeds/STUBA/TBO/Local DMC/BYOS can be represented on an itinerary per PRD §20.2.

## Acceptance Criteria
- Given a hotel option is added from search results, when the line item is created, then it stores `supplierRateId`, `propertyName`, `roomType`, `mealPlan`, `cancellationDeadline`, and the net/markup/buffer/sell/currency/fxSnapshot pricing fields.
- Given a hotel line item is added, when its cancellation policy is inspected, then `cancellationDeadline` is the earliest `from` date per PRD §10.2.1.

## Developer Notes
- **PRD reference(s):** §20.2 Line Item — Hotel; §9.3 Data Model
- **Module(s)/Screen(s):** booking, supplier
- **Story points:** 5 — New entity within the existing Itinerary aggregate; pricing fields reuse the Money type.
- **Dependencies:** BOK-01, FIN-04, FIN-05
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `HotelLineItem` entity + `HotelLineItemRepository` (package-private, own Flyway migration)
- [NEW] Backend: `addHotelLineItem` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `POST /api/v1/itineraries/{id}/line-items/hotel` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
