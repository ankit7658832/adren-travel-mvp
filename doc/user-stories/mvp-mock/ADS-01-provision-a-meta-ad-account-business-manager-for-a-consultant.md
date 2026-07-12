---
id: ADS-01
epic: Ads/Campaign Management
phase: mock
status: not-started
story_points: 8
dependencies: ["FND-01", "FND-02"]
labels: ["backend", "ads", "phase1"]
prd_references: ["§14.1", "§6"]
modules_or_screens: ["ads"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# ADS-01: Provision a Meta ad account/Business Manager for a Consultant

## Summary (business)
When a travel Consultant wants to run paid social media ads, the platform sets up their advertising account centrally, under Adren's own management, rather than letting the Consultant own it directly. This keeps every advertiser's account secure, consistent, and under Adren's oversight from day one, before any ad is ever created.

## User Story
**As a** Super Admin, **I want** provision a Meta ad account and Business Manager for a Consultant under Adren's umbrella structure, **so that** PRD §14.1's centrally-managed account model is implemented before any campaign can be built.

## Acceptance Criteria
- Given Super Admin provisions a Meta ad account for a Consultant, when provisioning completes, then the Consultant is linked to an Adren-managed Meta Business Manager, never a Consultant-owned one, per §6's 'No (executes)' row.

## Developer Notes
- **PRD reference(s):** §14.1 Ads/Campaign Overview; §6 Roles & Permissions Matrix
- **Module(s)/Screen(s):** ads
- **Story points:** 8 — New module (stub today) + first Meta-adjacent entity; MVP scope is provisioning bookkeeping (mocked Meta calls), full API wiring is Phase 2's MADS epic.
- **Dependencies:** FND-01, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `ads/package-info.java` → real module shape (`AdsApi`, `event/`, `internal/`)
- [NEW] Backend: `AdAccount` entity + provisioning service (mocked Meta call in MVP)
- [NEW] Backend: `POST /api/v1/consultants/{id}/ad-account` endpoint
- [NEW] Backend: unit test
- [NEW] Backend: module test
