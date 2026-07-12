---
id: PINF-02
epic: Production Infrastructure
phase: production
status: not-started
story_points: 5
dependencies: ["OPS-03"]
labels: ["devops", "security", "phase2"]
prd_references: ["§20.11", "§20.10"]
modules_or_screens: ["Infra (production AWS)"]
testing_tiers: ["integration (Testcontainers)"]
---

# PINF-02: Replace LocalStack S3 with production S3 and encryption-at-rest policy

## Summary (business)
This story moves storage of travel vouchers and sensitive traveler documents (like ID or payment records) from a test environment into a real, production-grade storage service with encryption built in. It ensures customer personal and financial documents are properly protected once the platform is live, not just during development.

## User Story
**As a** platform security owner, **I want** have vouchers and the document vault stored in real S3 with an explicit encryption-at-rest policy, **so that** OPS-03's LocalStack buckets are replaced with production-grade storage for traveler PII and financial documents.

## Acceptance Criteria
- Given a voucher or traveler document is written in production, when storage is inspected, then it resides in a real S3 bucket with server-side encryption enabled and a documented key-management policy.

## Developer Notes
- **PRD reference(s):** §20.11 Voucher; §20.10 Traveler Profile (document_vault)
- **Module(s)/Screen(s):** Infra (production AWS)
- **Story points:** 5 — Cutover from OPS-03's LocalStack buckets to real S3 with an explicit encryption policy.
- **Dependencies:** OPS-03
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: production S3 buckets (`vouchers`, `traveler-documents`) with encryption-at-rest policy
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
