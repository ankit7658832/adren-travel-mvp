---
id: SUP-13
epic: Supplier Live Integrations
phase: production
status: not-started
story_points: 5
dependencies: ["OBS-05"]
labels: ["backend", "supplier", "phase2"]
prd_references: ["§24.2"]
modules_or_screens: ["supplier"]
testing_tiers: ["integration (Testcontainers)"]
---

# SUP-13: Tune per-supplier circuit breakers against real production latency profiles

## Summary (business)
This adjusts the platform's automatic "circuit breaker" safeguards, technical protections that temporarily pause calls to a supplier if it's failing, so each travel supplier's specific reliability patterns in the real world are properly accounted for, rather than relying on generic settings. This keeps the platform stable and responsive even when an individual supplier has problems.

## User Story
**As a** platform reliability owner, **I want** have each supplier's circuit breaker's failure threshold and half-open window tuned to its real observed latency/error profile, **so that** PRD §24.2's isolation NFR holds under real supplier behavior, not just the MVP's default Resilience4j configuration.

## Acceptance Criteria
- Given a supplier's real production error/latency profile is observed for a defined window, when the circuit breaker's threshold is reviewed, then its failure-rate threshold and half-open wait duration are tuned per-supplier rather than left at generic defaults.

## Developer Notes
- **PRD reference(s):** §24.2 NFR Supplier Integration
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Configuration tuning informed by production observability data (depends on OBS-05's per-supplier trace spans).
- **Dependencies:** OBS-05
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [EXTEND] Backend: per-supplier circuit-breaker threshold/half-open tuning based on observed production data
- [NEW] Backend: integrationTest — tuned thresholds trip correctly under simulated real-profile failure
