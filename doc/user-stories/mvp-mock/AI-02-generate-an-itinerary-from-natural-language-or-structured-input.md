---
id: AI-02
epic: AI Layer
phase: mock
status: not-started
story_points: 8
dependencies: ["AI-01", "FND-14"]
labels: ["backend", "ai", "phase1"]
prd_references: ["§11.1", "§11.2"]
modules_or_screens: ["ai", "supplier", "booking"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "integration (Testcontainers)"]
---

# AI-02: Generate an itinerary from natural-language or structured input

## Summary (business)
This gives travel consultants the ability to describe a trip in plain language or simple form fields and have the AI draft a ready-to-review itinerary automatically, saving significant planning time. Crucially, the AI can only pull from real, currently bookable options confirmed by our suppliers, so customers are never shown trips that don't actually exist or can't be booked.

## User Story
**As a** Consultant/User, **I want** describe an itinerary in natural language or structured fields and get an AI-generated draft, **so that** PRD §11.1's AI-assisted itinerary generation capability is available, grounded only in live supplier-confirmed inventory (§11.2 principle 1).

## Acceptance Criteria
- Given a Consultant provides a natural-language itinerary request, when AI generation runs, then every suggested line item is selected only from live, supplier-confirmed inventory returned by `SupplierSearchApi` — never fabricated.

## Developer Notes
- **PRD reference(s):** §11.1 AI Capabilities; §11.2 principle 1 (grounded generation only)
- **Module(s)/Screen(s):** ai, supplier, booking
- **Story points:** 8 — Core AI capability integrating supplier search results into prompt construction and parsing a structured suggestion back out — the riskiest single AI story.
- **Dependencies:** AI-01, FND-14
- **Testing tier(s):** unit, module (@ApplicationModuleTest), integration (Testcontainers)

## Sub-tasks
- [NEW] Backend: AI itinerary generation from NL/structured input — Groq client call + prompt construction (`adren.ai.groq` config)
- [NEW] Backend: audit-log write as a transactional gate (backend-best-practices §7 — failed write blocks suggestion use)
- [NEW] Backend: REST endpoint
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
