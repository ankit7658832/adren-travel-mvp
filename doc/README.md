# ADREN TRAVEL — Documentation

| File | Purpose |
|---|---|
| `PRD_v1.1_master.md` | Master PRD — business scope, global market/compliance framing |
| `PRD_v2_detailed.md` | Detailed/Engineering Edition — per-supplier integration specs, full data dictionary, screen-by-screen UI spec, acceptance criteria, edge-case catalogue, NFRs, test scenario appendix |
| `architecture/` | Auto-generated module diagrams (see below) — empty until first generated |

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
