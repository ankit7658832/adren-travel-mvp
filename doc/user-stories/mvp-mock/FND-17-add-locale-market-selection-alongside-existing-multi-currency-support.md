---
id: FND-17
epic: Foundation
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-04"]
labels: ["backend", "foundation", "whitelabel", "phase1"]
prd_references: ["§13.3", "§12.2"]
modules_or_screens: ["whitelabel", "shared"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# FND-17: Add locale/market selection alongside existing multi-currency support

## Summary (business)
This story lets consultants choose their preferred display language (for example English, Hindi, or Danish, depending on their country) in the same way they already choose their billing currency. This makes the platform more usable and comfortable for consultants operating in non-English-speaking markets.

## User Story
**As a** Consultant, **I want** select my operating locale (English/Hindi/regional for India, English-primary elsewhere, Danish secondary for Denmark) alongside my settlement currency, **so that** the platform supports PRD §13.3's multi-language requirement using the same data-driven approach as currency.

## Acceptance Criteria
- Given a Consultant's home market is Denmark, when they open language settings, then Danish is offered as a secondary language alongside English-primary.
- Given a Consultant's home market is India, when they open language settings, then Hindi/regional options are offered alongside English.

## Developer Notes
- **PRD reference(s):** §13.3 Multi-Language & Multi-Currency; §12.2 Multi-Currency & FX Buffer
- **Module(s)/Screen(s):** whitelabel, shared
- **Story points:** 5 — Extends `shared.CurrencyCode`'s pattern with a parallel `LocaleCode`/market-language mapping; no i18n framework wiring included (frontend translation content is out of this story's scope).
- **Dependencies:** FND-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `LocaleCode` value type + market→available-locales mapping (mirrors `CurrencyCode`)
- [EXTEND] Backend: `Consultant` entity gains a `preferredLocale` field
- [NEW] Backend: unit test — market→locale mapping
- [NEW] Backend: module test
