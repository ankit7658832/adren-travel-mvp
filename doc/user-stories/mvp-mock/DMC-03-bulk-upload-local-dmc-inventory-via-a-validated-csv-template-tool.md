---
id: DMC-03
epic: Local DMC + BYOS
phase: mock
status: not-started
story_points: 8
dependencies: ["DMC-02"]
labels: ["backend", "frontend", "dmc", "supplier", "phase1"]
prd_references: ["§10.2.8"]
modules_or_screens: ["supplier", "Local DMC Onboarding — NEW feature folder"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# DMC-03: Bulk-upload Local DMC inventory via a validated CSV/template tool

## Summary (business)
This gives consultants a way to upload a local partner's entire catalogue of tours, transfers, or activities at once using a spreadsheet template, instead of typing each item in individually - saving significant time when onboarding a new supplier. If any required information (like price, availability, or cancellation terms) is missing, the whole upload is rejected with a clear list of what's wrong, so incomplete or incorrect listings never reach customers.

## User Story
**As a** Consultant, **I want** bulk-upload a Local DMC's inventory using a CSV template rather than entering products one at a time, **so that** PRD §10.2.8's data-entry/bulk-upload engineering scope is implemented, with required-field validation.

## Acceptance Criteria
- Given a Consultant uploads a CSV missing a required field (product name, category, net rate, currency, cancellation policy text, or availability calendar), when validation runs, then the upload is rejected with row-level, field-level errors — not a partial silent import.
- Given a valid CSV is uploaded, when validation passes, then every row becomes a Local DMC inventory item linked to the DMC record.

## Developer Notes
- **PRD reference(s):** §10.2.8 Local DMC — Manual Integration
- **Module(s)/Screen(s):** supplier, Local DMC Onboarding — NEW feature folder
- **Story points:** 8 — CSV parsing + row-level validation + bulk persistence is the most mechanically complex Local DMC story.
- **Dependencies:** DMC-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test

## Sub-tasks
- [NEW] Backend: CSV template parser + row-level Bean Validation
- [NEW] Backend: bulk-insert endpoint `POST /api/v1/local-dmc/{id}/inventory/bulk-upload`
- [NEW] Frontend: `useLocalDmcBulkUpload` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `LocalDmcBulkUpload.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
