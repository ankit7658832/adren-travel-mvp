---
id: AI-01
epic: AI Layer
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-01"]
labels: ["backend", "ai", "phase1"]
prd_references: ["§11.1"]
modules_or_screens: ["ai"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# AI-01: Integrate a Groq client wrapper for the AI module

## Summary (business)
This story sets up one shared connection point to the AI service that all our AI-powered features will use, instead of each feature building its own separate connection. This keeps the system easier to maintain, monitor, and secure as we add more AI capabilities over time.

## User Story
**As a** backend engineer, **I want** have a single, well-isolated Groq client used by every AI capability, **so that** the `ai` module's Groq dependency is centralized behind one internal client rather than duplicated per capability, per PRD §11.1 and `adren.ai.groq` config.

## Acceptance Criteria
- Given the `ai` module needs to call the LLM, when any capability invokes the client, then it goes through one `GroqClient` wrapper, not a direct HTTP call inlined in the capability's own class.

## Developer Notes
- **PRD reference(s):** §11.1 AI Capabilities in Scope
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — New module (currently package-info-only stub) — first real code in `ai`, establishing its internal shape.
- **Dependencies:** FND-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `ai/package-info.java` → real module shape (`AiApi`, `event/`, `internal/`)
- [NEW] Backend: `internal.GroqClient` wrapping `adren.ai.groq` config
- [NEW] Backend: unit test — request/response mapping, mocked HTTP
- [NEW] Backend: module test — module boundary respected (`ModularityTests`)
