# ADREN TRAVEL — Phases: Where We Are, What's Next

**Purpose:** the practical scheduling/sequencing reference — consolidates what's currently scattered across the PRD and the two story catalogues. For *why* the system is shaped the way it is, see `doc/architecture.md`. For the story catalogues themselves, see `doc/user-stories/mvp-mock-stories.md` and `doc/user-stories/production-stories.md` (83 stories), or their per-file split under `doc/user-stories/mvp-mock/` (149 stories, see below) and `doc/user-stories/production/`.

**Methodology note:** the epic-by-epic ordering below was not previously worked out anywhere in the repo — it's derived here from every story's `dependencies:` frontmatter field via topological sort (225 stories, both phases combined, since production stories frequently depend on mock-phase stories — see §3). One genuine cycle was found in the process, in the `BOK-13`/`BOK-14`/`FIN-07`/`FIN-08` cluster — it has since been fixed at the source (the story files' `dependencies:` frontmatter), not worked around here; see §4 for what changed and why. The full 225-story graph is now confirmed acyclic (re-verified after the fix, not assumed).

**Update (Stage 3, Step B, 2026-07-16):** 7 new mock-phase stories (`BOK-21`–`BOK-27`) were added after this graph was verified — a supplier-integration gap surfaced during Stage 3 scoping: `BOK-20` (hotel dedup) and `HRD-12` (sync cadence tuning) already referenced `StubaClient`/`TboClient`/a content-sync mechanism in their own text as if built, but no story ever built the STUBA/TBO/Transferz/Widgety/HBActivities client stubs, a circuit-breaker story (PRD §24.2), or a static-content sync/caching story. `BOK-20` and `HRD-12`'s `dependencies:` were updated accordingly (see their files). The new stories were checked by hand for cycles (none — `BOK-21`–`BOK-25` are leaves, `BOK-26`/`BOK-27` depend only on them) but **the full 232-story graph has not been mechanically re-verified** with Kahn's algorithm the way the original 225 were — flagged here rather than silently asserted. Mock-phase totals below (149 stories / 748 points) include these 7 additions; the "225 stories" / "142 mock stories" figures elsewhere in this section predate them.

---

## 1. Phase overview

Two phases, per PRD Part 8 (Release Plan) and the two story catalogues' own framing:

- **Mock (MVP / Phase 1)** — 149 stories, 748 story points, across 10 epics (originally 142/725; +7 stories/+23 points added in Stage 3, Step B — see the methodology note above). Builds the full functional platform (PRD §4's 18 in-scope areas) against sandboxed/mocked externals: LocalStack instead of real AWS, illustrative tax rates pending counsel sign-off, a mocked Meta ad account connection instead of the real Marketing API, Groq without a production failover path. PRD §8 gives the high-level release order (Foundation → Booking core → Financial layer → AI layer → Local DMC+BYOS → Ads/Campaign → Hardening); §3 below derives the actual per-story build order underneath that high-level shape.
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

- [ ] All 149 mock-phase stories' `status:` frontmatter is `done` (or an explicitly justified `wont-do` with reasoning, not silently dropped) — see §7's progress table, currently seeded at 0%.
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

This is the ongoing tracker, now backed by `doc/user-stories/mvp-mock/PROGRESS.md` (created in Stage 1; §5's "gap" note above predates it and is left as historical record). Update as stories close.

### Mock phase (149 stories / 748 points, updated Stage 4 — 2026-07-18)

| Epic | Stories | Points | Status |
|---|---|---|---|
| Foundation | 24 | 124 | 100% (24/24) |
| Booking Core | 27 | 121 | **100% (27/27)** — completed Stage 3 Batch 2 |
| Financial Layer | 18 | 95 | **100% (18/18)** — completed Stage 3 Batch 2 |
| AI Layer | 13 | 72 | **100% (13/13)** — completed Stage 4 |
| Local DMC + BYOS | 11 | 57 | 0% (0/11) |
| Ads/Campaign Management | 15 | 80 | 0% (0/15) |
| Hardening | 13 | 76 | 8% (1/13, 8 pts) |
| Frontend Shell | 10 | 55 | 20% (2/10, 10 pts) |
| DevOps/Infra | 9 | 30 | 0% (0/9) |
| Test Infrastructure | 9 | 38 | 0% (0/9) |
| **Total** | **149** | **748** | **57% (85/149 stories, 430/748 pts)** |

## 7a. Stage 1 & Stage 2 actual velocity, and a revised remaining-timeline estimate (Stage 3, Step A)

**Source:** git commit history, not estimates — `git log --pretty=format:"%h %ai %s"` across the `main`, `AD-stage2-hotel-verticle-slice`, and `AD-booking-stage` branches.

**Stage 1 (Foundation epic build-out):**
- Commits: `658640f` (2026-07-14 23:15) — seeding `PROGRESS.md` — through `a7a8e40` (2026-07-15 09:32), the last Foundation-epic story commit.
- Delivered **26 stories / 134 points** — all 24 Foundation stories (124 pts) **plus 2 Frontend Shell stories** (`FES-01`, `FES-03`, 10 pts) that `doc/phases.md` §2 already flags as genuinely interleaved with Foundation, not a separate later push. The "Foundation's 124" figure undercounts what Stage 1 actually shipped by 10 points.
- Calendar-day span: 2026-07-14 → 2026-07-15 (2 distinct dates). Wall-clock elapsed: ~10h17m.

**Stage 2 (`AD-stage2-hotel-verticle-slice` + `AD-booking-stage`, ending at the "Stage 2 checkpoint" commit):**
- Commits: `93e5172` (2026-07-15 10:22) through `e2f78b8` (2026-07-16 01:16, "Stage 2 checkpoint: full vertical-slice e2e test over real HTTP").
- Delivered **20 stories / 101 points**: `BOK-01,02,03,08,09,10,12,13,14,15` (10 stories, 47 pts), `FIN-01,02,03,04,05,06,07,10,11` (9 stories, 46 pts), `HRD-01` (1 story, 8 pts).
- Calendar-day span: 2026-07-15 → 2026-07-16 (2 distinct dates). Wall-clock elapsed: ~14h54m.

**Combined actual delivery:** 46 stories / **235 points**, wall-clock span 2026-07-14 23:15 → 2026-07-16 01:16 = **~26h (≈1.08 days), across 3 calendar dates**.

**⚠️ Caveat before using this as a velocity figure:** both stages were built as continuous, compressed AI-assisted implementation sessions (evenings/overnight, no multi-day gaps, no team-of-humans sprint cadence). A literal points/week extrapolation from ~26 hours of wall-clock time is **not a credible planning number for the remaining 513 points** — it would predict finishing the rest of the mock phase in about a day, which contradicts the whole premise of "sprint velocity." Reporting it anyway, as requested, with both a wall-clock and a calendar-day-count reading so the distortion is visible rather than hidden:

| Basis | Elapsed | Points | Velocity |
|---|---|---|---|
| Wall-clock hours | ~26.0h (1.08 days) | 235 | 216.8 pts/day → **~1,518 pts/week** |
| Calendar-date count (crude) | 3 distinct dates | 235 | 78.3 pts/day → **~548 pts/week** |

**Revised remaining-timeline estimate:** Total mock scope is now 748 points (149 stories, including Stage 3 Step B's +23 pts). Completed: 235 points. **Remaining: 513 points** (not the ~546 cited going into Stage 3 — that figure predates both the exact Stage 2 tally and Step B's 7 new stories). At the literal wall-clock rate above, 513 points ≈ **8.6 more hours** (calendar-date-count basis: ≈6.5 more calendar days) to finish the entire remaining mock phase — a number that should be read as "this project is being built at AI-agent implementation speed, not staffed-team speed," not as a real staffing commitment. If a human-team-comparable planning number is wanted instead, that requires a policy input (assumed sprint length, team size, etc.) this document can't derive from commit timestamps alone — flagging rather than guessing at one.

## 7b. Stage 3 Batch 2 completion and next-epic recommendation (Stage 3, Step F — 2026-07-17)

**What landed:** Batch 2 closed out **Booking Core (27/27) and Financial Layer (18/18) to 100%** — the last 17 stories of those two epics (`BOK-04–07`, `BOK-11`, `BOK-16–20`, `FIN-08`, `FIN-09`, `FIN-12–18`), each implemented/tested/committed individually per the same per-story discipline as Stage 1/2. Mock-phase total is now **72/149 stories, 358/748 points (48%)**.

**Step E's validation surfaced two real, previously-unexercised bugs**, both now fixed (see the "Step E" commit): `CreditLimitExceededException` wasn't mapped by `BookingControllerAdvice` (it 401'd via a generic error page instead of the intended 409 when reached through the real booking-confirmation HTTP path — `CreditLimitBreachIT` only ever called `paymentsApi.placeHold` directly, never through that controller), and BOK-11's new `whitelabelApi.findConsultantMarket` dependency in `publishPackage` broke every test (including Stage 2's own vertical-slice checkpoint and one pre-existing BOK-12 test) that used a never-onboarded `consultantId` — both are the kind of gap that only a real, executed end-to-end run catches, not a mocked-repository unit test or a `@PreAuthorize`/compile-time check. Also discovered mid-session: this environment's Docker (`backend-postgres-1`, the repo's own `docker-compose.yml` service) is actually reachable, so `FullVerticalSliceEndToEndIT` and every `@ApplicationModuleTest` module-integration test could be **executed for real** for the first time this stage (54/54 passing) — a materially stronger verification bar than "compiles, never run" from Batches 1 and prior. Testcontainers-spun-fresh-containers still doesn't work in this sandbox (a Gradle-JVM docker-context wiring gap, not investigated further — out of scope for this stage).

**Recommendation: build the AI Layer next (13 stories, 72 points).**

Both `AI Layer` and `Local DMC + BYOS` are fully unblocked right now — every dependency either epic has (`FND-01/02/14/16`, `BOK-08/12/13`) is already done, so either could be built top-to-bottom without the epic-interleaving `Booking Core`/`Financial Layer` needed throughout Stages 1–3. Recommending AI Layer over Local DMC+BYOS and Ads/Campaign for three reasons:

1. **It's what PRD §8's own high-level release order says is next** ("Booking core → Financial layer → AI layer → Local DMC+BYOS → Ads/Campaign → Hardening") — Booking Core and Financial Layer just finished, so this isn't a re-derivation, it's confirmation the derived build order (§2) and the PRD's stated intent agree.
2. **It unblocks `Ads/Campaign Management`, which cannot otherwise finish.** `ADS-04` (ad creative approval) depends on `AI-12` (AI-generated ad creative) — verified directly in `ADS-04`'s frontmatter. `ADS-01/02/03` could start without AI Layer, but the epic has a hard wall partway through regardless; building AI Layer first removes that wall before Ads/Campaign work begins, rather than discovering it mid-epic.
3. **It's the platform's core product differentiator** (PRD §11's AI Itinerary Governance — grounded generation, 100%-logged suggestions, "AI states inability rather than substituting"), not a peripheral feature; landing it earlier gets the highest-uncertainty, most architecturally novel epic (governance/audit-log-gating a third-party LLM call, a pattern nothing built so far resembles) de-risked while Booking Core/Financial Layer context is still fresh.

`Local DMC + BYOS` (11 stories, 57 points) is the natural epic after that — equally unblocked, self-contained (adds a new supplier channel + BYOS credential management), and doesn't block anything else, so it's a reasonable parallel-track candidate if a second work-stream is available, but not the recommended *next* single target.

## 7c. Stage 3 actual velocity — a third data point, and a refined remaining-timeline estimate (Stage 4, pre-Step A — 2026-07-17)

**Source:** same method as §7a — git commit timestamps, not estimates.

**Stage 3** (`AD-stage3-capture-real-velocity-and-supplier-integration` + `AD-stage3-batch2-booking-core-financial-layer`, Step A through Step F):
- Commits: `a063b0b` (2026-07-16 21:26:03, "Stage 3 Step A/B: real velocity capture, add BOK-21-BOK-27 supplier stories, implement BOK-21") through `149921a` (2026-07-17 22:04:16, the Batch-2-into-main merge commit).
- Delivered **26 stories / 123 points**: `BOK-21,22,23,24,25,26,27` (7 stories, 23 pts — the supplier-stub gap Step B found and filled), `BOK-04,05,06,07,11,16,17,18,19,20` (10 stories, 47 pts), `FIN-08,09,12,13,14,15,16,17,18` (9 stories, 46 pts).
- Calendar-day span: 2026-07-16 → 2026-07-17 (2 distinct dates). Wall-clock elapsed: ~24h38m.

**Updated three-stage comparison:**

| Stage | Stories | Points | Wall-clock | Wall-clock pts/day |
|---|---|---|---|---|
| Stage 1 | 26 | 134 | ~10h17m | 312.8 |
| Stage 2 | 20 | 101 | ~14h54m | 162.7 |
| Stage 3 | 26 | 123 | ~24h38m | 119.9 |

**⚠️ The trend across all three stages is a consistent, non-noise decline in wall-clock velocity** (312.8 → 162.7 → 119.9 pts/day, each stage running at roughly 40–75% of the prior stage's rate) — not attributable to one outlier stage. The likely driver, visible in the work itself rather than guessed at: each stage added genuine integration surface the next stage has to keep working (Stage 2 built the first cross-module booking flow; Stage 3 built 9 interlocking Financial Layer stories culminating in `FIN-16`'s three-module cancellation workflow, *plus* Step E's adversarial re-validation against a real, executed HTTP+Postgres stack — the first time in this project that tier of verification actually ran end-to-end rather than compiling-only). More stories/epics built ⇒ more surface a later story can regress ⇒ more verification time per subsequent story, which is exactly the dynamic a real engineering team also experiences as a codebase grows, not an artifact unique to AI-assisted pace.

**Combined actual delivery (all three stages):** 72 stories / **358 points**. Summing each stage's own active wall-clock (not the calendar span between stages, which includes idle gaps — e.g. ~20h elapsed between Stage 2's last commit and Stage 3's first): **10h17m + 14h54m + 24h38m ≈ 49h49m (≈2.08 days) of actual active work**, across 4 distinct calendar dates (2026-07-14 through 2026-07-17).

| Basis | Elapsed | Points | Velocity |
|---|---|---|---|
| Summed active wall-clock | ~49.8h (2.08 days) | 358 | 172.5 pts/day → **~1,207.7 pts/week** |
| Calendar-date count (crude) | 4 distinct dates | 358 | 89.5 pts/day → **~626.7 pts/week** |

**Revised remaining-timeline estimate:** Remaining: **390 points** (748 total − 358 done). At the summed-active-wall-clock rate, 390 points ≈ **2.3 more days of active work** (calendar-date-count basis: ≈4.4 more calendar days). Given the declining-velocity trend above, the wall-clock figure is now a **more optimistic bound, not a stable rate** — a straight-line extrapolation from Stage 3's own 119.9 pts/day (the most recent, and lowest, observed rate) is more defensible than the cumulative average: 390 / 119.9 ≈ **3.25 more days of active work** at Stage 3's demonstrated pace, before accounting for whatever the AI Layer epic's own novelty (a first real external-LLM integration, no precedent in this codebase) does to that rate in either direction.

## 7d. Stage 4 (AI Layer) completion, a fourth velocity data point, and the next-epic recommendation (Stage 4, Step D — 2026-07-18)

**What landed:** All 13 AI Layer stories (72 points) — `AI-01` (Groq client wrapper) through `AI-13` (bounded-retry latency control), built in two checkpointed batches per the same per-story discipline as prior stages. Mock-phase total is now **85/149 stories, 430/748 points (57%)**. The `ai` module went from a package-info-only stub to real content; `ads` also got its first real content (`AI-12`'s ad-creative generation, ahead of the rest of that epic).

**What building a real external LLM integration revealed:**

- **Groq's actual latency/rate-limit behavior under PRD §9.6's 10-minute itinerary target remains genuinely unvalidated against a real successful completion.** No real `GROQ_API_KEY` was available in this environment (flagged and accepted going into Stage 4 — see the Batch 1 report), so every real Groq call this stage made genuinely reached `https://api.groq.com` and genuinely 401'd; the *architecture* bounds worst-case latency (`AI-13`'s bounded retry: max 3 attempts × `adren.ai.groq.timeout-seconds` = 45s worst case before failing, well inside a 10-minute budget), but the actual multi-second-scale generation latency of a real 70B-parameter completion, and Groq's real rate-limit thresholds under concurrent load, are **not proven, only architected for**. This is the single most concrete follow-up a real key would resolve — not a redesign, a validation gap.
- **Prompt-grounding fragility is real but only unit-tested, not live-tested.** `validateAndGround`/`groundAdCreativeVariants`'s defensive handling (malformed JSON, hallucinated `supplierRateId`s, budget violations) was exercised only against hand-constructed mock Groq responses — never against a genuine model's actual tendency to wrap JSON in markdown fences, add explanatory prose despite an explicit "respond with ONLY JSON" instruction, or otherwise drift from the requested shape. The defensive posture (treat unparseable output as a grounding failure, never a crash) is architecturally sound, but its real-world trigger *frequency* is unknown until a real key is configured.
- **The most concrete, actually-validated revelation had nothing to do with AI specifically: this was the first time in the project that the full application was ever really started end-to-end** (`./gradlew bootRun`, not a module-slice test context). Step C's adversarial validation surfaced two genuine production-blocking bugs that had been silently masked since earlier stages by every test class's own `@TestConfiguration` workarounds — a missing production `WebClient.Builder` bean (the app couldn't start at all) and a Spring Security gap where any unmapped exception anywhere in the app was disguised as a misleading 401 (an existing comment in `BookingControllerAdvice` shows this exact class of bug was hit and patched case-by-case once before, without the root cause being recognized). Both are fixed now (commit `3719383`), but the lesson generalizes past AI Layer: **a real `bootRun` smoke test should be a standard verification step for future epics**, not an ad hoc adversarial add-on — module-slice test contexts and mocked-bean workarounds can hide real startup/wiring bugs indefinitely.

**Stage 4 velocity** (same method as §7a/§7c — git commit timestamps):
- Commits: `301f9ac` (2026-07-17 22:10:15) through `3719383` (2026-07-18 13:15:24, the Step C bugfix commit).
- Delivered **13 stories / 72 points** — the entire AI Layer epic — plus Step C's adversarial validation and two real production bugfixes (no story points, but real hardening work included in the elapsed time below).
- Calendar-day span: 2026-07-17 → 2026-07-18 (2 distinct dates). Raw wall-clock span: ~15h5m.
- **Methodology note, flagged rather than smoothed over:** this span contains one clear, identifiable idle gap — ~9h11m between the `AI-10` commit (01:30) and the `AI-11` commit (10:41), a genuine session break (visible in this conversation's own history as a context-compaction boundary), unlike anything explicitly excluded from Stages 1–3's reported spans. Excluding it gives an **active-only** wall-clock of ~5h54m; *not* excluding it (the same raw-span method §7a/§7c used) gives ~15h5m. Both are reported below rather than picking whichever makes the trend look better.

| Basis | Elapsed | Points | Velocity |
|---|---|---|---|
| Raw span (consistent with Stages 1–3's method) | ~15.09h | 72 | 114.6 pts/day |
| Active-only (excludes the ~9h11m session break) | ~5.91h | 72 | 292.5 pts/day |

**Updated four-stage comparison (raw-span basis, for apples-to-apples comparison with Stages 1–3):**

| Stage | Stories | Points | Wall-clock | Wall-clock pts/day |
|---|---|---|---|---|
| Stage 1 | 26 | 134 | ~10h17m | 312.8 |
| Stage 2 | 20 | 101 | ~14h54m | 162.7 |
| Stage 3 | 26 | 123 | ~24h38m | 119.9 |
| Stage 4 | 13 | 72 | ~15h5m | 114.6 |

**The decline continues but flattens sharply** — Stage 3→4 dropped only ~4.5% (119.9→114.6) versus Stage 2→3's ~26% drop and Stage 1→2's ~48% drop. A plausible reason, visible in the work itself: AI Layer is a mostly self-contained new module with exactly one integration point into an existing one (`booking`, plus `ads` for `AI-12`), unlike Financial Layer's deep interleaving across every existing module — less pre-existing surface for a new story to regress is consistent with the surface-accumulation explanation §7c already gave for the decline itself.

**Combined actual delivery (all four stages):** 85 stories / **430 points**. Summing each stage's own raw wall-clock span: 10h17m + 14h54m + 24h38m + 15h5m ≈ **64h54m (≈2.70 days)**, across 5 distinct calendar dates (2026-07-14 through 2026-07-18).

| Basis | Elapsed | Points | Velocity |
|---|---|---|---|
| Summed wall-clock | ~64.9h (2.70 days) | 430 | 159.0 pts/day → **~1,113 pts/week** |
| Calendar-date count (crude) | 5 distinct dates | 430 | 86.0 pts/day → **~602 pts/week** |

**Revised remaining-timeline estimate:** Remaining: **318 points** (748 total − 430 done). At Stage 4's own demonstrated rate (114.6 pts/day, the most recent and most defensible single-stage figure per §7c's same reasoning), 318 points ≈ **2.8 more days of active work**. At the cumulative summed-wall-clock rate (159.0 pts/day), ≈2.0 more days — a narrower gap between the two bases than Stage 3's report showed, consistent with the flattening trend above.

**Recommendation: build Local DMC + BYOS next (11 stories, 57 points).**

Verified directly against the dependency graph (every `DMC-*`/`ADS-*`/`HRD-*` story's `dependencies:` frontmatter), not just cited from PRD §8's stated order:

1. **Local DMC + BYOS is fully unblocked — all 11 stories buildable top-to-bottom right now.** Every dependency resolves to `FND-*` (Foundation, done), `BOK-16` (Booking Core, done), or an earlier `DMC-*` story in its own chain (`DMC-01→02→{03→10→11, 04→05}`, `DMC-06→07→08→09`). Zero cross-epic blockers.
2. **Ads/Campaign Management is genuinely blocked, 13 of its 15 stories deep.** `ADS-01`/`ADS-02` are unblocked, but `ADS-03` depends on `FES-08` ("adopt React Hook Form + Zod as the form validation standard") — confirmed **not done** (`PROGRESS.md`: only `FES-01`/`FES-03` are checked off) — and everything from `ADS-04` through `ADS-15` chains off `ADS-03`. `AI-12` (this stage's own work) already unblocked `ADS-04` specifically, but that was never the epic's only gate.
3. **Hardening is partially blocked** — 9 of 13 stories (`HRD-02` through `HRD-08`, `HRD-12`, `HRD-13`) are unblocked now that Booking Core/Financial Layer/AI Layer are all done, but `HRD-09` depends on `ADS-09` (Ads/Campaign, not done), and `HRD-10`/`HRD-11` chain off `HRD-09` — so Hardening can't fully close without Ads/Campaign finishing first either.
4. **This also matches PRD §8's stated order** ("AI layer → Local DMC+BYOS → Ads/Campaign → Hardening") — not a re-derivation, confirmation that the derived order and the PRD's own intent agree, same cross-check §7b already made for AI Layer.

Local DMC + BYOS is self-contained (adds a new supplier channel + BYOS credential management, PRD §10.2.9/§13) and doesn't block anything else discovered in this check — a clean, low-risk next target. **Stopping here per Step D's own instruction — not starting Local DMC + BYOS without an explicit go-ahead.**

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

Per-epic story counts/points independently cross-checked against the Summary tables in `mvp-mock-stories.md` and `production-stories.md` as of Stage 1 — they matched exactly then. **Known gap as of Stage 3, Step B:** `mvp-mock-stories.md` and `mvp-mock-stories.csv` (the consolidated single-file rollups) were **not** updated with `BOK-21`–`BOK-27` — only the canonical per-file stories under `doc/user-stories/mvp-mock/` and this document were. The per-file stories are the source of truth `PROGRESS.md` tracks against; the consolidated rollups are now stale by 7 stories/23 points until someone syncs them.
