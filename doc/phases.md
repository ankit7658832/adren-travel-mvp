# ADREN TRAVEL — Phases: Where We Are, What's Next

**Purpose:** the practical scheduling/sequencing reference — consolidates what's currently scattered across the PRD and the two story catalogues. For *why* the system is shaped the way it is, see `doc/architecture.md`. For the story catalogues themselves, see `doc/user-stories/mvp-mock-stories.md` (142 stories) and `doc/user-stories/production-stories.md` (83 stories), or their per-file split under `doc/user-stories/mvp-mock/` and `doc/user-stories/production/`.

**Methodology note:** the epic-by-epic ordering below was not previously worked out anywhere in the repo — it's derived here from every story's `dependencies:` frontmatter field via topological sort (225 stories, both phases combined, since production stories frequently depend on mock-phase stories — see §3). One genuine cycle was found in the process, in the `BOK-13`/`BOK-14`/`FIN-07`/`FIN-08` cluster — it has since been fixed at the source (the story files' `dependencies:` frontmatter), not worked around here; see §4 for what changed and why. The full 225-story graph is now confirmed acyclic (re-verified after the fix, not assumed).

---

## 1. Phase overview

Two phases, per PRD Part 8 (Release Plan) and the two story catalogues' own framing:

- **Mock (MVP / Phase 1)** — 142 stories, 725 story points, across 10 epics. Builds the full functional platform (PRD §4's 18 in-scope areas) against sandboxed/mocked externals: LocalStack instead of real AWS, illustrative tax rates pending counsel sign-off, a mocked Meta ad account connection instead of the real Marketing API, Groq without a production failover path. PRD §8 gives the high-level release order (Foundation → Booking core → Financial layer → AI layer → Local DMC+BYOS → Ads/Campaign → Hardening); §3 below derives the actual per-story build order underneath that high-level shape.
- **Production** — 83 stories, 476 story points, across 8 epics. Not new features — it's cutting every mocked/illustrative/sandboxed piece over to the real thing: real supplier sandbox→production integrations (`SUP-*`), a real swappable/failover LLM provider (`LLM-*`), the real Meta Marketing API (`MADS-*`), real AWS infra (`PINF-*`), security hardening and an external pentest (`SEC-*`), finalized tax/legal compliance (`CMP-*`), load testing (`PERF-*`), and production observability (`OBS-*`).

## 2. Mock-phase epic order

Derived build order (full per-story graph in the methodology note above), presented at epic granularity. Waves = groups of stories whose dependencies are all satisfied by prior waves; epics span multiple waves because later stories within an epic often depend on earlier stories in a *different* epic, which is the interleaving called out explicitly below rather than smoothed over.

**Can start immediately, in parallel (wave 0):** `Foundation` (security scaffold, ESLint, path-alias fix, pagination — `FND-01`, `FND-19`, `FND-20`, `FND-23`), `Frontend Shell` (route registration, provider-stack slot, Zustand store — `FES-01/02/03`), `DevOps/Infra` (LocalStack compose, Java toolchain, doc generation — `OPS-01/06/08`), and `Booking Core`'s `BOK-01` (transactional boundaries). Four epics, zero mutual blocking at the very start.

**⚠️ Foundation and Frontend Shell are genuinely interleaved, not sequential — this looks like a cycle at epic granularity and isn't one at the story level.** Backend-security/tenant stories in `Foundation` (`FND-16` Itinerary Builder screen, `FND-18` ErrorBoundary) depend on `Frontend Shell` stories (`FES-03` Zustand store, `FES-01` routes); meanwhile several `Frontend Shell` stories (`FES-04`, `FES-06`, `FES-07`, `FES-09`) depend back on early `Foundation` backend stories (`FND-01` auth, `FND-02` role enforcement, `FND-04` KYC wizard, `FND-06` branding). Both epics enter at wave 0 and both exit by wave 4 — build them together, not one-then-the-other. This matches reality: `Foundation` is really two sub-groups (backend security/tenant infra, `FND-01–12`; frontend screens, `FND-13–17`) that happen to share an epic label.

**Waves 1–2 — the next parallel front:** `AI Layer` (`AI-01` Groq wrapper), `Financial Layer` (`FIN-01` markup config, `FIN-06` wallet model), `Local DMC + BYOS` (`DMC-01` onboarding submission), `Test Infrastructure`, and `Ads/Campaign Management`'s `ADS-01` (Meta account provisioning) all become buildable once their `Foundation`/`Frontend Shell` prerequisites land. This is consistent with PRD §8's high-level ordering (Booking core → Financial layer → AI layer → Local DMC+BYOS as roughly-parallel middle phases), which is a useful cross-check that the derived order isn't contradicting the PRD's own intent, just making it precise.

**`Booking Core` has an unusually long tail (first story buildable at wave 0, last not until wave 12)** — not because early booking work is blocked, but because `BOK-13` (the Direct Booking & Payment flow, now landing at wave 11) is a hub a large cluster of later stories fan out from: `BOK-14` (self-contained, wave 0), `FIN-06` (wallet model, wave 1), then `BOK-13` itself, then `FIN-07`→`FIN-08`→`FIN-09`/`FIN-10` (waves 12–14) once `BOK-13`'s `confirmBooking` scaffold exists to hook into. Treat `Booking Core` as two tiers in practice: line-item stories (`BOK-03` through `BOK-12`, largely wave 0–3) build early and mostly independently; the `BOK-13`-centered cluster (§4) is a distinct, later push.

**`Ads/Campaign Management` and `Hardening` run latest** (entering wave 2, not exiting until wave 18) — expected, since `Ads` depends on a published `Package` (`BOK-12`) and AI creative generation (`AI-12`), and `Hardening`'s notification/PNR-search/dashboard stories intentionally depend on most other epics having real entities to notify about or search across. This also matches PRD §8 putting Ads/Campaign and Hardening last.

## 3. Production-phase epic order

Production stories frequently depend on specific mock-phase stories finishing first (e.g., `SUP-01` depends on mock's `FND-11` and `DMC-07`; `MADS-01` depends on mock's `ADS-01`) — 83 of the 225 total cross-phase edges found run mock→production, zero run production→mock, so there's no cross-phase cycle, only the expected one-directional gating.

- **Earliest (wave 0–1):** `Compliance Execution`'s licensing-tracking stories that don't need a finished KYC flow yet (`CMP-11`), `Production Infrastructure`'s messaging/storage cutover (`PINF-02/03`), and `Security Hardening`'s JWT/rate-limiting work (`SEC-01`, `SEC-04`) — these can start as soon as their specific mock-phase prerequisite (not the whole mock phase) lands.
- **Mid (wave 2–3):** `LLM Production Readiness`, `Production Observability`'s tracing/log-shipping stories, and the bulk of `Compliance Execution`'s market-specific licensing stories (`CMP-04` through `CMP-07`) — these need the mock-phase KYC wizard (`FND-04`) and relevant compliance-adjacent mock stories done, not the entire mock catalogue.
- **Later:** `Supplier Live Integrations` and `Meta Ads API Real Integration` gate on their mock-phase counterparts being fully built (`DMC-07` credential-agnostic supplier layer; `ADS-01/07/09/10/13/15`) — each `SUP-*`/`MADS-*` story maps to one specific mock story it's "cutting over," so these can proceed supplier-by-supplier/feature-by-feature rather than waiting for every other production epic.
- **Latest (exits at wave 19, the last wave overall):** `Production Observability` and `Meta Ads API Real Integration`'s tail stories — observability naturally lags because it needs the real infrastructure and real integrations it's observing to exist first.

**Practical reading:** production work does **not** need to wait for 100% mock completion as a phase gate — most `SUP-*`/`MADS-*`/`PINF-*`/`SEC-*` stories are gated on specific mock stories, not the mock phase as a whole. The one true phase-level gate is §5's Mock-complete definition, which is a *quality* gate (working end-to-end flow, tests passing), not a literal "all 142 stories closed" requirement for production to begin — though `CMP-01`/`CMP-02` (finalizing tax rates) explicitly cannot start meaningfully until `FIN-17`/`FIN-18`'s illustrative placeholders exist to replace.

## 4. Fixed: a dependency cycle in the story catalogue (resolved at the source)

**Status: resolved.** This section originally documented an unresolvable cycle rather than silently working around it; the fix has since been made directly in the story files' `dependencies:` frontmatter, and the full 225-story graph has been re-verified acyclic (Kahn's-algorithm topological sort now processes all 225 nodes with zero left over — not assumed, checked).

**What was wrong:** `BOK-13` ("Build the Direct Booking & Payment flow") listed `BOK-14` ("Capture Traveler Profile details") as a dependency, and `BOK-14` listed `BOK-13` right back — a direct two-story cycle. Independently, `BOK-13` → `FIN-08` ("Block booking confirmation on credit-limit breach") → `FIN-07` ("Place a hold on booking confirmation") → `BOK-13` was a second, 3-story cycle sharing the `BOK-13` node. Together these had pulled 24 stories into one strongly-connected component that couldn't be topologically sorted as authored: `BOK-13, BOK-14, BOK-15, BOK-16, BOK-17, BOK-19, FIN-07, FIN-08, FIN-09, FIN-10, DMC-04, DMC-05, HRD-07, HRD-08, AI-09, TST-03, CMP-03, CMP-08, CMP-09, LLM-08, OBS-04, PERF-02, PERF-05, SUP-10`.

**What changed, and why (not an arbitrary edge drop):**

- **`BOK-14`'s `dependencies:` changed from `["BOK-13"]` to `[]`.** `BOK-14` is a self-contained entity + endpoint (`TravelerProfile`, `POST /api/v1/travelers`) with no technical prerequisite among mock stories. The narrative dependency runs the other way: `BOK-13`'s own Acceptance Criteria describes its traveler-detail form requiring document fields inline — i.e., `BOK-13`'s screen *embeds* `BOK-14`'s capture flow, not the reverse. `BOK-13` already listed `BOK-14` as a dependency (kept, correctly directional); the back-edge was the error.
- **`BOK-13`'s `dependencies:` changed from `["BOK-12", "FIN-06", "FIN-08", "BOK-14"]` to `["BOK-12", "FIN-06", "BOK-14"]`** (dropped `FIN-08`). `FIN-07`'s own developer notes already stated it's "invoked from BOK-13's booking flow" — i.e., `FIN-07` (and `FIN-08`, which depends on `FIN-07`) needs `BOK-13`'s `confirmBooking` method to exist as the integration point, not the other way around. `BOK-13` listing `FIN-08` as a prerequisite was the error; both files now explain the intended build order in their Developer Notes (`BOK-13`'s `confirmBooking` scaffold lands first with the wallet-hold/breach-block calls stubbed, then `FIN-07`→`FIN-08` are built and wired in as a follow-up integration).

Both edits are documented inline in the story files themselves (`BOK-13-build-the-direct-booking-payment-flow-user-facing.md`, `BOK-14-capture-traveler-profile-details-including-passport-document-vault.md`), so the reasoning survives independent of this document. §2's epic-order narrative above already reflected the corrected shape (`BOK-14` at wave 0, `BOK-13` at wave 11, `FIN-07`→`FIN-08`→`FIN-09`/`FIN-10` at waves 12–14).

## 5. Mock-complete definition of done

No `PROGRESS.md` or equivalent tracker currently exists anywhere in the repo (`doc/user-stories/mvp-mock/` and `doc/user-stories/production/` were checked directly — neither directory has one). **Gap — recommend creating `doc/user-stories/mvp-mock/PROGRESS.md`** as a companion file that mechanically tracks per-story status (the frontmatter `status:` field, currently `not-started` on all 142 mock stories and all 83 production stories, is the natural source of truth to roll up from) rather than maintaining status by hand in prose.

Until that exists, mock-complete should be checked against:

- [ ] All 142 mock-phase stories' `status:` frontmatter is `done` (or an explicitly justified `wont-do` with reasoning, not silently dropped) — see §7's progress table, currently seeded at 0%.
- [ ] Every module listed in `backend/README.md`'s table (plus `security`, per `doc/architecture.md` §8's discrepancy note) has moved off "package-info stub" — i.e., `ai`, `payments`, `whitelabel`, `ads`, `compliance` each have a real `Api` implementation, not just a module boundary declaration.
- [ ] `./gradlew check` passes, including `ModularityTests.verify()` — module boundaries hold under the fully-built system, not just the current two-module reference implementation.
- [ ] Flow A (search → itinerary → quotation, PRD §9.1), Flow B (quotation → package → publish, PRD §9.1), and Flow C (direct booking → payment → voucher, PRD §9.1) are each verified manually end-to-end at least once against the mocked/sandboxed externals — not just unit-tested in isolation.
- [ ] The `⚠️ Reconcile` items in `RULES.md` (§9's backlog) that are marked as blocking real usage (items 1–2: transactional boundaries, authZ) are closed — a booking engine with no enforced tenant isolation is not mock-complete regardless of feature-story count, per `RULES.md` §5's own framing.
- [ ] `doc/architecture/` has at least one generated PlantUML diagram set (`backend/README.md`'s "Generating module documentation" step run at least once) — currently absent (`doc/architecture.md` §0 note).

## 6. Open business/legal decisions blocking specific stories

Every `⚠️ NEEDS CLARIFICATION` flag in the story catalogues, consolidated in one place, all tracing back to PRD §19's "Open Items for Business Confirmation":

| PRD §19 item | Blocks (mock) | Blocks (production) |
|---|---|---|
| Exact GST/TCS rates & mechanics (tax counsel sign-off) | `FIN-17` (illustrative placeholder, config-flag-gated) | `CMP-01` (cannot finalize until sign-off received) |
| Exact UK TOMS VAT rate & mechanics (UK tax counsel sign-off) | `FIN-18` (illustrative placeholder, config-flag-gated) | `CMP-02` (cannot finalize until sign-off received) |
| Ad-spend billing model per settlement currency | `ADS-14` (configurable-percentage placeholder pending confirmed model) | `MADS-08` (billing/liability isolation depends on the model being final — not itself flagged, but downstream of `ADS-14`) |
| EU/UK data residency approach | — | `CMP-09` (erasure/retention logic may need rework depending on `PINF-06`'s resolution), `PINF-06` itself (scope can't be sized until the decision is made) |
| Whether Adren itself requires market-specific licensing | — | `CMP-11` (implements the *tracking* mechanism only, not the legal determination) |
| Whether the Ads module needs per-market legal sign-off on templates | — | `MADS-09` (implements the tracking/gating mechanism assuming sign-off is required; remove the gate if business confirms otherwise) |

**Gap found, not silently omitted:** PRD §19 lists eight open items total. Four of them map to a `NEEDS CLARIFICATION` flag somewhere in the story catalogue: ad-spend billing model (table above), whether Adren itself needs market-specific licensing (table above), EU/UK data residency (table above), and exact GST/TCS + UK TOMS VAT rates (table above — one PRD bullet, but it drives four separate story-level flags since India and UK tax mechanics are distinct calculations). The remaining four items have **no corresponding flag in any of the 225 story files** (verified by full-text search, not sampling):

- **Exact commission tiers and their interaction with markup**
- **Local DMC vetting ownership (Adren vs. Consultant self-certification)**
- **Sub-agent hierarchy depth beyond Consultant → User**
- **Support coverage model across six timezones**

`FIN-02` (track Adren commission separately from markup) and `DMC-02` (Pending→Active vetting workflow) both implement *a* version of these without flagging that the underlying business rule is still open — worth a decision-owner's attention before those stories are treated as "just build the obvious version," since the story text doesn't currently surface that the business rule behind it is unconfirmed.

## 7. Living progress-summary table

Seeded at 0% — this is the ongoing tracker. Update the Status column as stories close; once `doc/user-stories/mvp-mock/PROGRESS.md` (§5) exists, link to it here instead of maintaining counts by hand in both places.

### Mock phase (142 stories / 725 points)

| Epic | Stories | Points | Status |
|---|---|---|---|
| Foundation | 24 | 124 | 0% (0/24) |
| Booking Core | 20 | 98 | 0% (0/20) |
| Financial Layer | 18 | 95 | 0% (0/18) |
| AI Layer | 13 | 72 | 0% (0/13) |
| Local DMC + BYOS | 11 | 57 | 0% (0/11) |
| Ads/Campaign Management | 15 | 80 | 0% (0/15) |
| Hardening | 13 | 76 | 0% (0/13) |
| Frontend Shell | 10 | 55 | 0% (0/10) |
| DevOps/Infra | 9 | 30 | 0% (0/9) |
| Test Infrastructure | 9 | 38 | 0% (0/9) |
| **Total** | **142** | **725** | **0%** |

### Production phase (83 stories / 476 points)

| Epic | Stories | Points | Status |
|---|---|---|---|
| Supplier Live Integrations | 16 | 99 | 0% (0/16) |
| LLM Production Readiness | 9 | 54 | 0% (0/9) |
| Meta Ads API Real Integration | 9 | 55 | 0% (0/9) |
| Production Infrastructure | 11 | 65 | 0% (0/11) |
| Security Hardening | 10 | 52 | 0% (0/10) |
| Compliance Execution | 12 | 73 | 0% (0/12) |
| Performance/Load Testing | 8 | 40 | 0% (0/8) |
| Production Observability | 8 | 38 | 0% (0/8) |
| **Total** | **83** | **476** | **0%** |

Per-epic story counts/points independently cross-checked against the Summary tables in `mvp-mock-stories.md` and `production-stories.md` — they match exactly, no discrepancy found there.
