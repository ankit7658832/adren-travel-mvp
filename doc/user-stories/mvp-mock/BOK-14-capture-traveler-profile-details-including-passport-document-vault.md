---
id: BOK-14
epic: Booking Core
phase: mock
status: not-started
story_points: 5
dependencies: []
labels: ["backend", "booking", "phase1"]
prd_references: ["§20.10", "§23.1"]
modules_or_screens: ["booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# BOK-14: Capture Traveler Profile details including passport/document vault

## Summary (business)
The system captures each traveler's name, date of birth, and — when the trip requires it (such as international or cruise travel) — their passport details and supporting documents, and keeps this information private to the Consultant who manages that traveler. This ensures bookings that legally require identity documents can't be confirmed without them, and keeps sensitive traveler data appropriately siloed between different Consultants.

## User Story
**As a** User, **I want** enter traveler name, date of birth, and (when required) passport details and documents, **so that** cruise and international bookings have the traveler data PRD §20.10 requires before confirmation.

## Acceptance Criteria
- Given a cruise line item with `passengerDocumentsRequired=true` is in the itinerary, when the User proceeds to booking, then passport number, expiry, and nationality are required before the booking can confirm (T22).
- Given a Traveler Profile is created, when it is inspected, then it is scoped to `consultant_id` — not shared across Consultants.

## Developer Notes
- **PRD reference(s):** §20.10 Traveler Profile; §23.1 (edge cases reference traveler data)
- **Module(s)/Screen(s):** booking
- **Story points:** 5 — New entity with encrypted document-vault references; the encryption mechanism itself is FND-12's KMS pattern reused, not rebuilt.
- **Dependencies:** None — this is a self-contained entity + endpoint (`TravelerProfile`, `POST /api/v1/travelers`), independently buildable. **BOK-13 depends on this story, not the other way around**: BOK-13's traveler-detail form embeds this story's capture flow (see BOK-13's own Acceptance Criteria — "when the User reaches the traveler form, then document fields are required inline"). An earlier revision of this file listed `BOK-13` as a dependency here, which — combined with BOK-13 listing this story as one of its own dependencies — created an unresolvable two-story cycle (flagged in `doc/phases.md` §4); corrected by removing this back-edge.
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: `TravelerProfile` entity + `TravelerProfileRepository` (package-private, own Flyway migration)
- [EXTEND] Backend: `createTravelerProfile` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [EXTEND] Backend: REST endpoint `POST /api/v1/travelers` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
