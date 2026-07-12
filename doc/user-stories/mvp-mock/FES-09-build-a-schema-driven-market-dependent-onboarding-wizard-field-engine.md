---
id: FES-09
epic: Frontend Shell
phase: mock
status: not-started
story_points: 8
dependencies: ["FND-04", "FES-08"]
labels: ["frontend", "foundation", "whitelabel", "phase1"]
prd_references: ["§24.7", "§13.1", "§21.6"]
modules_or_screens: ["Frontend shell (tooling)"]
testing_tiers: ["unit", "component test"]
---

# FES-09: Build a schema-driven, market-dependent onboarding wizard field engine

## Summary (business)
This makes the onboarding process (collecting required identity/compliance information, known as KYC) automatically adjust which questions are asked based on the traveler's or consultant's market/country, driven by a shared rules list rather than a hardcoded list buried in the app's code. This means compliance rule changes can be made in one place and take effect everywhere immediately, avoiding situations where the website and backend systems fall out of sync on legal requirements.

## User Story
**As a** frontend engineer, **I want** have the Consultant/Super Admin onboarding wizard resolve its required fields from data, not a hardcoded per-market conditional tree, **so that** PRD §24.7's data-driven KYC principle is mirrored on the frontend, matching FND-04's backend rule table so a market-rule change never requires a frontend deploy that can drift from the backend.

## Acceptance Criteria
- Given the market→required-fields rule table (FND-04) changes on the backend, when the onboarding wizard is rendered, then its required-field set updates without a frontend code change, fetched from the backend's rule table rather than a duplicated hardcoded map.

## Developer Notes
- **PRD reference(s):** §24.7 NFR Regional Compliance; §13.1 Consultant Onboarding; §21.6 Super Admin Console
- **Module(s)/Screen(s):** Frontend shell (tooling)
- **Story points:** 8 — Schema-driven form-field resolution engine consumed by both FND-04's Consultant wizard and any future market-dependent form — genuine architectural piece, not a simple form.
- **Dependencies:** FND-04, FES-08
- **Testing tier(s):** unit, component test

## Sub-tasks
- [NEW] Frontend: schema-driven field-resolution engine consuming FND-04's market rule table
- [NEW] Frontend: unit test — field set changes when the backend rule table changes
- [NEW] Frontend: component test — wizard renders correctly for two different markets
