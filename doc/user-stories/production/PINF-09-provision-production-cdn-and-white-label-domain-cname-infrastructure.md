---
id: PINF-09
epic: Production Infrastructure
phase: production
status: not-started
story_points: 5
dependencies: ["FND-07"]
labels: ["devops", "whitelabel", "phase2"]
prd_references: ["§13.2", "§24.5"]
modules_or_screens: ["Infra (production)"]
testing_tiers: ["integration (Testcontainers)"]
---

# PINF-09: Provision production CDN and white-label domain CNAME infrastructure

## Summary (business)
This story sets up the production infrastructure so that travel consultants' custom branded web addresses (white-label domains, meaning the consultant's own branding instead of Adren's) work reliably and quickly at full scale, not just in testing. This ensures consultants' branded storefronts load correctly and fast for real customers.

## User Story
**As a** Consultant, **I want** have my CNAME domain (FND-06) resolve through a production CDN with the propagation speed FND-07's NFR requires, **so that** PRD §13.2/§24.5's white-label domain requirements are met at production scale, not just FND-07's local propagation mechanism.

## Acceptance Criteria
- Given a Consultant's CNAME domain is mapped in production, when it is resolved, then it routes through the production CDN to the correct Consultant-branded storefront within the NFR's defined short window.

## Developer Notes
- **PRD reference(s):** §13.2 Branding Configuration; §24.5 NFR White-Label & Admin
- **Module(s)/Screen(s):** Infra (production)
- **Story points:** 5 — Production CDN + DNS provisioning on top of FND-06/FND-07's already-built domain-mapping and propagation mechanism.
- **Dependencies:** FND-07
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: production CDN provisioning + CNAME domain routing
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
