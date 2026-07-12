---
id: BOK-09
epic: Booking Core
phase: mock
status: not-started
story_points: 5
dependencies: ["BOK-08"]
labels: ["backend", "booking", "phase1"]
prd_references: ["§20.9"]
modules_or_screens: ["booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# BOK-09: Create Quotation entity with FX/rate validity window

## Summary (business)
Every Quotation records an expiry date reflecting how long its quoted price and exchange rate remain valid. This protects the business from honoring outdated prices after supplier rates or currency exchange rates have moved, and gives customers clarity on how long an offer stands.

## User Story
**As a** Consultant, **I want** have my Quotation carry a `valid_until` timestamp so I know when its rate/FX lock expires, **so that** the rate and FX snapshot the Quotation was built on has an explicit expiry per PRD §20.9.

## Acceptance Criteria
- Given a Quotation is created, when it is inspected, then it stores `quotationId`, `itineraryId`, `validUntil`, `sharedWithTraveler`, and a nullable `convertedToBookingId`.

## Developer Notes
- **PRD reference(s):** §20.9 Quotation
- **Module(s)/Screen(s):** booking
- **Story points:** 5 — New entity distinct from Itinerary, first-class Quotation lifecycle tracking.
- **Dependencies:** BOK-08
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `Quotation` entity + `QuotationRepository` (package-private, own Flyway migration)
- [EXTEND] Backend: `createQuotation` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [EXTEND] Backend: REST endpoint `(internal — created as part of BOK-08's transition)` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
