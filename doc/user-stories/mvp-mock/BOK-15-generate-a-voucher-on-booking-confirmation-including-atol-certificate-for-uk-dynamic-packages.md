---
id: BOK-15
epic: Booking Core
phase: mock
status: not-started
story_points: 8
dependencies: ["BOK-13", "BOK-11"]
labels: ["backend", "booking", "phase1"]
prd_references: ["§20.11", "§21.4", "§22.9"]
modules_or_screens: ["booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "integration (Testcontainers)"]
---

# BOK-15: Generate a Voucher on booking confirmation, including ATOL certificate for UK dynamic packages

## Summary (business)
As soon as a booking is confirmed, the customer automatically receives a voucher confirming their trip, and for qualifying UK package bookings, an ATOL certificate (proof of financial protection under UK travel law) is attached as well. This gives customers immediate, tangible proof of their booking and, where legally required, proof that their money is protected if the travel company were to fail.

## User Story
**As a** User, **I want** receive a voucher immediately when a booking is confirmed, with an ATOL certificate attached if applicable, **so that** PRD §21.4's confirmation state (voucher download link, ATOL certificate download link) and §20.11 are satisfied.

## Acceptance Criteria
- Given a booking is confirmed, when the Voucher is generated, then it references the booking, has a `pdfReference`, and — for a UK dynamic flight+hotel package — an `atolCertificateReference` is populated and attached (T5).
- Given a booking is confirmed that is not a UK dynamic package, when the Voucher is generated, then `atolCertificateReference` remains null.

## Developer Notes
- **PRD reference(s):** §20.11 Voucher; §21.4 Confirmation state; §22.9 T5
- **Module(s)/Screen(s):** booking
- **Story points:** 8 — PDF generation + conditional ATOL attachment is the most complex single piece of the booking-confirmation path.
- **Dependencies:** BOK-13, BOK-11
- **Testing tier(s):** unit, module (@ApplicationModuleTest), integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: `Voucher` entity + PDF generation service (stored to LocalStack S3 in MVP)
- [EXTEND] Backend: `confirmBooking` triggers voucher generation in the same transactional scope as BOK-01
- [NEW] Backend: ATOL certificate attachment conditional on `is_dynamic_flight_hotel_combo` + UK market
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — PDF stored and referenced correctly
