---
id: BOK-10
epic: Booking Core
phase: mock
status: not-started
story_points: 5
dependencies: ["BOK-09", "FIN-05"]
labels: ["backend", "booking", "phase1"]
prd_references: ["§9.1", "§22.3", "§20.7"]
modules_or_screens: ["booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# BOK-10: Convert a Quotation to a Package

## Summary (business)
A Consultant can turn a one-off Quotation they built for a specific customer into a reusable Package — giving it a name, valid dates, a price, and a maximum number of travelers — before deciding to make it publicly available. This lets successful custom itineraries be turned into ready-made products that can be sold repeatedly, saving rebuild effort.

## User Story
**As a** Consultant, **I want** convert a saved Quotation into a Package with a name, validity dates, pricing, and max pax, **so that** I can turn a one-off itinerary into a reusable, sellable product per PRD §9.1 Flow B.

## Acceptance Criteria
- Given a Consultant selects a saved Quotation and clicks 'Convert to Package', when they set name, validity dates, pricing, and max pax, then a Package is created referencing `sourceItineraryId` and is not yet published.

## Developer Notes
- **PRD reference(s):** §9.1 Flow B; §22.3 T4 (lifecycle); §20.7 Package
- **Module(s)/Screen(s):** booking
- **Story points:** 5 — New entity + conversion action; pricing fields (base auto-filled, markup editable) depend on FIN-05.
- **Dependencies:** BOK-09, FIN-05
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `Package` entity + `PackageRepository` (package-private, own Flyway migration)
- [NEW] Backend: `convertQuotationToPackage` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `POST /api/v1/quotations/{id}/package` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
