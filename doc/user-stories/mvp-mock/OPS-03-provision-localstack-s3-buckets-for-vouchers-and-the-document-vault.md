---
id: OPS-03
epic: DevOps/Infra
phase: mock
status: not-started
story_points: 3
dependencies: ["OPS-01"]
labels: ["devops", "foundation", "phase1"]
prd_references: ["§20.11", "§20.10"]
modules_or_screens: ["Infra (LocalStack)"]
testing_tiers: ["integration (Testcontainers)"]
---

# OPS-03: Provision LocalStack S3 buckets for vouchers and the document vault

## Summary (business)
This sets up dedicated, secure storage locations in the test environment for two things: travel booking vouchers (the confirmation documents customers receive) and travelers' uploaded documents (like passports). Having this in place lets the team fully test that vouchers and traveler documents are created, stored, and protected correctly before customers rely on them.

## User Story
**As a** backend engineer, **I want** have S3 buckets available locally for voucher PDFs and Traveler Profile document-vault files, **so that** BOK-15's voucher generation and BOK-14's document vault have a storage target before those stories can be verified end-to-end.

## Acceptance Criteria
- Given the provisioning script is run, when it completes, then a `vouchers` bucket and a `traveler-documents` bucket exist with the encryption-at-rest configuration BOK-15/BOK-14 expect.

## Developer Notes
- **PRD reference(s):** §20.11 Voucher (pdf_reference); §20.10 Traveler Profile (document_vault[])
- **Module(s)/Screen(s):** Infra (LocalStack)
- **Story points:** 3 — Small, scripted bucket provisioning.
- **Dependencies:** OPS-01
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: LocalStack S3 bucket provisioning script (`vouchers`, `traveler-documents`)
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
