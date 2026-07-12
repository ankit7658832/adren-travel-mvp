---
id: PINF-03
epic: Production Infrastructure
phase: production
status: not-started
story_points: 5
dependencies: ["OPS-01"]
labels: ["devops", "phase2"]
prd_references: ["§5"]
modules_or_screens: ["Infra (production AWS)"]
testing_tiers: ["integration (Testcontainers)"]
---

# PINF-03: Replace LocalStack SQS/SNS with real AWS messaging for async event fan-out at scale

## Summary (business)
This story puts in place a production-ready messaging system to reliably pass information between different parts of the platform (for example, notifying other systems when a booking happens) as customer volume grows. Without it, the platform's internal communication could become a bottleneck or fail once usage scales beyond early testing levels.

## User Story
**As a** platform reliability owner, **I want** have any async messaging introduced for event fan-out run on real SQS/SNS in production, **so that** OPS-01's LocalStack messaging services have a production equivalent before the platform scales beyond a single-instance event-listener model.

## Acceptance Criteria
- Given event volume exceeds what in-process `@ApplicationModuleListener` dispatch can handle at production scale, when fan-out is evaluated, then real SQS/SNS is available as the production messaging backbone, matching OPS-01's LocalStack-emulated shape.

## Developer Notes
- **PRD reference(s):** §5 System Architecture Overview
- **Module(s)/Screen(s):** Infra (production AWS)
- **Story points:** 5 — Production messaging infra cutover; scope is provisioning + connectivity, not a redesign of the event model itself.
- **Dependencies:** OPS-01
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: production SQS/SNS provisioning and connectivity
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
