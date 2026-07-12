---
id: BOK-01
epic: Booking Core
phase: mock
status: not-started
story_points: 3
dependencies: []
labels: ["backend", "booking", "phase1"]
prd_references: ["§4.3"]
modules_or_screens: ["booking"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# BOK-01: Add @Transactional boundaries to booking state-change methods

## Summary (business)
When a booking is saved or confirmed, all the related record-keeping must happen together as one all-or-nothing step. This prevents situations where a booking looks confirmed in our system but downstream processes (like sending confirmations or updating records) never got triggered, or vice versa — protecting customers and staff from confusing, inconsistent booking states.

## User Story
**As a** backend engineer, **I want** have `BookingServiceImpl.saveAsQuotation`/`confirmBooking` wrap their entity mutation and event publication in a single transaction, **so that** the outbox pattern's atomicity guarantee actually holds — today a state change can persist with no corresponding event, or an event can publish for a change that then rolls back, per RULES.md §4.3.

## Acceptance Criteria
- Given `saveAsQuotation` mutates the Itinerary and publishes `ItineraryQuotationSavedEvent`, when the underlying repository save throws, then the event is not published (single transactional scope).
- Given `confirmBooking` succeeds, when the transaction commits, then `BookingConfirmedEvent` is guaranteed to have been queued in the same commit, per Spring Modulith's JPA event publication registry.

## Developer Notes
- **PRD reference(s):** §4.3 Transaction boundaries (RULES.md, top reconciliation item)
- **Module(s)/Screen(s):** booking
- **Story points:** 3 — Scoped fix to two existing methods; risk is in getting the transaction boundary exactly right, not breadth.
- **Dependencies:** None
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: `@Transactional` on `saveAsQuotation` and `confirmBooking`
- [NEW] Backend: unit test — repository failure prevents event publication
- [NEW] Backend: integrationTest proving outbox atomicity (per testing-strategy skill's explicit guidance for this exact case)
