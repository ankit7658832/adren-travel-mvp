---
id: AI-13
epic: AI Layer
phase: mock
status: not-started
story_points: 3
dependencies: ["AI-01", "AI-07"]
labels: ["backend", "ai", "phase1"]
prd_references: ["§24.3", "§9.6"]
modules_or_screens: ["ai"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)"]
---

# AI-13: Bound AI response latency to protect the 10-minute itinerary target

## Summary (business)
This puts a strict time limit on how long the business will wait for an AI suggestion before moving on, ensuring AI assistance never slows down the goal of building a complete trip itinerary in under 10 minutes. Every timeout or retry is also logged, so any performance issues can be tracked and addressed.

## User Story
**As a** Consultant/User, **I want** have AI suggestions return within a few seconds per segment, **so that** PRD §24.3's NFR is met so the overall itinerary build stays within the 10-minute target from §9.6.

## Acceptance Criteria
- Given an AI itinerary-completion request is made, when the Groq call is issued, then a bounded per-segment timeout is enforced, with each retry/timeout attempt logged distinctly per AI-07's audit requirement.

## Developer Notes
- **PRD reference(s):** §24.3 NFR AI Governance; §9.6 NFR (10-minute target)
- **Module(s)/Screen(s):** ai
- **Story points:** 3 — Timeout/retry configuration on top of AI-01's client wrapper.
- **Dependencies:** AI-01, AI-07
- **Testing tier(s):** unit, module (@ApplicationModuleTest)

## Sub-tasks
- [EXTEND] Backend: bounded per-segment timeout on `GroqClient`
- [NEW] Backend: distinct per-attempt audit logging on retry (not just the final attempt)
- [NEW] Backend: unit test — timeout triggers distinct-attempt logging
