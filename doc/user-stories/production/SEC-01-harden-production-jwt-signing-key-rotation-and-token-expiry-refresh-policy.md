---
id: SEC-01
epic: Security Hardening
phase: production
status: not-started
story_points: 8
dependencies: ["FND-01"]
labels: ["backend", "security", "phase2"]
prd_references: ["§5.1"]
modules_or_screens: ["shared (security)"]
testing_tiers: ["unit", "integration (Testcontainers)"]
---

# SEC-01: Harden production JWT signing-key rotation and token expiry/refresh policy

## Summary (business)
This story puts an expiration and renewal system on the digital "keys" that prove a user is logged in, and makes sure those keys are periodically swapped out for new ones. It protects the business from a stolen or leaked login credential being usable forever, while making sure customers aren't randomly logged out during the swap.

## User Story
**As a** platform security owner, **I want** have JWT signing keys rotate on a defined schedule with a real access/refresh-token expiry policy, **so that** FND-01's MVP authentication foundation is production-hardened before GA.

## Acceptance Criteria
- Given the JWT signing key rotation schedule triggers, when rotation completes, then tokens signed under the previous key remain valid until their own expiry (grace period), and no service disruption occurs.
- Given an access token expires, when the client presents a refresh token, then a new access token is issued per the defined refresh policy, without requiring re-authentication.

## Developer Notes
- **PRD reference(s):** §5.1 (RULES.md)
- **Module(s)/Screen(s):** shared (security)
- **Story points:** 8 — Production-grade key management + refresh-token flow — materially more than FND-01's MVP stateless JWT foundation.
- **Dependencies:** FND-01
- **Testing tier(s):** unit, integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: signing-key rotation with grace-period validation
- [NEW] Backend: refresh-token issuance/validation flow
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — rotation-during-active-session scenario
