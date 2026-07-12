---
id: PINF-05
epic: Production Infrastructure
phase: production
status: not-started
story_points: 8
dependencies: ["PINF-04"]
labels: ["devops", "phase2"]
prd_references: ["§2"]
modules_or_screens: ["Infra (production deployment)"]
testing_tiers: ["integration (Testcontainers)"]
---

# PINF-05: Define the production deployment topology for the Spring Modulith monolith

## Summary (business)
This story defines how the platform's software will actually be run and scaled in production, including automatically adding capacity during busy periods and checking that servers are healthy before sending them customer traffic. This ensures the platform stays fast and available as demand grows, rather than being limited to a single fixed setup.

## User Story
**As a** platform reliability owner, **I want** have a defined Kubernetes/ECS deployment topology for the monolith, **so that** the platform can actually run in production with defined scaling/health-check behavior.

## Acceptance Criteria
- Given production load increases, when the deployment topology is evaluated, then the monolith scales horizontally per its defined topology with health checks gating traffic to unready instances.

## Developer Notes
- **PRD reference(s):** §2 Goals & Success Metrics (99.5%+ uptime)
- **Module(s)/Screen(s):** Infra (production deployment)
- **Story points:** 8 — First production deployment topology decision for a not-yet-deployed monolith — architecturally significant, not just a config file.
- **Dependencies:** PINF-04
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: Kubernetes/ECS deployment topology, health checks, horizontal scaling policy
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
