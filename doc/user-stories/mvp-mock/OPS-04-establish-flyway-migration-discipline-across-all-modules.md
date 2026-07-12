---
id: OPS-04
epic: DevOps/Infra
phase: mock
status: not-started
story_points: 3
dependencies: ["OPS-01"]
labels: ["devops", "foundation", "phase1"]
prd_references: ["§4.2"]
modules_or_screens: ["Infra (Flyway)"]
testing_tiers: ["integration (Testcontainers)"]
---

# OPS-04: Establish Flyway migration discipline across all modules

## Summary (business)
This establishes a disciplined process for how the database is changed over time as new features are built, ensuring changes are always additive and never overwrite or break past changes. This protects the business from data loss or outages caused by conflicting or careless database updates as the product grows.

## User Story
**As a** backend engineer, **I want** have every module's first real entity land with a correctly-numbered, additive-only Flyway migration, **so that** RULES.md §4.2's migration discipline (never edit a merged migration, one file per change, module-owned tables) is enforced as modules move from stub to real.

## Acceptance Criteria
- Given a new module's first entity is added (e.g. `ai`'s `AiSuggestionAuditLog`), when its migration is written, then it is `V<n>__ai_init.sql`, strictly incrementing from the current head, owning only `ai`-prefixed tables.

## Developer Notes
- **PRD reference(s):** §4.2 Migration discipline (RULES.md)
- **Module(s)/Screen(s):** Infra (Flyway)
- **Story points:** 3 — Convention documentation plus a CI check that migration numbering is strictly incrementing and never edited post-merge.
- **Dependencies:** OPS-01
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: CI check: Flyway migration numbering strictly incrementing, no edits to merged migrations
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
