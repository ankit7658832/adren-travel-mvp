---
id: CMP-03
epic: Compliance Execution
phase: production
status: not-started
story_points: 8
dependencies: ["BOK-15", "BOK-11"]
labels: ["backend", "compliance", "phase2"]
prd_references: ["§17.1", "§20.11"]
modules_or_screens: ["compliance"]
testing_tiers: ["integration (Testcontainers)"]
---

# CMP-03: Integrate UK ATOL license registration and certificate issuance

## Summary (business)
In the UK, companies that sell flight-and-hotel packages must be licensed under ATOL (a government scheme that protects travelers' money if a travel company fails) and must issue real ATOL certificates to customers. Today our system only stores a placeholder reference instead of a genuine certificate. This story connects our platform to Adren's actual ATOL license so customers booking UK package holidays receive real, legally valid proof of protection.

## User Story
**As a** Consultant, **I want** have real ATOL certificates issued against Adren's own ATOL license registration, not just a stored reference field, **so that** PRD §17.1's UK ATOL requirement moves from BOK-15/BOK-11's MVP internal-reference implementation to a real license-backed integration.

## Acceptance Criteria
- Given Adren's ATOL license registration is confirmed, when a UK dynamic flight+hotel package is booked, then a real ATOL certificate is issued against that registration and referenced on the Voucher, not a placeholder value.

## Developer Notes
- **PRD reference(s):** §17.1 Market-by-Market Requirements (UK); §20.11 Voucher (atol_certificate_reference)
- **Module(s)/Screen(s):** compliance
- **Story points:** 8 — Requires Adren's own real ATOL license registration as a precondition — an operational/legal dependency, not just code.
- **Dependencies:** BOK-15, BOK-11
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: real ATOL certificate issuance against Adren's confirmed license registration
- [NEW] Backend: integrationTest
