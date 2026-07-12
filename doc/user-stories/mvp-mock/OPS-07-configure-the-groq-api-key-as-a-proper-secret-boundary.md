---
id: OPS-07
epic: DevOps/Infra
phase: mock
status: not-started
story_points: 2
dependencies: ["OPS-02", "AI-01"]
labels: ["devops", "foundation", "security", "phase1"]
prd_references: ["§5.3"]
modules_or_screens: ["Infra (secrets)"]
testing_tiers: ["integration (Testcontainers)"]
---

# OPS-07: Configure the Groq API key as a proper secret boundary

## Summary (business)
This ensures that the access key used to connect to our AI service provider (Groq, which powers AI features) is stored securely rather than written in plain text anywhere in the code or settings, except in the local practice environment. This reduces the risk of the AI service being compromised by someone gaining unauthorized access to that key.

## User Story
**As a** backend engineer, **I want** have the Groq API key sourced from LocalStack Secrets Manager in test/dev and real Secrets Manager in any non-local profile, **so that** RULES.md §5.3's rule — no real integration credential as a plaintext config value or environment variable outside local Docker Compose — applies to `adren.ai.groq` exactly as it does to supplier credentials.

## Acceptance Criteria
- Given the `ai` module resolves its Groq API key in a non-local profile, when resolution runs, then it reads from Secrets Manager by ARN, never a plaintext env var default.

## Developer Notes
- **PRD reference(s):** §5.3 Secrets handling (RULES.md)
- **Module(s)/Screen(s):** Infra (secrets)
- **Story points:** 2 — Same pattern as FND-11/OPS-02, applied to one additional secret.
- **Dependencies:** OPS-02, AI-01
- **Testing tier(s):** integration (Testcontainers)

## Sub-tasks
- [NEW] Infra: Groq API key sourced from Secrets Manager by ARN outside local profile
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
