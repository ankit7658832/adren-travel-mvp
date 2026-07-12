---
id: CMP-12
epic: Compliance Execution
phase: production
status: not-started
story_points: 5
dependencies: ["FND-04", "CMP-06"]
labels: ["backend", "frontend", "compliance", "phase2"]
prd_references: ["§24.7"]
modules_or_screens: ["compliance"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# CMP-12: Harden the data-driven KYC rule engine for production rule changes without a release cycle

## Summary (business)
Compliance rules (like which documents a new state or country requires from consultants) change over time. Today, updating those rules requires a full software release. This story lets authorized compliance staff update the rules directly through an admin tool, so new legal requirements can be applied immediately without waiting on engineering to ship a new version of the platform.

## User Story
**As a** compliance owner, **I want** update a market's KYC rules (e.g. a new US state's Seller of Travel requirement) without waiting for a full platform release, **so that** PRD §24.7's NFR is fully realized at production maturity — FND-04's data-driven rule table is administrable, not just data-driven in schema.

## Acceptance Criteria
- Given a market rule changes (e.g. a new required KYC field), when an authorized compliance owner updates the rule table via an admin interface, then the change takes effect for new onboarding sessions without a backend deploy.

## Developer Notes
- **PRD reference(s):** §24.7 NFR Regional Compliance
- **Module(s)/Screen(s):** compliance
- **Story points:** 5 — Adds an administrable interface on top of FND-04's already-data-driven rule table — the MVP made it data-driven in the database; this makes it editable without a deploy.
- **Dependencies:** FND-04, CMP-06
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [EXTEND] Backend: `updateKycRuleTable` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `PUT /api/v1/compliance/kyc-rules/{market}`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [NEW] Frontend: `useKycRuleAdmin` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `KycRuleAdmin.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
