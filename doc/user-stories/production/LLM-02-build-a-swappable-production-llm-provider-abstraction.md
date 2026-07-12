---
id: LLM-02
epic: LLM Production Readiness
phase: production
status: not-started
story_points: 8
dependencies: ["LLM-01", "AI-01"]
labels: ["backend", "ai", "phase2"]
prd_references: ["§4"]
modules_or_screens: ["ai"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# LLM-02: Build a swappable production LLM provider abstraction

## Summary (business)
We will change how our system connects to the AI provider so that switching providers in the future is a simple settings change rather than a major rebuild. This protects the business from being stuck with one vendor and lets us react quickly if a better or cheaper option becomes available.

## User Story
**As a** backend engineer, **I want** have the `ai` module depend on an internal provider interface rather than being hard-wired to `GroqClient`, **so that** LLM-01's decision (stay on Groq or switch) can be executed as a configuration change, per backend-best-practices §4's DI discipline.

## Acceptance Criteria
- Given the production LLM provider decision changes, when the provider bean is swapped, then no caller of the `ai` module's internal generation logic changes — only the injected provider implementation differs.

## Developer Notes
- **PRD reference(s):** backend-best-practices skill §4 (DI conventions)
- **Module(s)/Screen(s):** ai
- **Story points:** 8 — Structural refactor of AI-01's client wrapper into an interface + implementation split — must happen before/alongside whichever provider LLM-01 selects.
- **Dependencies:** LLM-01, AI-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [NEW] Backend: `LlmProvider` interface (prompt in, structured suggestion out)
- [EXTEND] Backend: `GroqClient` becomes one `LlmProvider` implementation
- [NEW] Backend: unit test — provider swap via config, no caller change
- [NEW] Backend: module test
