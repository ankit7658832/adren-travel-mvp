---
id: FND-04
epic: Foundation
phase: mock
status: not-started
story_points: 8
dependencies: ["FND-01", "FND-02"]
labels: ["backend", "frontend", "foundation", "whitelabel", "phase1"]
prd_references: ["§13.1", "§21.6", "§24.7"]
modules_or_screens: ["whitelabel", "Super Admin Console (21.6)"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test", "e2e"]
---

# FND-04: Super Admin onboards a Consultant via a market-driven KYC wizard

## Summary (business)
This story gives platform administrators a guided sign-up form for onboarding new travel consultants, where the required identity and compliance documents automatically change based on the consultant's home country. This ensures each consultant provides the right paperwork for their market (for example, tax or trade-license documents) without staff having to remember country-specific rules.

## User Story
**As a** Super Admin, **I want** onboard a new Consultant through a multi-step form whose required fields change based on the selected home market, **so that** each Consultant's KYC requirements match PRD §13.1's per-market table without hardcoding a conditional per market.

## Acceptance Criteria
- Given Super Admin selects 'USA' as home market, when the wizard renders, then EIN/business registration and state-level Seller of Travel fields appear, sourced from data, not a hardcoded conditional.
- Given Super Admin selects 'India', when the wizard renders, then GST registration, business PAN, and IATA/TAAI fields appear as mandatory.

## Developer Notes
- **PRD reference(s):** §13.1 Consultant Onboarding; §21.6 Super Admin Console; §24.7 (data-driven KYC)
- **Module(s)/Screen(s):** whitelabel, Super Admin Console (21.6)
- **Story points:** 8 — New entity + data-driven rules engine + multi-step wizard UI — the first schema-driven compliance surface in the platform.
- **Dependencies:** FND-01, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test, e2e

## Sub-tasks
- [NEW] Backend: `Consultant` entity + `ConsultantRepository` (package-private, own Flyway migration)
- [NEW] Backend: `onboardConsultant` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `POST /api/v1/consultants` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
- [NEW] Frontend: `useConsultantOnboarding` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `ConsultantOnboardingWizard.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Backend: market→required-fields rule table (data-driven, per RULES.md §24.7)
