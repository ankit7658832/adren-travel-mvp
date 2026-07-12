---
id: BOK-07
epic: Booking Core
phase: mock
status: not-started
story_points: 5
dependencies: ["BOK-03"]
labels: ["backend", "booking", "phase1"]
prd_references: ["§20.6", "§10.2.7"]
modules_or_screens: ["booking", "supplier"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# BOK-07: Add Activity line items to an itinerary (HBActivities)

## Summary (business)
Consultants can add a bookable activity (e.g., a tour or excursion) to an itinerary with a specific time slot and a fixed number of participants, and the system blocks any attempt to change the headcount after the booking is confirmed, warning staff before payment is taken. This reflects that many activity suppliers cannot accommodate headcount changes once booked, avoiding costly last-minute surprises.

## User Story
**As a** Consultant/User, **I want** add an activity line item with a specific time slot and fixed headcount, **so that** activity products from HBActivities can be represented on an itinerary per PRD §20.6.

## Acceptance Criteria
- Given an activity option is added, when the line item is created, then it stores `activityId`, `durationMinutes`, `timeSlot` (a specific time, not a date range), and `headcount`.
- Given the Consultant/User attempts to change headcount post-confirmation, when the edit is attempted, then it is blocked per the supplier's fixed-at-booking constraint (§10.2.7), surfaced before payment.

## Developer Notes
- **PRD reference(s):** §20.6 Line Item — Activity (HBActivities); §10.2.7 HBActivities
- **Module(s)/Screen(s):** booking, supplier
- **Story points:** 5 — Same shape as BOK-03 plus the time-slot/headcount constraint.
- **Dependencies:** BOK-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `ActivityLineItem` entity + `ActivityLineItemRepository` (package-private, own Flyway migration)
- [EXTEND] Backend: `addActivityLineItem` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [EXTEND] Backend: REST endpoint `POST /api/v1/itineraries/{id}/line-items/activity` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
