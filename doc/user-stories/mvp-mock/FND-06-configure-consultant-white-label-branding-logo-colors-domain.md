---
id: FND-06
epic: Foundation
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-04"]
labels: ["backend", "foundation", "whitelabel", "phase1"]
prd_references: ["§13.2", "§21.6"]
modules_or_screens: ["whitelabel", "Super Admin Console (21.6)"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# FND-06: Configure Consultant white-label branding (logo, colors, domain)

## Summary (business)
This story lets administrators customize each consultant's storefront with their own logo, colors, and web address, so every consultant's booking site looks like their own brand rather than a generic Adren page. This is core to the platform's white-label (rebrandable, resold-under-another-company's-name) business model.

## User Story
**As a** Super Admin, **I want** configure a Consultant's logo, background image, primary/secondary text color, and CNAME domain mapping, **so that** each Consultant's storefront reflects their own brand per PRD §13.2.

## Acceptance Criteria
- Given Super Admin uploads a logo and sets primary/secondary colors for a Consultant, when the change is saved, then the Consultant's theme tokens update.
- Given Super Admin maps a CNAME domain to a Consultant, when the mapping is saved, then the domain registry used by dynamic CORS (FND-08) reflects it.

## Developer Notes
- **PRD reference(s):** §13.2 Branding Configuration; §21.6 Super Admin Console
- **Module(s)/Screen(s):** whitelabel, Super Admin Console (21.6)
- **Story points:** 5 — New entity (BrandingProfile) plus file upload; UI is a form over that entity.
- **Dependencies:** FND-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [NEW] Backend: `BrandingProfile` entity + `BrandingProfileRepository` (package-private, own Flyway migration)
- [NEW] Backend: `updateBranding` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `PATCH /api/v1/consultants/{id}/branding` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
