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

`doc/user-stories/mvp-mock/PROGRESS.md` (created Stage 1) is the source of truth for per-story status (§7's own note). This section's checklist was last actually validated end-to-end on **2026-07-21** (mock-complete DoD validation, Stage 9) — results below, reported honestly rather than rounded up to a clean pass:

- [x] **All 149 mock-phase stories are done** per `PROGRESS.md` (149/149, 748/748 pts, §7).
- [~] **Every module has moved off "package-info stub"** — `ai`/`payments`/`whitelabel`/`ads` all have real `Api` implementations; `compliance` is still a package-info stub. **Not a gap**, on inspection: no mock-phase story ever targeted `compliance` (PRD §17's Regional Compliance is entirely a production-phase, `CMP-*` concern) — this checklist item's own wording was too broad and is corrected here rather than silently left to imply a missing story.
- [x] **`./gradlew check` passes, including `ModularityTests.verify()`** — confirmed repeatedly through Stage 9, including after two new test files each initially introduced (and were caught by) a real module-boundary violation before being fixed.
- [~] **Flow A/B/C verified end-to-end, across ≥2 product types and one white-label path** — real, but split findings, not a clean pass:
  - **Backend chain (search→itinerary→quotation→package→publish→booking→payment→voucher→notification→wallet) is fully verified for both required product types** — `FullVerticalSliceEndToEndIT` (Hotel-only, INDIA) and the new `MockCompleteDoDValidationIT` (UK dynamic Flight+Hotel combo, exercising BOK-11's real ATOL gate) — plus a white-label-themed path (real branding PATCH+GET). All pass reliably over real HTTP against real Postgres.
  - **A genuine, previously-undiscovered gap surfaced along the way:** `VoucherService.generateFor` always persists a `null` `atolCertificateReference`, even for a real UK dynamic combo booking that completed ATOL disclosure — its own comment claims no Flight line item type exists yet, which is stale (`FlightLineItem` has existed since `BOK-04`). Flagged for a product/scope decision, not silently fixed.
  - **The frontend cannot walk Flow B/C end-to-end via real UI.** `ItineraryBuilder` never got a "Save as Quotation" action (no Quotations-list screen either), and — more fundamentally — **no REST endpoint anywhere creates a new `Itinerary` row**; every booking endpoint assumes one already exists (`new Itinerary(...)` is only ever constructed in test code). `BOK-13`'s frontend (Flow C — traveler details/payment/confirmation) was deferred and never built; `BookingPaymentFlow.tsx` is still a bare placeholder (already honestly tracked in `PROGRESS.md`'s own `BOK-13` line). All 10 Playwright e2e specs pass, but they test what's real today (Flow A fully, Flow B's first leg, Flow C's placeholder), not a fabricated full UI chain over these gaps.
- [x] **The `⚠️ Reconcile` items in `RULES.md` §9 blocking real usage are closed** — re-verified directly against current code, not assumed from the backlog's own stale text: `@Transactional` is present on every state-mutating `BookingServiceImpl` method (item 1's backlog line was itself stale, now corrected), and method-level `@PreAuthorize` + tenant-scoped authorization are real and enforced (item 2).
- [x] **`doc/architecture/` has a generated PlantUML diagram set** — 12 diagrams, generated via `./gradlew updateModuleDocs` (`OPS-08`).

**Verdict: mock phase is feature-complete (149/149 stories) and its backend is genuinely, verifiably end-to-end functional across both required product types and a white-label path. It is not "zero-gap"** — one real, product-relevant gap (ATOL certificate reference) and two structural frontend gaps (no itinerary-persist UI action, Flow C's screen unbuilt) remain, all now precisely documented rather than newly-hidden. Whether these block calling the mock phase "done enough to move on" is a product decision, not a testing one — flagged for that decision rather than made unilaterally here.

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

**Source-of-truth note (added Stage 8 Step D-retroactive, 2026-07-21):** this section had gone stale — it still read "Stage 7 — 76%" after Stage 8 (Ads/Campaign Management + Hardening's tail) had already landed and been merged, because the epic that closed it out never got a completion write-up here. Going forward, **`doc/user-stories/mvp-mock/PROGRESS.md`'s per-story checkboxes are the source of truth.** If this table and `PROGRESS.md` ever disagree, `PROGRESS.md` is correct and this table is the one that's stale and needs refreshing — not the other way around. `PROGRESS.md` should still be updated per-story as each one closes (unchanged practice); this table should be refreshed at the end of every stage, not left to drift until someone notices.

### Mock phase (149 stories / 748 points, updated Stage 9 — 2026-07-21)

| Epic | Stories | Points | Status |
|---|---|---|---|
| Foundation | 24 | 124 | **100% (24/24)** |
| Booking Core | 27 | 121 | **100% (27/27)** — completed Stage 3 Batch 2 |
| Financial Layer | 18 | 95 | **100% (18/18)** — completed Stage 3 Batch 2 |
| AI Layer | 13 | 72 | **100% (13/13)** — completed Stage 4 |
| Local DMC + BYOS | 11 | 57 | **100% (11/11)** — completed Stage 5 |
| Ads/Campaign Management | 15 | 80 | **100% (15/15)** — completed Stage 8 |
| Hardening | 13 | 76 | **100% (13/13)** — completed Stage 8 (`HRD-09/10/11` closed the tail once `ADS-09` landed) |
| Frontend Shell | 10 | 55 | **100% (10/10)** — completed Stage 7 |
| DevOps/Infra | 9 | 30 | **100% (9/9)** — completed Stage 9 |
| Test Infrastructure | 9 | 38 | **100% (9/9)** — completed Stage 9 |
| **Total** | **149** | **748** | **100% (149/149 stories, 748/748 pts)** |

**Mock phase is feature-complete as of Stage 9 (2026-07-21).** See §7i for what Stage 9 actually found/fixed along the way (several genuine, previously-undiscovered gaps — not just infra/test scaffolding), and §5's Definition of Done checklist for the mock-complete validation this completeness figure still needs before treating the phase as truly done, not just "every story's checkbox is ticked."

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

## 7e. Stage 5 (Local DMC + BYOS) completion, a fifth velocity data point, and the next-epic recommendation (Stage 5, Step D — 2026-07-18)

**What landed:** All 11 Local DMC + BYOS stories (57 points) across two checkpointed batches — Batch 1 (`DMC-01, 02, 03, 04, 05, 10, 11` — Local DMC onboarding, vetting, CSV bulk-upload, quality-signal tracking, threshold flagging, per-item inventory editing, staleness alerting; 34 points) and Batch 2 (`DMC-06, 07, 08, 09` — BYOS credential entry, credential-source-agnostic supplier layer, search-merge, tenant scoping; 23 points). Mock-phase total is now **96/149 stories, 487/748 points (64%)**.

**What this stage revealed, flagged rather than smoothed over:**

- **A large fraction of Batch 2's foundational work already existed before this stage started.** `FND-12` (row-level KMS-encrypted BYOS credential storage — `ByosCredential` entity, `KmsEnvelopeEncryptionService`, `ByosCredentialService#save/read`, a full Testcontainers cross-tenant-denial test) had been built in an earlier stage and never wired into a public API, REST endpoint, or the search fan-out — DMC-06 through DMC-09 were substantially "build the surface on top of an existing, already-correct foundation" rather than new mechanism from scratch. This is the single biggest driver of this stage's velocity figure below, and is exactly the kind of thing a raw points/time number hides unless stated explicitly.
- **Local DMC's own schema-upfront discipline (established in Stage 5 Batch 1's `DMC-03` commit) paid off directly in `DMC-10`.** `LocalDmcInventoryItem.update(...)` was already fully built, anticipating the exact CRUD story that came 4 commits later — `DMC-10`'s actual new work was three thin delegation layers (service → API → controller) plus a domain event and frontend, not a new mutation method.
- **Testcontainers-backed integration tests remain non-executable in this sandbox** (confirmed pre-existing, not something this stage broke — an untouched, already-existing test file, `CreditLimitBreachIT`, fails with the identical `docker-java` API-version-negotiation error against this environment's Rancher-Desktop-backed Docker daemon: "client version 1.32 is too old, minimum supported API version is 1.41"). Every Testcontainers-tier test this stage wrote (`ByosCredentialCrossTenantIT`'s DMC-06/DMC-09 additions) is code-complete and compiles clean but unverified end-to-end in this environment; the mocked-KMS unit tier and the real-Postgres `@ApplicationModuleTest` tier (`SupplierModuleIntegrationTests`) carried the actual verification load instead. Worth a dedicated OPS-tier fix (Docker client/API-version compatibility) before a future stage that depends more heavily on this tier.

**Stage 5 velocity** (same method as §7a/§7c/§7d — git commit timestamps, `AD-stage5-local-dmc-byos` branch):
- Commits: `4f079c8` (2026-07-18 14:05:25) through `99a6340` (2026-07-18 16:12:06).
- Delivered **11 stories / 57 points** — the entire Local DMC + BYOS epic.
- Calendar-day span: 2026-07-18 only (1 distinct date). Wall-clock elapsed: ~2h7m.

| Basis | Elapsed | Points | Velocity |
|---|---|---|---|
| Wall-clock hours | ~2.11h (0.088 days) | 57 | 648.1 pts/day → **~4,537 pts/week** |

**⚠️ This is not a genuine 5.7x acceleration over Stage 4 — treat it as an outlier explained by the pre-existing-foundation effect above, not a new sustainable rate.** Unlike Stages 1–4's steady decline (312.8 → 162.7 → 119.9 → 114.6 pts/day), Stage 5 reverses the trend sharply upward, and the reason is identifiable in the work itself (FND-12 was already built) rather than a genuine improvement in this stage's own build discipline. Averaging it into the cumulative rate below would materially overstate what to expect from a future epic that doesn't have a comparable head start.

**Combined actual delivery (all five stages):** 96 stories / **487 points**. Summing each stage's own raw wall-clock span: 10h17m + 14h54m + 24h38m + 15h5m + 2h7m ≈ **67h1m (≈2.79 days)**, across 5 distinct calendar dates (2026-07-14 through 2026-07-18, Stage 4 and Stage 5 sharing 2026-07-18).

| Basis | Elapsed | Points | Velocity |
|---|---|---|---|
| Summed wall-clock | ~67.0h (2.79 days) | 487 | 174.7 pts/day → **~1,223 pts/week** |
| Calendar-date count (crude) | 5 distinct dates | 487 | 97.4 pts/day → **~682 pts/week** |

**Revised remaining-timeline estimate:** Remaining: **261 points** (748 total − 487 done). Given Stage 5's figure is flagged above as an outlier, Stage 4's own demonstrated rate (114.6 pts/day, per §7d's reasoning — the most recent *non-outlier* single-stage figure) remains the more defensible basis: 261 / 114.6 ≈ **2.3 more days of active work**. The cumulative summed-wall-clock rate (174.7 pts/day, inflated by Stage 5) gives a more optimistic ≈1.5 days — reported for completeness, not recommended as the planning number.

**Recommendation: build Frontend Shell next (8 remaining stories, 45 points), not the PRD §8-stated Ads/Campaign Management.**

Verified directly against the dependency graph (every `FES-*`/`ADS-*`/`HRD-*`/`TST-*` story's `dependencies:` frontmatter), not just cited from PRD §8:

1. **Ads/Campaign Management is still genuinely blocked, exactly as §7d already found before Local DMC + BYOS was built.** `ADS-01`/`ADS-02` are unblocked, but `ADS-03` depends on `FES-08` ("adopt React Hook Form + Zod as the form validation standard") — confirmed **still not done** (`PROGRESS.md`: only `FES-01`/`FES-03` checked off, unchanged since §7d) — and `ADS-04` through `ADS-15` chain off `ADS-03`. Local DMC + BYOS finishing did not touch this blocker at all; it was never on the blocking path.
2. **Frontend Shell's remaining 8 stories (`FES-02, 04, 05, 06, 07, 08, 09, 10`) are fully unblocked, buildable top-to-bottom, right now.** Every dependency resolves to `FND-*` (Foundation, 100% done) or an earlier `FES-*` story in its own chain (`FES-04→FES-08→FES-09`) — verified by reading all 10 `FES-*` files' `dependencies:` frontmatter directly, not inferred.
3. **Building it unblocks two other epics at once, not just one.** `FES-08` is `ADS-03`'s specific gate (per point 1) — closing it reopens the PRD-stated next epic. It's also `TST-04`'s gate (`TST-04` depends on `FES-08`; `TST-05` chains off `TST-04`) — Test Infrastructure is otherwise still 0/9 and partially blocked by the same story. No other single epic in this catalogue unblocks two others simultaneously.
4. **This deviates from PRD §8's literal epic-label order** ("... → Local DMC+BYOS → Ads/Campaign → Hardening" — Frontend Shell isn't named as a phase in that list at all), but not from its intent: PRD §8 describes feature-epic sequencing, not the tooling/scaffolding work those epics assume already exists. §2's own derived build order already treats `Frontend Shell` as interleaved with `Foundation` at the start, not a separate later phase — the same "this is infrastructure the named epics depend on, not a competing epic" reasoning applies here to its remaining 8 stories.
5. **Hardening (9/13 stories: `HRD-02` through `HRD-08`, `HRD-12`, `HRD-13`) is the other fully-available option** — genuinely unblocked now that Booking Core/Financial Layer/AI Layer/Local DMC+BYOS are all done, and could run as a parallel track if a second work-stream exists. It's not the primary recommendation because it doesn't unblock anything else (`HRD-09/10/11` still need `ADS-09`, so even finishing all 9 available Hardening stories leaves the epic at 10/13, still gated on Ads/Campaign) — lower leverage than Frontend Shell's double-unblock.

**Stopping here per Step D's own instruction — not starting Frontend Shell without an explicit go-ahead.**

## 7f. Stage 6 (Hardening) completion, a sixth velocity data point, and the next-epic recommendation (Stage 6, Step D — 2026-07-19)

**What landed:** 9 of Hardening's 13 stories (49 points) across two checkpointed batches — Batch 1 (`HRD-02, 03, 04, 06` — trigger-event notification wiring, listener idempotency, the preferences screen, dispute ticketing; 23 points) and Batch 2 (`HRD-07, 08, 05, 12, 13` — PNR/booking search backend+frontend, full cancellation-workflow notification, config-driven sync cadence, sync-staleness alerting; 26 points). Combined with `HRD-01` (already done in Stage 2), Hardening is now **10/13 stories, 57/76 points (77%)**. Mock-phase total is now **105/149 stories, 536/748 points (70%)**.

**What this stage revealed, flagged rather than smoothed over:**

- **This epic's own name undersells how much of it was "prove a pre-existing thing actually works" rather than new mechanism** — the same pattern §7e flagged for Stage 5's `FND-12`. `HRD-05`'s literal scope ("implement the full cancellation workflow") turned out to already exist: `BookingServiceImpl.submitCancellation`/`approveCancellation` had fully orchestrated FIN-16's policy-check → refund-calculation → approval-if-needed → refund state machine in an earlier stage, and `BookingCancelledEvent`'s own pre-existing Javadoc had already flagged the one missing piece (notification dispatch) as HRD-05's actual job. `HRD-12`'s literal scope ("make sync cadence config-driven") was likewise already done — BOK-27's `@Scheduled(cron = "${...:default}")` placeholders were already operator-tunable; the real remaining work for both stories was proving the acceptance criteria true (new tests), not building new mechanism.
- **Step C's adversarial validation targeted the epic's dominant theme — idempotency under real redelivery — and caught a real flaw in the validation itself before it caught anything in the code.** The first version of the before/after test passed even with `BookingCancelledNotificationListener`'s dedup guard deliberately disabled, because the manually-redelivered event still routed through the `@Async` proxy onto its own thread, and the assertion ran before that second dispatch landed — a false pass that would have shipped a broken adversarial test. Fixed with an explicit `Awaitility` poll delay; re-ran against the disabled guard and got a genuine failure (`Expected size: 1 but was: 2`), then confirmed green again with the guard restored. Worth remembering for any future adversarial test that manually re-invokes an `@Async`-annotated bean method through its Spring-proxied reference: an immediate post-call assertion is not evidence of anything.
- **Testcontainers remains non-executable in this sandbox** (same identified, pre-existing Docker API-version-negotiation gap as Stages 3/5 — `client version 1.32 too old, minimum 1.41`) — confirmed again this stage via `HRD-12`'s new `SupplierContentSyncCadenceIT`, which compiles clean and fails identically to the untouched `CreditLimitBreachIT`, not a regression. The real-Postgres `@ApplicationModuleTest` tier (used for every other new integration test this stage, including a from-scratch one added for `HRD-13`'s new JPA column) continues to carry the actual verification load in this environment.

**Stage 6 velocity** (same method as §7a–§7e — git commit timestamps, `AD-stage6` + `AD-stage6-batch2-hardening-search-cancellation-sync` branches):
- Commits: `aba6c04` (2026-07-19 11:36:28) through `74c37ed` (2026-07-19 13:48:34, Step C's before/after commit).
- Delivered **9 stories / 49 points** — Batch 1 + Batch 2 of Hardening — plus Step C's adversarial validation (no story points).
- Calendar-day span: 2026-07-19 only (1 distinct date). Wall-clock elapsed: ~2h12m.

| Basis | Elapsed | Points | Velocity |
|---|---|---|---|
| Wall-clock hours | ~2.20h (0.092 days) | 49 | 534.1 pts/day → **~3,739 pts/week** |

**⚠️ Another outlier, same driver as Stage 5's — not a new sustainable rate.** Two of Batch 2's five stories (`HRD-05`, `HRD-12`) were substantially pre-existing mechanism plus new test coverage, per the finding above — materially less new-code volume per point than a from-scratch story. Treat this stage's raw velocity the same way §7e treated Stage 5's: informative about *why* it was fast, not a number to extrapolate from.

**Combined actual delivery (all six stages):** 105 stories / **536 points**. Summing each stage's own raw wall-clock span: 10h17m + 14h54m + 24h38m + 15h5m + 2h7m + 2h12m ≈ **69h13m (≈2.88 days)**, across 6 distinct calendar dates (2026-07-14 through 2026-07-19).

| Basis | Elapsed | Points | Velocity |
|---|---|---|---|
| Summed wall-clock | ~69.2h (2.88 days) | 536 | 185.9 pts/day → **~1,301 pts/week** |
| Calendar-date count (crude) | 6 distinct dates | 536 | 89.3 pts/day → **~625 pts/week** |

**Revised remaining-timeline estimate:** Remaining: **212 points** (748 total − 536 done). Stage 4's own demonstrated rate (114.6 pts/day) remains the most defensible non-outlier single-stage basis, per §7d/§7e's same reasoning (both Stage 5 and Stage 6 are flagged outliers above): 212 / 114.6 ≈ **1.85 more days of active work**. The cumulative summed-wall-clock rate (186.1 pts/day, inflated by two outlier stages now) gives a more optimistic ≈1.14 days — reported for completeness, not recommended as the planning number.

**Epic-completion flag, per the standing Step D instruction:** Hardening is **not** among the completed epics — 10/13 stories, 3 remaining (`HRD-09/10/11`, 19 points) genuinely blocked on `ADS-09` (Ads/Campaign, still 0/15). So the trigger condition ("Booking Core, Financial Layer, AI Layer, Local DMC+BYOS, Ads/Campaign, and Hardening all complete") is **not met** — Ads/Campaign hasn't started, and Hardening can't finish without it. What's actually true now: **every mock-phase story not gated on Ads/Campaign is done.** The only path to closing out Hardening, Ads/Campaign, and (per point 3 below) part of Test Infrastructure runs through the same single blocker.

**Recommendation: build Frontend Shell next (8 remaining stories, 45 points) — same recommendation as §7e, now with Hardening's available scope exhausted.**

Re-verified directly against the dependency graph, not carried over unchecked from §7e:

1. **Ads/Campaign Management is still genuinely blocked on exactly the same story.** `ADS-03` depends on `FES-08` ("adopt React Hook Form + Zod as the form validation standard") — confirmed **still not done** (`PROGRESS.md`: only `FES-01`/`FES-03` checked off, unchanged since §7d/§7e) — and `ADS-04` through `ADS-15` chain off it. Hardening's Batch 1/2 work did not touch this blocker.
2. **Frontend Shell's remaining 8 stories are still fully unblocked, buildable top-to-bottom, right now** — same finding as §7e, re-checked: every dependency resolves to `FND-*` (done) or an earlier `FES-*` story in its own chain.
3. **Building `FES-08` specifically unblocks three things at once, not two.** It's `ADS-03`'s gate (point 1) *and* `TST-04`'s gate (`TST-04` depends on `FES-08`; `TST-05` chains off `TST-04`) *and*, transitively, `HRD-09/10/11`'s gate (`HRD-09` depends on `ADS-09`, which is downstream of `ADS-03`) — closing one story reopens paths in three separate epics (Ads/Campaign, Test Infrastructure, and the last of Hardening). No other single remaining story in the catalogue has that reach.
4. **DevOps/Infra (9 stories, 30 points) is also fully unblocked right now** (every `OPS-*` dependency resolves to `FND-*`/earlier `OPS-*`, all done) and **7 of Test Infrastructure's 9 stories (32 of 38 points)** are unblocked too (`TST-01/02/03/06/07/08/09`; only `TST-04/05` wait on `FES-08`) — both are legitimate parallel-track candidates if a second work-stream exists, matching the user's own framing that remaining scope is "primarily Frontend Shell, DevOps/Infra, and Test Infrastructure." Frontend Shell remains the primary recommendation over these two because of point 3's triple-unblock; DevOps/Infra and Test Infrastructure don't unblock anything else.

**Once Ads/Campaign is unblocked and built out, Hardening's final 3 stories (`HRD-09/10/11`, 19 points) become buildable and should be swept up as a short closing batch** — they're Consultant/Super Admin dashboard screens with no further hidden dependencies beyond `ADS-09`.

**Stopping here per Step D's own instruction — not starting Frontend Shell without an explicit go-ahead.**

## 7g. Stage 7 (Frontend Shell) completion, a seventh velocity data point, and the next-epic recommendation (Stage 7, Step D — 2026-07-19)

**What landed:** All 8 remaining Frontend Shell stories (45 points) across two checkpointed batches — Batch 1 (`FES-02, 04, 05` — provider-stack slot, shared UI primitives, shared map/results layout; 16 points) and Batch 2 (`FES-08, 06, 07` — react-hook-form+zod adoption, runtime white-label theming, auth/session route guards; 18 points) — plus a corrective Batch 3 (`FES-09, 10` — schema-driven onboarding field engine, global toast queue; 11 points) after an initial completion report incorrectly claimed the epic was fully done when only Batches 1–2 had actually shipped; caught and fixed within the same stage rather than left standing. **Frontend Shell is now 10/10 stories, 55/55 points (100%).** Mock-phase total is now **113/149 stories, 581/748 points (76%)**.

**What this stage revealed, flagged rather than smoothed over:**

- **The "pre-existing foundation, just needs proving/extracting" pattern (§7e/§7f) showed up a third time, across half this stage's stories.** `FES-02` was fully built in Stage 1. `FES-06`'s CSS-custom-property runtime-theming mechanism already existed; only the actual fetch-from-FND-06's-real-endpoint provider was missing (a stale code comment claimed no backend endpoint existed — FND-06/FND-07 had shipped one since). `FES-09`'s data-driven KYC field resolution already worked end-to-end in `ConsultantOnboardingWizard`; the story's own Developer Notes framed the real remaining work as *extracting* it into a reusable engine, which is what actually happened. This is the same dynamic Stage 5 and Stage 6 both hit — worth treating as a standing expectation for future stages, not a one-off surprise.
- **A real, load-bearing bug was caught by the story's own test, not shipped and found later.** `FES-10`'s toast auto-dismiss test failed on first write: `ToastItem`'s dismiss timer was keyed to an inline arrow-function prop, so queuing a *second* toast silently reset the *first* toast's already-ticking timer (new callback identity on every `ToastContainer` render re-triggers the effect). Fixed by reading the store's `removeToast` action directly (a stable Zustand reference) instead of threading a fresh callback through props each render. This is exactly the class of bug a shallow "renders successfully" test would have missed — the fix only exists because the test asserted the *actual auto-dismiss timing*, not just presence.
- **This stage also included a real self-correction.** The first Batch 2 completion report to the user claimed "Frontend Shell is now 8/10 stories, 45/45 remaining points — fully done," which was wrong on the remaining-points count (`FES-09`/`FES-10`, 11 points, hadn't been started) — caught on the very next turn while preparing this Step D section, not by the user. Flagging this here rather than silently fixing it, since the accuracy of these completion claims is exactly what `phases.md`'s tracking is for.

**Stage 7 velocity** (same method as §7a–§7f — git commit timestamps, `AD-stage7-frontend-shell` branch):
- Commits: `29288f4` (2026-07-19 14:36:46) through `054a4df` (2026-07-19 16:43:54).
- Delivered **8 stories / 45 points** — the entire remainder of Frontend Shell.
- Calendar-day span: 2026-07-19 only (same date as Stage 6). Wall-clock elapsed: ~2h7m.

| Basis | Elapsed | Points | Velocity |
|---|---|---|---|
| Wall-clock hours | ~2.12h (0.088 days) | 45 | 509.6 pts/day → **~3,567 pts/week** |

**⚠️ A third consecutive outlier, same driver as Stages 5 and 6 — not a new sustainable rate.** Roughly half this stage's stories (`FES-02`, large parts of `FES-06`/`FES-09`) were substantially pre-existing mechanism needing tests/extraction rather than new builds, per the finding above. Averaging this into the cumulative rate would overstate what to expect from a story that doesn't have that head start — same caveat §7e and §7f both already gave.

**Combined actual delivery (all seven stages):** 113 stories / **581 points**. Summing each stage's own raw wall-clock span: 10h17m + 14h54m + 24h38m + 15h5m + 2h7m + 2h12m + 2h7m ≈ **71h20m (≈2.97 days)**, across 6 distinct calendar dates (2026-07-14 through 2026-07-19, Stage 6 and Stage 7 sharing 2026-07-19).

| Basis | Elapsed | Points | Velocity |
|---|---|---|---|
| Summed wall-clock | ~71.3h (2.97 days) | 581 | 195.5 pts/day → **~1,368 pts/week** |
| Calendar-date count (crude) | 6 distinct dates | 581 | 96.8 pts/day → **~678 pts/week** |

**Revised remaining-timeline estimate:** Remaining: **167 points** (748 total − 581 done). Stage 4's own demonstrated rate (114.6 pts/day) remains the most defensible non-outlier single-stage basis — three of the last four stages are now flagged outliers, which is itself worth noting rather than continuing to average them in: 167 / 114.6 ≈ **1.46 more days of active work**. The cumulative summed-wall-clock rate (195.5 pts/day, now inflated by three outlier stages) gives a more optimistic ≈0.85 days — reported for completeness, not recommended as the planning number.

**Epic-completion flag, per the standing Step D instruction:** the trigger ("Booking Core, Financial Layer, AI Layer, Local DMC+BYOS, Ads/Campaign, and Hardening all complete") is **still not met** — Ads/Campaign is still 0/15 and Hardening still has 3 stories pending. But this stage changes the picture materially: **Ads/Campaign Management is now fully unblocked, top to bottom** — verified directly against every `ADS-*` story's `dependencies:` frontmatter, not inferred. `ADS-03` was the epic's sole remaining gate (`["ADS-02", "BOK-12", "FES-08"]` — `ADS-02` was already unblocked, `BOK-12` has been done since Stage 3, and `FES-08` just landed in this stage's Batch 2); every story from `ADS-04` through `ADS-15` chains off `ADS-03` and has no other blocker. Once Ads/Campaign is built, `HRD-09/10/11` (Hardening's last 3 stories, 19 points) become buildable too, closing that epic out completely.

Per the user's own standing instruction, the estimate for finishing the non-Ads/Campaign remainder (Frontend Shell is now done; DevOps/Infra and Test Infrastructure are what's left of that original three-epic list) rather than waiting to be asked: **DevOps/Infra (9 stories, 30 points, fully unblocked) + Test Infrastructure (9 stories, 38 points, 7 already unblocked and the remaining 2 — `TST-04/05` — unblocked as of this stage) = 68 points ≈ 0.6 days at Stage 4's demonstrated rate.** Combined with Ads/Campaign (80 points) and Hardening's tail (19 points), the full remaining 167 points ≈ 1.46 days at that same rate — consistent with the total already given above, since these are the only pieces left.

**Recommendation: build Ads/Campaign Management next (15 stories, 80 points)** — not Frontend Shell (now done), and not DevOps/Infra/Test Infrastructure, even though both are also fully or mostly unblocked:

1. **It's the PRD §8-stated order, unblocked for the first time this project.** §8's release order ("... → Local DMC+BYOS → Ads/Campaign → Hardening") named Ads/Campaign as next after Local DMC+BYOS three stages ago (§7d); it stayed blocked through Stages 5 and 6 purely on `FES-08`, which is now done — this is the first stage where building it top-to-bottom is actually possible.
2. **It's the highest-leverage remaining epic — finishing it closes out Hardening too.** `HRD-09/10/11`'s only blocker is `ADS-09`. No other remaining epic (DevOps/Infra, Test Infrastructure) unblocks anything else in the catalogue.
3. **DevOps/Infra and Test Infrastructure remain legitimate parallel-track candidates** (both fully or mostly unblocked, 30 and 38 points respectively) if a second work-stream is available, but neither has Ads/Campaign's downstream unblock value.

**Stopping here per Step D's own instruction — not starting Ads/Campaign Management without an explicit go-ahead.**

## 7h. Stage 8 (Ads/Campaign Management + Hardening's tail) completion, an eighth velocity data point, and the final-68-points projection (retroactive write-up, 2026-07-21)

**Why this is retroactive:** Stage 8 (branch `AD-stage8-ads-campaign-management`) closed out both Ads/Campaign Management (15/15) and, by extension, Hardening's last 3 stories (`HRD-09/10/11`) — but no Step D report was written at the time, which is exactly how this table went stale (§7's source-of-truth note above). Reconstructed here from git history rather than left undocumented.

**What landed:** All 15 Ads/Campaign Management stories (80 points, `ADS-01` through `ADS-15`), each committed individually per the established per-story discipline, followed after a session gap by `HRD-09/HRD-10/HRD-11` (Consultant + Super Admin Dashboards, 19 points) in **one combined commit** — a deviation from every prior stage's one-commit-per-story practice, flagged below. Mock-phase total is now **131/149 stories, 680/748 points (88%)**. Ads/Campaign Management and Hardening are both **100% complete** — the last two epics with open stories before this stage.

**What this stage revealed, flagged rather than smoothed over:**

- **`HRD-09/10/11` were committed as a single squashed commit** (`a22a02a`), not three per-story commits like every other closed-out epic in this catalogue. This has two consequences: (1) it breaks the "one commit per story, `PROGRESS.md` updated per commit" discipline every prior stage followed, which is worth restoring for Stage 9 rather than repeating; (2) it means this stage's velocity figure can't be cleanly split into a per-story or even a "which day was this dashboard work actually done on" breakdown the way Stages 1–7 could — see the data-quality caveat below.
- **A genuine ~38h25m idle gap sits between `ADS-15` (2026-07-19 23:17:10) and `HRD-09/10/11` (2026-07-21 13:42:34)** — a real session break (2026-07-20 has zero commits), the same kind of gap Stage 4 identified and excluded from its "active-only" figure. Unlike Stage 4's gap, though, this one can't be cleanly excluded here: because `HRD-09/10/11` is a single commit with no intermediate timestamps, there's no way to tell how much of the 2026-07-21 session before 13:42:34 was actually spent on these 3 stories versus something else — an "active-only" figure for this stage would be a guess dressed up as a measurement, not a real exclusion like Stage 4's. Reporting raw span only for this stage, with this limitation stated rather than papered over with a fabricated active-only number.
- **A small non-story commit followed immediately after** (`eb35f9c`, 2026-07-21 13:43:05, "Add a local-dev seed Consultant + dev-only token-minting endpoint for local login") — dev-convenience tooling, not a catalogue story, excluded from points/velocity math here.

**Stage 8 velocity** (same method as §7a–§7g — git commit timestamps, `AD-stage8-ads-campaign-management` branch):
- Commits: `6d29aa0` (2026-07-19 17:18:06, `ADS-01`) through `a22a02a` (2026-07-21 13:42:34, `HRD-09/10/11`).
- Delivered **18 stories / 99 points**: `ADS-01`–`ADS-15` (15 stories, 80 pts) + `HRD-09/10/11` (3 stories, 19 pts).
- Calendar-day span: 2026-07-19 → 2026-07-21 (3 distinct dates, but zero commits on 2026-07-20 — a real idle day, not just a quiet one). Raw wall-clock elapsed: ~44h24m28s.
- Sub-span for the cleanly-measurable portion (`ADS-01`→`ADS-15` only, all individually committed): 2026-07-19 17:18:06 → 23:17:10 = **~5h59m4s for the full 80-pt Ads/Campaign epic**, a genuine, trustworthy figure since it's per-story-committed like Stages 1–7.

| Basis | Elapsed | Points | Velocity |
|---|---|---|---|
| Raw span, full stage (18 stories) | ~44.41h (1.85 days) | 99 | 53.5 pts/day |
| `ADS-01`–`15` only (clean, per-story-committed) | ~5.98h | 80 | 320.9 pts/day |

**⚠️ Neither figure above should anchor the remaining-points projection.** The raw-span figure (53.5 pts/day) is deflated by the 38h25m idle gap the same way an un-excluded Stage 4 span would have been (§7d already showed that pattern: 114.6 pts/day raw vs. 292.5 pts/day active-only, same underlying stage). The Ads-only figure (320.9 pts/day) is inflated the same way Stages 5–7 were flagged as outliers — no evidence either way here since Ads/Campaign was largely new mechanism (a whole new module), but it's a single-stage figure with no cross-check, and it excludes the Hardening tail entirely. **Stage 4's 114.6 pts/day remains the most defensible non-outlier basis** — it's now the *only* one of the last five stages (4, 5, 6, 7, 8) not flagged for either a pre-existing-foundation inflation effect or a data-quality/idle-gap issue, per §7d/§7e/§7f/§7g's own reasoning plus this section's findings.

**Combined actual delivery (all eight stages):** 131 stories / **680 points** — matches `PROGRESS.md`'s 131/149 exactly, a useful cross-check that this reconstruction is consistent with the per-story tracker. Summing each stage's own raw wall-clock span: 10h17m + 14h54m + 24h38m + 15h5m + 2h7m + 2h12m + 2h7m + 44h24m28s ≈ **115h44m28s (≈4.82 days)**, across 7 distinct calendar dates (2026-07-14 through 2026-07-19, plus 2026-07-21 — 2026-07-20 was fully idle).

| Basis | Elapsed | Points | Velocity |
|---|---|---|---|
| Summed raw wall-clock | ~115.7h (4.82 days) | 680 | 141.0 pts/day → **~987 pts/week** |
| Calendar-date count (crude, 7 dates) | 7 distinct dates | 680 | 97.1 pts/day → **~680 pts/week** |

**Final-68-points projection (`DevOps/Infra` 30 pts + `Test Infrastructure` 38 pts, 18 stories):** using Stage 4's 114.6 pts/day (the basis this document has consistently favored since §7d, for the reasons restated above): **68 / 114.6 ≈ 0.59 days ≈ 14.2 hours of active work.** The cumulative summed-raw-wall-clock rate (141.0 pts/day, now carrying two idle-gap-deflated stages) gives a more optimistic ≈0.48 days (≈11.6h) — reported for completeness, not recommended as the planning number, same convention as every prior stage's report.

**Epic-completion flag, per the standing Step D instruction:** the original trigger ("Booking Core, Financial Layer, AI Layer, Local DMC+BYOS, Ads/Campaign, and Hardening all complete") is **now met** — all six are 100%. What's left of the mock phase is exactly the two epics §7f/§7g already identified as having no further leverage over anything else: `DevOps/Infra` (9 stories, 30 pts) and `Test Infrastructure` (9 stories, 38 pts), 68 points / 18 stories total, both fully unblocked.

**Recommendation (already the user's own stated plan): `DevOps/Infra` before `Test Infrastructure`.** Checked directly against dependency chains rather than assumed: `OPS-01` (LocalStack services in docker-compose) is a prerequisite `TST-01` ("extend the Testcontainers base infrastructure for new modules") needs a stable container topology to extend; `OPS-05` (CI wiring for `gradlew check` + npm test/coverage/lint) is what `TST-03`/`TST-06` actually plug into; `OPS-06` (Java 25 toolchain bump) changes the JVM `TST-01`'s Testcontainers client runs under. Building `DevOps/Infra` first avoids `Test Infrastructure` extending scaffolding that gets rebuilt out from under it a few stories later.

## 7i. Stage 9 (DevOps/Infra + Test Infrastructure) completion — mock phase is 149/149 (2026-07-21)

**What landed:** all 18 remaining mock-phase stories — `DevOps/Infra` (`OPS-01/02/03/07/09` then `OPS-04/05/06/08`, two batches) and `Test Infrastructure` (`TST-01/02/06/08` then `TST-03/04/05/07/09`, two batches) — closing out the mock phase's 149/149 stories, 748/748 points. Same per-story discipline as every prior stage (implement, verify against the story's own acceptance criterion, update `PROGRESS.md`, commit).

**This stage was materially more than infra/test scaffolding — several genuine, previously-undiscovered gaps surfaced and were fixed or explicitly flagged, not glossed over:**

- **The Testcontainers/Docker "known caveat" flagged since Stage 3 (§7c/§7e) was actually root-caused, not just worked around.** Three distinct issues: docker-java's undeclared default API version (1.32) rejected by modern Docker Engines — fixed via a JVM system property, not the commonly-assumed `DOCKER_API_VERSION` env var; Ryuk's cleanup sidecar failing to bind-mount Rancher Desktop's non-standard socket path; and LocalStack being silently OOM-killed on a 2GB VM (raised to 6GB with explicit go-ahead, per `TST-01`). A fourth, real parallel-execution flakiness issue (17 `@ApplicationModuleTest` contexts' HikariCP pools vs. Postgres's `max_connections`) was also root-caused and fixed via headroom, not pool-shrinking, after an aggressive first attempt broke two legitimate concurrency tests.
- **The frontend could not make a single successful authenticated API call against a real, security-enabled backend.** `apiClient.ts` had zero request interceptor attaching the auth token anywhere, despite the backend enforcing auth on everything except a tiny allowlist. Confirmed with a direct `curl` comparison before/after fixing it (`TST-03`). All 10 e2e specs pass for the first time ever as a result (previously 0–4 ever passed for real).
- **No REST endpoint anywhere creates a new `Itinerary` row** — every booking endpoint (line-items, save-as-quotation, convert-to-package) requires one to already exist; `new Itinerary(...)` is only ever constructed in unit tests. This is a genuine booking-flow gap (not test-infra's job to fix), flagged rather than silently worked around — `package-creation-flow.spec.ts` documents it precisely and asserts the real, confirmed failure mode.
- **`BOK-13`'s frontend (Flow C — traveler details/payment/confirmation) was deferred and never followed up on** — `BookingPaymentFlow.tsx` is still a bare placeholder. Already honestly tracked in `PROGRESS.md`'s own `BOK-13` entry, confirmed rather than assumed; Flow C's e2e spec asserts what's actually there today per an explicit decision, not a fabricated passing chain.
- **A genuine Spring Modulith cycle was caught by the module-boundary check doing its job** (`TST-07`): a new test file placed in the `infra` package (meant to stay a dependency-free leaf, like `TestInfrastructure` itself) imported `BookingApi`, closing a cycle (`ai -> supplier -> infra -> booking -> ai`). Moved the file to the `booking` package; confirmed the cycle is gone.

**Epic-completion flag:** the mock phase is now **100% by story count (149/149, 748/748 pts)** — every epic in the table above is complete. This is a *story-checklist* completeness figure, not yet a validated "mock-complete" claim: §5's Definition of Done (a manual/e2e walkthrough of Flow A/B/C end-to-end) is the next, and last, mock-phase gate, and given this stage's own findings above (no itinerary-creation endpoint, Flow C's placeholder), that walkthrough is expected to hit those same real, already-documented gaps rather than pass cleanly — reporting the actual result, not a rosy one, is the point of running it at all.

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
