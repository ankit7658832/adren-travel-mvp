# ADREN TRAVEL — Documentation

**Start here.** This file is the entry point to all project documentation, not just the PRD index it used to be.

- **New engineer/stakeholder wanting the *why*, not just the *what*?** Read [`architecture.md`](architecture.md) first — narrative reasoning behind the modular monolith, event-driven design, tenant isolation, and known risks/gaps stated honestly.
- **Need to know where the project actually is and what's next?** Read [`phases.md`](phases.md) — Mock vs. Production phase status, derived epic build order, open business/legal decisions blocking specific stories, and the living progress table.
- **Writing or reviewing backend/frontend code?** Read [`architecture/RULES.md`](architecture/RULES.md) — the enforceable engineering rules (module boundaries, events, API/data/security/observability standards, frontend conventions) `architecture.md` explains the reasoning behind.
- **Touching anything visual, or Consultant white-label branding?** Read [`DESIGN.md`](DESIGN.md) — the Layer 1 (fixed Adren chrome) / Layer 2 (tenant-themed storefront) design-token architecture and the contrast-safety algorithm.

| File | Purpose |
|---|---|
| `architecture.md` | Narrative architecture reasoning — the *why* behind the module map, event-driven design, tenant isolation, and a stated-honestly list of current risks/gaps and cross-file discrepancies |
| `phases.md` | Mock vs. Production phase status, derived epic build order, mock-complete definition of done, consolidated open business/legal decisions, living progress table |
| `PRD_v1.1_master.md` | Master PRD — business scope, global market/compliance framing |
| `PRD_v2_detailed.md` | Detailed/Engineering Edition — per-supplier integration specs, full data dictionary, screen-by-screen UI spec, acceptance criteria, edge-case catalogue, NFRs, test scenario appendix |
| `DESIGN.md` | Frontend design system — Layer 1/Layer 2 token architecture, contrast-safety algorithm, component specs |
| `architecture/RULES.md` | Enforceable backend/frontend engineering rules and the reasoning behind each one |
| `architecture/` (PlantUML diagrams) | Auto-generated module diagrams (see below) — empty until first generated |
| `user-stories/mvp-mock-stories.md`, `user-stories/mvp-mock/` | 142 MVP Mock-phase stories (full catalogue, and one-file-per-story split) |
| `user-stories/production-stories.md`, `user-stories/production/` | 83 Production-phase stories (full catalogue, and one-file-per-story split) |

## Regenerating architecture diagrams

The backend's `ModularityTests.writeModuleDocumentation()`
(`backend/src/test/java/com/adren/travel/ModularityTests.java`) uses Spring
Modulith's `Documenter` to generate PlantUML module diagrams straight from
the actual code structure — not hand-drawn, so they can't drift from
reality. Regenerate and copy them in as part of your release checklist:

```bash
cd backend
./gradlew test --tests ModularityTests
cp build/spring-modulith-docs/*.puml ../doc/architecture/
```

## Using these docs during development

- **Writing a user story?** Reference PRD section numbers directly in the
  Jira ticket (e.g., "per PRD_v2_detailed.md Section 10.2.1, Hotelbeds
  error handling") — the headings are stable across revisions.
- **Building a screen?** Part 21 of the detailed PRD is the UI spec —
  check it before designing a new component's states.
- **Writing acceptance criteria for a Jira sub-task?** Part 22 of the
  detailed PRD already has Given/When/Then criteria for most MVP features —
  copy and adapt rather than writing from scratch.
- **Not sure which module something belongs in?** Check the Roles &
  Permissions Matrix (Section 6) and the module list (Sections 9–17) before
  creating a new package — cross-reference against
  `backend/README.md`'s module map.
