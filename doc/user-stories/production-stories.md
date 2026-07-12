# ADREN TRAVEL — Phase 2 (Production) User Stories

Generated from `doc/PRD_v2_detailed.md`. Every story cites the PRD section(s) it derives from, the backend module (per `backend/README.md`'s module table) and/or frontend screen (per PRD Part 21) it touches, Given/When/Then acceptance criteria (pulled from PRD Part 22 where a matching criterion exists, else newly written in the same style, cross-referenced to PRD Part 25 test scenario IDs where applicable), a Fibonacci story-point estimate with reasoning, explicit dependencies, required testing tier(s) per the `testing-strategy` skill, and a Jira sub-task breakdown tagged `[NEW]` (net-new work) vs. `[EXTEND]`/`[REUSE]` (builds on the `booking`/`supplier`/`notification` backend reference implementation or the `search-dashboard` frontend reference screen).

Stories marked with a **⚠️ NEEDS CLARIFICATION** note implement the PRD's illustrative/placeholder shape only, pending a business or legal decision flagged in PRD Part 19 (Open Items) — they are not blocked from starting, but cannot be marked Done against final business rules until that decision lands.

## Summary

| Epic | Story Count | Total Story Points |
|---|---|---|
| Supplier Live Integrations | 16 | 99 |
| LLM Production Readiness | 9 | 54 |
| Meta Ads API Real Integration | 9 | 55 |
| Production Infrastructure | 11 | 65 |
| Security Hardening | 10 | 52 |
| Compliance Execution | 12 | 73 |
| Performance/Load Testing | 8 | 40 |
| Production Observability | 8 | 38 |
| **Total** | **83** | **476** |

---

## Table of Contents

- [Supplier Live Integrations](#supplier-live-integrations) (16 stories)
- [LLM Production Readiness](#llm-production-readiness) (9 stories)
- [Meta Ads API Real Integration](#meta-ads-api-real-integration) (9 stories)
- [Production Infrastructure](#production-infrastructure) (11 stories)
- [Security Hardening](#security-hardening) (10 stories)
- [Compliance Execution](#compliance-execution) (12 stories)
- [Performance/Load Testing](#performanceload-testing) (8 stories)
- [Production Observability](#production-observability) (8 stories)

---

## Supplier Live Integrations

*16 stories, 99 story points.*

#### SUP-01: Replace the Hotelbeds stub with a live sandbox integration

**As a** backend engineer, **I want** have `HotelbedsClient` call Hotelbeds' real sandbox API using SHA-256-signed requests, **so that** PRD §10.2.1's full authentication, mapping, and error-handling spec is implemented against a real (sandbox) endpoint instead of the MVP mock.

- **PRD reference(s):** §10.2.1 Hotelbeds
- **Module(s)/Screen(s):** supplier
- **Story points:** 8 — Real external API replaces a stub — first live-credential integration, highest first-integration risk in this epic.
- **Dependencies:** FND-11, DMC-07
- **Testing tier(s):** unit, integration (Testcontainers, sandbox)
- **Labels:** backend, supplier, phase2

**Acceptance Criteria**
- Given a search is issued against Hotelbeds sandbox, when the request is signed, then `X-Signature` is computed as SHA-256(apiKey+secret+UTC timestamp) and IP whitelisting is satisfied per the sandbox account setup.
- Given Hotelbeds returns `RATE_STALE` at booking, when the response is handled, then the user sees 'This rate has expired — please re-search,' forcing a new search rather than a silent re-price.

**Sub-tasks**
- [NEW] Backend: `internal.hotelbeds.HotelbedsClient` in its own sub-package (live, per backend-best-practices §6)
- [EXTEND] Backend: response mapping into normalized `SupplierSearchResult` shape (pattern from `HotelbedsClient`)
- [NEW] Backend: Hotelbeds-specific exception types + user-facing message mapping (PRD §10.2 table)
- [EXTEND] Backend: rate limiter + circuit breaker wiring, independently configured per supplier
- [NEW] Backend: unit test (mapping + error handling)
- [NEW] Backend: module/integration test, wired into `SupplierAggregationService` parallel fan-out

#### SUP-02: Cut Hotelbeds over to production with contract-tier rate limits and nightly content sync

**As a** Super Admin, **I want** have Hotelbeds run against production with the contracted rate-limit tier and a working nightly Content API sync, **so that** PRD §10.2.1's production sync-frequency and rate-limit requirements are fully live, not just sandbox-verified.

- **PRD reference(s):** §10.2.1 Hotelbeds (Rate limits, Sync frequency)
- **Module(s)/Screen(s):** supplier
- **Story points:** 8 — Production cutover carries contract-tier configuration and IP whitelisting risk beyond what sandbox testing can fully surface.
- **Dependencies:** SUP-01
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, supplier, phase2

**Acceptance Criteria**
- Given Hotelbeds' contracted per-second call cap is approached in production, when the token-bucket limiter engages, then overflow requests queue with backoff rather than being dropped.
- Given the nightly Content API batch job runs in production, when it completes, then static content (images, descriptions, amenities) refreshes without affecting real-time search latency.

**Sub-tasks**
- [EXTEND] Backend: production credential + IP whitelist configuration (via FND-11's Secrets Manager pattern)
- [EXTEND] Backend: token-bucket limiter tuned to the contracted production tier
- [NEW] Backend: production-fixture-shaped integrationTest (per TST-06's CI separation)
- [NEW] Backend: production nightly Content API sync job verified against real data

#### SUP-03: Replace the STUBA stub with a live sandbox integration

**As a** backend engineer, **I want** have a `StubaClient` call STUBA's real XML session-token API, including reverse-markup net-rate derivation where needed, **so that** PRD §10.2.2's authentication and mapping spec is implemented against a real sandbox.

- **PRD reference(s):** §10.2.2 STUBA
- **Module(s)/Screen(s):** supplier
- **Story points:** 8 — XML/session-token auth model is materially different from Hotelbeds' signed-hash model — genuinely new integration shape, not a copy-paste.
- **Dependencies:** FND-11, DMC-07
- **Testing tier(s):** unit, integration (Testcontainers, sandbox)
- **Labels:** backend, supplier, phase2

**Acceptance Criteria**
- Given a STUBA session token expires mid-search, when the error is handled, then automatic re-authentication is attempted with a single retry before surfacing 'temporarily unavailable'.
- Given STUBA's contract returns sell price directly instead of net, when the mapping runs, then a reverse-markup calculation derives the true net rate per the confirmed contract terms.

**Sub-tasks**
- [NEW] Backend: `internal.stuba.StubaClient` in its own sub-package (live, per backend-best-practices §6)
- [EXTEND] Backend: response mapping into normalized `SupplierSearchResult` shape (pattern from `HotelbedsClient`)
- [NEW] Backend: Stuba-specific exception types + user-facing message mapping (PRD §10.2 table)
- [EXTEND] Backend: rate limiter + circuit breaker wiring, independently configured per supplier
- [NEW] Backend: unit test (mapping + error handling)
- [NEW] Backend: module/integration test, wired into `SupplierAggregationService` parallel fan-out

#### SUP-04: Cut STUBA over to production with independent throttling and confirmed content-sync cadence

**As a** Super Admin, **I want** have STUBA run against production with its own throttle bucket, independent of Hotelbeds, **so that** PRD §10.2.2's explicit 'lower default concurrency, throttle independently' requirement is met in production.

- **PRD reference(s):** §10.2.2 STUBA (Rate limits, Sync frequency)
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Narrower cutover than SUP-02 since STUBA's static-content sync cadence was flagged as an open item pending confirmation during technical due diligence.
- **Dependencies:** SUP-03
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, supplier, phase2

**Acceptance Criteria**
- Given STUBA and Hotelbeds both receive concurrent production traffic, when rate limiting is evaluated, then STUBA's throttle bucket is entirely independent of Hotelbeds' — one supplier's headroom never borrows from the other's budget.

**Sub-tasks**
- [EXTEND] Backend: independent production throttle bucket for STUBA
- [NEW] Backend: content-sync cadence confirmed with STUBA and configured
- [NEW] Backend: production-fixture-shaped integrationTest

#### SUP-05: Replace the TBO stub with a live sandbox integration

**As a** backend engineer, **I want** have a `TboClient` call TBO's real API with correct TraceId session handling, **so that** PRD §10.2.3's TraceId-scoped-to-itinerary-draft requirement is implemented against a real sandbox.

- **PRD reference(s):** §10.2.3 TBO
- **Module(s)/Screen(s):** supplier
- **Story points:** 8 — TraceId session-state persistence tied to the itinerary draft (not request-scoped) is the most architecturally distinct integration in the supplier set.
- **Dependencies:** FND-11, DMC-07
- **Testing tier(s):** unit, integration (Testcontainers, sandbox)
- **Labels:** backend, supplier, phase2

**Acceptance Criteria**
- Given a TraceId expires mid-itinerary-build, when booking is attempted, then the system detects the expiry and prompts a full re-search rather than a partial retry or silent failure (T19).

**Sub-tasks**
- [NEW] Backend: `internal.tbo.TboClient` in its own sub-package (live, per backend-best-practices §6)
- [EXTEND] Backend: response mapping into normalized `SupplierSearchResult` shape (pattern from `HotelbedsClient`)
- [NEW] Backend: Tbo-specific exception types + user-facing message mapping (PRD §10.2 table)
- [EXTEND] Backend: rate limiter + circuit breaker wiring, independently configured per supplier
- [NEW] Backend: unit test (mapping + error handling)
- [NEW] Backend: module/integration test, wired into `SupplierAggregationService` parallel fan-out

#### SUP-06: Cut TBO over to production with UAT/prod credential separation

**As a** Super Admin, **I want** have TBO run against production using dedicated production credentials, distinct from UAT, **so that** PRD §10.2.3's explicit UAT-vs-production credential separation requirement is met.

- **PRD reference(s):** §10.2.3 TBO (Authentication)
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Credential-separation cutover; rate-limit tier confirmation is the remaining open item from sandbox testing.
- **Dependencies:** SUP-05
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, supplier, phase2

**Acceptance Criteria**
- Given the application runs under a production profile, when TBO credentials resolve, then they are the production credential set, never the UAT set, sourced via FND-11's Secrets Manager pattern.

**Sub-tasks**
- [EXTEND] Backend: production vs. UAT credential separation enforced via profile-scoped Secrets Manager entries
- [NEW] Backend: account-tier rate limit confirmed and configured
- [NEW] Backend: production-fixture-shaped integrationTest

#### SUP-07: Replace the Mystifly stub with a live flight-search/PNR integration

**As a** backend engineer, **I want** have a `MystiflyClient` issue real flight searches and PNR issuance with fast fare-expiry re-validation, **so that** PRD §10.2.4's fare-expiry-sensitive booking flow is implemented against a real sandbox.

- **PRD reference(s):** §10.2.4 Mystifly
- **Module(s)/Screen(s):** supplier
- **Story points:** 8 — Flight fares expire in minutes, not hours — the re-validation-immediately-pre-payment requirement is the highest-stakes correctness rule in the supplier set.
- **Dependencies:** FND-11, DMC-07
- **Testing tier(s):** unit, integration (Testcontainers, sandbox)
- **Labels:** backend, supplier, phase2

**Acceptance Criteria**
- Given a Mystifly fare expires between search and payment capture, when payment is attempted, then the price is re-validated immediately pre-payment and a 'price changed, please confirm' prompt is shown rather than charging a stale amount (T20).

**Sub-tasks**
- [NEW] Backend: `internal.mystifly.MystiflyClient` in its own sub-package (live, per backend-best-practices §6)
- [EXTEND] Backend: response mapping into normalized `SupplierSearchResult` shape (pattern from `HotelbedsClient`)
- [NEW] Backend: Mystifly-specific exception types + user-facing message mapping (PRD §10.2 table)
- [EXTEND] Backend: rate limiter + circuit breaker wiring, independently configured per supplier
- [NEW] Backend: unit test (mapping + error handling)
- [NEW] Backend: module/integration test, wired into `SupplierAggregationService` parallel fan-out

#### SUP-08: Cut Mystifly over to production with a dedicated rate-limit bucket

**As a** Super Admin, **I want** have Mystifly run against production with its own dedicated rate-limit bucket, separate from hotel suppliers, **so that** PRD §10.2.4's explicit requirement (flights are the most frequently re-searched product given fast fare expiry) is met in production, per backend-best-practices §3.

- **PRD reference(s):** §10.2.4 Mystifly (Rate limits)
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Narrower cutover focused specifically on the dedicated-bucket requirement §10.2.4 calls out by name.
- **Dependencies:** SUP-07
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, supplier, phase2

**Acceptance Criteria**
- Given Mystifly and the hotel suppliers all receive concurrent production traffic, when rate limiting is evaluated, then Mystifly's per-minute search cap is enforced from its own dedicated bucket, never sharing budget with Hotelbeds/STUBA/TBO.

**Sub-tasks**
- [EXTEND] Backend: dedicated production rate-limit bucket for Mystifly, isolated from hotel-supplier buckets
- [NEW] Backend: production-fixture-shaped integrationTest

#### SUP-09: Replace the Transferz stub with a live transfer integration

**As a** backend engineer, **I want** have a `TransferzClient` distinguish no-coverage-at-location from no-availability, **so that** PRD §10.2.5's two distinct failure messages are implemented against a real sandbox.

- **PRD reference(s):** §10.2.5 Transferz
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Standard REST integration with one specific two-message distinction — lower complexity than the session/TraceId-based suppliers.
- **Dependencies:** FND-11, DMC-07
- **Testing tier(s):** unit, integration (Testcontainers, sandbox)
- **Labels:** backend, supplier, phase2

**Acceptance Criteria**
- Given Transferz does not service a given pickup/dropoff pair at all, when a search is run, then the user sees 'Transfers not available for this route,' distinct from 'No transfer options available right now'.

**Sub-tasks**
- [NEW] Backend: `internal.transferz.TransferzClient` in its own sub-package (live, per backend-best-practices §6)
- [EXTEND] Backend: response mapping into normalized `SupplierSearchResult` shape (pattern from `HotelbedsClient`)
- [NEW] Backend: Transferz-specific exception types + user-facing message mapping (PRD §10.2 table)
- [EXTEND] Backend: rate limiter + circuit breaker wiring, independently configured per supplier
- [NEW] Backend: unit test (mapping + error handling)
- [NEW] Backend: module/integration test, wired into `SupplierAggregationService` parallel fan-out

#### SUP-10: Replace the Widgety stub with a live cruise integration

**As a** backend engineer, **I want** have a `WidgetyClient` flatten multi-port cruise itineraries into Adren's single-line-item model with port metadata, **so that** PRD §10.2.6's port-flattening and passenger-documentation-capture requirements are implemented against a real sandbox.

- **PRD reference(s):** §10.2.6 Widgety
- **Module(s)/Screen(s):** supplier
- **Story points:** 8 — Multi-port metadata flattening plus a partner-tier access model make this the most structurally distinct remaining supplier integration.
- **Dependencies:** FND-11, DMC-07, BOK-14
- **Testing tier(s):** unit, integration (Testcontainers, sandbox)
- **Labels:** backend, supplier, phase2

**Acceptance Criteria**
- Given a sailing's cabin category sells out, when a booking is attempted, then a distinct, clearly-labeled 'cabin category sold out' failure state is shown, not a generic rate-expired message.
- Given a cruise booking requires passport details, when booking proceeds, then they are captured in the Traveler Profile before confirmation, not deferred to check-in.

**Sub-tasks**
- [NEW] Backend: `internal.widgety.WidgetyClient` in its own sub-package (live, per backend-best-practices §6)
- [EXTEND] Backend: response mapping into normalized `SupplierSearchResult` shape (pattern from `HotelbedsClient`)
- [NEW] Backend: Widgety-specific exception types + user-facing message mapping (PRD §10.2 table)
- [EXTEND] Backend: rate limiter + circuit breaker wiring, independently configured per supplier
- [NEW] Backend: unit test (mapping + error handling)
- [NEW] Backend: module/integration test, wired into `SupplierAggregationService` parallel fan-out

#### SUP-11: Replace the HBActivities stub with a live time-slot activity integration

**As a** backend engineer, **I want** have an `HbActivitiesClient` handle time-slot-specific sellouts and fixed-headcount booking constraints, **so that** PRD §10.2.7's slot-specific error messaging is implemented against a real sandbox.

- **PRD reference(s):** §10.2.7 HBActivities
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Standard REST integration with one specific slot-vs-day distinction.
- **Dependencies:** FND-11, DMC-07
- **Testing tier(s):** unit, integration (Testcontainers, sandbox)
- **Labels:** backend, supplier, phase2

**Acceptance Criteria**
- Given a specific tour departure time is full while others on the same day are open, when a search is run, then the empty state communicates 'this time slot is full, try another time,' not a blanket unavailability message (T23).

**Sub-tasks**
- [NEW] Backend: `internal.hbactivities.HbActivitiesClient` in its own sub-package (live, per backend-best-practices §6)
- [EXTEND] Backend: response mapping into normalized `SupplierSearchResult` shape (pattern from `HotelbedsClient`)
- [NEW] Backend: HbActivities-specific exception types + user-facing message mapping (PRD §10.2 table)
- [EXTEND] Backend: rate limiter + circuit breaker wiring, independently configured per supplier
- [NEW] Backend: unit test (mapping + error handling)
- [NEW] Backend: module/integration test, wired into `SupplierAggregationService` parallel fan-out

#### SUP-12: Tune property-matching/deduplication against real supplier data volumes

**As a** Consultant/User, **I want** see accurate deduplication once real Hotelbeds/STUBA/TBO data (not synthetic MVP fixtures) is flowing through search, **so that** PRD §9.4's deduplication requirement holds up under real-world naming/address inconsistencies across suppliers, which BOK-20's MVP heuristic was only validated against synthetic data.

- **PRD reference(s):** §9.4 Business Rules & Edge Cases
- **Module(s)/Screen(s):** supplier, booking
- **Story points:** 8 — Fuzzy-matching tuning against real data is inherently iterative and highest-uncertainty — carries over BOK-20's risk profile at production scale.
- **Dependencies:** BOK-20, SUP-02, SUP-04, SUP-06
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, supplier, booking, phase2

**Acceptance Criteria**
- Given real Hotelbeds and STUBA production data for the same physical property is compared, when the matcher runs, then the false-positive and false-negative rate is measured and tuned against a labeled sample of real properties, not just the MVP's synthetic fixture set.

**Sub-tasks**
- [EXTEND] Backend: property-matching heuristic tuned against a labeled real-property sample set
- [NEW] Backend: false-positive/false-negative rate measurement harness
- [NEW] Backend: integrationTest — regression suite of known-tricky real property pairs

#### SUP-13: Tune per-supplier circuit breakers against real production latency profiles

**As a** platform reliability owner, **I want** have each supplier's circuit breaker's failure threshold and half-open window tuned to its real observed latency/error profile, **so that** PRD §24.2's isolation NFR holds under real supplier behavior, not just the MVP's default Resilience4j configuration.

- **PRD reference(s):** §24.2 NFR Supplier Integration
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Configuration tuning informed by production observability data (depends on OBS-05's per-supplier trace spans).
- **Dependencies:** OBS-05
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, supplier, phase2

**Acceptance Criteria**
- Given a supplier's real production error/latency profile is observed for a defined window, when the circuit breaker's threshold is reviewed, then its failure-rate threshold and half-open wait duration are tuned per-supplier rather than left at generic defaults.

**Sub-tasks**
- [EXTEND] Backend: per-supplier circuit-breaker threshold/half-open tuning based on observed production data
- [NEW] Backend: integrationTest — tuned thresholds trip correctly under simulated real-profile failure

#### SUP-14: Run a supplier sandbox-vs-production divergence regression suite in CI

**As a** QA engineer, **I want** have every supplier client change automatically run against both sandbox and production-like fixtures with divergence flagged, **so that** PRD §23.2 Edge Case #4 is enforced continuously at production scale, not just verified once during initial integration.

- **PRD reference(s):** §23.2 Edge Case #4; §25 T19/T20
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Extends TST-06's MVP-stage separation with real production fixture data now available post-cutover.
- **Dependencies:** TST-06, SUP-02, SUP-04, SUP-06
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, testing, supplier, phase2

**Acceptance Criteria**
- Given a supplier client's mapping logic changes, when CI runs, then both the sandbox-fixture and production-fixture-shaped test suites (TST-06) execute, and any divergence in mapped output is flagged as a distinct CI failure, not silently passed.

**Sub-tasks**
- [NEW] Test infra: sandbox-vs-production divergence regression suite using real post-cutover production fixtures
- [NEW] Test infra: sample test exercising the new harness
- [NEW] Test infra: CI wiring / gradle-or-npm script update

#### SUP-15: Harden BYOS credential encryption for production key rotation

**As a** platform security owner, **I want** have the KMS CMK wrapping BYOS credential data keys rotate on a defined production schedule, **so that** FND-12's row-level encryption pattern has a real production key-rotation policy, not just the MVP's dev-scoped KMS setup.

- **PRD reference(s):** §5.3 Secrets handling (RULES.md)
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Production key-management policy work on top of FND-12's already-built envelope-encryption mechanism.
- **Dependencies:** FND-12
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, supplier, security, phase2

**Acceptance Criteria**
- Given the production KMS CMK rotation schedule triggers, when rotation completes, then existing BYOS ciphertext remains decryptable via KMS's key-versioning, with no Consultant-visible downtime.

**Sub-tasks**
- [EXTEND] Backend: production KMS CMK rotation policy configured
- [NEW] Backend: integrationTest — decrypt succeeds across a simulated key-version rotation

#### SUP-16: Publish a production Local DMC onboarding runbook

**As a** Super Admin, **I want** have a documented review SLA and verification-step process for Local DMC onboarding at production scale, **so that** PRD §10.3's onboarding workflow (DMC-02) has an operational runbook once real Local DMCs, not MVP test fixtures, are being reviewed.

- **PRD reference(s):** §10.3 Local DMC Onboarding & Vetting Workflow
- **Module(s)/Screen(s):** supplier
- **Story points:** 3 — Documentation/process deliverable, not code — but a real production-readiness gap the MVP's DMC-02 story didn't need to address.
- **Dependencies:** DMC-02
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** ops, supplier, phase2

**Acceptance Criteria**
- Given a real Local DMC submission is received in production, when the runbook is followed, then the documented review SLA and verification-step sequence is applied consistently, not improvised per reviewer.

**Sub-tasks**
- [NEW] Infra: Local DMC onboarding runbook — review SLA and verification-step checklist
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

---

## LLM Production Readiness

*9 stories, 54 story points.*

#### LLM-01: Run a production LLM provider bake-off against latency/cost/accuracy criteria

**As a** Super Admin, **I want** have a documented comparison of Groq against alternative production LLM providers before committing to Groq for GA, **so that** PRD §24.3's latency NFR and general production cost/accuracy concerns are evaluated before Groq is locked in beyond MVP.

- **PRD reference(s):** §24.3 NFR AI Governance
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — Evaluation/research deliverable with a lightweight harness — not a production code change itself.
- **Dependencies:** AI-01
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, ai, phase2

**Acceptance Criteria**
- Given the bake-off completes, when results are reviewed, then Groq and at least one alternative provider are compared on latency-per-segment, cost-per-suggestion, and grounding accuracy against a fixed evaluation set.

**Sub-tasks**
- [NEW] Backend: evaluation harness running the same prompt set against Groq and at least one alternative
- [NEW] Backend: latency/cost/accuracy comparison report

#### LLM-02: Build a swappable production LLM provider abstraction

**As a** backend engineer, **I want** have the `ai` module depend on an internal provider interface rather than being hard-wired to `GroqClient`, **so that** LLM-01's decision (stay on Groq or switch) can be executed as a configuration change, per backend-best-practices §4's DI discipline.

- **PRD reference(s):** backend-best-practices skill §4 (DI conventions)
- **Module(s)/Screen(s):** ai
- **Story points:** 8 — Structural refactor of AI-01's client wrapper into an interface + implementation split — must happen before/alongside whichever provider LLM-01 selects.
- **Dependencies:** LLM-01, AI-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ai, phase2

**Acceptance Criteria**
- Given the production LLM provider decision changes, when the provider bean is swapped, then no caller of the `ai` module's internal generation logic changes — only the injected provider implementation differs.

**Sub-tasks**
- [NEW] Backend: `LlmProvider` interface (prompt in, structured suggestion out)
- [EXTEND] Backend: `GroqClient` becomes one `LlmProvider` implementation
- [NEW] Backend: unit test — provider swap via config, no caller change
- [NEW] Backend: module test

#### LLM-03: Run adversarial prompt-injection / grounding-bypass testing

**As a** Super Admin, **I want** have the AI governance principles (grounded generation, no hallucination) tested against deliberate adversarial inputs, **so that** PRD §11.2's governance principles hold under attack, not just under well-formed input, before GA.

- **PRD reference(s):** §11.2 Governance Framework
- **Module(s)/Screen(s):** ai
- **Story points:** 8 — Adversarial test-suite construction against an LLM's actual behavior is inherently exploratory and high-effort.
- **Dependencies:** LLM-02, AI-05
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, ai, phase2

**Acceptance Criteria**
- Given an adversarial prompt attempts to induce the AI to fabricate a non-supplier-confirmed line item, when the request is processed, then the grounding-only principle holds — no fabricated item is produced, and AI-05's explicit-failure-state path is triggered instead.

**Sub-tasks**
- [NEW] Backend: adversarial prompt test corpus targeting each of §11.2's five governance principles
- [NEW] Backend: automated regression suite run against the corpus on every `ai` module change

#### LLM-04: Log every retry/timeout attempt distinctly in production

**As a** Super Admin, **I want** see each retried LLM call attempt as a distinct audit entry in production, not just the final successful one, **so that** backend-best-practices §7 and RULES.md §6.3's audit-completeness requirement holds under real production retry conditions, not just the MVP's synthetic test scenarios.

- **PRD reference(s):** backend-best-practices skill §7; §6.3 (RULES.md)
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — Extends AI-13's timeout/retry logic with production-scale audit granularity.
- **Dependencies:** AI-13, LLM-02
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, ai, phase2

**Acceptance Criteria**
- Given a production Groq/LLM call times out twice and succeeds on the third attempt, when the audit log is inspected, then three distinct attempt entries exist, not one entry representing only the final successful call.

**Sub-tasks**
- [EXTEND] Backend: per-attempt audit logging on every retry, in production configuration
- [NEW] Backend: integrationTest — 3-attempt scenario produces 3 audit entries

#### LLM-05: Monitor and alert on AI response latency SLOs in production

**As a** on-call engineer, **I want** be alerted when AI suggestion latency breaches the per-segment SLO that protects the 10-minute itinerary target, **so that** PRD §24.3's NFR is continuously monitored in production, not just tested once pre-launch.

- **PRD reference(s):** §24.3 NFR AI Governance
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — Dashboard/alert wiring on top of AI-13's bounded-timeout mechanism and OBS-01's tracing.
- **Dependencies:** AI-13, OBS-01
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, ai, phase2

**Acceptance Criteria**
- Given AI suggestion latency exceeds the defined per-segment SLO in production, when the monitoring threshold is breached, then an alert fires to on-call before the 10-minute end-to-end itinerary target is meaningfully at risk.

**Sub-tasks**
- [NEW] Backend: AI latency SLO dashboard + alert rule
- [NEW] Backend: integrationTest — simulated SLO breach triggers the alert path

#### LLM-06: Add production content-safety filtering for AI ad-creative generation

**As a** Super Admin, **I want** have AI-generated ad creative pass a content-safety filter before it reaches the Consultant approval step, **so that** Meta policy compliance risk on AI-generated creative (PRD §14.4) is reduced before real ad spend is at stake in production.

- **PRD reference(s):** §14.4 AI Creative Generation
- **Module(s)/Screen(s):** ai, ads
- **Story points:** 5 — Content-safety filter layered onto AI-12's already-built creative-generation path.
- **Dependencies:** AI-12, LLM-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ai, ads, phase2

**Acceptance Criteria**
- Given AI generates a creative variant that would violate Meta's advertising content policy, when the safety filter runs, then the variant is excluded from the gallery before it ever reaches Consultant approval (ADS-05), rather than being caught only at Meta's own review.

**Sub-tasks**
- [NEW] Backend: content-safety filter service (policy rule set + provider-level moderation check)
- [EXTEND] Backend: filter applied before creative variants are persisted to `AdCampaign.creative_variants[]`
- [NEW] Backend: unit test
- [NEW] Backend: module test

#### LLM-07: Implement per-Consultant AI usage quota / budget guardrails

**As a** Super Admin, **I want** cap the AI generation cost/volume a single Consultant can consume, **so that** production LLM cost is a real, unbounded line item once Groq/its replacement is billed on production volume, unlike the MVP's fixed dev usage.

- **PRD reference(s):** §24.3 NFR AI Governance (production cost concern)
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — New quota-tracking mechanism; the calculation itself is straightforward, the policy configuration surface is the work.
- **Dependencies:** LLM-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ai, phase2

**Acceptance Criteria**
- Given a Consultant's AI usage approaches their configured quota, when a request is made, then the system either warns or blocks per the configured policy, rather than allowing unbounded per-Consultant AI cost.

**Sub-tasks**
- [NEW] Backend: per-Consultant AI usage counter + configurable quota
- [NEW] Backend: quota-breach warn/block policy
- [NEW] Backend: unit test
- [NEW] Backend: module test

#### LLM-08: Tie AI suggestion caching invalidation to the rate-staleness signal at production scale

**As a** backend engineer, **I want** have any AI suggestion cache invalidate based on the same staleness signal that triggers re-validation, never a fixed TTL, **so that** backend-best-practices §5's explicit warning is honored if/when AI suggestion caching is introduced for production latency reasons.

- **PRD reference(s):** backend-best-practices skill §5 (Caching strategy)
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — Caching-layer story explicitly scoped to avoid the exact anti-pattern backend-best-practices §5 warns against — only relevant once caching is actually introduced for latency.
- **Dependencies:** AI-09, LLM-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ai, phase2

**Acceptance Criteria**
- Given an AI suggestion cache is introduced for latency reasons, when a cached suggestion's underlying rate goes stale, then the cache entry is invalidated by the same staleness signal AI-09's re-validation uses, never surviving past it on a fixed TTL alone.

**Sub-tasks**
- [NEW] Backend: AI suggestion cache keyed with a staleness-signal-bound TTL, not a fixed TTL
- [NEW] Backend: unit test — cache entry invalidated when the staleness signal fires before a fixed TTL would have expired it

#### LLM-09: Add an AI provider failover path for primary-provider degradation

**As a** Consultant/User, **I want** still get an AI suggestion (or an explicit failure state) if the primary LLM provider is degraded, **so that** LLM-02's provider abstraction is used for resilience, not just swap-at-deploy-time flexibility.

- **PRD reference(s):** §11.2 Governance Framework (resilience)
- **Module(s)/Screen(s):** ai
- **Story points:** 8 — Runtime failover logic (not just swap-at-deploy) built on LLM-02's abstraction — the most operationally complex LLM story.
- **Dependencies:** LLM-02, LLM-05
- **Testing tier(s):** unit, module (@ApplicationModuleTest), integration (Testcontainers)
- **Labels:** backend, ai, phase2

**Acceptance Criteria**
- Given the primary LLM provider is degraded (elevated error rate/timeout), when an AI request is made, then the system fails over to a configured secondary `LlmProvider` implementation, or returns AI-05's explicit `NoViableSuggestion` state if no failover is configured — never a silent hang.

**Sub-tasks**
- [EXTEND] Backend: `LlmProvider` failover chain with health-based routing
- [NEW] Backend: unit test — failover triggers on simulated degradation
- [NEW] Backend: integrationTest — end-to-end failover path

---

## Meta Ads API Real Integration

*9 stories, 55 story points.*

#### MADS-01: Replace mocked Meta account provisioning with the real Meta Business API

**As a** Super Admin, **I want** provision a real Meta ad account and Business Manager for a Consultant via the Meta Business API, **so that** ADS-01's MVP-mocked provisioning is replaced with a real integration before any real ad spend can occur.

- **PRD reference(s):** §14.1 Ads/Campaign Overview
- **Module(s)/Screen(s):** ads
- **Story points:** 8 — First real external Meta API integration — carries real account-liability risk per PRD §7's named risk.
- **Dependencies:** ADS-01
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, ads, phase2

**Acceptance Criteria**
- Given Super Admin provisions a Meta ad account for a Consultant, when the real API call is made, then a genuine Meta Business Manager and ad account are created under Adren's umbrella structure, replacing ADS-01's mocked bookkeeping-only entity.

**Sub-tasks**
- [EXTEND] Backend: `AdAccount` provisioning calls the real Meta Business API
- [NEW] Backend: Meta API credential handling via the same Secrets Manager pattern as FND-11
- [NEW] Backend: unit test — request/response mapping
- [NEW] Backend: integrationTest against Meta's sandbox/test environment

#### MADS-02: Launch real Meta Campaigns/Ad Sets/Ads via the Marketing API

**As a** Consultant, **I want** have my approved campaign actually launch on Meta, not just record a mocked `meta_campaign_ref`, **so that** ADS-07's MVP-mocked launch call is replaced with the real Meta Marketing API.

- **PRD reference(s):** §14.2 Flow step 6
- **Module(s)/Screen(s):** ads
- **Story points:** 8 — Real Marketing API object-hierarchy creation (Campaign→AdSet→Ad) is materially more complex than the MVP's single mocked call.
- **Dependencies:** MADS-01, ADS-07
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, ads, phase2

**Acceptance Criteria**
- Given a campaign passes policy review, when launch is triggered, then a real Meta Campaign/Ad Set/Ad hierarchy is created via the Marketing API and the real `meta_campaign_ref` is stored.

**Sub-tasks**
- [EXTEND] Backend: real Marketing API Campaign/AdSet/Ad creation replacing ADS-07's mock
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest against Meta's sandbox

#### MADS-03: Upload real creative (image/copy) via the Meta Marketing API

**As a** Consultant, **I want** have my approved creative variants actually uploaded to Meta, not just stored locally, **so that** ADS-04's locally-persisted `creative_variants[]` are pushed to Meta as real ad creative assets.

- **PRD reference(s):** §14.2 Flow step 3; §14.4 AI Creative Generation
- **Module(s)/Screen(s):** ads
- **Story points:** 5 — Asset-upload API call layered onto MADS-02's launch flow.
- **Dependencies:** MADS-02
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, ads, phase2

**Acceptance Criteria**
- Given a creative variant is Consultant-approved (ADS-05), when the campaign launches (MADS-02), then the approved image/copy is uploaded to Meta as a real ad-creative asset referenced by the launched Ad.

**Sub-tasks**
- [NEW] Backend: Meta creative-asset upload call
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest against Meta's sandbox

#### MADS-04: Poll real Meta performance/insights data into performance_snapshot

**As a** Consultant, **I want** see real impressions, clicks, and attributed bookings from Meta, not ADS-09's mocked scheduled data, **so that** PRD §14.2 step 7's performance flow-back is backed by the real Meta Insights API.

- **PRD reference(s):** §14.2 Flow step 7; §20.13 performance_snapshot
- **Module(s)/Screen(s):** ads
- **Story points:** 5 — Polling-job integration against a real read-only Meta endpoint — lower risk than the write-path stories in this epic.
- **Dependencies:** MADS-02, ADS-09
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, ads, phase2

**Acceptance Criteria**
- Given a Live campaign has accrued real Meta spend/engagement, when the polling job runs, then `performance_snapshot` reflects real Meta Insights API data, replacing ADS-09's mocked interval-populated figures.

**Sub-tasks**
- [EXTEND] Backend: Meta Insights API polling job replacing ADS-09's mock
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest against Meta's sandbox

#### MADS-05: Enforce spend caps via real Meta API-level budget controls

**As a** Super Admin/Consultant, **I want** have a campaign's spend cap enforced by Meta's own budget controls, reconciled against Adren's near-real-time tracking, **so that** ADS-10's MVP-mocked spend-tracking is reconciled against Meta's authoritative real spend data, closing the gap processing lag could otherwise introduce.

- **PRD reference(s):** §14.3 Controls & Guardrails; §24.6 NFR Ads/Campaign
- **Module(s)/Screen(s):** ads
- **Story points:** 8 — Reconciling two independent spend-tracking sources (Meta's own controls + Adren's polling) with a real-money liability concern makes this the highest-stakes MADS story.
- **Dependencies:** MADS-04, ADS-10
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, ads, phase2

**Acceptance Criteria**
- Given a Live campaign's real Meta spend approaches `budget_cap`, when reconciliation runs, then Meta's own budget control (Ad Set-level spend cap) plus Adren's polling-based `SpendCapReached` transition together ensure the campaign never meaningfully overshoots, even under processing lag.

**Sub-tasks**
- [EXTEND] Backend: Meta Ad Set-level budget cap set at launch (MADS-02)
- [EXTEND] Backend: `SpendCapReached` reconciliation against real Meta spend data
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — simulated near-cap spend against Meta's sandbox

#### MADS-06: Handle real Meta policy/brand-safety rejection webhooks

**As a** Super Admin, **I want** be notified when Meta itself rejects a campaign for policy reasons, distinct from Adren's own pre-check (ADS-15), **so that** PRD §14.2 step 5's review gate accounts for Meta-side rejection, not just Adren's internal policy review.

- **PRD reference(s):** §14.2 Flow step 5
- **Module(s)/Screen(s):** ads
- **Story points:** 5 — Webhook-handling integration; the distinct-status requirement is the specific correctness bar.
- **Dependencies:** MADS-02, ADS-15
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ads, phase2

**Acceptance Criteria**
- Given Meta rejects a launched campaign for a policy violation Adren's own pre-check (ADS-15) didn't catch, when the rejection webhook is received, then the campaign's status reflects the Meta-side rejection with the reason surfaced to the Consultant, distinct from an Adren-internal Rejected status.

**Sub-tasks**
- [NEW] Backend: Meta policy-rejection webhook handler
- [NEW] Backend: unit test
- [NEW] Backend: module test

#### MADS-07: Handle real Meta account-suspension webhooks

**As a** Consultant, **I want** see the 'suspended — action required' status (ADS-13) triggered by a real Meta suspension event, not a mocked signal, **so that** PRD §23.5 Edge Case #12 and T17 hold at production scale against Meta's real webhook delivery.

- **PRD reference(s):** §23.5 Edge Case #12; §25 T17
- **Module(s)/Screen(s):** ads
- **Story points:** 5 — Replaces ADS-13's mocked signal handler with a real, authenticated Meta webhook receiver.
- **Dependencies:** MADS-01, ADS-13
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ads, phase2

**Acceptance Criteria**
- Given Meta suspends a real ad account mid-campaign, when the suspension webhook is received, then every active campaign under that Consultant transitions to ADS-13's 'suspended — action required' status via the real webhook, replacing the MVP's mocked suspension-signal handler.

**Sub-tasks**
- [EXTEND] Backend: real Meta webhook receiver (authenticated, signature-verified) replacing the mocked suspension handler
- [NEW] Backend: unit test
- [NEW] Backend: module test

#### MADS-08: Isolate Meta Business Manager billing/liability per Consultant at production scale

**As a** Super Admin, **I want** be certain one Consultant's ad spend/billing can never bleed into another Consultant's account under the shared Adren umbrella, **so that** PRD §7's named risk ('ad account liability — Adren manages Meta accounts/billing on Consultants' behalf') is mitigated with real controls, not just documented as a risk.

- **PRD reference(s):** §7 Assumptions, Dependencies, Risks
- **Module(s)/Screen(s):** ads
- **Story points:** 8 — Billing-isolation guarantee across a shared third-party account structure — genuinely hard multi-tenancy problem at the Meta-account level, not just Adren's own database.
- **Dependencies:** MADS-01, MADS-05
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, ads, security, phase2

**Acceptance Criteria**
- Given two Consultants each have a Meta ad account provisioned under Adren's Business Manager, when billing is reconciled, then each Consultant's spend is fully attributable and isolated — no cross-Consultant billing leakage is possible even under a shared umbrella structure.

**Sub-tasks**
- [NEW] Backend: per-Consultant Meta sub-account/campaign-budget isolation model
- [NEW] Backend: billing reconciliation audit — cross-Consultant leakage check
- [NEW] Backend: integrationTest — two-Consultant billing isolation scenario

#### MADS-09: Track per-market legal sign-off on Meta ad policy templates

**As a** Super Admin, **I want** know which markets' ad creative templates have completed legal sign-off before campaigns launch there, **so that** PRD §19's open item — whether the Ads module needs per-market legal sign-off on templates — is resolved and tracked operationally.

- **PRD reference(s):** §19 Open Items for Business Confirmation
- **Module(s)/Screen(s):** ads, compliance
- **Story points:** 3 — Tracking/gating mechanism; the actual legal review itself is a business process outside engineering scope.
- **Dependencies:** MADS-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ads, compliance, phase2

**Acceptance Criteria**
- Given a campaign targets a market whose ad-template legal sign-off is not yet recorded, when launch is attempted, then the system blocks launch (or flags for manual override) until sign-off is recorded for that market.

**Sub-tasks**
- [NEW] Backend: per-market ad-template legal-signoff flag + launch gate
- [NEW] Backend: unit test

> ⚠️ **NEEDS CLARIFICATION:** PRD §19: whether the Ads module needs per-market legal sign-off on templates is an open item for business confirmation — this story implements the tracking/gating mechanism assuming sign-off is required; remove the gate if business confirms it is not needed.

---

## Production Infrastructure

*11 stories, 65 story points.*

#### PINF-01: Replace LocalStack Secrets Manager with real AWS Secrets Manager and rotation Lambdas

**As a** platform security owner, **I want** have every credential FND-11/FND-12/OPS-02/OPS-07 sourced from real AWS Secrets Manager with automated rotation, **so that** RULES.md §5.3's rotation-Lambda pattern is live in production, not just LocalStack-emulated.

- **PRD reference(s):** §5.3 Secrets handling (RULES.md)
- **Module(s)/Screen(s):** Infra (production AWS)
- **Story points:** 8 — Real AWS Secrets Manager + rotation Lambda wiring, replacing the entire LocalStack-emulated foundation from OPS-01/02/07.
- **Dependencies:** FND-11, FND-12, OPS-02, OPS-07
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, security, phase2

**Acceptance Criteria**
- Given a production credential's rotation schedule triggers, when the rotation Lambda runs, then the credential rotates with no service disruption, and every consumer resolves the new value via the same ARN.

**Sub-tasks**
- [NEW] Infra: real AWS Secrets Manager + rotation Lambda per credential family (supplier, BYOS, Meta, Groq)
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### PINF-02: Replace LocalStack S3 with production S3 and encryption-at-rest policy

**As a** platform security owner, **I want** have vouchers and the document vault stored in real S3 with an explicit encryption-at-rest policy, **so that** OPS-03's LocalStack buckets are replaced with production-grade storage for traveler PII and financial documents.

- **PRD reference(s):** §20.11 Voucher; §20.10 Traveler Profile (document_vault)
- **Module(s)/Screen(s):** Infra (production AWS)
- **Story points:** 5 — Cutover from OPS-03's LocalStack buckets to real S3 with an explicit encryption policy.
- **Dependencies:** OPS-03
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, security, phase2

**Acceptance Criteria**
- Given a voucher or traveler document is written in production, when storage is inspected, then it resides in a real S3 bucket with server-side encryption enabled and a documented key-management policy.

**Sub-tasks**
- [NEW] Infra: production S3 buckets (`vouchers`, `traveler-documents`) with encryption-at-rest policy
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### PINF-03: Replace LocalStack SQS/SNS with real AWS messaging for async event fan-out at scale

**As a** platform reliability owner, **I want** have any async messaging introduced for event fan-out run on real SQS/SNS in production, **so that** OPS-01's LocalStack messaging services have a production equivalent before the platform scales beyond a single-instance event-listener model.

- **PRD reference(s):** §5 System Architecture Overview
- **Module(s)/Screen(s):** Infra (production AWS)
- **Story points:** 5 — Production messaging infra cutover; scope is provisioning + connectivity, not a redesign of the event model itself.
- **Dependencies:** OPS-01
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, phase2

**Acceptance Criteria**
- Given event volume exceeds what in-process `@ApplicationModuleListener` dispatch can handle at production scale, when fan-out is evaluated, then real SQS/SNS is available as the production messaging backbone, matching OPS-01's LocalStack-emulated shape.

**Sub-tasks**
- [NEW] Infra: production SQS/SNS provisioning and connectivity
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### PINF-04: Stand up production Postgres with backup/PITR and connection-pool tuning

**As a** platform reliability owner, **I want** have a managed production Postgres (RDS/Aurora) with point-in-time recovery and tuned connection pooling, **so that** the platform's booking-critical data has a real production database, not just Testcontainers-verified local Postgres.

- **PRD reference(s):** §2 Goals & Success Metrics (99.5%+ uptime)
- **Module(s)/Screen(s):** Infra (production database)
- **Story points:** 8 — Managed database provisioning with backup/recovery and pooling tuning — foundational production infra with real operational risk if under-provisioned.
- **Dependencies:** OPS-04
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, phase2

**Acceptance Criteria**
- Given a production incident requires point-in-time recovery, when a restore is performed, then data is recoverable to within the defined RPO, and connection pooling is tuned to the platform's real concurrent-load profile.

**Sub-tasks**
- [NEW] Infra: production Postgres (RDS/Aurora) with PITR backup policy and tuned connection pooling
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### PINF-05: Define the production deployment topology for the Spring Modulith monolith

**As a** platform reliability owner, **I want** have a defined Kubernetes/ECS deployment topology for the monolith, **so that** the platform can actually run in production with defined scaling/health-check behavior.

- **PRD reference(s):** §2 Goals & Success Metrics (99.5%+ uptime)
- **Module(s)/Screen(s):** Infra (production deployment)
- **Story points:** 8 — First production deployment topology decision for a not-yet-deployed monolith — architecturally significant, not just a config file.
- **Dependencies:** PINF-04
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, phase2

**Acceptance Criteria**
- Given production load increases, when the deployment topology is evaluated, then the monolith scales horizontally per its defined topology with health checks gating traffic to unready instances.

**Sub-tasks**
- [NEW] Infra: Kubernetes/ECS deployment topology, health checks, horizontal scaling policy
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### PINF-06: Evaluate multi-region/data-residency requirements for EU/UK traveler PII

**As a** compliance owner, **I want** have a documented data-residency approach for EU/UK traveler PII before GA in those markets, **so that** PRD §17.2's 'EU/UK data residency evaluation for traveler PII' requirement and §19's open item are resolved with an implementable decision.

- **PRD reference(s):** §17.2 Platform Enforcement Requirements; §19 Open Items
- **Module(s)/Screen(s):** Infra (production, compliance)
- **Story points:** 8 — Requires a compliance/legal decision before implementation can be scoped precisely — architecturally significant if it requires a regional deployment split.
- **Dependencies:** PINF-05
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, compliance, phase2

**Acceptance Criteria**
- Given EU/UK traveler PII is written, when storage location is evaluated, then it complies with the resolved data-residency approach — either an EU-region deployment or a documented alternative compliant with UK/EU GDPR.

**Sub-tasks**
- [NEW] Infra: EU/UK data-residency decision + implementation (regional deployment or documented alternative)
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

> ⚠️ **NEEDS CLARIFICATION:** PRD §19: the EU/UK data residency approach is an explicit open item for business/legal confirmation — this story's scope depends entirely on that decision and cannot be sized precisely until it's made.

#### PINF-07: Build a blue/green or canary deployment pipeline

**As a** platform reliability owner, **I want** deploy new releases with a blue/green or canary strategy rather than a hard cutover, **so that** the 99.5%+ uptime target (PRD §2) is protected during releases, not just steady-state operation.

- **PRD reference(s):** §2 Goals & Success Metrics
- **Module(s)/Screen(s):** Infra (production deployment)
- **Story points:** 5 — CI/CD pipeline enhancement on top of PINF-05's topology.
- **Dependencies:** PINF-05
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, phase2

**Acceptance Criteria**
- Given a new release is deployed, when the pipeline runs, then traffic shifts gradually (canary) or via an instant blue/green swap, with automatic rollback on health-check failure.

**Sub-tasks**
- [NEW] Infra: blue/green or canary deployment pipeline with automatic rollback
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### PINF-08: Write a production Flyway migration rollout/rollback runbook

**As a** backend engineer, **I want** have a documented runbook for rolling out and, if needed, rolling back a production migration, **so that** OPS-04's migration discipline extends to a real production operational procedure, given migrations are additive-only and can't simply be reverted in place.

- **PRD reference(s):** §4.2 Migration discipline (RULES.md)
- **Module(s)/Screen(s):** Infra (production database)
- **Story points:** 3 — Documentation deliverable formalizing OPS-04's discipline for the production operational context.
- **Dependencies:** OPS-04, PINF-04
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, phase2

**Acceptance Criteria**
- Given a production migration needs to be rolled back, when the runbook is followed, then the rollback is executed as a new additive migration reversing the change, never an edit to the already-merged migration, per RULES.md §4.2.

**Sub-tasks**
- [NEW] Infra: production Flyway rollout/rollback runbook
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### PINF-09: Provision production CDN and white-label domain CNAME infrastructure

**As a** Consultant, **I want** have my CNAME domain (FND-06) resolve through a production CDN with the propagation speed FND-07's NFR requires, **so that** PRD §13.2/§24.5's white-label domain requirements are met at production scale, not just FND-07's local propagation mechanism.

- **PRD reference(s):** §13.2 Branding Configuration; §24.5 NFR White-Label & Admin
- **Module(s)/Screen(s):** Infra (production)
- **Story points:** 5 — Production CDN + DNS provisioning on top of FND-06/FND-07's already-built domain-mapping and propagation mechanism.
- **Dependencies:** FND-07
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, whitelabel, phase2

**Acceptance Criteria**
- Given a Consultant's CNAME domain is mapped in production, when it is resolved, then it routes through the production CDN to the correct Consultant-branded storefront within the NFR's defined short window.

**Sub-tasks**
- [NEW] Infra: production CDN provisioning + CNAME domain routing
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### PINF-10: Establish production secrets rotation for Adren-owned supplier and Meta credentials

**As a** platform security owner, **I want** have Adren's own supplier and Meta credentials rotate on a defined production schedule, **so that** PINF-01's rotation mechanism is specifically applied and scheduled for the highest-value credential families (per RULES.md §5.3's explicit Meta-credential risk note).

- **PRD reference(s):** §5.3 Secrets handling (RULES.md)
- **Module(s)/Screen(s):** Infra (production)
- **Story points:** 5 — Applies PINF-01's rotation mechanism specifically to supplier + Meta credentials with a defined schedule.
- **Dependencies:** PINF-01, MADS-01
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, security, phase2

**Acceptance Criteria**
- Given a supplier or Meta credential's rotation schedule triggers in production, when rotation completes, then every live supplier/Meta integration continues functioning without a manual credential-swap step.

**Sub-tasks**
- [NEW] Infra: production rotation schedule for supplier and Meta credentials
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### PINF-11: Run a disaster-recovery/backup-restore drill for booking-critical data

**As a** platform reliability owner, **I want** prove that booking-critical data (bookings, wallet ledger, traveler profiles) can actually be restored from backup within the defined RPO/RTO, **so that** PINF-04's PITR capability is verified by a real drill, not just assumed to work because it's configured.

- **PRD reference(s):** §2 Goals & Success Metrics (99.5%+ uptime)
- **Module(s)/Screen(s):** Infra (production)
- **Story points:** 5 — Operational verification exercise against PINF-04's backup infrastructure.
- **Dependencies:** PINF-04
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, phase2

**Acceptance Criteria**
- Given a DR drill is run against a production-equivalent environment, when a restore is performed, then booking-critical data is recovered within the defined RPO/RTO, and the drill's results are documented.

**Sub-tasks**
- [NEW] Infra: DR/backup-restore drill against a production-equivalent environment, results documented
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

---

## Security Hardening

*10 stories, 52 story points.*

#### SEC-01: Harden production JWT signing-key rotation and token expiry/refresh policy

**As a** platform security owner, **I want** have JWT signing keys rotate on a defined schedule with a real access/refresh-token expiry policy, **so that** FND-01's MVP authentication foundation is production-hardened before GA.

- **PRD reference(s):** §5.1 (RULES.md)
- **Module(s)/Screen(s):** shared (security)
- **Story points:** 8 — Production-grade key management + refresh-token flow — materially more than FND-01's MVP stateless JWT foundation.
- **Dependencies:** FND-01
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, security, phase2

**Acceptance Criteria**
- Given the JWT signing key rotation schedule triggers, when rotation completes, then tokens signed under the previous key remain valid until their own expiry (grace period), and no service disruption occurs.
- Given an access token expires, when the client presents a refresh token, then a new access token is issued per the defined refresh policy, without requiring re-authentication.

**Sub-tasks**
- [EXTEND] Backend: signing-key rotation with grace-period validation
- [NEW] Backend: refresh-token issuance/validation flow
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — rotation-during-active-session scenario

#### SEC-02: Remediate findings from a full OWASP API Top 10 penetration test pass

**As a** platform security owner, **I want** have every OWASP API Top 10 category assessed and remediated before GA, **so that** RULES.md §5.4's OWASP-relevant concerns are verified by an actual penetration test, not just the design-time mitigations already built (FND-03, FND-08, SEC-03, SEC-04, SEC-05).

- **PRD reference(s):** §5.4 OWASP-relevant concerns (RULES.md)
- **Module(s)/Screen(s):** shared (security)
- **Story points:** 8 — Scope depends entirely on pentest findings — sized as the upper bound for a full-platform OWASP Top 10 pass with real remediation work, not just the test itself.
- **Dependencies:** FND-03, FND-08, SEC-03, SEC-04, SEC-05
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, security, phase2

**Acceptance Criteria**
- Given the penetration test completes, when findings are reviewed, then every finding is triaged, and all Critical/High findings are remediated before GA sign-off.

**Sub-tasks**
- [NEW] Backend: engage penetration test against the full OWASP API Top 10 checklist
- [NEW] Backend: remediate all Critical/High findings
- [NEW] Backend: regression test per remediated finding

#### SEC-03: Add SSRF protections for Consultant-configured URLs

**As a** platform security owner, **I want** have any Consultant-supplied URL (webhook URLs, whitelabel domain verification, BYOS base-URL overrides) validated against SSRF, **so that** RULES.md §5.4's explicit SSRF concern — including the AWS metadata endpoint `169.254.169.254` — is closed before any such input surface ships for real.

- **PRD reference(s):** §5.4 OWASP-relevant concerns (RULES.md, SSRF)
- **Module(s)/Screen(s):** whitelabel, supplier
- **Story points:** 5 — Focused validation-layer story covering every current and near-term Consultant-supplied-URL surface.
- **Dependencies:** FND-06
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, security, phase2

**Acceptance Criteria**
- Given a Consultant supplies a URL that resolves to an internal/link-local address (e.g. `169.254.169.254`), when the backend validates it before fetching, then the request is rejected — an allow-list of expected external hosts is checked, not a deny-list of forbidden ones.

**Sub-tasks**
- [NEW] Backend: SSRF validation helper (allow-list based) applied to every Consultant-supplied URL input
- [NEW] Backend: unit test — internal/link-local address rejected
- [NEW] Backend: integrationTest

#### SEC-04: Add rate limiting on search/booking endpoints against inbound scraping

**As a** platform security owner, **I want** have search/booking endpoints rate-limited against a competitor scraping Adren's re-exposed supplier pricing, **so that** RULES.md §5.4's distinct inbound-scraping concern (separate from per-supplier outbound rate limiting) is closed before GA.

- **PRD reference(s):** §5.4 OWASP-relevant concerns (RULES.md, inbound rate limiting)
- **Module(s)/Screen(s):** booking
- **Story points:** 5 — Inbound rate-limiting layer, deliberately distinct from the outbound per-supplier limiters already built in Phase 1.
- **Dependencies:** FND-13
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, security, phase2

**Acceptance Criteria**
- Given a single client issues an abnormally high volume of search requests in a short window, when the inbound rate limiter evaluates, then further requests are throttled/rejected, distinct from and independent of the per-supplier outbound limiters in `supplier`.

**Sub-tasks**
- [NEW] Backend: inbound rate limiter on search/booking endpoints
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — burst-request scenario throttled

#### SEC-05: Audit every request DTO for mass-assignment exposure

**As a** platform security owner, **I want** have every request DTO audited to confirm it binds explicit fields onto entities via business methods, never raw entity binding, **so that** RULES.md §5.4's mass-assignment concern is verified across every endpoint that shipped during Phase 1, not just the one it was originally called out on.

- **PRD reference(s):** §5.4 OWASP-relevant concerns (RULES.md, mass assignment)
- **Module(s)/Screen(s):** shared (security)
- **Story points:** 5 — Audit + remediation pass across the full endpoint surface built in Phase 1 — breadth-bound, not individually complex.
- **Dependencies:** FND-22
- **Testing tier(s):** unit
- **Labels:** backend, security, phase2

**Acceptance Criteria**
- Given every Phase 1 request-body-accepting endpoint is audited, when the audit runs, then each one binds onto a request DTO mapped explicitly onto the entity's business constructor/methods — no endpoint allows a client to set `status`, `ai_generated`, or any other entity-internal field via extra JSON fields.

**Sub-tasks**
- [NEW] Backend: mass-assignment audit checklist run against every Phase 1 endpoint
- [NEW] Backend: remediation for any endpoint found binding raw entities
- [NEW] Backend: unit test per remediated endpoint

#### SEC-06: Harden the dynamic per-Consultant CORS allow-list for production

**As a** platform security owner, **I want** have FND-08's dynamic CORS allow-list verified against production domain-mapping edge cases (domain removal, re-mapping, expired CNAME), **so that** the MVP's dynamic CORS mechanism holds under real domain-lifecycle churn, not just the happy-path mapping FND-08 tested.

- **PRD reference(s):** §5.4 OWASP-relevant concerns (RULES.md, CORS)
- **Module(s)/Screen(s):** whitelabel
- **Story points:** 3 — Edge-case hardening pass on top of FND-08's already-built dynamic allow-list mechanism.
- **Dependencies:** FND-08
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, security, whitelabel, phase2

**Acceptance Criteria**
- Given a Consultant's CNAME domain is unmapped or reassigned, when a request from the old domain arrives, then CORS immediately reflects the current domain registry state — no stale allow-list entry persists past the unmapping.

**Sub-tasks**
- [EXTEND] Backend: CORS allow-list cache invalidation on domain unmapping/remapping
- [NEW] Backend: integrationTest — unmapping edge case

#### SEC-07: Audit PCI-DSS scope for the Stripe hosted-elements integration

**As a** compliance owner, **I want** have an independent audit confirm no raw card data ever reaches the Adren backend, **so that** PRD §24.4's PCI-scope-minimization NFR is externally verified, not just assumed correct from FIN-11's design.

- **PRD reference(s):** §24.4 NFR Payments & Wallet
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — Audit + any remediation surfaced by it, against FIN-11's already-built hosted-elements integration.
- **Dependencies:** FIN-11
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, security, payments, phase2

**Acceptance Criteria**
- Given the PCI-DSS scope audit runs against FIN-11's Stripe integration, when every code path is reviewed, then no raw PAN/CVV field exists in any DTO, log line, or database column — Stripe's hosted elements are confirmed as the sole card-data touchpoint.

**Sub-tasks**
- [NEW] Backend: PCI-DSS scope audit against the Stripe integration
- [NEW] Backend: remediation for any finding
- [NEW] Backend: regression test preventing raw card data from ever entering a DTO

#### SEC-08: Audit secrets/credential/PII log redaction across all modules

**As a** platform security owner, **I want** have every module's logging audited to confirm no secret, token, or full PII value is ever logged, **so that** RULES.md §6.2's redaction rule is verified across the full module set that shipped in Phase 1, not just spot-checked.

- **PRD reference(s):** §6.2 Structured logging standards (RULES.md)
- **Module(s)/Screen(s):** shared (security)
- **Story points:** 5 — Audit + remediation pass, structurally similar to SEC-05 but targeting logging rather than request binding.
- **Dependencies:** FND-24
- **Testing tier(s):** unit
- **Labels:** backend, security, observability, phase2

**Acceptance Criteria**
- Given the log-redaction audit runs across every module, when log statements are reviewed, then no supplier/BYOS/Meta credential, payment token, or full traveler PII value appears in any log line or exception message — masking is applied wherever a request/response body might be logged.

**Sub-tasks**
- [NEW] Backend: log-redaction audit checklist run against every module
- [NEW] Backend: remediation for any unmasked secret/PII log statement found
- [NEW] Backend: unit test per remediated log statement

#### SEC-09: Wire dependency vulnerability scanning into the CI gate

**As a** engineering team, **I want** have every Gradle and npm dependency scanned for known vulnerabilities on every PR, **so that** the platform doesn't ship a known-CVE dependency silently, extending OPS-05's CI gate list.

- **PRD reference(s):** §8 PR / Code Review Checklist (RULES.md)
- **Module(s)/Screen(s):** Infra (CI)
- **Story points:** 3 — Standard CI tooling addition (e.g. Dependabot/Snyk/OWASP Dependency-Check) on top of OPS-05's existing pipeline.
- **Dependencies:** OPS-05
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, security, phase2

**Acceptance Criteria**
- Given a PR introduces a dependency with a known Critical/High CVE, when CI runs, then the vulnerability scan fails the build, blocking merge until resolved.

**Sub-tasks**
- [NEW] Infra: dependency vulnerability scanning (Gradle + npm) wired into the CI gate
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### SEC-10: Complete an external security review / pentest engagement sign-off

**As a** Super Admin, **I want** have an external security firm's sign-off before GA, **so that** the platform's security posture is independently validated beyond internal review, closing out the security hardening epic.

- **PRD reference(s):** §7 Assumptions, Dependencies, Risks
- **Module(s)/Screen(s):** shared (security)
- **Story points:** 5 — Process/coordination-heavy deliverable depending on SEC-02's remediation being complete first.
- **Dependencies:** SEC-02
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, security, phase2

**Acceptance Criteria**
- Given the external engagement completes, when the sign-off report is reviewed, then all Critical/High findings from SEC-02's internal pentest and this external review are resolved, and the sign-off is recorded before GA.

**Sub-tasks**
- [NEW] Backend: external pentest engagement scoping + execution
- [NEW] Backend: remediation of any new external-review findings
- [NEW] Backend: sign-off recorded as a release gate

---

## Compliance Execution

*12 stories, 73 story points.*

#### CMP-01: Finalize India GST/TCS rates and mechanics with tax counsel

**As a** Consultant, **I want** have India GST/TCS calculated using confirmed rates and mechanics, not FIN-17's illustrative placeholder, **so that** PRD §12.1 Example C and §19's open item are formally resolved, replacing the MVP's config-flagged illustrative calculation.

- **PRD reference(s):** §12.1 Worked Example C; §19 Open Items
- **Module(s)/Screen(s):** payments, compliance
- **Story points:** 8 — Depends entirely on external tax-counsel sign-off before the real implementation can be finalized — scope is the upper bound assuming straightforward confirmation.
- **Dependencies:** FIN-17
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, compliance, payments, phase2

**Acceptance Criteria**
- Given tax-counsel sign-off is received on exact GST/TCS rates and mechanics, when the config flag from FIN-17 is updated, then the calculation layer applies the confirmed rates in production, with the illustrative-rate flag removed.

**Sub-tasks**
- [EXTEND] Backend: confirmed GST/TCS rates and mechanics replace FIN-17's illustrative config
- [NEW] Backend: unit test — confirmed-rate calculation
- [NEW] Backend: integrationTest

> ⚠️ **NEEDS CLARIFICATION:** PRD §19: exact GST/TCS rates and mechanics require tax-counsel sign-off — this story cannot be finalized until that sign-off is received; scope/estimate may change once the real mechanics are known.

#### CMP-02: Finalize UK TOMS VAT rate and mechanics with UK tax counsel

**As a** Consultant, **I want** have UK TOMS VAT calculated using the confirmed rate and mechanics, not FIN-18's illustrative placeholder, **so that** PRD §12.1 Example D and §19's open item are formally resolved.

- **PRD reference(s):** §12.1 Worked Example D; §19 Open Items
- **Module(s)/Screen(s):** payments, compliance
- **Story points:** 8 — Same external-dependency shape as CMP-01, for the UK market.
- **Dependencies:** FIN-18
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, compliance, payments, phase2

**Acceptance Criteria**
- Given UK tax-counsel sign-off is received on the exact TOMS VAT rate and mechanics, when the config flag from FIN-18 is updated, then the calculation layer applies the confirmed rate in production, with the illustrative-rate flag removed.

**Sub-tasks**
- [EXTEND] Backend: confirmed TOMS VAT rate and mechanics replace FIN-18's illustrative config
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest

> ⚠️ **NEEDS CLARIFICATION:** PRD §19: exact TOMS VAT rate and mechanics require UK tax-counsel sign-off — this story cannot be finalized until that sign-off is received.

#### CMP-03: Integrate UK ATOL license registration and certificate issuance

**As a** Consultant, **I want** have real ATOL certificates issued against Adren's own ATOL license registration, not just a stored reference field, **so that** PRD §17.1's UK ATOL requirement moves from BOK-15/BOK-11's MVP internal-reference implementation to a real license-backed integration.

- **PRD reference(s):** §17.1 Market-by-Market Requirements (UK); §20.11 Voucher (atol_certificate_reference)
- **Module(s)/Screen(s):** compliance
- **Story points:** 8 — Requires Adren's own real ATOL license registration as a precondition — an operational/legal dependency, not just code.
- **Dependencies:** BOK-15, BOK-11
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, compliance, phase2

**Acceptance Criteria**
- Given Adren's ATOL license registration is confirmed, when a UK dynamic flight+hotel package is booked, then a real ATOL certificate is issued against that registration and referenced on the Voucher, not a placeholder value.

**Sub-tasks**
- [EXTEND] Backend: real ATOL certificate issuance against Adren's confirmed license registration
- [NEW] Backend: integrationTest

#### CMP-04: Implement Danish Package Travel Act compliance

**As a** Consultant, **I want** have Denmark-market bookings comply with the Danish Package Travel Act, **so that** PRD §17.1's Denmark row is implemented as real platform enforcement, not just a KYC field.

- **PRD reference(s):** §17.1 Market-by-Market Requirements (Denmark)
- **Module(s)/Screen(s):** compliance
- **Story points:** 5 — New market-specific compliance gate following BOK-11's established ATOL-gate pattern.
- **Dependencies:** FND-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, compliance, phase2

**Acceptance Criteria**
- Given a Denmark-based Consultant publishes a package meeting the Danish Package Travel Act's package-definition criteria, when publish is attempted, then the platform enforces the Act's applicable disclosure/protection requirements before allowing publish, mirroring the UK ATOL gate's enforcement pattern.

**Sub-tasks**
- [NEW] Backend: `enforceDanishPackageTravelAct` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — publish-gate, mirrors UK ATOL pattern)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### CMP-05: Capture and enforce Australia ATAS accreditation

**As a** Consultant, **I want** have my ATAS accreditation captured at onboarding and enforced where required, **so that** PRD §17.1's Australia row and §13.1's ATAS-if-applicable KYC field move from capture-only (FND-04) to real enforcement.

- **PRD reference(s):** §17.1 Market-by-Market Requirements (Australia); §13.1 Consultant Onboarding
- **Module(s)/Screen(s):** compliance
- **Story points:** 5 — Enforcement layer on top of FND-04's already-captured ATAS field.
- **Dependencies:** FND-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, compliance, phase2

**Acceptance Criteria**
- Given an Australia-based Consultant's ATAS accreditation is not yet verified, when they attempt an action requiring it, then the platform blocks or flags the action per the confirmed enforcement rule, rather than only capturing the field at onboarding with no downstream check.

**Sub-tasks**
- [NEW] Backend: `enforceAtasAccreditation` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — action-gate for Australia-market Consultants)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### CMP-06: Capture and enforce USA state-level Seller of Travel registration

**As a** Consultant, **I want** have my state-level Seller of Travel registration (CA/FL/WA/HI/IA) captured and enforced, **so that** PRD §17.1's USA row and §22.9's T18 acceptance criterion are fully implemented, not just flagged at onboarding.

- **PRD reference(s):** §17.1 Market-by-Market Requirements (USA); §22.9 T18; §25 T18
- **Module(s)/Screen(s):** compliance
- **Story points:** 8 — Five-state-specific rule set (CA/FL/WA/HI/IA), each potentially with distinct requirements — broader than the single-rule market stories in this epic.
- **Dependencies:** FND-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, compliance, phase2

**Acceptance Criteria**
- Given a Consultant's declared home market is USA and their declared state is California, when they complete onboarding, then the system flags the California Seller of Travel registration requirement (T18), and blocks state-specific actions until registration is confirmed.

**Sub-tasks**
- [NEW] Backend: `enforceStateSellerOfTravel` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — per-state rule table, data-driven per §24.7)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### CMP-07: Verify Dubai/UAE DTCM trade license

**As a** Consultant, **I want** have my DTCM trade license verified as part of onboarding enforcement, **so that** PRD §17.1's Dubai/UAE row moves from capture-only to a real verification workflow.

- **PRD reference(s):** §17.1 Market-by-Market Requirements (Dubai/UAE); §13.1 Consultant Onboarding
- **Module(s)/Screen(s):** compliance
- **Story points:** 5 — Same enforcement-layer pattern as CMP-05, applied to the DTCM license.
- **Dependencies:** FND-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, compliance, phase2

**Acceptance Criteria**
- Given a Dubai/UAE-based Consultant's DTCM license is not yet verified, when they attempt a licensed action, then the platform blocks or flags the action per the confirmed verification workflow.

**Sub-tasks**
- [NEW] Backend: `enforceDtcmVerification` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — action-gate for Dubai/UAE-market Consultants)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### CMP-08: Implement India DPDP Act data-handling compliance

**As a** compliance owner, **I want** have India-market traveler PII handled per the Digital Personal Data Protection Act, **so that** PRD §17.1's India data-protection row is implemented as real data-handling controls.

- **PRD reference(s):** §17.1 Market-by-Market Requirements (India, Data Protection)
- **Module(s)/Screen(s):** compliance
- **Story points:** 5 — Data-handling controls scoped to the India market, parallel in shape to CMP-09's EU/UK GDPR story but a distinct legal framework.
- **Dependencies:** BOK-14
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, compliance, phase2

**Acceptance Criteria**
- Given India-market traveler PII is processed, when DPDP Act-relevant controls are evaluated, then consent capture, purpose limitation, and data-subject rights handling meet the Act's requirements.

**Sub-tasks**
- [NEW] Backend: `enforceDpdpDataHandling` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — India-scoped PII handling controls)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### CMP-09: Implement EU/UK GDPR data-subject rights (access/erasure/portability)

**As a** traveler (data subject), **I want** be able to exercise GDPR access, erasure, and portability rights on my data held by Adren, **so that** PRD §17.2's EU/UK data residency/GDPR requirement is implemented as real data-subject-rights tooling, pending PINF-06's residency decision.

- **PRD reference(s):** §17.2 Platform Enforcement Requirements; §19 Open Items
- **Module(s)/Screen(s):** compliance
- **Story points:** 8 — Cross-module data-subject-rights tooling (booking, payments, ai all hold traveler-linked data) — broad reach, and blocked on PINF-06's residency decision for full correctness.
- **Dependencies:** PINF-06, BOK-14
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, compliance, phase2

**Acceptance Criteria**
- Given an EU/UK traveler submits a data-access request, when it is processed, then all PII held about them across `booking`/`payments`/`ai` modules is compiled and returned within the GDPR-mandated response window.
- Given an erasure request is submitted, when it is processed, then the data is erased or anonymized per GDPR, respecting any legal-retention exceptions (e.g. financial records).

**Sub-tasks**
- [NEW] Backend: cross-module PII compilation for access requests
- [NEW] Backend: erasure/anonymization workflow respecting legal-retention exceptions
- [NEW] Backend: integrationTest — full access-request round trip

> ⚠️ **NEEDS CLARIFICATION:** PRD §19: the EU/UK data residency approach is an open item — this story's erasure/retention logic may need rework depending on PINF-06's resolved residency decision.

#### CMP-10: Re-trigger the KYC checklist at production scale when a Consultant's market changes

**As a** Super Admin, **I want** have a Consultant's KYC checklist re-trigger correctly for their new market when they relocate, even under real production data volume, **so that** PRD §23.6 Edge Case #13 and T18 hold under production scale and concurrent-edit conditions the MVP's FND-04 rule table wasn't stress-tested against.

- **PRD reference(s):** §23.6 Edge Case #13; §25 T18
- **Module(s)/Screen(s):** compliance
- **Story points:** 5 — Production-scale/concurrency hardening of a rule that FND-04's MVP rule table already implements at small scale.
- **Dependencies:** FND-04, FND-17
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, compliance, phase2

**Acceptance Criteria**
- Given a Consultant's declared home market changes after onboarding (e.g. relocates India → UK) under production load, when the change is saved, then the system re-triggers the KYC checklist for the new market rather than leaving the account under the original market's rules, verified under concurrent-access conditions.

**Sub-tasks**
- [EXTEND] Backend: market-change KYC re-trigger hardened for concurrent-edit safety
- [NEW] Backend: integrationTest — concurrent market-change scenario

#### CMP-11: Track whether Adren itself requires market-specific licensing

**As a** Super Admin, **I want** have a documented, tracked determination of whether Adren itself (not just its Consultants) needs market-specific licensing in each of the six markets, **so that** PRD §19's open item is resolved and its operational implication (if any) is tracked, not left implicit.

- **PRD reference(s):** §19 Open Items for Business Confirmation
- **Module(s)/Screen(s):** compliance
- **Story points:** 3 — Tracking/gating mechanism; the legal determination itself is a business process outside engineering scope.
- **Dependencies:** None
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, compliance, phase2

**Acceptance Criteria**
- Given legal counsel determines Adren itself requires licensing in a given market, when the determination is recorded, then it is tracked against that market's operational readiness, gating GA launch in that market if unresolved.

**Sub-tasks**
- [NEW] Infra: per-market Adren-licensing determination tracker, gating GA launch in that market
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

> ⚠️ **NEEDS CLARIFICATION:** PRD §19: whether Adren itself requires market-specific licensing is an open item for business confirmation — this story implements the tracking mechanism, not the legal determination itself.

#### CMP-12: Harden the data-driven KYC rule engine for production rule changes without a release cycle

**As a** compliance owner, **I want** update a market's KYC rules (e.g. a new US state's Seller of Travel requirement) without waiting for a full platform release, **so that** PRD §24.7's NFR is fully realized at production maturity — FND-04's data-driven rule table is administrable, not just data-driven in schema.

- **PRD reference(s):** §24.7 NFR Regional Compliance
- **Module(s)/Screen(s):** compliance
- **Story points:** 5 — Adds an administrable interface on top of FND-04's already-data-driven rule table — the MVP made it data-driven in the database; this makes it editable without a deploy.
- **Dependencies:** FND-04, CMP-06
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, compliance, phase2

**Acceptance Criteria**
- Given a market rule changes (e.g. a new required KYC field), when an authorized compliance owner updates the rule table via an admin interface, then the change takes effect for new onboarding sessions without a backend deploy.

**Sub-tasks**
- [EXTEND] Backend: `updateKycRuleTable` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `PUT /api/v1/compliance/kyc-rules/{market}`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [NEW] Frontend: `useKycRuleAdmin` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `KycRuleAdmin.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

---

## Performance/Load Testing

*8 stories, 40 story points.*

#### PERF-01: Build a load-test harness for the search aggregation SLA

**As a** platform reliability owner, **I want** load-test search aggregation against PRD §24.1's low-single-digit-second SLA for cached/normalized inventory, **so that** the search path holds its NFR under realistic concurrent load, not just functional-test conditions.

- **PRD reference(s):** §24.1 NFR Booking Engine
- **Module(s)/Screen(s):** Infra (performance test)
- **Story points:** 5 — New load-test harness (k6 or Gatling) targeting an already-built endpoint (FND-13's search).
- **Dependencies:** FND-13, SUP-02, SUP-04, SUP-06
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, performance, phase2

**Acceptance Criteria**
- Given a k6/Gatling scenario simulates realistic concurrent search volume, when the test runs, then p95 latency for cached/normalized inventory categories stays within the low-single-digit-second SLA.

**Sub-tasks**
- [NEW] Backend: k6/Gatling search-load scenario
- [NEW] Backend: p95/p99 latency assertion against the §24.1 SLA
- [NEW] Backend: CI-runnable load-test job (non-blocking, reported)

#### PERF-02: Load-test concurrent booking race conditions at scale

**As a** platform reliability owner, **I want** validate BOK-16's optimistic-locking behavior under real concurrent load, not just a two-request unit test, **so that** PRD §23.1 Edge Case #1 holds at production-realistic concurrency levels.

- **PRD reference(s):** §23.1 Edge Case #1; §25 T21
- **Module(s)/Screen(s):** Infra (performance test)
- **Story points:** 5 — Load-test harness targeting BOK-16's already-built `@Version` mechanism at higher concurrency than its unit/integration test covers.
- **Dependencies:** BOK-16
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, performance, phase2

**Acceptance Criteria**
- Given N concurrent requests target the last available unit of the same inventory item, when the load test runs, then exactly one succeeds and N-1 fail gracefully with 'no longer available' — no duplicate booking occurs at any tested concurrency level.

**Sub-tasks**
- [NEW] Backend: concurrent-booking load-test scenario (N simultaneous requests on one contended unit)
- [NEW] Backend: assertion — exactly one success, N-1 graceful failures, zero duplicates

#### PERF-03: Load-test supplier fan-out under partial supplier degradation

**As a** platform reliability owner, **I want** validate that one supplier's simulated downtime doesn't degrade search latency for the others under real concurrent load, **so that** PRD §24.2's circuit-breaker isolation NFR holds under load, not just SUP-13's tuned configuration in isolation.

- **PRD reference(s):** §24.2 NFR Supplier Integration
- **Module(s)/Screen(s):** Infra (performance test)
- **Story points:** 5 — Load-test scenario specifically validating SUP-13's per-supplier isolation under concurrency.
- **Dependencies:** SUP-13
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, performance, phase2

**Acceptance Criteria**
- Given one supplier is simulated as degraded/down under concurrent search load, when the load test runs, then the other suppliers' search latency remains within SLA, and the degraded supplier's circuit breaker trips per SUP-13's tuned thresholds without affecting the others.

**Sub-tasks**
- [NEW] Backend: supplier-degradation load-test scenario (one supplier simulated down under load)
- [NEW] Backend: assertion — other suppliers' latency unaffected, degraded supplier's breaker trips independently

#### PERF-04: Load-test AI itinerary generation latency under concurrent load

**As a** platform reliability owner, **I want** validate AI-13's bounded per-segment timeout holds under realistic concurrent AI request volume, **so that** PRD §24.3's NFR holds at production concurrency, informing LLM-05's production SLO alerting thresholds.

- **PRD reference(s):** §24.3 NFR AI Governance
- **Module(s)/Screen(s):** Infra (performance test)
- **Story points:** 5 — Load-test harness targeting AI-13/LLM-02's already-built timeout mechanism under concurrency.
- **Dependencies:** AI-13, LLM-05
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, performance, phase2

**Acceptance Criteria**
- Given concurrent AI itinerary-completion requests are load-tested, when the test runs, then per-segment latency stays within AI-13's bounded timeout at the tested concurrency level, or the system degrades to AI-05's explicit failure state rather than an unbounded hang.

**Sub-tasks**
- [NEW] Backend: concurrent AI-request load-test scenario
- [NEW] Backend: latency/timeout assertion under load

#### PERF-05: Load-test wallet ledger write contention

**As a** platform reliability owner, **I want** validate FIN-10's idempotent, atomic wallet writes hold under concurrent debit/hold contention on the same wallet, **so that** PRD §24.4's atomicity/idempotency NFR holds at production concurrency, not just FIN-10's two-writer integrationTest.

- **PRD reference(s):** §24.4 NFR Payments & Wallet
- **Module(s)/Screen(s):** Infra (performance test)
- **Story points:** 5 — Load-test harness targeting FIN-08/FIN-10's already-built DB-constraint-backed wallet writes under higher concurrency.
- **Dependencies:** FIN-10
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, performance, phase2

**Acceptance Criteria**
- Given N concurrent debit/hold requests target the same Consultant's wallet, when the load test runs, then the final balance is correct with zero double-debits at every tested concurrency level, consistent with FIN-10's idempotency guarantee.

**Sub-tasks**
- [NEW] Backend: concurrent wallet-write load-test scenario
- [NEW] Backend: balance-correctness assertion under load

#### PERF-06: Establish a frontend performance budget for map/results rendering under large pin counts

**As a** frontend engineer, **I want** have a defined performance budget for the map+results split-panel screens as pin/result counts grow, **so that** frontend-best-practices §2's map-rendering guidance (memoized markers once real map integration lands) is verified against a measured budget, not just implemented and assumed sufficient.

- **PRD reference(s):** frontend-best-practices skill §2 (Map rendering)
- **Module(s)/Screen(s):** Search Dashboard (21.1), Itinerary Builder (21.2)
- **Story points:** 5 — Performance-budget definition + measurement harness against FES-05's MapPanel component, informed by frontend-best-practices' explicit prediction of this exact jank risk.
- **Dependencies:** FES-05
- **Testing tier(s):** component test
- **Labels:** frontend, performance, phase2

**Acceptance Criteria**
- Given a search result set with a large number of location pins is rendered, when render performance is measured, then it stays within the defined frame-budget threshold, with `React.memo`'d pin/marker components confirmed not to re-render on unrelated state changes (e.g. a date-picker interaction).

**Sub-tasks**
- [NEW] Frontend: performance budget definition for MapPanel/ResultsPanel rendering
- [NEW] Frontend: render-count/timing measurement harness
- [NEW] Frontend: component test — unrelated state change does not re-render unchanged pins

#### PERF-07: Validate the 10-minute itinerary target end-to-end under realistic network conditions

**As a** product owner, **I want** confirm the median search-to-complete-itinerary time stays within PRD §2's ≤10-minute target under realistic (not just local-dev) network latency, **so that** PRD §2's Goals & Success Metrics target and §9.6's NFR are validated holistically, not just per-component (search SLA, AI latency, etc. individually).

- **PRD reference(s):** §2 Goals & Success Metrics; §9.6 NFR (10-minute target)
- **Module(s)/Screen(s):** Infra (performance test)
- **Story points:** 5 — Composite end-to-end timing validation across PERF-01/03/04's individually-validated component SLAs.
- **Dependencies:** PERF-01, PERF-03, PERF-04
- **Testing tier(s):** e2e
- **Labels:** performance, phase2

**Acceptance Criteria**
- Given a realistic end-to-end itinerary-build session (search → default selection → AI completion → save as Quotation) is run under simulated realistic network latency, when timing is measured, then the median time stays at or below 10 minutes.

**Sub-tasks**
- [NEW] Backend/Frontend: end-to-end timed Playwright scenario simulating realistic network latency
- [NEW] Backend/Frontend: median-time assertion against the 10-minute target

#### PERF-08: Validate capacity planning and autoscaling policy against projected GMV growth

**As a** platform reliability owner, **I want** confirm PINF-05's autoscaling policy actually scales ahead of projected GMV/booking-volume growth, not just current load, **so that** the platform's production topology is validated against a growth projection, not just today's load.

- **PRD reference(s):** §2 Goals & Success Metrics (Revenue/GMV)
- **Module(s)/Screen(s):** Infra (performance test)
- **Story points:** 5 — Capacity-planning validation exercise against PINF-05's already-defined topology and PINF-07's deployment pipeline.
- **Dependencies:** PINF-05, PINF-07
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, performance, phase2

**Acceptance Criteria**
- Given load is ramped to match a projected future GMV/booking-volume milestone, when autoscaling is observed, then PINF-05's topology scales out ahead of saturation, and the 99.5%+ uptime target holds throughout the ramp.

**Sub-tasks**
- [NEW] Backend: capacity-ramp load-test scenario against a projected GMV milestone
- [NEW] Backend: autoscaling-behavior and uptime assertion during the ramp

---

## Production Observability

*8 stories, 38 story points.*

#### OBS-01: Wire OTel/Zipkin tracing export for production correlation IDs

**As a** on-call engineer, **I want** have FND-21's correlation-ID propagation exported to a real tracing backend in production, **so that** RULES.md §6.1's traceId propagation across the async event-listener boundary is queryable in production, not just verified by FND-21's local integrationTest.

- **PRD reference(s):** §6.1 Correlation IDs (RULES.md)
- **Module(s)/Screen(s):** shared (observability)
- **Story points:** 5 — Exporter wiring on top of FND-21's already-built propagation mechanism — `spring-modulith-observability` is on the classpath but unconfigured per RULES.md's explicit reconciliation note.
- **Dependencies:** FND-21
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, observability, phase2

**Acceptance Criteria**
- Given a production request triggers an async event-listener chain, when the trace is exported, then the same traceId is queryable across the request span and every downstream listener span in the production tracing backend.

**Sub-tasks**
- [NEW] Backend: OTel/Zipkin exporter configuration in production `application.yml`
- [NEW] Backend: integrationTest — traceId queryable across the async boundary in the exported trace

#### OBS-02: Ship structured JSON logs to production log aggregation with a retention policy

**As a** on-call engineer, **I want** have FND-24's structured JSON logs shipped to a production log aggregator with a defined retention policy, **so that** log aggregation/search is actually usable as the primary debugging tool in production across six jurisdictions, not just structured locally.

- **PRD reference(s):** §6.2 Structured logging standards (RULES.md)
- **Module(s)/Screen(s):** shared (observability)
- **Story points:** 5 — Shipping/retention infra on top of FND-24's already-built structured logging.
- **Dependencies:** FND-24
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, observability, phase2

**Acceptance Criteria**
- Given a production log line is emitted, when it is shipped, then it lands in the log aggregator with all mandatory MDC fields (traceId, consultantId, currency/market where applicable) intact, retained per the defined policy.

**Sub-tasks**
- [NEW] Infra: production log-shipping pipeline with a defined retention policy
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### OBS-03: Provide production retention and query tooling for the AI governance audit trail

**As a** Super Admin/compliance owner, **I want** query the AI suggestion audit trail (AI-07/AI-08) in production with retention independent of the application-log window, **so that** RULES.md §6.3's explicit distinction — audit trail retention/immutability differs from sampled/rotated application logs — is honored operationally, not just architecturally.

- **PRD reference(s):** §6.3 (RULES.md, AI governance audit)
- **Module(s)/Screen(s):** ai (observability)
- **Story points:** 5 — Production retention/query-tooling story on top of AI-07/AI-08's already-built insert-only audit table.
- **Dependencies:** AI-07, AI-08
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, observability, ai, phase2

**Acceptance Criteria**
- Given a compliance review needs an AI suggestion from 6 months prior, when a query is run against the production audit-trail store, then the record is retrievable, unaffected by the application-log aggregator's shorter retention window.

**Sub-tasks**
- [NEW] Backend: production retention policy for `ai_suggestion_audit_log`, independent of app-log retention
- [NEW] Backend: query tooling/interface for compliance review
- [NEW] Backend: integrationTest — retrieval beyond the app-log retention window

#### OBS-04: Provide production retention for compliance state-transition audit trails

**As a** compliance owner, **I want** have GST/TCS calculation inputs/outputs, ATOL disclosure completion, and KYC state changes queryable independent of app-log retention, **so that** RULES.md §6.3's compliance-audit retention requirement is honored operationally for a regulator/tax-authority query scenario.

- **PRD reference(s):** §6.3 (RULES.md, compliance audit)
- **Module(s)/Screen(s):** compliance (observability)
- **Story points:** 5 — Mirrors OBS-03's pattern for the `compliance` module's state-transition history (FIN-17/FIN-18/CMP-01–12).
- **Dependencies:** CMP-01, CMP-03, CMP-12
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, observability, compliance, phase2

**Acceptance Criteria**
- Given a tax authority asks 'what did the system calculate and why' for a booking from 6 months prior, when a query is run against the `compliance` module's own persisted audit trail, then GST/TCS inputs/outputs, ATOL disclosure state, and KYC checklist state changes for that booking are all retrievable, independent of the app-log retention window.

**Sub-tasks**
- [NEW] Backend: production retention policy for compliance state-transition audit records
- [NEW] Backend: query tooling for regulator/tax-authority review
- [NEW] Backend: integrationTest

#### OBS-05: Build per-supplier trace-span dashboards for latency/circuit-breaker visibility

**As a** on-call engineer, **I want** see each supplier's (Hotelbeds/STUBA/TBO/Mystifly/Transferz/Widgety/HBActivities) latency and circuit-breaker state individually, not collapsed into one aggregate search span, **so that** RULES.md §6.3's explicit per-supplier trace-span requirement is realized as an actual dashboard, feeding SUP-13's circuit-breaker tuning.

- **PRD reference(s):** §6.3 (RULES.md, per-supplier trace spans); §24.2 NFR Supplier Integration
- **Module(s)/Screen(s):** supplier (observability)
- **Story points:** 5 — Dashboard built on top of OBS-01's tracing export, specifically surfacing the per-supplier span granularity RULES.md §6.3 calls out.
- **Dependencies:** OBS-01
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, observability, supplier, phase2

**Acceptance Criteria**
- Given a single search request fans out to all seven live suppliers in parallel, when the trace is inspected, then each supplier's latency/error is visible as its own distinct span on the dashboard, with one slow supplier clearly identifiable rather than hidden in an aggregate 'search' span.

**Sub-tasks**
- [NEW] Backend: per-supplier trace-span dashboard
- [NEW] Backend: integrationTest — one distinct span per supplier in a fan-out search

#### OBS-06: Add production alerting for supplier auth failure, sync staleness, and credit-limit breach spikes

**As a** on-call engineer, **I want** be alerted on a supplier auth failure, inventory-sync staleness beyond threshold, or an unusual spike in credit-limit breaches, **so that** PRD §10.2's Super-Admin-alert requirement and §10.5's staleness-alert requirement are wired to real production alerting, not just logged.

- **PRD reference(s):** §10.2 Per-Supplier Integration (auth failure alerting); §10.5 Inventory Sync (staleness alerting)
- **Module(s)/Screen(s):** Infra (observability)
- **Story points:** 5 — Alert-rule wiring on top of already-built failure-handling logic (Phase 1's supplier error tables and HRD-13's staleness alerting) plus a new anomaly-spike rule.
- **Dependencies:** HRD-13, OBS-01
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, observability, phase2

**Acceptance Criteria**
- Given a supplier auth failure (e.g. Hotelbeds `INVALID_SIGNATURE`) occurs in production, when the alert rule evaluates, then on-call is paged, and that supplier's results are disabled per §10.2.1's error-handling table until resolved.
- Given credit-limit breaches spike unusually across Consultants, when the alert rule evaluates, then on-call is notified of the anomaly for investigation.

**Sub-tasks**
- [NEW] Backend: production alert rules — supplier auth failure, sync staleness, credit-limit-breach spike
- [NEW] Backend: integrationTest — one simulated trigger per alert rule

#### OBS-07: Build an uptime/SLO dashboard for the 99.5%+ booking-engine target

**As a** Super Admin/on-call engineer, **I want** see the booking engine's real-time and historical uptime against the 99.5%+ SLO, **so that** PRD §2's Goals & Success Metrics target is continuously visible, not just assumed met.

- **PRD reference(s):** §2 Goals & Success Metrics (Platform reliability)
- **Module(s)/Screen(s):** Infra (observability)
- **Story points:** 3 — Standard SLO dashboard on top of OBS-01/PINF-05's health-check and tracing infrastructure.
- **Dependencies:** OBS-01, PINF-05
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, observability, phase2

**Acceptance Criteria**
- Given the uptime dashboard is opened, when it loads, then current and historical uptime against the 99.5%+ SLO is visible, with any SLO-breaching window clearly flagged.

**Sub-tasks**
- [NEW] Backend: uptime/SLO dashboard against the 99.5%+ target
- [NEW] Backend: SLO-breach flagging

#### OBS-08: Add ad-spend/campaign anomaly alerting in production

**As a** Super Admin, **I want** be alerted if a campaign's real Meta spend pattern looks anomalous, **so that** PRD §14.3's guardrail and §24.6's near-real-time NFR are backed by proactive alerting, not just MADS-05's reconciliation-on-read enforcement.

- **PRD reference(s):** §14.3 Controls & Guardrails; §24.6 NFR Ads/Campaign
- **Module(s)/Screen(s):** ads (observability)
- **Story points:** 5 — Proactive anomaly-detection alerting layered on top of MADS-04/MADS-05's already-built spend-tracking data.
- **Dependencies:** MADS-05, OBS-01
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, observability, ads, phase2

**Acceptance Criteria**
- Given a Live campaign's real Meta spend rate deviates sharply from its historical/expected pace, when the anomaly rule evaluates, then Super Admin is alerted proactively, ahead of MADS-05's spend-cap reconciliation catching it only at the cap boundary.

**Sub-tasks**
- [NEW] Backend: ad-spend anomaly-detection alert rule
- [NEW] Backend: integrationTest — simulated anomalous spend pattern triggers the alert

---
