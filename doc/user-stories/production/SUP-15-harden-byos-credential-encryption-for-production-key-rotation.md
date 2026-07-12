---
id: SUP-15
epic: Supplier Live Integrations
phase: production
status: not-started
story_points: 5
dependencies: ["FND-12"]
labels: ["backend", "supplier", "security", "phase2"]
prd_references: ["§5.3"]
modules_or_screens: ["supplier"]
testing_tiers: ["integration (Testcontainers)"]
---

# SUP-15: Harden BYOS credential encryption for production key rotation

## Summary (business)
This strengthens the security process for encrypted supplier login credentials (used when partners bring their own supplier accounts) by putting a real, scheduled key-rotation policy in place for production, rather than the relaxed setup used during development. This reduces long-term security risk while ensuring credential access continues without any service interruption.

## User Story
**As a** platform security owner, **I want** have the KMS CMK wrapping BYOS credential data keys rotate on a defined production schedule, **so that** FND-12's row-level encryption pattern has a real production key-rotation policy, not just the MVP's dev-scoped KMS setup.

## Acceptance Criteria
- Given the production KMS CMK rotation schedule triggers, when rotation completes, then existing BYOS ciphertext remains decryptable via KMS's key-versioning, with no Consultant-visible downtime.

## Developer Notes
- **PRD reference(s):** §5.3 Secrets handling (RULES.md)
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Production key-management policy work on top of FND-12's already-built envelope-encryption mechanism.
- **Dependencies:** FND-12
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: production KMS CMK rotation policy configured
- [NEW] Backend: integrationTest — decrypt succeeds across a simulated key-version rotation
