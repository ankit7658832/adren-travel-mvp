# ADREN TRAVEL — Phase 1 (MVP Mock Development) User Stories

Generated from `doc/PRD_v2_detailed.md`. Every story cites the PRD section(s) it derives from, the backend module (per `backend/README.md`'s module table) and/or frontend screen (per PRD Part 21) it touches, Given/When/Then acceptance criteria (pulled from PRD Part 22 where a matching criterion exists, else newly written in the same style, cross-referenced to PRD Part 25 test scenario IDs where applicable), a Fibonacci story-point estimate with reasoning, explicit dependencies, required testing tier(s) per the `testing-strategy` skill, and a Jira sub-task breakdown tagged `[NEW]` (net-new work) vs. `[EXTEND]`/`[REUSE]` (builds on the `booking`/`supplier`/`notification` backend reference implementation or the `search-dashboard` frontend reference screen).

Stories marked with a **⚠️ NEEDS CLARIFICATION** note implement the PRD's illustrative/placeholder shape only, pending a business or legal decision flagged in PRD Part 19 (Open Items) — they are not blocked from starting, but cannot be marked Done against final business rules until that decision lands.

## Summary

| Epic | Story Count | Total Story Points |
|---|---|---|
| Foundation | 24 | 124 |
| Booking Core | 20 | 98 |
| Financial Layer | 18 | 95 |
| AI Layer | 13 | 72 |
| Local DMC + BYOS | 11 | 57 |
| Ads/Campaign Management | 15 | 80 |
| Hardening | 13 | 76 |
| Frontend Shell | 10 | 55 |
| DevOps/Infra | 9 | 30 |
| Test Infrastructure | 9 | 38 |
| **Total** | **142** | **725** |

---

## Table of Contents

- [Foundation](#foundation) (24 stories)
- [Booking Core](#booking-core) (20 stories)
- [Financial Layer](#financial-layer) (18 stories)
- [AI Layer](#ai-layer) (13 stories)
- [Local DMC + BYOS](#local-dmc-+-byos) (11 stories)
- [Ads/Campaign Management](#adscampaign-management) (15 stories)
- [Hardening](#hardening) (13 stories)
- [Frontend Shell](#frontend-shell) (10 stories)
- [DevOps/Infra](#devopsinfra) (9 stories)
- [Test Infrastructure](#test-infrastructure) (9 stories)

---

## Foundation

*24 stories, 124 story points.*

#### FND-01: Stand up stateless Spring Security with role- and tenant-aware principal

**As a** backend engineer, **I want** authenticate every request and attach a principal carrying user ID, role, and consultant_id, **so that** every module can enforce the PRD §6 role matrix instead of the platform having zero authorization as it does today.

- **PRD reference(s):** §5.1 (RULES.md); §6 Roles & Permissions Matrix
- **Module(s)/Screen(s):** shared (cross-cutting security infra)
- **Story points:** 8 — Net-new cross-cutting infra (JWT filter chain, principal model) touching every future controller — highest-risk foundational piece.
- **Dependencies:** None
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, foundation, security, phase1

**Acceptance Criteria**
- Given an unauthenticated request hits any non-public endpoint, when it is received, then the platform returns 401 rather than serving the request.
- Given a valid JWT is presented, when the request is processed, then the principal exposes userId, role (SUPER_ADMIN/CONSULTANT/USER), and consultant_id (null only for SUPER_ADMIN).

**Sub-tasks**
- [NEW] Backend: `SecurityConfig` + JWT filter chain (stateless, no session)
- [NEW] Backend: `AdrenPrincipal` carrying userId/role/consultantId
- [NEW] Backend: unit test (filter chain, token parsing, 401/403 paths)
- [NEW] Backend: module test (principal available to a sample secured endpoint)

#### FND-02: Enforce PRD §6 role matrix via method-level @PreAuthorize on module Api interfaces

**As a** Super Admin, **I want** have role checks enforced on the Api interface itself, not just at the controller, **so that** every caller (a future scheduled job, another module, a controller) inherits the same authorization guarantee per RULES.md §5.1.

- **PRD reference(s):** §5.1 (RULES.md); §6 Roles & Permissions Matrix
- **Module(s)/Screen(s):** booking, supplier, shared
- **Story points:** 5 — Well-understood pattern (@PreAuthorize expressions) but must be threaded across every existing Api method.
- **Dependencies:** FND-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, foundation, security, phase1

**Acceptance Criteria**
- Given a USER principal calls a method reserved for CONSULTANT/SUPER_ADMIN (e.g. onboarding a Local DMC), when the call reaches the Api layer, then it is rejected with 403 regardless of which controller/listener invoked it.
- Given a capability is marked 'No (unless granted)' for USER in §6 (e.g. create package), when the per-Consultant grant flag is false, then the call is rejected; when the flag is true, it succeeds.

**Sub-tasks**
- [NEW] Backend: `@PreAuthorize` expressions on `BookingApi`/`SupplierSearchApi` methods
- [NEW] Backend: per-Consultant capability-grant flag (data-driven, not a role switch) for 'unless granted' cases
- [NEW] Backend: unit test per role/capability combination
- [NEW] Backend: module test asserting a listener/job path also gets the check

#### FND-03: Close tenant-isolation gap on itinerary/booking lookups

**As a** Consultant, **I want** be certain another Consultant can never read or act on my itinerary even if they guess or observe its UUID, **so that** the platform is not exposed to Broken Object Level Authorization (OWASP API1:2023), which RULES.md §5.2 flags as the top realistic risk.

- **PRD reference(s):** §5.2 (RULES.md); §22.6 BYOS (scoping pattern)
- **Module(s)/Screen(s):** booking
- **Story points:** 5 — Security-critical fix scoped to existing BookingServiceImpl/ItineraryController call sites; well-bounded.
- **Dependencies:** FND-01, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, foundation, security, phase1

**Acceptance Criteria**
- Given Consultant B calls `saveAsQuotation` with an itinerary_id belonging to Consultant A, when the authenticated principal's consultant_id does not match the itinerary's owner, then the request is rejected, not silently executed.
- Given SUPER_ADMIN calls the same endpoint, when the 'view/act on all' exception path is used, then it succeeds via its own explicitly `@PreAuthorize`'d path, not the default.

**Sub-tasks**
- [EXTEND] Backend: `BookingServiceImpl` methods re-fetch by (itineraryId, authenticated consultantId), never client-supplied consultantId alone
- [NEW] Backend: SUPER_ADMIN 'view all' explicit code path
- [NEW] Backend: unit test — cross-tenant access attempt
- [NEW] Backend: module test — cross-tenant access attempt end-to-end through the Api

#### FND-04: Super Admin onboards a Consultant via a market-driven KYC wizard

**As a** Super Admin, **I want** onboard a new Consultant through a multi-step form whose required fields change based on the selected home market, **so that** each Consultant's KYC requirements match PRD §13.1's per-market table without hardcoding a conditional per market.

- **PRD reference(s):** §13.1 Consultant Onboarding; §21.6 Super Admin Console; §24.7 (data-driven KYC)
- **Module(s)/Screen(s):** whitelabel, Super Admin Console (21.6)
- **Story points:** 8 — New entity + data-driven rules engine + multi-step wizard UI — the first schema-driven compliance surface in the platform.
- **Dependencies:** FND-01, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test, e2e
- **Labels:** backend, frontend, foundation, whitelabel, phase1

**Acceptance Criteria**
- Given Super Admin selects 'USA' as home market, when the wizard renders, then EIN/business registration and state-level Seller of Travel fields appear, sourced from data, not a hardcoded conditional.
- Given Super Admin selects 'India', when the wizard renders, then GST registration, business PAN, and IATA/TAAI fields appear as mandatory.

**Sub-tasks**
- [NEW] Backend: `Consultant` entity + `ConsultantRepository` (package-private, own Flyway migration)
- [NEW] Backend: `onboardConsultant` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `POST /api/v1/consultants` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
- [NEW] Frontend: `useConsultantOnboarding` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `ConsultantOnboardingWizard.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Backend: market→required-fields rule table (data-driven, per RULES.md §24.7)

#### FND-05: Super Admin manages Consultant lifecycle (view, suspend, reinstate)

**As a** Super Admin, **I want** view all onboarded Consultants and change their status, **so that** I retain oversight of the Consultant base per PRD §3.1's Super Admin persona description.

- **PRD reference(s):** §3.1 Super Admin persona; §21.6 Super Admin Console
- **Module(s)/Screen(s):** whitelabel, Super Admin Console (21.6)
- **Story points:** 5 — Standard CRUD + status transition on top of FND-04's entity.
- **Dependencies:** FND-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, foundation, phase1

**Acceptance Criteria**
- Given Super Admin opens the Consultants list, when the page loads, then every Consultant is shown with status, home market, and onboarding date, paginated per RULES.md §3.4.
- Given Super Admin suspends a Consultant, when the action is confirmed, then that Consultant's Users can no longer search/book until reinstated.

**Sub-tasks**
- [EXTEND] Backend: `suspendConsultant/reinstateConsultant` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `PATCH /api/v1/consultants/{id}/status`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [NEW] Frontend: `useConsultantList` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `ConsultantList.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

#### FND-06: Configure Consultant white-label branding (logo, colors, domain)

**As a** Super Admin, **I want** configure a Consultant's logo, background image, primary/secondary text color, and CNAME domain mapping, **so that** each Consultant's storefront reflects their own brand per PRD §13.2.

- **PRD reference(s):** §13.2 Branding Configuration; §21.6 Super Admin Console
- **Module(s)/Screen(s):** whitelabel, Super Admin Console (21.6)
- **Story points:** 5 — New entity (BrandingProfile) plus file upload; UI is a form over that entity.
- **Dependencies:** FND-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, foundation, whitelabel, phase1

**Acceptance Criteria**
- Given Super Admin uploads a logo and sets primary/secondary colors for a Consultant, when the change is saved, then the Consultant's theme tokens update.
- Given Super Admin maps a CNAME domain to a Consultant, when the mapping is saved, then the domain registry used by dynamic CORS (FND-08) reflects it.

**Sub-tasks**
- [NEW] Backend: `BrandingProfile` entity + `BrandingProfileRepository` (package-private, own Flyway migration)
- [NEW] Backend: `updateBranding` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `PATCH /api/v1/consultants/{id}/branding` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)

#### FND-07: Propagate branding/domain changes to the live storefront without a redeploy

**As a** Consultant, **I want** see my branding and domain changes reflected on my live storefront within a short window, **so that** PRD §24.5's NFR is met and I don't have to wait for a platform release to see my own branding update.

- **PRD reference(s):** §24.5 NFR White-Label & Admin
- **Module(s)/Screen(s):** whitelabel
- **Story points:** 5 — Requires a cache-invalidation/propagation mechanism (e.g. short-TTL cache + event) rather than a full redeploy path.
- **Dependencies:** FND-06
- **Testing tier(s):** module (@ApplicationModuleTest), integration (Testcontainers)
- **Labels:** backend, foundation, whitelabel, phase1

**Acceptance Criteria**
- Given Super Admin saves a branding change for a Consultant, when the change is persisted, then the Consultant's storefront reflects it within the NFR's defined short window without a backend redeploy.

**Sub-tasks**
- [NEW] Infra: short-TTL branding cache + invalidation event on save
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)
- [NEW] Backend: unit test for cache invalidation trigger

#### FND-08: Enforce dynamic per-Consultant CORS allow-list for white-label domains

**As a** platform security owner, **I want** have CORS resolved per-request from the whitelabel domain registry, **so that** a wildcard CORS policy never becomes the default path to cross-tenant credential/session theft, per RULES.md §5.4.

- **PRD reference(s):** §5.4 OWASP concerns (RULES.md); §13.2 Branding Configuration
- **Module(s)/Screen(s):** whitelabel, shared
- **Story points:** 5 — Security-sensitive but well-scoped: one CORS filter reading FND-06's domain registry.
- **Dependencies:** FND-06
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, foundation, security, phase1

**Acceptance Criteria**
- Given a request originates from a Consultant's mapped CNAME domain, when CORS is evaluated, then it is allowed.
- Given a request originates from an unmapped/unknown origin, when CORS is evaluated, then it is rejected — no wildcard fallback exists anywhere in config.

**Sub-tasks**
- [NEW] Backend: dynamic CORS filter resolving allow-list from `whitelabel` domain registry
- [NEW] Backend: unit test — mapped vs. unmapped origin
- [NEW] Backend: module test — no wildcard present in any active profile

#### FND-09: Consultant manages Users (staff/sub-agents) under their own account

**As a** Consultant, **I want** add and manage Users under my account, **so that** my staff can search, build itineraries, and book products without me sharing my own login, per PRD §3.3.

- **PRD reference(s):** §3.2 Consultant persona; §3.3 User persona; §6 Roles & Permissions Matrix
- **Module(s)/Screen(s):** whitelabel
- **Story points:** 5 — Standard CRUD entity plus per-User capability-grant flags reused by FND-02's authorization checks.
- **Dependencies:** FND-01, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, foundation, phase1

**Acceptance Criteria**
- Given a Consultant adds a new User, when the User is created, then that User can log in scoped to the Consultant's consultant_id and cannot change markup, onboard suppliers, or manage branding.
- Given a Consultant grants a User the 'create package' permission, when the grant is saved, then that specific User can create packages while others under the same Consultant cannot.

**Sub-tasks**
- [NEW] Backend: `ConsultantUser` entity + `ConsultantUserRepository` (package-private, own Flyway migration)
- [NEW] Backend: `addUser/updateUserGrants` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `POST /api/v1/users` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
- [NEW] Frontend: `useUserManagement` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `UserManagement.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

#### FND-10: Super Admin manages Adren-owned supplier API credentials

**As a** Super Admin, **I want** add and update Hotelbeds/STUBA/TBO/Mystifly/Transferz/Widgety/HBActivities credentials with masked fields and an audit trail of who last changed them, **so that** supplier access can be rotated and reviewed without exposing raw secrets in the UI, per PRD §21.6.

- **PRD reference(s):** §21.6 Super Admin Console — Supplier credential management; §10.2 Per-Supplier Integration Requirements
- **Module(s)/Screen(s):** supplier, Super Admin Console (21.6)
- **Story points:** 8 — New entity + audit trail + masked-field UI, feeding 7 supplier integrations — foundational for the whole supplier module.
- **Dependencies:** FND-01, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, foundation, supplier, security, phase1

**Acceptance Criteria**
- Given Super Admin opens the credential screen for Hotelbeds, when the credential set is displayed, then the secret value is masked and only the last-modified user/timestamp is shown.
- Given Super Admin updates a supplier's credentials, when the change is saved, then an audit log entry records who changed it and when.

**Sub-tasks**
- [NEW] Backend: `SupplierCredential` entity + `SupplierCredentialRepository` (package-private, own Flyway migration)
- [NEW] Backend: `updateSupplierCredential` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `PUT /api/v1/suppliers/{supplierId}/credentials` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
- [NEW] Frontend: `useSupplierCredentials` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `SupplierCredentialManagement.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Backend: credential-change audit log (who/when, never the secret value)

#### FND-11: Store Adren-owned supplier credentials in Secrets Manager, not plaintext config

**As a** platform security owner, **I want** have Hotelbeds/STUBA/TBO/etc. credentials live in AWS Secrets Manager (LocalStack in dev) referenced by ARN, **so that** no real integration credential is ever a plaintext config value, committed file, or environment variable outside local Docker Compose, per RULES.md §5.3.

- **PRD reference(s):** §5.3 Secrets handling (RULES.md)
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Wires `aws-secretsmanager` (already a build dependency) into the credential save path.
- **Dependencies:** FND-10
- **Testing tier(s):** unit, integration (Testcontainers + LocalStack)
- **Labels:** backend, foundation, security, phase1

**Acceptance Criteria**
- Given a supplier credential is saved via FND-10's screen, when the write completes, then the secret is stored in Secrets Manager and only its ARN is persisted in Postgres.
- Given application config is inspected in any non-local profile, when no supplier credential appears, then only ARNs/references are present.

**Sub-tasks**
- [NEW] Backend: Secrets Manager write on credential save (LocalStack-backed in dev/test)
- [EXTEND] Backend: `SupplierCredential` entity stores ARN only
- [NEW] Backend: unit test (ARN persisted, no plaintext)
- [NEW] Backend: integrationTest against LocalStack Secrets Manager

#### FND-12: Store BYOS credentials as row-level, per-Consultant encrypted secrets

**As a** Consultant, **I want** have my BYOS supplier credentials encrypted at the row level and reachable only through my own tenant scope, **so that** another Consultant's BYOS credential read is never possible through the same lookup key, per RULES.md §5.3 and PRD §10.4.

- **PRD reference(s):** §5.3 Secrets handling (RULES.md); §10.4 BYOS
- **Module(s)/Screen(s):** supplier
- **Story points:** 8 — KMS envelope encryption + per-row secret pattern is materially different from FND-11's Secrets-Manager-by-ARN pattern — new mechanism, high blast-radius if wrong.
- **Dependencies:** FND-03, FND-11
- **Testing tier(s):** unit, integration (Testcontainers + LocalStack KMS)
- **Labels:** backend, foundation, security, phase1

**Acceptance Criteria**
- Given a Consultant saves their own Hotelbeds BYOS credentials, when the write completes, then the ciphertext is stored per-row with a KMS-wrapped data key, not in the shared Secrets Manager entry used for Adren's own credentials.
- Given Consultant B's service call attempts to read Consultant A's BYOS credential row, when the tenant-isolation check runs, then access is denied.

**Sub-tasks**
- [NEW] Backend: `ByosCredential` entity — ciphertext + wrapped data key columns
- [NEW] Backend: KMS envelope encryption service (LocalStack KMS in dev/test)
- [NEW] Backend: tenant-scoped read path (reuses FND-03's isolation check)
- [NEW] Backend: unit test (encryption round-trip)
- [NEW] Backend: integrationTest — cross-tenant read attempt denied

#### FND-13: Extend Search Dashboard with map-based multi-location, multi-select search

**As a** Consultant/User, **I want** enter multiple locations in a single search box and see them geocoded on a map, **so that** I can start building a multi-location itinerary in one search step, per PRD §9.1 Flow A steps 2–4.

- **PRD reference(s):** §9.1 Flow A (steps 1-4); §21.1 Search Dashboard; §22.1 Multi-Location Search
- **Module(s)/Screen(s):** booking, supplier, Search Dashboard (21.1) — EXISTING reference
- **Story points:** 8 — Extends the existing `search-dashboard` reference feature with real geocoding + a map panel — the current implementation is a mocked, single-shot hook per RULES.md §7.1's reconcile note.
- **Dependencies:** FND-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test, e2e
- **Labels:** backend, frontend, foundation, booking, phase1

**Acceptance Criteria**
- Given a Consultant enters 3+ locations in the search box, when they submit the search, then the system geocodes and displays a map pin for every location, even one with no inventory (T1).
- Given a search is in progress, when the Consultant edits the search box, then the in-progress search is cancelled rather than merging both result sets.

**Sub-tasks**
- [NEW] Backend: `geocodeAndSearch` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `POST /api/v1/search`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [EXTEND] Frontend: `useMultiLocationSearch` hook (React Query for server data per RULES.md §7.1)
- [EXTEND] Frontend: `SearchDashboard.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Frontend: Playwright e2e spec (extends `search-flow.spec.ts` pattern, PRD §9.1 flow)
- [EXTEND] Frontend: `MapPanel` shared component wired to geocoded pins

#### FND-14: Implement the Default Selection Algorithm for per-location product pre-selection

**As a** Consultant/User, **I want** have the system pre-select one default product per category per location using availability, my configured priority, margin, and rating in that order, **so that** I don't have to manually compare every option to get a reasonable starting itinerary, per PRD §9.2.

- **PRD reference(s):** §9.2 Default Selection Algorithm; §22.2 Default Selection Algorithm
- **Module(s)/Screen(s):** booking, supplier
- **Story points:** 5 — Well-specified 4-step ranking algorithm; the complexity is in wiring it against live aggregated supplier results.
- **Dependencies:** FND-13
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, foundation, booking, phase1

**Acceptance Criteria**
- Given multiple hotel options are available for a location, when the system auto-selects a default, then the selected option is the highest-margin confirmable option (T2).
- Given the Consultant has configured a preferred supplier, when auto-selection runs, then the preferred supplier's option is selected if available, overriding pure margin ranking (T3).

**Sub-tasks**
- [NEW] Backend: `DefaultSelectionService` implementing the 4-step ranking
- [EXTEND] Backend: wired into `SupplierAggregationService` result post-processing
- [NEW] Backend: unit test — each of the 4 tie-break steps
- [NEW] Backend: module test — end-to-end against aggregated results

#### FND-15: Surface 'Auto-selected: Best available match' label on defaulted line items

**As a** Consultant/User, **I want** see a visible label whenever the system has auto-selected a product for me, **so that** auto-selection is never mistaken for a deliberate manual choice, per PRD §9.2's explicit surfacing requirement.

- **PRD reference(s):** §9.2 Default Selection Algorithm; §22.2 T2/T3
- **Module(s)/Screen(s):** Itinerary Builder (21.2)
- **Story points:** 2 — Small, purely presentational addition once FND-14's flag exists on the line item.
- **Dependencies:** FND-14, FND-16
- **Testing tier(s):** component test
- **Labels:** frontend, foundation, phase1

**Acceptance Criteria**
- Given the UI displays an auto-selected item, when the Consultant views it, then a visible 'Auto-selected: Best available match' label is present.

**Sub-tasks**
- [NEW] Frontend: auto-selected badge on the per-location line-item card
- [NEW] Frontend: component test asserting the label renders when `autoSelected=true`

#### FND-16: Build the Itinerary Builder screen with per-location cards and alternate-selection panel

**As a** Consultant/User, **I want** see each location's auto-selected line item and open a side panel to swap it for an alternate, **so that** I can accept defaults or override any product before saving the itinerary, per PRD §21.2.

- **PRD reference(s):** §21.2 Itinerary Builder; §9.1 Flow A steps 5-6
- **Module(s)/Screen(s):** booking, Itinerary Builder (21.2) — NEW feature folder
- **Story points:** 8 — New feature folder, non-trivial state (per-location, per-category selection across up to 5 product types) — first real use of the Zustand draft store from FES.
- **Dependencies:** FND-13, FND-14, FES-03
- **Testing tier(s):** unit, component test, e2e
- **Labels:** backend, frontend, foundation, phase1

**Acceptance Criteria**
- Given a location card shows its default line item, when the Consultant clicks 'Change', then a side panel opens showing a filterable/sortable list (price, rating, supplier) of alternates for that category/location.
- Given the Consultant selects an alternate, when they confirm the change, then the line item updates and the auto-selected badge (FND-15) is removed.

**Sub-tasks**
- [NEW] Backend: `GET /api/v1/itineraries/{id}/alternates?location=&category=` endpoint
- [NEW] Frontend: `useItineraryBuilder` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `ItineraryBuilder.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Frontend: Playwright e2e spec (extends `search-flow.spec.ts` pattern, PRD §9.1 flow)

#### FND-17: Add locale/market selection alongside existing multi-currency support

**As a** Consultant, **I want** select my operating locale (English/Hindi/regional for India, English-primary elsewhere, Danish secondary for Denmark) alongside my settlement currency, **so that** the platform supports PRD §13.3's multi-language requirement using the same data-driven approach as currency.

- **PRD reference(s):** §13.3 Multi-Language & Multi-Currency; §12.2 Multi-Currency & FX Buffer
- **Module(s)/Screen(s):** whitelabel, shared
- **Story points:** 5 — Extends `shared.CurrencyCode`'s pattern with a parallel `LocaleCode`/market-language mapping; no i18n framework wiring included (frontend translation content is out of this story's scope).
- **Dependencies:** FND-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, foundation, whitelabel, phase1

**Acceptance Criteria**
- Given a Consultant's home market is Denmark, when they open language settings, then Danish is offered as a secondary language alongside English-primary.
- Given a Consultant's home market is India, when they open language settings, then Hindi/regional options are offered alongside English.

**Sub-tasks**
- [NEW] Backend: `LocaleCode` value type + market→available-locales mapping (mirrors `CurrencyCode`)
- [EXTEND] Backend: `Consultant` entity gains a `preferredLocale` field
- [NEW] Backend: unit test — market→locale mapping
- [NEW] Backend: module test

#### FND-18: Add a root and per-route ErrorBoundary

**As a** Consultant/User, **I want** see a graceful fallback instead of a blank white screen if any part of the app crashes, **so that** one feature's bug (e.g. the not-yet-built itinerary-builder) never takes down navigation or an in-progress booking on another screen, per RULES.md §7.4.

- **PRD reference(s):** §7.4 Error boundary strategy (RULES.md, reconciliation item #6)
- **Module(s)/Screen(s):** Frontend shell (all screens)
- **Story points:** 3 — Well-scoped per RULES.md's explicit spec; react-error-boundary library adoption plus wiring at two levels.
- **Dependencies:** FES-01
- **Testing tier(s):** component test
- **Labels:** frontend, foundation, phase1

**Acceptance Criteria**
- Given any component throws during render, when React unwinds to the nearest boundary, then a root-level boundary renders a generic 'something went wrong, reload' fallback as the last line of defense.
- Given a component inside one routed feature throws, when the error propagates, then only that route's boundary catches it — navigation and other in-progress screens remain usable.

**Sub-tasks**
- [NEW] Frontend: root `ErrorBoundary` wrapping the router in `main.tsx`
- [NEW] Frontend: per-route boundary wrapping each `<Route element={...}>`
- [NEW] Frontend: `useQueryErrorResetBoundary` wiring for query-originated failures
- [NEW] Frontend: component test — simulated throw at each boundary level

#### FND-19: Add ESLint flat config with jsx-a11y

**As a** frontend engineer, **I want** have `npm run lint` actually run something, including accessibility linting, **so that** the accessibility baseline in RULES.md §7.3 is enforced in CI rather than just hoped for — currently there is no ESLint config at all despite the dependencies being installed.

- **PRD reference(s):** §7.3 Accessibility baseline (RULES.md, reconciliation item #5)
- **Module(s)/Screen(s):** Frontend shell (tooling)
- **Story points:** 2 — Config-only change; `eslint-plugin-jsx-a11y` needs adding as a new devDependency.
- **Dependencies:** None
- **Testing tier(s):** module (lint run in CI)
- **Labels:** frontend, foundation, tooling, phase1

**Acceptance Criteria**
- Given `npm run lint` is run, when the command executes, then it lints the codebase using `eslint-plugin-jsx-a11y`, `eslint-plugin-react-hooks`, and `eslint-plugin-react-refresh` and exits non-zero on a violation.

**Sub-tasks**
- [NEW] Infra: `eslint.config.js` flat config with jsx-a11y + react-hooks + react-refresh plugins
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### FND-20: Resolve the `@/*` path-alias half-configuration

**As a** frontend engineer, **I want** have the `@/*` import alias either fully wired or fully removed, **so that** the codebase doesn't carry a dangling, half-configured convention where `tsconfig.json` declares an alias `vite.config.ts` doesn't resolve, per RULES.md §7.5.

- **PRD reference(s):** §7.5 Path alias (RULES.md, reconciliation item #7)
- **Module(s)/Screen(s):** Frontend shell (tooling)
- **Story points:** 1 — Single decision + one-line config change (or one deletion) — the smallest story in the catalogue.
- **Dependencies:** None
- **Testing tier(s):** unit
- **Labels:** frontend, foundation, tooling, phase1

**Acceptance Criteria**
- Given a new cross-feature import is written, when the chosen convention is followed, then it either uses `@/shared/...` (alias wired into `vite.config.ts`) or a relative path — never a mix that silently fails to resolve.

**Sub-tasks**
- [NEW] Frontend: wire `resolve.alias` into `vite.config.ts` to match `tsconfig.json`'s `@/*` (or remove the `paths` entry if relative imports are chosen instead)
- [NEW] Frontend: convert at least one existing cross-feature import to prove the chosen convention resolves

#### FND-21: Propagate a correlation ID (traceId) across the async event-listener boundary

**As a** on-call engineer, **I want** see the same traceId in a request's log line and in the async listener log lines it triggers, **so that** an event-driven flow (e.g. booking confirmation → notification) remains traceable end-to-end, per RULES.md §6.1 — today MDC context does not cross `@ApplicationModuleListener`'s async thread hop at all.

- **PRD reference(s):** §6.1 Correlation IDs (RULES.md, reconciliation item #4)
- **Module(s)/Screen(s):** shared, notification
- **Story points:** 5 — Requires `TaskDecorator`-based MDC propagation across Spring's async executor — a known non-trivial async/observability pattern, explicitly flagged as needed before the first real listener body ships.
- **Dependencies:** FND-01
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, foundation, observability, phase1

**Acceptance Criteria**
- Given a request generates a traceId at the edge and triggers a domain event, when an `@ApplicationModuleListener` handles it on a different thread, then the same traceId appears in that listener's log lines.
- Given an error response is returned, when the client inspects it, then its `traceId` matches the server-side log lines for that request (RFC 7807 shape, FND-22).

**Sub-tasks**
- [NEW] Backend: servlet filter generating traceId at the edge
- [NEW] Backend: `TaskDecorator` propagating MDC across the `@Async` executor backing `@ApplicationModuleListener`
- [NEW] Backend: unit test — decorator propagates context
- [NEW] Backend: integrationTest asserting identical traceId in request log and listener log for one real flow

#### FND-22: Adopt RFC 7807 Problem Details error responses with per-module @ControllerAdvice

**As a** API consumer (frontend engineer), **I want** receive a consistent error shape from every endpoint, including a traceId and field-level errors where applicable, **so that** the frontend can build one error-handling code path instead of reverse-engineering each module's ad hoc error format, per RULES.md §3.3.

- **PRD reference(s):** §3.3 Error response shape (RULES.md)
- **Module(s)/Screen(s):** shared, booking
- **Story points:** 5 — New shared `ProblemDetailFactory` plus one `@ControllerAdvice` per existing module (booking today, more as modules land).
- **Dependencies:** FND-21
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, foundation, phase1

**Acceptance Criteria**
- Given any endpoint returns a 4xx/5xx, when the response body is inspected, then it matches the RFC 7807 shape (`type`, `title`, `status`, `detail`, `instance`, `traceId`) with `errors[]` present only for field-level validation failures.
- Given a `@Valid` Bean Validation failure occurs, when the response is built, then `errors[]` lists the specific field(s) and message(s).

**Sub-tasks**
- [NEW] Backend: `shared.ProblemDetailFactory` (shape only, per RULES.md — not per-module type/title catalogue)
- [NEW] Backend: `booking`'s `@ControllerAdvice` using the factory
- [NEW] Backend: unit test — validation failure produces `errors[]`
- [NEW] Backend: module test — traceId matches the request's generated ID

#### FND-23: Convert all collection endpoints to paginated responses

**As a** API consumer, **I want** receive a paginated response from any endpoint returning a collection, never a bare unbounded list, **so that** a Consultant who accumulates thousands of bookings never causes an unbounded response, per RULES.md §3.4 — `BookingApi.findBookingsByConsultant` currently returns a bare `List<UUID>` and must not be wired to a controller as-is.

- **PRD reference(s):** §3.4 Pagination (RULES.md, reconciliation item #8)
- **Module(s)/Screen(s):** booking
- **Story points:** 3 — Scoped, mechanical fix (`Pageable` end-to-end) but blocks BOK-08 and HRD-07 from being wired to controllers incorrectly.
- **Dependencies:** None
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, foundation, phase1

**Acceptance Criteria**
- Given `GET /api/v1/bookings?consultantId=...` is called, when the response is built, then it returns `{content, page, size, totalElements, totalPages}`, never a bare array.
- Given `findBookingsByConsultant` is called from any caller, when the signature is inspected, then it takes and returns `Page<...>`, not `List<UUID>`.

**Sub-tasks**
- [EXTEND] Backend: `BookingApi.findBookingsByConsultant` → `Page<UUID>`/DTO page
- [NEW] Backend: repository query takes `Pageable` end-to-end (no in-memory skip/limit)
- [NEW] Backend: unit test — page metadata correctness
- [NEW] Backend: module test

#### FND-24: Adopt structured JSON logging with mandatory MDC fields

**As a** on-call engineer, **I want** have every log line inside a request or listener scope carry traceId, consultantId, and (where relevant) currency/market, **so that** log aggregation/search is usable as the primary debugging tool across six jurisdictions and six currencies, per RULES.md §6.2.

- **PRD reference(s):** §6.2 Structured logging standards (RULES.md)
- **Module(s)/Screen(s):** shared
- **Story points:** 5 — Logback structured-encoder config plus an MDC-population convention enforced at the filter/listener boundary established in FND-21.
- **Dependencies:** FND-21
- **Testing tier(s):** unit
- **Labels:** backend, foundation, observability, phase1

**Acceptance Criteria**
- Given any log line is written inside a request or listener scope, when it is emitted, then it is JSON-structured and carries `traceId` and `consultantId` at minimum.
- Given a log line concerns a monetary amount or a compliance-relevant calculation, when it is emitted, then it also carries `currency` and/or `market` — never a bare number or a market-less compliance line.

**Sub-tasks**
- [NEW] Backend: Logback structured JSON encoder configuration
- [EXTEND] Backend: MDC population (`traceId`, `consultantId`, `currency`, `market`) at the filter and listener boundary from FND-21
- [NEW] Backend: unit test — a sample monetary log line asserts `currency` is present

---

## Booking Core

*20 stories, 98 story points.*

#### BOK-01: Add @Transactional boundaries to booking state-change methods

**As a** backend engineer, **I want** have `BookingServiceImpl.saveAsQuotation`/`confirmBooking` wrap their entity mutation and event publication in a single transaction, **so that** the outbox pattern's atomicity guarantee actually holds — today a state change can persist with no corresponding event, or an event can publish for a change that then rolls back, per RULES.md §4.3.

- **PRD reference(s):** §4.3 Transaction boundaries (RULES.md, top reconciliation item)
- **Module(s)/Screen(s):** booking
- **Story points:** 3 — Scoped fix to two existing methods; risk is in getting the transaction boundary exactly right, not breadth.
- **Dependencies:** None
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given `saveAsQuotation` mutates the Itinerary and publishes `ItineraryQuotationSavedEvent`, when the underlying repository save throws, then the event is not published (single transactional scope).
- Given `confirmBooking` succeeds, when the transaction commits, then `BookingConfirmedEvent` is guaranteed to have been queued in the same commit, per Spring Modulith's JPA event publication registry.

**Sub-tasks**
- [EXTEND] Backend: `@Transactional` on `saveAsQuotation` and `confirmBooking`
- [NEW] Backend: unit test — repository failure prevents event publication
- [NEW] Backend: integrationTest proving outbox atomicity (per testing-strategy skill's explicit guidance for this exact case)

#### BOK-02: Fix BookingConfirmedEvent to carry Money instead of decomposed amount+currency

**As a** backend engineer, **I want** have `BookingConfirmedEvent` carry a single `Money totalSellPrice` field, **so that** nothing can pair the amount with the wrong currency downstream, matching the Money rule (RULES.md §4.4) the event currently violates.

- **PRD reference(s):** §2.3 Event schema versioning (RULES.md reconcile); §4.4 The Money rule (RULES.md)
- **Module(s)/Screen(s):** booking
- **Story points:** 2 — Pre-GA fix on an event nothing yet consumes for real (notification listener is a TODO stub) — cheapest possible time to do it.
- **Dependencies:** BOK-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given `BookingConfirmedEvent` is published, when its payload is inspected, then it exposes one `Money totalSellPrice` field, not separate `BigDecimal`/`CurrencyCode` fields.

**Sub-tasks**
- [EXTEND] Backend: `BookingConfirmedEvent` record signature change
- [EXTEND] Backend: `BookingServiceImpl` publish call site update
- [NEW] Backend: unit test
- [NEW] Backend: module test — event shape asserted via Modulith's `Scenario` API

#### BOK-03: Add Hotel line items to an itinerary

**As a** Consultant/User, **I want** add a hotel line item to an itinerary with supplier, rate, and pricing fields, **so that** hotel products from Hotelbeds/STUBA/TBO/Local DMC/BYOS can be represented on an itinerary per PRD §20.2.

- **PRD reference(s):** §20.2 Line Item — Hotel; §9.3 Data Model
- **Module(s)/Screen(s):** booking, supplier
- **Story points:** 5 — New entity within the existing Itinerary aggregate; pricing fields reuse the Money type.
- **Dependencies:** BOK-01, FIN-04, FIN-05
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given a hotel option is added from search results, when the line item is created, then it stores `supplierRateId`, `propertyName`, `roomType`, `mealPlan`, `cancellationDeadline`, and the net/markup/buffer/sell/currency/fxSnapshot pricing fields.
- Given a hotel line item is added, when its cancellation policy is inspected, then `cancellationDeadline` is the earliest `from` date per PRD §10.2.1.

**Sub-tasks**
- [NEW] Backend: `HotelLineItem` entity + `HotelLineItemRepository` (package-private, own Flyway migration)
- [NEW] Backend: `addHotelLineItem` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `POST /api/v1/itineraries/{id}/line-items/hotel` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)

#### BOK-04: Add Flight line items to an itinerary (Mystifly)

**As a** Consultant/User, **I want** add a flight line item with airline, fare basis, cabin class, and PNR fields, **so that** transport products from Mystifly can be represented on an itinerary per PRD §20.3.

- **PRD reference(s):** §20.3 Line Item — Flight (Mystifly)
- **Module(s)/Screen(s):** booking, supplier
- **Story points:** 5 — Same shape as BOK-03, one product-type variant.
- **Dependencies:** BOK-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given a flight option is added from search results, when the line item is created, then it stores `airlineCode`, `flightNumber`, `fareBasisCode`, `cabinClass`, `baggageAllowance`, with `pnr` nullable until booked.

**Sub-tasks**
- [EXTEND] Backend: `FlightLineItem` entity + `FlightLineItemRepository` (package-private, own Flyway migration)
- [EXTEND] Backend: `addFlightLineItem` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [EXTEND] Backend: REST endpoint `POST /api/v1/itineraries/{id}/line-items/flight` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)

#### BOK-05: Add Transfer line items to an itinerary (Transferz)

**As a** Consultant/User, **I want** add a transfer line item with vehicle type and pickup/dropoff points, **so that** transfer products from Transferz can be represented on an itinerary per PRD §20.4.

- **PRD reference(s):** §20.4 Line Item — Transfer (Transferz)
- **Module(s)/Screen(s):** booking, supplier
- **Story points:** 5 — Same shape as BOK-03, one product-type variant.
- **Dependencies:** BOK-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given a transfer option is added, when the line item is created, then it stores `vehicleType` and geocoded `pickupPoint`/`dropoffPoint` linked to the itinerary's location entries.

**Sub-tasks**
- [EXTEND] Backend: `TransferLineItem` entity + `TransferLineItemRepository` (package-private, own Flyway migration)
- [EXTEND] Backend: `addTransferLineItem` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [EXTEND] Backend: REST endpoint `POST /api/v1/itineraries/{id}/line-items/transfer` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)

#### BOK-06: Add Cruise line items to an itinerary (Widgety)

**As a** Consultant/User, **I want** add a cruise line item with sailing, cabin category, and multi-port metadata, **so that** cruise products from Widgety can be represented on an itinerary per PRD §20.5, including the passenger-documentation flag.

- **PRD reference(s):** §20.5 Line Item — Cruise (Widgety); §10.2.6 Widgety
- **Module(s)/Screen(s):** booking, supplier
- **Story points:** 5 — Same shape as BOK-03 plus the port-flattening and documentation-flag rules called out in §10.2.6.
- **Dependencies:** BOK-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given a cruise option is added, when the line item is created, then it stores `sailingId`, `cruiseLine`, `cabinCategory`, and `ports[]` as itinerary metadata rather than separate line items.
- Given the selected sailing requires passenger documentation, when the line item is created, then `passengerDocumentsRequired=true` is set, driving the Traveler Profile passport requirement (BOK-14).

**Sub-tasks**
- [EXTEND] Backend: `CruiseLineItem` entity + `CruiseLineItemRepository` (package-private, own Flyway migration)
- [EXTEND] Backend: `addCruiseLineItem` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [EXTEND] Backend: REST endpoint `POST /api/v1/itineraries/{id}/line-items/cruise` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)

#### BOK-07: Add Activity line items to an itinerary (HBActivities)

**As a** Consultant/User, **I want** add an activity line item with a specific time slot and fixed headcount, **so that** activity products from HBActivities can be represented on an itinerary per PRD §20.6.

- **PRD reference(s):** §20.6 Line Item — Activity (HBActivities); §10.2.7 HBActivities
- **Module(s)/Screen(s):** booking, supplier
- **Story points:** 5 — Same shape as BOK-03 plus the time-slot/headcount constraint.
- **Dependencies:** BOK-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given an activity option is added, when the line item is created, then it stores `activityId`, `durationMinutes`, `timeSlot` (a specific time, not a date range), and `headcount`.
- Given the Consultant/User attempts to change headcount post-confirmation, when the edit is attempted, then it is blocked per the supplier's fixed-at-booking constraint (§10.2.7), surfaced before payment.

**Sub-tasks**
- [EXTEND] Backend: `ActivityLineItem` entity + `ActivityLineItemRepository` (package-private, own Flyway migration)
- [EXTEND] Backend: `addActivityLineItem` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [EXTEND] Backend: REST endpoint `POST /api/v1/itineraries/{id}/line-items/activity` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)

#### BOK-08: Save an itinerary as a Quotation

**As a** Consultant/User, **I want** save a finalized itinerary as a read-only Quotation, **so that** the itinerary lifecycle progresses from Draft to Quotation per PRD §9.1 Flow A step 8.

- **PRD reference(s):** §9.1 Flow A step 8; §22.3 T4; §20.9 Quotation
- **Module(s)/Screen(s):** booking
- **Story points:** 3 — Extends the existing `ItineraryController`/`saveAsQuotation` reference endpoint with the full status-machine rule and Quotation entity linkage.
- **Dependencies:** BOK-01, BOK-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given an itinerary has at least one line item per required category, when 'Save as Quotation' is clicked, then the itinerary status transitions Draft → Quotation and becomes read-only except via explicit edit (T4).

**Sub-tasks**
- [EXTEND] Backend: `Itinerary.markAsQuotation()` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/itineraries/{id}/quotation`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### BOK-09: Create Quotation entity with FX/rate validity window

**As a** Consultant, **I want** have my Quotation carry a `valid_until` timestamp so I know when its rate/FX lock expires, **so that** the rate and FX snapshot the Quotation was built on has an explicit expiry per PRD §20.9.

- **PRD reference(s):** §20.9 Quotation
- **Module(s)/Screen(s):** booking
- **Story points:** 5 — New entity distinct from Itinerary, first-class Quotation lifecycle tracking.
- **Dependencies:** BOK-08
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given a Quotation is created, when it is inspected, then it stores `quotationId`, `itineraryId`, `validUntil`, `sharedWithTraveler`, and a nullable `convertedToBookingId`.

**Sub-tasks**
- [EXTEND] Backend: `Quotation` entity + `QuotationRepository` (package-private, own Flyway migration)
- [EXTEND] Backend: `createQuotation` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [EXTEND] Backend: REST endpoint `(internal — created as part of BOK-08's transition)` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)

#### BOK-10: Convert a Quotation to a Package

**As a** Consultant, **I want** convert a saved Quotation into a Package with a name, validity dates, pricing, and max pax, **so that** I can turn a one-off itinerary into a reusable, sellable product per PRD §9.1 Flow B.

- **PRD reference(s):** §9.1 Flow B; §22.3 T4 (lifecycle); §20.7 Package
- **Module(s)/Screen(s):** booking
- **Story points:** 5 — New entity + conversion action; pricing fields (base auto-filled, markup editable) depend on FIN-05.
- **Dependencies:** BOK-09, FIN-05
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given a Consultant selects a saved Quotation and clicks 'Convert to Package', when they set name, validity dates, pricing, and max pax, then a Package is created referencing `sourceItineraryId` and is not yet published.

**Sub-tasks**
- [NEW] Backend: `Package` entity + `PackageRepository` (package-private, own Flyway migration)
- [NEW] Backend: `convertQuotationToPackage` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `POST /api/v1/quotations/{id}/package` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)

#### BOK-11: Build the Package Builder screen with UK ATOL disclosure gate

**As a** Consultant, **I want** fill out the Package Builder form and be blocked from publishing a UK dynamic flight+hotel package until I complete the ATOL disclosure step, **so that** PRD §21.3's validation states and §17.2's UK ATOL/PTR 2018 auto-enforcement are both satisfied.

- **PRD reference(s):** §21.3 Package Builder; §17.2 Platform Enforcement; §22.3 T5; §20.7 is_dynamic_flight_hotel_combo
- **Module(s)/Screen(s):** booking, compliance, Package Builder (21.3) — NEW feature folder
- **Story points:** 8 — New screen with a non-trivial conditional gate (market + product-mix detection) — first cross-module (booking↔compliance) UI dependency in the catalogue.
- **Dependencies:** BOK-10, FND-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test, e2e
- **Labels:** backend, frontend, booking, phase1

**Acceptance Criteria**
- Given a Package includes both a flight and a hotel line item and the Consultant's market is UK, when the Consultant attempts to publish, then the system blocks publish until the ATOL disclosure step is completed (T5).
- Given required fields are incomplete, when the Consultant attempts to publish, then publish is blocked with field-level validation errors.

**Sub-tasks**
- [NEW] Backend: `is_dynamic_flight_hotel_combo` detection on Package save
- [NEW] Backend: ATOL disclosure-completion gate on the publish endpoint
- [NEW] Frontend: `usePackageBuilder` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `PackageBuilder.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Frontend: Playwright e2e spec (extends `search-flow.spec.ts` pattern, PRD §9.1 flow)

#### BOK-12: Publish a Package, making it visible to Users

**As a** Consultant, **I want** publish a Package so it becomes visible to my Users and eligible for Meta campaign promotion, **so that** Users can search/sell it and I can opt into the Ads flow, per PRD §9.1 Flow B step 3 and §22.3.

- **PRD reference(s):** §9.1 Flow B step 3; §22.3 (Package published → visible to Users)
- **Module(s)/Screen(s):** booking
- **Story points:** 3 — Status-transition method plus a visibility query, on top of BOK-11's gate.
- **Dependencies:** BOK-11
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given a Quotation is converted to a Package, when the Package is published, then it becomes visible to the Consultant's Users (T4-adjacent lifecycle check).
- Given the Consultant opts into 'Promote this Package', when publish completes, then the flow hands off into the Ads Campaign Builder (ADS-03).

**Sub-tasks**
- [EXTEND] Backend: `Package.publish()` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/packages/{id}/publish`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### BOK-13: Build the Direct Booking & Payment flow (User-facing)

**As a** User, **I want** search available Packages or a custom itinerary, enter traveler details, review the price breakdown, and choose a payment method, **so that** a traveler-facing booking can be completed end-to-end per PRD §9.1 Flow C and §21.4.

- **PRD reference(s):** §9.1 Flow C; §21.4 Booking & Payment Flow
- **Module(s)/Screen(s):** booking, payments, Booking & Payment Flow (21.4) — NEW feature folder
- **Story points:** 8 — New end-to-end screen orchestrating booking, traveler profile, and payment method selection — the largest single frontend surface in Booking Core.
- **Dependencies:** BOK-12, FIN-06, FIN-08, BOK-14
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test, e2e
- **Labels:** backend, frontend, booking, phase1

**Acceptance Criteria**
- Given a User selects traveler(s) and enters details, when they proceed, then the price breakdown (collapsible net/markup detail per Consultant visibility settings) is shown before payment method selection.
- Given a line item flags `passenger_documents_required` or the itinerary includes international travel, when the User reaches the traveler form, then document fields are required inline before proceeding.

**Sub-tasks**
- [EXTEND] Backend: `confirmBooking` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/bookings`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [NEW] Frontend: `useBookingPaymentFlow` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `BookingPaymentFlow.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Frontend: Playwright e2e spec (extends `search-flow.spec.ts` pattern, PRD §9.1 flow)

#### BOK-14: Capture Traveler Profile details including passport/document vault

**As a** User, **I want** enter traveler name, date of birth, and (when required) passport details and documents, **so that** cruise and international bookings have the traveler data PRD §20.10 requires before confirmation.

- **PRD reference(s):** §20.10 Traveler Profile; §23.1 (edge cases reference traveler data)
- **Module(s)/Screen(s):** booking
- **Story points:** 5 — New entity with encrypted document-vault references; the encryption mechanism itself is FND-12's KMS pattern reused, not rebuilt.
- **Dependencies:** BOK-13
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given a cruise line item with `passengerDocumentsRequired=true` is in the itinerary, when the User proceeds to booking, then passport number, expiry, and nationality are required before the booking can confirm (T22).
- Given a Traveler Profile is created, when it is inspected, then it is scoped to `consultant_id` — not shared across Consultants.

**Sub-tasks**
- [EXTEND] Backend: `TravelerProfile` entity + `TravelerProfileRepository` (package-private, own Flyway migration)
- [EXTEND] Backend: `createTravelerProfile` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [EXTEND] Backend: REST endpoint `POST /api/v1/travelers` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)

#### BOK-15: Generate a Voucher on booking confirmation, including ATOL certificate for UK dynamic packages

**As a** User, **I want** receive a voucher immediately when a booking is confirmed, with an ATOL certificate attached if applicable, **so that** PRD §21.4's confirmation state (voucher download link, ATOL certificate download link) and §20.11 are satisfied.

- **PRD reference(s):** §20.11 Voucher; §21.4 Confirmation state; §22.9 T5
- **Module(s)/Screen(s):** booking
- **Story points:** 8 — PDF generation + conditional ATOL attachment is the most complex single piece of the booking-confirmation path.
- **Dependencies:** BOK-13, BOK-11
- **Testing tier(s):** unit, module (@ApplicationModuleTest), integration (Testcontainers)
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given a booking is confirmed, when the Voucher is generated, then it references the booking, has a `pdfReference`, and — for a UK dynamic flight+hotel package — an `atolCertificateReference` is populated and attached (T5).
- Given a booking is confirmed that is not a UK dynamic package, when the Voucher is generated, then `atolCertificateReference` remains null.

**Sub-tasks**
- [NEW] Backend: `Voucher` entity + PDF generation service (stored to LocalStack S3 in MVP)
- [EXTEND] Backend: `confirmBooking` triggers voucher generation in the same transactional scope as BOK-01
- [NEW] Backend: ATOL certificate attachment conditional on `is_dynamic_flight_hotel_combo` + UK market
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — PDF stored and referenced correctly

#### BOK-16: Prevent double-booking of the last available inventory unit under concurrent requests

**As a** platform reliability owner, **I want** have the second of two simultaneous booking attempts on the last unit fail gracefully, **so that** PRD §23.1 Edge Case #1 is closed with a real concurrency guarantee, not just documented as a risk.

- **PRD reference(s):** §23.1 Edge Case #1; §22.x concurrency; §25 T21
- **Module(s)/Screen(s):** booking
- **Story points:** 5 — Concrete, well-specified concurrency fix (`@Version` + service-layer exception mapping) per backend-best-practices §3.
- **Dependencies:** BOK-13
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given two Users under the same Consultant attempt to book the last available inventory unit simultaneously, when the second commit is attempted, then it fails with an `OptimisticLockException` mapped to a 'no longer available' message, not a duplicate booking (T21).

**Sub-tasks**
- [NEW] Backend: `@Version` field on `Itinerary`/`Booking`
- [NEW] Backend: service-layer mapping of `OptimisticLockException` → domain 'no longer available' exception
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — two concurrent commits against Testcontainers Postgres, second fails

#### BOK-17: Consolidate mixed-currency line items to the Consultant's sell currency at checkout

**As a** Consultant, **I want** see one consolidated total in my sell currency even if a BYOS supplier line item is priced in a different currency, **so that** PRD §23.1 Edge Case #2 is closed — the system must never present a mixed-currency total.

- **PRD reference(s):** §23.1 Edge Case #2
- **Module(s)/Screen(s):** booking, payments
- **Story points:** 5 — Depends on FIN-04's FX-snapshot mechanism; the booking-side work is the checkout-time consolidation rule itself.
- **Dependencies:** BOK-13, FIN-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given an itinerary contains a mix of INR and AED line items due to a differently-configured BYOS supplier, when checkout is reached, then the system consolidates to the Consultant's sell currency using the FX layer, never presenting a mixed-currency total.

**Sub-tasks**
- [EXTEND] Backend: `consolidateCheckoutCurrency` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — invoked during confirmBooking)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### BOK-18: Recalculate price when traveler count changes after Quotation but before booking

**As a** Consultant, **I want** have the price recalculate, not carry over stale, if traveler count changes after a Quotation was generated, **so that** PRD §23.1 Edge Case #3 is closed.

- **PRD reference(s):** §23.1 Edge Case #3
- **Module(s)/Screen(s):** booking
- **Story points:** 3 — Single, well-scoped business rule on an existing entity method (`Quotation.recalculate()` throwing rather than silently no-op'ing per backend-best-practices §1).
- **Dependencies:** BOK-09
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given traveler count changes after a Quotation is generated but before booking, when the booking is attempted, then price is recalculated from current rates, not carried over from the stale Quotation.

**Sub-tasks**
- [EXTEND] Backend: `Quotation.recalculate()` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — invoked on traveler-count change)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### BOK-19: Generate a PNR-searchable reference on every Booking

**As a** User, **I want** have every booking carry Adren's own searchable reference regardless of product type, **so that** PNR search (HRD-07) can look up a booking without depending on airline PNRs or supplier booking IDs, per PRD §20.8.

- **PRD reference(s):** §20.8 Booking (pnr_searchable_ref)
- **Module(s)/Screen(s):** booking
- **Story points:** 2 — Small addition to the Booking entity's confirmation path.
- **Dependencies:** BOK-13
- **Testing tier(s):** unit
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given a booking is confirmed, when the Booking entity is inspected, then it carries a `pnrSearchableRef` distinct from any airline PNR or supplier booking reference.

**Sub-tasks**
- [EXTEND] Backend: `Booking.pnrSearchableRef` generation on confirm
- [NEW] Backend: unit test — uniqueness and format

#### BOK-20: Deduplicate the same physical hotel property offered by two suppliers

**As a** Consultant/User, **I want** see one entry for a hotel even when two suppliers (e.g. Hotelbeds and STUBA) return the same physical property, **so that** search results aren't cluttered with duplicate listings and the Default Selection Algorithm can choose cleanly, per PRD §9.4.

- **PRD reference(s):** §9.4 Business Rules & Edge Cases; §22.2 (interacts with Default Selection)
- **Module(s)/Screen(s):** supplier, booking
- **Story points:** 8 — Property-matching/dedup across heterogeneous supplier content (names, addresses, no shared ID) is inherently fuzzy-matching work — the highest-uncertainty story in this epic.
- **Dependencies:** FND-14
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, booking, supplier, phase1

**Acceptance Criteria**
- Given two suppliers offer the same physical hotel for a search, when results are aggregated, then the entries are deduplicated via property-matching before the Default Selection Algorithm runs.

**Sub-tasks**
- [NEW] Backend: property-matching service (name/geo/address heuristic match across `HotelbedsClient`/`StubaClient`/`TboClient` results)
- [EXTEND] Backend: wired ahead of `DefaultSelectionService` in the aggregation pipeline
- [NEW] Backend: unit test — known duplicate pairs matched, known distinct properties not merged
- [NEW] Backend: module test — end-to-end aggregation with dedup

---

## Financial Layer

*18 stories, 95 story points.*

#### FIN-01: Configure per-Consultant, per-category markup rules

**As a** Consultant, **I want** configure my markup per product category as a percentage or a flat fee, **so that** my margin is applied consistently across every hotel/flight/transfer/cruise/activity line item, per PRD §12.1.

- **PRD reference(s):** §12.1 Markup & Commission Engine
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — New configurable rule entity per Consultant×category; calculation logic is straightforward, configuration surface is the work.
- **Dependencies:** FND-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, financial, payments, phase1

**Acceptance Criteria**
- Given a Consultant configures a 15% markup on hotels, when a hotel line item is added, then the sell_rate reflects net_rate × 1.15 (Worked Example A, T6).
- Given a Consultant configures a flat-fee markup on activities, when an activity line item is added, then the flat fee is added to net_rate rather than a percentage.

**Sub-tasks**
- [NEW] Backend: `MarkupRule` entity + `MarkupRuleRepository` (package-private, own Flyway migration)
- [NEW] Backend: `configureMarkup` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `PUT /api/v1/consultants/{id}/markup-rules` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)

#### FIN-02: Track Adren commission separately from Consultant markup

**As a** Super Admin, **I want** have Adren's commission tracked separately from the Consultant's markup on every booking, **so that** PRD §12.1's Worked Example A distinction (markup vs. commission, deducted from Consultant payout) is reflected in the ledger.

- **PRD reference(s):** §12.1 Worked Example A
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — Requires a distinct ledger-entry type (`CommissionDeduction`, PRD §20.12) alongside the markup calculation from FIN-01.
- **Dependencies:** FIN-01, FIN-06
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, financial, payments, phase1

**Acceptance Criteria**
- Given a booking is confirmed with a 15% Consultant markup and a 5% Adren commission on net, when the ledger is inspected, then markup and commission appear as two distinct, separately-attributable amounts, not netted into one figure.

**Sub-tasks**
- [NEW] Backend: `calculateCommission` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — invoked during sell-rate calculation)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### FIN-03: Apply a configurable currency buffer on top of markup

**As a** Consultant, **I want** have a 2–5% currency buffer applied above my markup, configurable per Consultant/market, **so that** FX exposure on multi-currency bookings is absorbed per PRD §12.2 and Worked Example B.

- **PRD reference(s):** §12.2 Multi-Currency & FX Buffer; §12.1 Worked Example B
- **Module(s)/Screen(s):** payments
- **Story points:** 3 — Single configurable percentage applied at a defined point in the pricing pipeline.
- **Dependencies:** FIN-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, financial, payments, phase1

**Acceptance Criteria**
- Given a Consultant has a 3% currency buffer configured on a Hotelbeds EUR rate, when a hotel line item is added, then the buffer is applied to the FX-converted base before markup, matching Worked Example B's INR 9,600 → INR 9,888 step.

**Sub-tasks**
- [EXTEND] Backend: `applyCurrencyBuffer` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — pricing pipeline step)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### FIN-04: Snapshot and lock the FX rate at quotation time

**As a** Consultant, **I want** have the FX rate locked at the moment I generate a quote, unaffected by later market movement, **so that** PRD §12.2 and §22.4's T7 requirement are met — a booking price must use the locked snapshot, never the current rate.

- **PRD reference(s):** §12.2 FX rate snapshot; §22.4 T7; §4.4 The Money rule (RULES.md)
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — Immutable-once-written value per RULES.md §4.4 — the discipline is in never re-fetching it on any downstream code path, not just writing it once.
- **Dependencies:** FIN-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, financial, payments, phase1

**Acceptance Criteria**
- Given a booking's supplier currency differs from the Consultant's sell currency, when a quote is generated, then the `fx_rate_snapshot` is locked and does not change even if market rates move before booking confirmation (T7).

**Sub-tasks**
- [EXTEND] Backend: `snapshotFxRate` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — pricing pipeline step)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### FIN-05: Calculate sell rate through the full net→buffer→markup→commission pipeline

**As a** Consultant, **I want** see a correct sell_rate on every line item combining net rate, currency buffer, markup, and commission, **so that** PRD §12.1's worked examples produce decimal-safe, auditable results end-to-end.

- **PRD reference(s):** §12.1 Worked Examples A & B; §24.1 (decimal-safe arithmetic)
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — Orchestrates FIN-01/02/03/04 into one pipeline — the composition is the risk, not any individual step.
- **Dependencies:** FIN-01, FIN-02, FIN-03, FIN-04
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, financial, payments, phase1

**Acceptance Criteria**
- Given a hotel line item is priced per Worked Example B's inputs, when the pipeline runs, then sell_rate matches the worked example to the cent using `Money`'s `BigDecimal`/`HALF_UP` semantics, never `double`.

**Sub-tasks**
- [NEW] Backend: `PricingPipeline` composing buffer→markup→commission→FX-snapshot in order
- [NEW] Backend: unit test — both worked examples reproduced exactly
- [NEW] Backend: integrationTest — full pipeline against a real line item persist/read round trip

#### FIN-06: Model the Wallet with balance, credit limit, and pending holds

**As a** Consultant, **I want** see my available balance, credit limit, and any pending holds in one place, **so that** PRD §12.3's wallet model is the source of truth for booking confirmation eligibility.

- **PRD reference(s):** §12.3 Wallet & Credit Limit; §20.12 Wallet Ledger Entry
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — New Wallet + WalletLedgerEntry entities with the enum-typed ledger from PRD §20.12.
- **Dependencies:** FND-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, financial, payments, phase1

**Acceptance Criteria**
- Given a Consultant's wallet is queried, when the response is inspected, then it exposes available balance, credit limit, and pending holds, denominated in the home-market currency.

**Sub-tasks**
- [NEW] Backend: `Wallet` entity + `WalletRepository` (package-private, own Flyway migration)
- [NEW] Backend: `getWallet` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `GET /api/v1/wallet` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)

#### FIN-07: Place a hold on booking confirmation, release or debit on final confirmation

**As a** Consultant, **I want** have a booking place a hold on my wallet that converts to a debit on final confirmation, or releases if the booking doesn't complete, **so that** PRD §12.3's hold lifecycle is enforced, preventing a Consultant from over-committing funds mid-booking.

- **PRD reference(s):** §12.3 Wallet & Credit Limit
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — State-machine on top of FIN-06's Wallet entity, invoked from BOK-13's booking flow.
- **Dependencies:** FIN-06, BOK-13
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, financial, payments, phase1

**Acceptance Criteria**
- Given a booking reaches the payment step with wallet selected, when a hold is placed, then the wallet's pending-holds figure increases by the booking total.
- Given the booking confirms, when the hold resolves, then it converts to a `Debit` ledger entry and pending holds decreases correspondingly.
- Given the booking is abandoned/cancelled before confirmation, when the hold resolves, then it is released back to available balance.

**Sub-tasks**
- [EXTEND] Backend: `placeHold/resolveHold` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — invoked during confirmBooking)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### FIN-08: Block booking confirmation on credit-limit breach with an actionable message

**As a** Consultant, **I want** be blocked from confirming a booking that would exceed my wallet balance plus available credit, with a clear top-up prompt, **so that** PRD §22.4's T8 requirement is met.

- **PRD reference(s):** §22.4 T8; §12.3 Wallet & Credit Limit
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — Enforcement rule on top of FIN-07's hold mechanism; the DB-level constraint (backend-best-practices §3) is what makes it a real guarantee, not just an app-level check.
- **Dependencies:** FIN-07
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, financial, payments, phase1

**Acceptance Criteria**
- Given a Consultant's wallet balance plus available credit is less than the booking total, when they attempt to confirm payment via wallet, then the system blocks confirmation with an actionable 'top up required' message (T8).

**Sub-tasks**
- [EXTEND] Backend: conditional `UPDATE ... WHERE balance + credit_limit >= amount` with row-count check (DB-level enforcement, not app-level-only)
- [NEW] Backend: service-layer mapping to the actionable top-up message
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — breach attempt against Testcontainers Postgres

#### FIN-09: Build the Wallet & Billing screen with pre-payment breach warning

**As a** Consultant, **I want** see my balance, transaction ledger, and an inline warning before I reach the payment step if a pending booking would breach my credit limit, **so that** PRD §21.7's layout and breach-state requirement (warning appears before payment, not after) are both implemented.

- **PRD reference(s):** §21.7 Wallet & Billing Screen
- **Module(s)/Screen(s):** payments, Wallet & Billing Screen (21.7) — NEW feature folder
- **Story points:** 8 — New screen with a cross-screen breach-warning requirement (must appear before, not on, the payment step) — genuinely cross-cutting UI state.
- **Dependencies:** FIN-08
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, financial, payments, phase1

**Acceptance Criteria**
- Given a pending booking would breach the credit limit, when the User is on any screen before the payment step, then an inline warning appears before they reach payment, not after.
- Given the Consultant filters the transaction ledger by type, when they apply the filter, then only entries matching that `WalletLedgerEntry.type` (TopUp/Hold/Debit/Refund/CommissionDeduction) are shown.

**Sub-tasks**
- [NEW] Backend: `GET /api/v1/wallet/ledger?type=` paginated endpoint
- [NEW] Frontend: `useWalletBilling` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `WalletBilling.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

#### FIN-10: Guarantee atomic, idempotent wallet ledger writes

**As a** platform reliability owner, **I want** have every wallet ledger write be atomic and safe to retry without a double-debit, **so that** PRD §24.4's NFR is met, matching RULES.md §4.3's transaction-boundary discipline.

- **PRD reference(s):** §24.4 NFR Payments & Wallet; §4.3 Transaction boundaries (RULES.md)
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — Idempotency-key pattern layered onto FIN-07's ledger writes, per RULES.md §2.2's dedup-key guidance.
- **Dependencies:** FIN-07
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, financial, payments, phase1

**Acceptance Criteria**
- Given a wallet debit request is retried after a network timeout on the first attempt that actually succeeded server-side, when the retry is processed, then no second debit occurs — the retry is a no-op against the same idempotency key.

**Sub-tasks**
- [EXTEND] Backend: idempotency key (e.g. `(booking_id, ledger_entry_type)` unique constraint) on `WalletLedgerEntry`
- [NEW] Backend: unit test — duplicate write is a no-op
- [NEW] Backend: integrationTest — concurrent retrying writers against Testcontainers Postgres

#### FIN-11: Integrate Stripe for payment collection across six settlement currencies

**As a** User, **I want** pay for a booking via Stripe in INR, AUD, GBP, USD, AED, or DKK, **so that** PRD §12.4 and §24.4's PCI-minimization NFR (hosted elements, no raw card data server-side) are both satisfied.

- **PRD reference(s):** §12.4 Stripe Integration; §24.4 NFR (PCI-DSS scope)
- **Module(s)/Screen(s):** payments
- **Story points:** 8 — Multi-currency Stripe wiring plus webhook handling — the most externally-integrated story in this epic.
- **Dependencies:** FIN-05
- **Testing tier(s):** unit, module (@ApplicationModuleTest), integration (Testcontainers)
- **Labels:** backend, financial, payments, phase1

**Acceptance Criteria**
- Given a User selects Stripe as the payment method for a GBP booking, when they submit payment, then Stripe's hosted payment element handles card capture — no raw PAN ever reaches the Adren backend.
- Given payment succeeds, when the webhook is received, then the booking confirms and the wallet/ledger (if applicable) reconciles.

**Sub-tasks**
- [NEW] Backend: Stripe PaymentIntent creation per currency + webhook handler
- [NEW] Backend: booking confirmation gated on webhook receipt
- [NEW] Backend: unit test — PaymentIntent request shape per currency
- [NEW] Backend: integrationTest — webhook-driven confirmation flow

#### FIN-12: Support On-Account billing as a payment method

**As a** Consultant, **I want** bill a booking to my on-account balance instead of Stripe or wallet, **so that** PRD §21.4's three payment-method options (Stripe / Wallet / On-Account) are all available at checkout.

- **PRD reference(s):** §21.4 Booking & Payment Flow; §20.8 Booking (payment_method enum)
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — Third payment-method branch reusing FIN-06's wallet/ledger machinery.
- **Dependencies:** FIN-11
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, financial, payments, phase1

**Acceptance Criteria**
- Given a Consultant selects On-Account at checkout, when they confirm, then the booking's `payment_method` is `OnAccount` and a corresponding ledger entry is created without a Stripe call.

**Sub-tasks**
- [EXTEND] Backend: `payOnAccount` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — confirmBooking payment-method branch)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### FIN-13: Process refunds and credit notes tied to supplier cancellation policy

**As a** Consultant, **I want** have a cancellation's refund or penalty calculated against the actual supplier cancellation policy, **so that** PRD §12.4/§12.5's refund workflow reflects real policy terms rather than a flat rule.

- **PRD reference(s):** §12.4 Stripe Integration (refund/credit-note); §12.5 Cancellation & Dispute Handling
- **Module(s)/Screen(s):** payments, booking
- **Story points:** 5 — Reads existing line-item cancellation-policy fields; the calculation branches by policy shape.
- **Dependencies:** FIN-11, BOK-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, financial, payments, phase1

**Acceptance Criteria**
- Given a booking is cancelled before its cancellation deadline, when the refund is calculated, then it reflects the supplier's cancellation policy captured on the line item (e.g. `cancellation_deadline`) rather than a flat percentage.
- Given a penalty applies, when the refund is calculated, then Consultant approval is required before the refund is processed.

**Sub-tasks**
- [NEW] Backend: `calculateRefund` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `POST /api/v1/bookings/{id}/cancellation`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### FIN-14: Reuse the original FX snapshot when calculating a refund

**As a** Consultant, **I want** have a refund calculated against the FX rate locked at booking time, not the current market rate, **so that** PRD §23.4 Edge Case #9 and T15 are closed.

- **PRD reference(s):** §23.4 Edge Case #9; §25 T15
- **Module(s)/Screen(s):** payments
- **Story points:** 3 — Single, well-scoped rule reading FIN-04's immutable snapshot rather than re-fetching FX.
- **Dependencies:** FIN-04, FIN-13
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, financial, payments, phase1

**Acceptance Criteria**
- Given a refund is issued in a currency different from the original booking currency due to an FX rate change between booking and cancellation, when the refund is calculated, then the amount uses the original locked `fx_rate_snapshot`, not the current rate (T15).

**Sub-tasks**
- [EXTEND] Backend: `calculateRefund (FX reuse)` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — extends FIN-13)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### FIN-15: Reconcile wallet top-up when the payment gateway webhook is delayed or fails

**As a** Consultant, **I want** have my wallet balance reconcile once a delayed/retried webhook is received, without being falsely allowed to book against unconfirmed funds in the interim, **so that** PRD §23.4 Edge Case #10 is closed.

- **PRD reference(s):** §23.4 Edge Case #10
- **Module(s)/Screen(s):** payments
- **Story points:** 5 — Reconciliation state machine on top of FIN-11's webhook handling and FIN-06's wallet balance.
- **Dependencies:** FIN-11, FIN-06
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, financial, payments, phase1

**Acceptance Criteria**
- Given a wallet top-up succeeds at the payment gateway but the confirming webhook fails/delays, when a booking is attempted against the not-yet-reconciled top-up, then the booking flow is blocked, not falsely allowed, until the webhook is received/retried.

**Sub-tasks**
- [NEW] Backend: top-up pending/reconciled state on the wallet ledger entry
- [EXTEND] Backend: booking-eligibility check excludes pending (unreconciled) top-ups
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — delayed webhook scenario

#### FIN-16: Build the cancellation & dispute handling workflow

**As a** Consultant, **I want** have a cancellation go through policy check → refund/penalty calculation → my approval (if a penalty applies) → refund processed, and a dispute create a tracked ticket, **so that** PRD §12.5's full workflow is implemented, not just the refund-calculation piece.

- **PRD reference(s):** §12.5 Cancellation & Dispute Handling
- **Module(s)/Screen(s):** payments, booking, notification
- **Story points:** 8 — Orchestrates FIN-13 across three modules (payments/booking/notification) with an explicit approval gate — the most cross-cutting Financial Layer story.
- **Dependencies:** FIN-13
- **Testing tier(s):** unit, module (@ApplicationModuleTest), integration (Testcontainers)
- **Labels:** backend, financial, payments, phase1

**Acceptance Criteria**
- Given a cancellation with an applicable penalty is submitted, when the workflow runs, then it pauses for explicit Consultant approval before the refund is processed.
- Given a dispute is flagged on a booking, when the flag is submitted, then a tracked ticket entity is created, not just an email handoff.

**Sub-tasks**
- [NEW] Backend: `DisputeTicket` entity
- [EXTEND] Backend: cancellation state machine with approval gate
- [NEW] Backend: domain event on dispute creation, consumed by `notification`
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — full policy-check→approval→refund path

#### FIN-17: Implement India GST/TCS calculation layer for outbound packages

**As a** Consultant, **I want** see GST and TCS applied to an outbound package sold to an India-based traveler per current tax rules, **so that** PRD §12.1 Worked Example C and §17.2's India tax-layer requirement are implemented, distinct from UK TOMS logic.

- **PRD reference(s):** §12.1 Worked Example C; §17.2 Platform Enforcement; §25 T24
- **Module(s)/Screen(s):** payments, compliance
- **Story points:** 5 — Calculation layer's shape only — exact rates are an explicit open item (PRD §19) pending tax-counsel sign-off.
- **Dependencies:** FIN-05
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, financial, payments, compliance, phase1

**Acceptance Criteria**
- Given an outbound package is sold to an India-based Consultant's traveler, when the sale completes, then the tax-calculation layer applies GST to the margin/service component and TCS to the outbound package value per Section 12.1 Example C (T24).

**Sub-tasks**
- [NEW] Backend: `calculateIndiaGstTcs` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — pricing pipeline step)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

> ⚠️ **NEEDS CLARIFICATION:** PRD §19/§12.1 Example C: exact GST/TCS rates and mechanics require tax-counsel sign-off before implementation — this story implements the calculation layer's shape using the PRD's illustrative rates only, gated behind a config flag until counsel confirms the real figures.

#### FIN-18: Implement UK TOMS VAT calculation layer

**As a** Consultant, **I want** see UK TOMS VAT applied correctly to a package's margin component, not the full sale price, **so that** PRD §12.1 Worked Example D's requirement — TOMS must not be approximated as a flat percentage of total sale price — is implemented.

- **PRD reference(s):** §12.1 Worked Example D; §17.2 Platform Enforcement
- **Module(s)/Screen(s):** payments, compliance
- **Story points:** 5 — Calculation layer's shape only — exact rate requires UK tax-counsel sign-off per PRD §19.
- **Dependencies:** FIN-05, FIN-17
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, financial, payments, compliance, phase1

**Acceptance Criteria**
- Given a UK Consultant's package cost (margin only, per the TOMS mechanism) is GBP 200, when VAT is calculated, then it applies to the margin component only, matching Worked Example D's shape, never to the full package price.

**Sub-tasks**
- [NEW] Backend: `calculateUkTomsVat` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — pricing pipeline step)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

> ⚠️ **NEEDS CLARIFICATION:** PRD §19/§12.1 Example D: exact TOMS VAT rate and mechanics require UK tax-counsel sign-off before implementation — this story implements the calculation layer's shape (margin-only base) using the PRD's illustrative rate only, gated behind a config flag until counsel confirms the real figure.

---

## AI Layer

*13 stories, 72 story points.*

#### AI-01: Integrate a Groq client wrapper for the AI module

**As a** backend engineer, **I want** have a single, well-isolated Groq client used by every AI capability, **so that** the `ai` module's Groq dependency is centralized behind one internal client rather than duplicated per capability, per PRD §11.1 and `adren.ai.groq` config.

- **PRD reference(s):** §11.1 AI Capabilities in Scope
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — New module (currently package-info-only stub) — first real code in `ai`, establishing its internal shape.
- **Dependencies:** FND-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ai, phase1

**Acceptance Criteria**
- Given the `ai` module needs to call the LLM, when any capability invokes the client, then it goes through one `GroqClient` wrapper, not a direct HTTP call inlined in the capability's own class.

**Sub-tasks**
- [NEW] Backend: `ai/package-info.java` → real module shape (`AiApi`, `event/`, `internal/`)
- [NEW] Backend: `internal.GroqClient` wrapping `adren.ai.groq` config
- [NEW] Backend: unit test — request/response mapping, mocked HTTP
- [NEW] Backend: module test — module boundary respected (`ModularityTests`)

#### AI-02: Generate an itinerary from natural-language or structured input

**As a** Consultant/User, **I want** describe an itinerary in natural language or structured fields and get an AI-generated draft, **so that** PRD §11.1's AI-assisted itinerary generation capability is available, grounded only in live supplier-confirmed inventory (§11.2 principle 1).

- **PRD reference(s):** §11.1 AI Capabilities; §11.2 principle 1 (grounded generation only)
- **Module(s)/Screen(s):** ai, supplier, booking
- **Story points:** 8 — Core AI capability integrating supplier search results into prompt construction and parsing a structured suggestion back out — the riskiest single AI story.
- **Dependencies:** AI-01, FND-14
- **Testing tier(s):** unit, module (@ApplicationModuleTest), integration (Testcontainers)
- **Labels:** backend, ai, phase1

**Acceptance Criteria**
- Given a Consultant provides a natural-language itinerary request, when AI generation runs, then every suggested line item is selected only from live, supplier-confirmed inventory returned by `SupplierSearchApi` — never fabricated.

**Sub-tasks**
- [NEW] Backend: AI itinerary generation from NL/structured input — Groq client call + prompt construction (`adren.ai.groq` config)
- [NEW] Backend: audit-log write as a transactional gate (backend-best-practices §7 — failed write blocks suggestion use)
- [NEW] Backend: REST endpoint
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### AI-03: Complete a partially built itinerary with AI

**As a** Consultant/User, **I want** click 'Complete with AI' on a partially built itinerary and have the AI fill in the remaining locations/categories, **so that** PRD §9.1 Flow A step 7 and §21.2's persistent AI-assist entry point are both satisfied.

- **PRD reference(s):** §9.1 Flow A step 7; §21.2 Itinerary Builder (AI-assist entry point)
- **Module(s)/Screen(s):** ai, booking
- **Story points:** 8 — Same grounding/generation risk as AI-02 but scoped to partial-completion, reusing AI-02's core generation path.
- **Dependencies:** AI-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ai, phase1

**Acceptance Criteria**
- Given an itinerary has some locations/categories already selected, when 'Complete with AI' is invoked, then the AI only proposes line items for the remaining gaps, respecting existing selections.

**Sub-tasks**
- [EXTEND] Backend: partial-itinerary completion — Groq client call + prompt construction (`adren.ai.groq` config)
- [NEW] Backend: audit-log write as a transactional gate (backend-best-practices §7 — failed write blocks suggestion use)
- [NEW] Backend: REST endpoint
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### AI-04: Include supplier source and live-availability status on every AI suggestion

**As a** Consultant/User, **I want** see which supplier and how current the availability is for every AI-suggested line item before approving it, **so that** PRD §11.3's acceptance criterion — every line item shows supplier source and live status before approval — is met as a first-class response field, not something the frontend has to infer.

- **PRD reference(s):** §11.3 Acceptance Criteria; §11.2 principle 2 (confidence & availability indicators)
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — DTO-shape discipline on top of AI-02/03's response — backend-best-practices §7 calls this out explicitly as a first-class-field requirement.
- **Dependencies:** AI-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ai, phase1

**Acceptance Criteria**
- Given an AI-generated itinerary is produced, when each line item is inspected, then it carries `supplierId` and an availability-as-of timestamp as explicit response fields.

**Sub-tasks**
- [EXTEND] Backend: `AiSuggestionResponse (supplierId + availabilityAsOf fields)` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — response DTO shape)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### AI-05: Model AI failure/no-viable-suggestion as an explicit response state

**As a** Consultant/User, **I want** have the AI explicitly state it cannot produce a valid suggestion rather than substituting an over-budget or fabricated option, **so that** PRD §11.2 principle 4, §23.3 Edge Case #7, and T13 are satisfied.

- **PRD reference(s):** §11.2 principle 4; §23.3 Edge Case #7; §25 T13
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — Well-specified sealed-response-type requirement (backend-best-practices §7) — `NoViableSuggestion(reason)` as a legitimate typed value, not an exception.
- **Dependencies:** AI-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ai, phase1

**Acceptance Criteria**
- Given zero available suppliers exist for a location, when AI generation runs, then it states 'no inventory available' explicitly (§11.3).
- Given AI is asked to complete an itinerary with a budget no available inventory can meet, when generation runs, then it explicitly states this inability rather than silently picking the closest over-budget option (T13).

**Sub-tasks**
- [EXTEND] Backend: `NoViableSuggestion result type` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — response DTO shape)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### AI-06: Enforce mandatory human-in-the-loop approval before an AI itinerary reaches the traveler

**As a** Consultant/User, **I want** have every AI-generated itinerary require my explicit approval before it can be shared with a traveler, **so that** PRD §11.2 principle 3 is enforced at the workflow level, not left to convention.

- **PRD reference(s):** §11.2 principle 3; §6 Roles & Permissions Matrix (AI approval row)
- **Module(s)/Screen(s):** ai, booking
- **Story points:** 5 — Workflow gate wired into BOK-08's Quotation-save transition — the enforcement point matters more than the approval UI itself (AI-10 covers the UI).
- **Dependencies:** AI-02, BOK-08
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ai, phase1

**Acceptance Criteria**
- Given an AI-generated itinerary is produced, when an attempt is made to save it as a Quotation or share it with a traveler without approval, then the workflow blocks the transition until the Consultant/permitted User explicitly approves.

**Sub-tasks**
- [EXTEND] Backend: `requireAiApproval` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — gate on Itinerary.markAsQuotation for AI-generated itineraries)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### AI-07: Write a 100%-logged, insert-only AI suggestion audit trail

**As a** Super Admin, **I want** have every single AI suggestion logged with its input, source data, output, and disposition — no sampling, **so that** PRD §11.2 principle 5, §24.3's 100%-logged NFR, and RULES.md §6.3's audit-trail requirement are satisfied.

- **PRD reference(s):** §11.2 principle 5; §24.3 NFR AI Governance; §6.3 (RULES.md)
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — Dedicated insert-only `ai_suggestion_audit_log` table distinct from application logs, per RULES.md §6.3's explicit retention/immutability distinction.
- **Dependencies:** AI-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), integration (Testcontainers)
- **Labels:** backend, ai, phase1

**Acceptance Criteria**
- Given any AI suggestion is generated, when the audit-log write is attempted, then if the write fails, the suggestion is not usable/displayed — the audit write is a transactional gate, not a fire-and-forget side channel (backend-best-practices §7).
- Given 100 AI calls are made in a load test, when the audit log is inspected, then exactly 100 entries exist — zero sampling.

**Sub-tasks**
- [NEW] Backend: `AiSuggestionAuditLog` entity (insert-only)
- [EXTEND] Backend: every AI capability writes the audit log transactionally before returning a usable suggestion
- [NEW] Backend: unit test — failed audit write blocks suggestion use
- [NEW] Backend: integrationTest — 100% logging under concurrent calls

#### AI-08: Capture both the original AI suggestion and the Consultant's edited final version in the audit trail

**As a** Super Admin, **I want** see both what the AI originally suggested and what the Consultant changed it to before approval, **so that** PRD §23.3 Edge Case #8 and T14 are satisfied — the audit log must never overwrite the original.

- **PRD reference(s):** §23.3 Edge Case #8; §25 T14
- **Module(s)/Screen(s):** ai
- **Story points:** 5 — Extends AI-07's audit entity with a linked edit-history record.
- **Dependencies:** AI-07
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ai, phase1

**Acceptance Criteria**
- Given an AI-suggested itinerary is edited by the Consultant after generation, then re-approved, when the audit log is inspected, then both the original AI output and the edited final version are present, not overwritten (T14).

**Sub-tasks**
- [EXTEND] Backend: `AiSuggestionAuditLog` gains a linked `editedFinalVersion` record on approval
- [NEW] Backend: unit test — original preserved after edit
- [NEW] Backend: module test

#### AI-09: Re-validate AI-suggested pricing at booking time if it has gone stale

**As a** Consultant/User, **I want** have any AI-approved itinerary re-validated against live supplier pricing before booking confirms, **so that** PRD §11.3's re-validation acceptance criterion is met — stale pricing discovered post-approval triggers re-validation at booking time.

- **PRD reference(s):** §11.3 Acceptance Criteria (stale pricing re-validation)
- **Module(s)/Screen(s):** ai, supplier
- **Story points:** 5 — Reuses the same re-validation pattern already required for Mystifly (§10.2.4) — applied specifically to AI-sourced line items at booking time.
- **Dependencies:** AI-02, BOK-13
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ai, phase1

**Acceptance Criteria**
- Given stale pricing is discovered post-approval on an AI-generated line item, when booking is attempted, then the system re-validates against the live supplier before confirming, following the same 'price changed, please confirm' pattern as §10.2.4's Mystifly fare-expiry rule.

**Sub-tasks**
- [EXTEND] Backend: `revalidateAiPricingAtBooking` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — invoked during confirmBooking for ai_generated itineraries)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### AI-10: Build the 'Complete with AI' entry point with source/availability badges

**As a** Consultant/User, **I want** invoke AI assistance and see AI-suggested line items with source-supplier and availability badges before they're added — never silently inserted, **so that** PRD §21.2's AI-assist entry point spec is implemented exactly as described.

- **PRD reference(s):** §21.2 Itinerary Builder (AI-assist entry point); §11.2 principle 2
- **Module(s)/Screen(s):** Itinerary Builder (21.2)
- **Story points:** 5 — Frontend consumer of AI-02/AI-04/AI-05's typed response states (Suggested/NoViableSuggestion) — the UI must render all three explicitly.
- **Dependencies:** AI-02, AI-04, AI-05, FND-16
- **Testing tier(s):** component test, e2e
- **Labels:** frontend, ai, phase1

**Acceptance Criteria**
- Given 'Complete with AI' is clicked, when suggestions are returned, then they are shown with source-supplier and availability badges (AI-04's fields) and require explicit accept/reject — never silently inserted into the itinerary.

**Sub-tasks**
- [NEW] Frontend: `useAiAssist` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `AiAssistPanel.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Frontend: Playwright e2e spec (extends `search-flow.spec.ts` pattern, PRD §9.1 flow)

#### AI-11: Build the AI Governance/Audit Log viewer in the Super Admin Console

**As a** Super Admin, **I want** browse the AI suggestion audit log across all Consultants, **so that** PRD §6's 'View AI governance/audit logs (Yes, all)' capability and §21.6's Super Admin Console navigation are implemented.

- **PRD reference(s):** §6 Roles & Permissions Matrix; §21.6 Super Admin Console
- **Module(s)/Screen(s):** ai, Super Admin Console (21.6)
- **Story points:** 5 — Read-only UI over AI-07/AI-08's audit trail with pagination and role-gating.
- **Dependencies:** AI-07, AI-08, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, ai, phase1

**Acceptance Criteria**
- Given Super Admin opens the AI Governance Logs section, when the page loads, then every AI suggestion's input, source data, output, and disposition is browsable and paginated, filterable by Consultant.
- Given a Consultant attempts to reach the equivalent view, when the request is made, then it is rejected — only Super Admin has 'all' visibility per §6.

**Sub-tasks**
- [NEW] Backend: `GET /api/v1/ai/audit-log` paginated, Super-Admin-only endpoint
- [NEW] Frontend: `useAiGovernanceLog` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `AiGovernanceLogViewer.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

#### AI-12: Generate AI ad-creative variants grounded in package content and live pricing

**As a** Consultant, **I want** have the AI generate multiple ad-creative variants for my package, grounded in its actual content and current price, **so that** PRD §14.4's AI Creative Generation requirement is met — creative must never reference stale or fabricated package details.

- **PRD reference(s):** §14.4 AI Creative Generation; §14.2 step 3
- **Module(s)/Screen(s):** ai, ads
- **Story points:** 8 — Cross-module (ai↔ads) generation feeding directly into the Campaign Builder's creative gallery (ADS-04) — depends on both AI-02's generation discipline and a live Package read.
- **Dependencies:** AI-02, BOK-12
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ai, ads, phase1

**Acceptance Criteria**
- Given a Consultant requests creative variants for a published Package, when generation runs, then each variant's copy text is grounded in the Package's actual name/description/current sell price — never fabricated or stale.

**Sub-tasks**
- [EXTEND] Backend: ad-creative variant generation grounded in package content — Groq client call + prompt construction (`adren.ai.groq` config)
- [NEW] Backend: audit-log write as a transactional gate (backend-best-practices §7 — failed write blocks suggestion use)
- [NEW] Backend: REST endpoint
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### AI-13: Bound AI response latency to protect the 10-minute itinerary target

**As a** Consultant/User, **I want** have AI suggestions return within a few seconds per segment, **so that** PRD §24.3's NFR is met so the overall itinerary build stays within the 10-minute target from §9.6.

- **PRD reference(s):** §24.3 NFR AI Governance; §9.6 NFR (10-minute target)
- **Module(s)/Screen(s):** ai
- **Story points:** 3 — Timeout/retry configuration on top of AI-01's client wrapper.
- **Dependencies:** AI-01, AI-07
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ai, phase1

**Acceptance Criteria**
- Given an AI itinerary-completion request is made, when the Groq call is issued, then a bounded per-segment timeout is enforced, with each retry/timeout attempt logged distinctly per AI-07's audit requirement.

**Sub-tasks**
- [EXTEND] Backend: bounded per-segment timeout on `GroqClient`
- [NEW] Backend: distinct per-attempt audit logging on retry (not just the final attempt)
- [NEW] Backend: unit test — timeout triggers distinct-attempt logging

---

## Local DMC + BYOS

*11 stories, 57 story points.*

#### DMC-01: Submit a new Local DMC for onboarding

**As a** Consultant, **I want** submit a Local DMC's business info, product categories, sample rates, and references, **so that** PRD §10.3 step 1's onboarding submission is captured before any vetting can begin.

- **PRD reference(s):** §10.3 Local DMC Onboarding step 1; §20.14 Local DMC Record
- **Module(s)/Screen(s):** supplier, Local DMC Onboarding — NEW feature folder
- **Story points:** 5 — New entity + submission form; the Pending-by-default rule is the load-bearing invariant (enforced on the entity, per backend-best-practices §1).
- **Dependencies:** FND-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, dmc, supplier, phase1

**Acceptance Criteria**
- Given a Consultant submits a new Local DMC, when the submission is saved, then its status is Pending, not Active, until at least one verification step completes (T9).

**Sub-tasks**
- [NEW] Backend: `LocalDmcRecord` entity + `LocalDmcRecordRepository` (package-private, own Flyway migration)
- [NEW] Backend: `submitLocalDmc` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `POST /api/v1/local-dmc` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)
- [NEW] Frontend: `useLocalDmcOnboarding` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `LocalDmcOnboarding.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

#### DMC-02: Run the Local DMC Pending → Active vetting workflow

**As a** Super Admin (or delegated Consultant-level reviewer), **I want** review a submitted Local DMC for basic legitimacy and mark it Active only after at least one verification step, **so that** PRD §10.3 steps 2–3 are implemented as an explicit workflow, not an implicit status flip.

- **PRD reference(s):** §10.3 Local DMC Onboarding steps 2-3; §22.5 T9
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — State-machine method on DMC-01's entity plus a reviewer-facing action.
- **Dependencies:** DMC-01, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, dmc, supplier, phase1

**Acceptance Criteria**
- Given a Local DMC is Pending, when a reviewer completes at least one verification step, then status transitions Pending → Active.
- Given no verification step has been completed, when a reviewer attempts to mark the record Active directly, then the transition is rejected — the entity throws rather than silently allowing it, per backend-best-practices §1.

**Sub-tasks**
- [EXTEND] Backend: `LocalDmcRecord.activate()` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/local-dmc/{id}/activate`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### DMC-03: Bulk-upload Local DMC inventory via a validated CSV/template tool

**As a** Consultant, **I want** bulk-upload a Local DMC's inventory using a CSV template rather than entering products one at a time, **so that** PRD §10.2.8's data-entry/bulk-upload engineering scope is implemented, with required-field validation.

- **PRD reference(s):** §10.2.8 Local DMC — Manual Integration
- **Module(s)/Screen(s):** supplier, Local DMC Onboarding — NEW feature folder
- **Story points:** 8 — CSV parsing + row-level validation + bulk persistence is the most mechanically complex Local DMC story.
- **Dependencies:** DMC-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, dmc, supplier, phase1

**Acceptance Criteria**
- Given a Consultant uploads a CSV missing a required field (product name, category, net rate, currency, cancellation policy text, or availability calendar), when validation runs, then the upload is rejected with row-level, field-level errors — not a partial silent import.
- Given a valid CSV is uploaded, when validation passes, then every row becomes a Local DMC inventory item linked to the DMC record.

**Sub-tasks**
- [NEW] Backend: CSV template parser + row-level Bean Validation
- [NEW] Backend: bulk-insert endpoint `POST /api/v1/local-dmc/{id}/inventory/bulk-upload`
- [NEW] Frontend: `useLocalDmcBulkUpload` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `LocalDmcBulkUpload.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

#### DMC-04: Track Local DMC quality signal — cancellation rate and complaint count

**As a** Super Admin/Consultant, **I want** see a Local DMC's rolling cancellation rate and complaint count, **so that** PRD §10.3 step 5's ongoing quality signal is visible, feeding the flagging rule in DMC-05.

- **PRD reference(s):** §10.3 Local DMC Onboarding step 5; §20.14 Local DMC Record
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Rolling-metric calculation triggered by booking/cancellation events from `booking`.
- **Dependencies:** DMC-02, BOK-16
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, dmc, supplier, phase1

**Acceptance Criteria**
- Given a booking against a Local DMC product is cancelled, when the quality signal updates, then `cancellation_rate` recalculates as a rolling figure on the DMC record.

**Sub-tasks**
- [EXTEND] Backend: `recalculateQualitySignal` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — event-driven on cancellation)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### DMC-05: Flag a Local DMC to both the onboarding Consultant and Super Admin on threshold breach

**As a** Super Admin/Consultant, **I want** see a visible flag on a Local DMC's record once its cancellation rate or complaint count exceeds a defined threshold, **so that** PRD §22.5's second acceptance criterion is met.

- **PRD reference(s):** §22.5 Local DMC Onboarding (threshold flag)
- **Module(s)/Screen(s):** supplier, Super Admin Console (21.6)
- **Story points:** 3 — Threshold check + visibility rule on top of DMC-04's rolling metric.
- **Dependencies:** DMC-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, dmc, supplier, phase1

**Acceptance Criteria**
- Given a Local DMC's cancellation rate exceeds a defined threshold, when the quality signal updates, then both the onboarding Consultant and Super Admin see a flag on that DMC's record.

**Sub-tasks**
- [EXTEND] Backend: threshold check on quality-signal recalculation, sets a `flagged` boolean
- [EXTEND] Frontend: flag badge on the Local DMC record in both Consultant and Super Admin views
- [NEW] Backend: unit test
- [NEW] Frontend: component test

#### DMC-06: Let a Consultant enter their own supplier API credentials (BYOS)

**As a** Consultant, **I want** enter my own Hotelbeds/STUBA/etc. API credentials so BYOS inventory is scoped to my account, **so that** PRD §10.4's BYOS entry flow is available, feeding FND-12's row-level encrypted storage.

- **PRD reference(s):** §10.4 BYOS
- **Module(s)/Screen(s):** supplier, BYOS credential entry — NEW feature folder
- **Story points:** 5 — UI + endpoint over FND-12's already-built encrypted storage mechanism.
- **Dependencies:** FND-12
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, dmc, supplier, phase1

**Acceptance Criteria**
- Given a Consultant enters their own Hotelbeds credentials, when they save the form, then the credentials are stored via FND-12's row-level encryption, scoped only to that Consultant.

**Sub-tasks**
- [EXTEND] Backend: `saveByosCredential` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/consultants/{id}/byos-credentials`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [NEW] Frontend: `useByosCredentialEntry` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `ByosCredentialEntry.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

#### DMC-07: Make the supplier integration layer credential-source-agnostic

**As a** backend engineer, **I want** have the same Hotelbeds integration code path work whether credentials are Adren's own or a Consultant's BYOS credentials, **so that** PRD §10.2.9's requirement is met as a dependency-injection concern, not a branching concern, per backend-best-practices §6.

- **PRD reference(s):** §10.2.9 BYOS (Technical integration pattern)
- **Module(s)/Screen(s):** supplier
- **Story points:** 8 — Structural refactor of the credential-resolution path across every supplier client — must be done once, correctly, before more suppliers land (Phase 2 SUP epic depends on this).
- **Dependencies:** DMC-06, FND-11, FND-12
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, dmc, supplier, phase1

**Acceptance Criteria**
- Given a search request is scoped to a Consultant with BYOS Hotelbeds credentials configured, when `HotelbedsClient` is invoked, then it receives the resolved credential set as a parameter — no `if (isByos)` branch exists inside the client itself.

**Sub-tasks**
- [EXTEND] Backend: credential-set-as-parameter contract across `HotelbedsClient` (and future supplier clients)
- [NEW] Backend: upstream credential-source resolver (Adren vs. BYOS) invoked once per request, injected downstream
- [NEW] Backend: unit test — same client class, both credential sources
- [NEW] Backend: module test

#### DMC-08: Merge BYOS inventory into search results using standard normalization and Default Selection

**As a** Consultant, **I want** have my BYOS supplier's inventory appear in search results using the same normalization and default-selection logic as Adren's own suppliers, **so that** PRD §22.6's T10 acceptance criterion is met.

- **PRD reference(s):** §10.4 BYOS; §22.6 T10
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Wires DMC-07's credential-agnostic client into `SupplierAggregationService`'s existing fan-out.
- **Dependencies:** DMC-07, FND-14
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, dmc, supplier, phase1

**Acceptance Criteria**
- Given a Consultant adds their own Hotelbeds credentials via BYOS, when search runs, then BYOS inventory appears in results using the same normalization logic as Adren's own Hotelbeds connection (T10).

**Sub-tasks**
- [EXTEND] Backend: `BYOS fan-out inclusion` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `(internal — SupplierAggregationService)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### DMC-09: Scope BYOS inventory and credentials strictly to the owning Consultant

**As a** Consultant, **I want** be certain another Consultant can never see or use my BYOS credentials or the inventory they unlock, **so that** PRD §22.6's tenant-scoping half of T10 and RULES.md §5.2/§5.3 are both satisfied.

- **PRD reference(s):** §22.6 T10 (scoping); §5.2/§5.3 (RULES.md)
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Security-critical scoping test on top of DMC-08 — the IDOR risk RULES.md §5.3 explicitly calls out as worse than an itinerary leak.
- **Dependencies:** DMC-08, FND-03
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, dmc, supplier, security, phase1

**Acceptance Criteria**
- Given Consultant B's search runs, when BYOS credentials are resolved, then only Consultant B's own BYOS credentials are ever loaded — Consultant A's are neither visible nor usable, verified via the same tenant-isolation check as FND-03.

**Sub-tasks**
- [EXTEND] Backend: BYOS credential resolver enforces tenant scope on every read
- [NEW] Backend: unit test — cross-tenant BYOS read attempt
- [NEW] Backend: integrationTest — cross-tenant BYOS read attempt end-to-end

#### DMC-10: Manage Local DMC inventory items after onboarding (CRUD)

**As a** Consultant, **I want** edit or remove individual Local DMC inventory items after initial bulk upload, **so that** PRD §10.2.8's CRUD scope extends beyond the initial bulk-upload tool.

- **PRD reference(s):** §10.2.8 Local DMC — Manual Integration
- **Module(s)/Screen(s):** supplier, Local DMC Onboarding — NEW feature folder
- **Story points:** 5 — Standard CRUD over DMC-03's bulk-uploaded records.
- **Dependencies:** DMC-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, dmc, supplier, phase1

**Acceptance Criteria**
- Given a Consultant edits a Local DMC inventory item's rate, when they save the change, then the updated rate is reflected in subsequent search results for that DMC's inventory.

**Sub-tasks**
- [EXTEND] Backend: `updateLocalDmcInventoryItem` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `PATCH /api/v1/local-dmc/{id}/inventory/{itemId}`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [NEW] Frontend: `useLocalDmcInventory` hook (React Query for server data per RULES.md §7.1)
- [EXTEND] Frontend: `LocalDmcInventoryManagement.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

#### DMC-11: Alert on stale Local DMC inventory beyond a defined threshold

**As a** Super Admin, **I want** be alerted when a Local DMC's manually-entered inventory hasn't been updated beyond a defined staleness threshold, **so that** PRD §10.5's sync-failure alerting requirement extends to the manual/no-live-API Local DMC path, which has no automatic sync to fail.

- **PRD reference(s):** §10.5 Inventory Sync
- **Module(s)/Screen(s):** supplier
- **Story points:** 3 — Scheduled job + threshold check over DMC-10's inventory records.
- **Dependencies:** DMC-10
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, dmc, supplier, phase1

**Acceptance Criteria**
- Given a Local DMC's inventory calendar has not been updated within the defined staleness threshold, when the scheduled check runs, then Super Admin receives an alert flagging that DMC's inventory as stale.

**Sub-tasks**
- [NEW] Backend: scheduled staleness-check job over Local DMC inventory `updated_at`
- [NEW] Backend: alert dispatch to Super Admin on breach
- [NEW] Backend: unit test — threshold boundary

---

## Ads/Campaign Management

*15 stories, 80 story points.*

#### ADS-01: Provision a Meta ad account/Business Manager for a Consultant

**As a** Super Admin, **I want** provision a Meta ad account and Business Manager for a Consultant under Adren's umbrella structure, **so that** PRD §14.1's centrally-managed account model is implemented before any campaign can be built.

- **PRD reference(s):** §14.1 Ads/Campaign Overview; §6 Roles & Permissions Matrix
- **Module(s)/Screen(s):** ads
- **Story points:** 8 — New module (stub today) + first Meta-adjacent entity; MVP scope is provisioning bookkeeping (mocked Meta calls), full API wiring is Phase 2's MADS epic.
- **Dependencies:** FND-01, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ads, phase1

**Acceptance Criteria**
- Given Super Admin provisions a Meta ad account for a Consultant, when provisioning completes, then the Consultant is linked to an Adren-managed Meta Business Manager, never a Consultant-owned one, per §6's 'No (executes)' row.

**Sub-tasks**
- [NEW] Backend: `ads/package-info.java` → real module shape (`AdsApi`, `event/`, `internal/`)
- [NEW] Backend: `AdAccount` entity + provisioning service (mocked Meta call in MVP)
- [NEW] Backend: `POST /api/v1/consultants/{id}/ad-account` endpoint
- [NEW] Backend: unit test
- [NEW] Backend: module test

#### ADS-02: Model the Ad Campaign entity and its status state machine

**As a** backend engineer, **I want** have an `AdCampaign` entity enforcing the PendingApproval → PendingPolicyReview → Live → Paused/Rejected/SpendCapReached state machine, **so that** PRD §20.13's status enum is enforced as entity-owned transitions, not scattered service-layer conditionals, per backend-best-practices §1.

- **PRD reference(s):** §20.13 Ad Campaign
- **Module(s)/Screen(s):** ads
- **Story points:** 5 — State-machine entity design, mirroring `Itinerary.markAsQuotation()`'s established pattern.
- **Dependencies:** ADS-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ads, phase1

**Acceptance Criteria**
- Given a campaign transitions from PendingApproval to Live, when an invalid transition is attempted instead (e.g. Rejected → Live), then the entity throws `IllegalStateException` rather than silently no-op'ing.

**Sub-tasks**
- [NEW] Backend: `AdCampaign` entity + `AdCampaignRepository` (package-private, own Flyway migration)
- [NEW] Backend: `createCampaign` on the module `ServiceImpl` + domain event publication (`@Transactional`)
- [NEW] Backend: REST endpoint `POST /api/v1/campaigns` (Controller depends on the module's `Api` interface only)
- [NEW] Backend: unit test (service logic, mocked repository/publisher)
- [NEW] Backend: `@ApplicationModuleTest` module-slice test (event publish/consume)

#### ADS-03: Build the Campaign Builder screen — package selector, audience/budget/duration inputs

**As a** Consultant, **I want** select a published Package and provide audience, budget, and duration inputs to start a campaign, **so that** PRD §14.2 steps 1–2 and §21.8's layout are implemented.

- **PRD reference(s):** §14.2 Flow steps 1-2; §21.8 Campaign Builder
- **Module(s)/Screen(s):** ads, Campaign Builder (21.8) — NEW feature folder
- **Story points:** 8 — New multi-step screen; form/validation library (FES-08) is a direct dependency given the multi-field campaign-input form.
- **Dependencies:** ADS-02, BOK-12, FES-08
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, ads, phase1

**Acceptance Criteria**
- Given a Consultant opts into 'Promote this Package' from the Package Builder, when the Campaign Builder opens, then the selected Package is pre-populated and audience/budget/duration fields are required before proceeding.

**Sub-tasks**
- [EXTEND] Backend: `submitCampaignInputs` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/campaigns/{id}/inputs`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [NEW] Frontend: `useCampaignBuilder` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `CampaignBuilder.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

#### ADS-04: Generate and display AI creative variant gallery in the Campaign Builder

**As a** Consultant, **I want** see multiple AI-generated creative variants (image/copy combinations) for my campaign, **so that** PRD §14.2 step 3 and §21.8's creative-variant-gallery layout are implemented.

- **PRD reference(s):** §14.2 Flow step 3; §21.8 Campaign Builder
- **Module(s)/Screen(s):** ads, ai, Campaign Builder (21.8)
- **Story points:** 5 — Frontend consumer of AI-12's already-built generation capability, plus the `creative_variants[]` persistence on `AdCampaign`.
- **Dependencies:** AI-12, ADS-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, ads, phase1

**Acceptance Criteria**
- Given a Campaign Builder session reaches the creative step, when AI-12's generation runs, then multiple image/copy variant combinations are displayed in a gallery, each grounded in the Package's actual content and live price.

**Sub-tasks**
- [EXTEND] Backend: `creative_variants[]` persisted on `AdCampaign` from AI-12's output
- [EXTEND] Frontend: `useCreativeVariants` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `CreativeVariantGallery.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

#### ADS-05: Require Consultant approval per creative variant before submission

**As a** Consultant, **I want** approve each creative variant individually before the campaign is submitted for policy review, **so that** PRD §14.2 step 4's mandatory approval step is enforced, not skippable.

- **PRD reference(s):** §14.2 Flow step 4; §21.8 Campaign Builder (approval checkboxes)
- **Module(s)/Screen(s):** ads, Campaign Builder (21.8)
- **Story points:** 5 — Approval-gate enforcement on top of ADS-04's gallery, mirroring AI-06's human-in-the-loop pattern.
- **Dependencies:** ADS-04
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, ads, phase1

**Acceptance Criteria**
- Given creative variants are displayed, when the Consultant attempts to submit for review without checking any approval checkbox, then submission is blocked — approval per variant is mandatory, not optional.

**Sub-tasks**
- [EXTEND] Backend: `approveCreativeVariant` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/campaigns/{id}/creative-variants/{variantId}/approval`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [EXTEND] Frontend: `useCreativeApproval` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `CreativeApprovalPanel.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

#### ADS-06: Route approved campaigns through Super Admin brand-safety/policy review

**As a** Super Admin, **I want** review a Consultant-approved campaign for brand-safety and policy compliance before it launches, **so that** PRD §14.2 step 5's review gate is implemented, transitioning the campaign to PendingPolicyReview.

- **PRD reference(s):** §14.2 Flow step 5; §20.13 status enum (PendingPolicyReview)
- **Module(s)/Screen(s):** ads, Super Admin Console (21.6)
- **Story points:** 5 — Second state-machine transition + Super Admin queue UI on top of ADS-02/ADS-05.
- **Dependencies:** ADS-05, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, ads, phase1

**Acceptance Criteria**
- Given a campaign has all creative variants Consultant-approved, when it is submitted, then its status transitions to PendingPolicyReview and appears in the Super Admin's review queue.
- Given Super Admin rejects the campaign, when the rejection is submitted, then status transitions to Rejected with a reason visible to the Consultant.

**Sub-tasks**
- [EXTEND] Backend: `reviewCampaign` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/campaigns/{id}/policy-review`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [NEW] Frontend: `useCampaignPolicyReview` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `CampaignPolicyReviewQueue.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

#### ADS-07: Launch an approved campaign under the Adren-managed Meta account

**As a** Consultant, **I want** have my approved campaign go live and receive a `meta_campaign_ref`, **so that** PRD §14.2 step 6 is implemented (mocked Meta launch call in MVP; real integration is Phase 2's MADS epic).

- **PRD reference(s):** §14.2 Flow step 6; §20.13 meta_campaign_ref
- **Module(s)/Screen(s):** ads
- **Story points:** 5 — Third state-machine transition; the mock/real boundary is deliberate MVP scoping (see Phase 2 MADS-02 for the live equivalent).
- **Dependencies:** ADS-06, ADS-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ads, phase1

**Acceptance Criteria**
- Given a campaign passes policy review, when launch is triggered, then status transitions to Live and a `meta_campaign_ref` is stored, using a mocked Meta launch call in MVP scope.

**Sub-tasks**
- [EXTEND] Backend: `launchCampaign` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `POST /api/v1/campaigns/{id}/launch`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

#### ADS-08: Display a campaign status stepper matching the status enum

**As a** Consultant, **I want** see a visual status stepper (Pending Approval → Pending Policy Review → Live / Rejected) for my campaign, **so that** PRD §21.8's status-tracking requirement matches §20.13's enum exactly.

- **PRD reference(s):** §21.8 Campaign Builder (status tracking); §20.13 status enum
- **Module(s)/Screen(s):** Campaign Builder (21.8)
- **Story points:** 3 — Presentational component driven entirely by ADS-02's enum — no new backend work.
- **Dependencies:** ADS-07
- **Testing tier(s):** component test
- **Labels:** frontend, ads, phase1

**Acceptance Criteria**
- Given a campaign is in any of the six `status` enum values, when the Consultant views the Campaign Builder, then the stepper highlights the exact matching stage — no divergence between UI labels and the backend enum.

**Sub-tasks**
- [NEW] Frontend: `CampaignStatusStepper` presentational component
- [NEW] Frontend: component test — one case per enum value

#### ADS-09: Flow campaign performance data back to the Consultant Dashboard

**As a** Consultant, **I want** see my campaign's impressions, clicks, and attributed bookings on my dashboard, **so that** PRD §14.2 step 7 and §20.13's `performance_snapshot` are surfaced to the Consultant.

- **PRD reference(s):** §14.2 Flow step 7; §20.13 performance_snapshot
- **Module(s)/Screen(s):** ads, Consultant Dashboard (21.5)
- **Story points:** 5 — Read-side wiring; write-side (mocked Meta insights polling) uses the same MVP-mock boundary as ADS-07.
- **Dependencies:** ADS-07
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, ads, phase1

**Acceptance Criteria**
- Given a Live campaign has performance data, when the Consultant Dashboard's Active Campaigns tab loads, then impressions, clicks, and bookings_attributed are shown per campaign, sourced from `performance_snapshot`.

**Sub-tasks**
- [NEW] Backend: `performance_snapshot` mock-populated on a scheduled interval (MVP)
- [NEW] Backend: `GET /api/v1/campaigns?consultantId=` paginated endpoint
- [NEW] Backend: unit test
- [NEW] Frontend: component test for the Active Campaigns tab (co-developed with HRD-09's Consultant Dashboard)

#### ADS-10: Enforce near-real-time spend-cap on active campaigns

**As a** Super Admin/Consultant, **I want** have a campaign's spend never meaningfully overshoot its budget cap, **so that** PRD §14.3's guardrail and §24.6's near-real-time NFR are both met.

- **PRD reference(s):** §14.3 Controls & Guardrails; §24.6 NFR Ads/Campaign
- **Module(s)/Screen(s):** ads
- **Story points:** 8 — Near-real-time enforcement against a mocked spend feed in MVP is still non-trivial polling/threshold logic — the highest-uncertainty Ads story alongside ADS-01.
- **Dependencies:** ADS-07
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ads, phase1

**Acceptance Criteria**
- Given a Live campaign's `spend_to_date` approaches `budget_cap`, when spend tracking updates, then the campaign transitions to SpendCapReached before processing lag allows a meaningful overshoot.

**Sub-tasks**
- [NEW] Backend: spend-tracking poller (mocked feed in MVP) + threshold-triggered `SpendCapReached` transition
- [NEW] Backend: unit test — threshold boundary
- [NEW] Backend: module test — transition triggers correctly under simulated spend growth

#### ADS-11: Implement campaign approval workflow guardrails and billing transparency

**As a** Consultant, **I want** see exactly what I'm being billed for ad spend, with clear approval-workflow guardrails throughout, **so that** PRD §14.3's full guardrail set (spend caps, approval workflow, brand-safety review, billing transparency, account-suspension escalation) is implemented as a cohesive whole beyond the individual state transitions.

- **PRD reference(s):** §14.3 Controls & Guardrails
- **Module(s)/Screen(s):** ads
- **Story points:** 5 — Consolidates ADS-05/06/10's individual guardrails into a coherent billing-transparency view.
- **Dependencies:** ADS-10
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, ads, phase1

**Acceptance Criteria**
- Given a Consultant views a Live campaign, when they open the billing detail, then spend-to-date, budget cap, and a per-transaction breakdown are all visible, not summarized into one opaque figure.

**Sub-tasks**
- [NEW] Backend: `GET /api/v1/campaigns/{id}/billing-detail` endpoint
- [NEW] Frontend: `useCampaignBillingDetail` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `CampaignBillingDetail.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

#### ADS-12: Auto-pause a campaign when its linked Package price changes

**As a** Super Admin/Consultant, **I want** have a Live campaign automatically pause if its linked Package's price changes, **so that** PRD §23.5 Edge Case #11 and T16 are satisfied.

- **PRD reference(s):** §23.5 Edge Case #11; §25 T16
- **Module(s)/Screen(s):** ads, booking
- **Story points:** 5 — Event listener on a Package-price-changed event (published by `booking`, consumed by `ads` per RULES.md §2.1's cross-module event pattern).
- **Dependencies:** ADS-07, BOK-12
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ads, booking, phase1

**Acceptance Criteria**
- Given a campaign's linked Package is edited (price change) while the campaign is Live, when the change is saved, then the system detects the mismatch and pauses the campaign until creative/pricing is re-approved (T16).

**Sub-tasks**
- [NEW] Backend: `PackagePriceChangedEvent` published by `booking`
- [NEW] Backend: `ads` `@ApplicationModuleListener` pausing linked Live campaigns (idempotent per RULES.md §2.2)
- [NEW] Backend: unit test
- [NEW] Backend: module test — event triggers pause

#### ADS-13: Surface a clear 'suspended — action required' status on Meta account suspension

**As a** Consultant, **I want** see an explicit, actionable status if Meta suspends my ad account mid-campaign, not a silently-stopped campaign, **so that** PRD §23.5 Edge Case #12 and T17 are satisfied.

- **PRD reference(s):** §23.5 Edge Case #12; §25 T17
- **Module(s)/Screen(s):** ads
- **Story points:** 5 — MVP-mock suspension-signal handling; real Meta webhook handling is Phase 2's MADS-07.
- **Dependencies:** ADS-01, ADS-07
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, ads, phase1

**Acceptance Criteria**
- Given Meta suspends an ad account mid-campaign (simulated in MVP via a mocked suspension signal), when the suspension is detected, then all active campaigns under that Consultant show 'suspended — action required,' not a silent stop with no explanation (T17).

**Sub-tasks**
- [NEW] Backend: mocked suspension-signal handler → campaign status flag
- [NEW] Frontend: `useCampaignSuspension` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `CampaignSuspensionBanner.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

#### ADS-14: Define ad-spend billing model per settlement currency

**As a** Super Admin, **I want** bill ad spend to each Consultant in their settlement currency using a defined managed-service fee model, **so that** PRD §1's 'managed-service fee on ad spend' business model is reflected in the billing calculation.

- **PRD reference(s):** §1 Executive Summary (managed-service fee); §19 Open Items
- **Module(s)/Screen(s):** ads, payments
- **Story points:** 5 — Calculation logic is straightforward once the business rule is confirmed; flagged pending business sign-off.
- **Dependencies:** ADS-11, FIN-06
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ads, payments, phase1

**Acceptance Criteria**
- Given a campaign accrues spend in a Consultant's settlement currency, when billing is calculated, then the managed-service fee is applied per the configured model and reconciled against FIN-06's wallet/ledger.

**Sub-tasks**
- [NEW] Backend: `calculateAdSpendBilling` business logic / state-transition method
- [NEW] Backend: domain event publication (`@Transactional`, same method scope)
- [NEW] Backend: REST endpoint `(internal — billing pipeline step)`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test

> ⚠️ **NEEDS CLARIFICATION:** PRD §19: exact ad-spend billing model per settlement currency is an open item for business confirmation — this story implements a configurable-percentage placeholder pipeline pending the confirmed model.

#### ADS-15: Apply brand-safety policy template guardrails to campaign submissions

**As a** Super Admin, **I want** have campaign creative checked against a brand-safety policy template before it reaches the manual policy-review queue, **so that** PRD §14.3's brand-safety review is assisted by a first-pass automated guardrail, reducing manual review load.

- **PRD reference(s):** §14.3 Controls & Guardrails
- **Module(s)/Screen(s):** ads
- **Story points:** 3 — Rule-based template check (not AI-driven) layered ahead of ADS-06's manual review queue.
- **Dependencies:** ADS-06
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, ads, phase1

**Acceptance Criteria**
- Given a campaign's creative variants are submitted for policy review, when the template check runs first, then obvious policy-template violations are flagged before a human reviewer sees the submission, without auto-rejecting.

**Sub-tasks**
- [NEW] Backend: brand-safety policy template rule set (data-driven)
- [EXTEND] Backend: pre-check applied ahead of the manual review queue
- [NEW] Backend: unit test

---

## Hardening

*13 stories, 76 story points.*

#### HRD-01: Implement notification dispatch for email plus region-configurable secondary channel

**As a** User/Consultant, **I want** receive notifications by email everywhere and by a region-appropriate secondary channel (WhatsApp for India/Dubai, SMS for UK/US/Australia/Denmark), **so that** PRD §15's channel model and §22.7's T11 requirement are implemented, replacing today's empty `BookingNotificationListener` TODO stub.

- **PRD reference(s):** §15 Notifications & Cancellation Management; §22.7 T11
- **Module(s)/Screen(s):** notification
- **Story points:** 8 — Fills in the currently-empty reference listener with real email + region-routed secondary-channel dispatch across two provider integrations.
- **Dependencies:** FND-21, BOK-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), integration (Testcontainers)
- **Labels:** backend, notification, phase1

**Acceptance Criteria**
- Given a booking is confirmed for a Dubai-based Consultant, when the confirmation notification fires, then WhatsApp is used as the default secondary channel unless the Consultant has overridden this preference (T11).
- Given a booking is confirmed for a UK-based Consultant, when the confirmation notification fires, then SMS is used as the region default secondary channel.

**Sub-tasks**
- [EXTEND] Backend: `BookingNotificationListener.on(BookingConfirmedEvent)` real implementation (email + region-routed secondary channel)
- [NEW] Backend: WhatsApp provider client
- [NEW] Backend: SMS provider client
- [NEW] Backend: unit test — region→channel routing
- [NEW] Backend: integrationTest — full dispatch against LocalStack-emulated provider endpoints where applicable

#### HRD-02: Wire all PRD §15 notification trigger events

**As a** User/Consultant, **I want** be notified on booking confirmed, payment received, cancellation, refund, AI approval needed, campaign status change, and credit threshold breach, **so that** PRD §15's full trigger-event list is wired, not just booking confirmation.

- **PRD reference(s):** §15 Notifications (Trigger events)
- **Module(s)/Screen(s):** notification, booking, payments, ai, ads
- **Story points:** 8 — Requires a listener per trigger event across four other modules' already-published (or newly-added) domain events — broad but mechanically repetitive.
- **Dependencies:** HRD-01
- **Testing tier(s):** module (@ApplicationModuleTest), integration (Testcontainers)
- **Labels:** backend, notification, phase1

**Acceptance Criteria**
- Given any of the seven PRD §15 trigger events occurs, when the corresponding domain event is published by its owning module, then the `notification` module's listeners consume it and dispatch per HRD-01's channel routing.

**Sub-tasks**
- [NEW] Backend: `@ApplicationModuleListener` per trigger event (payment received, cancellation, refund, AI approval needed, campaign status, credit threshold)
- [NEW] Backend: any missing domain event added to its owning module (e.g. `CreditThresholdBreachedEvent` in `payments`)
- [NEW] Backend: module test per trigger
- [NEW] Backend: integrationTest — at least one full cross-module trigger path

#### HRD-03: Make every notification listener idempotent

**As a** platform reliability owner, **I want** have every notification listener safe to run twice for the same event, **so that** RULES.md §2.2's mandatory idempotency rule is met — a traveler must never be double-notified on an at-least-once redelivery.

- **PRD reference(s):** §2.2 Idempotency is mandatory (RULES.md)
- **Module(s)/Screen(s):** notification
- **Story points:** 5 — Explicit reconciliation item flagged in RULES.md §2.2 as required before the notification listener's real body ships — sequenced right after HRD-01/02.
- **Dependencies:** HRD-01, HRD-02
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, notification, phase1

**Acceptance Criteria**
- Given a notification listener is redelivered the same event after a crash-and-retry, when it runs a second time, then no duplicate notification is sent — a dedup key (event_id, listener_name) with a DB unique constraint prevents the resend.

**Sub-tasks**
- [NEW] Backend: `processed_events` table with unique `(event_id, listener_name)` constraint
- [EXTEND] Backend: every listener checks-then-inserts before dispatching
- [NEW] Backend: unit test — duplicate delivery is a no-op
- [NEW] Backend: integrationTest — simulated crash-and-retry redelivery

#### HRD-04: Build the Notification Preferences screen

**As a** Consultant, **I want** toggle my secondary notification channel with a regional default pre-selected but overridable, **so that** PRD §21.10's layout is implemented.

- **PRD reference(s):** §21.10 Notification Preferences Screen; §15 Notifications
- **Module(s)/Screen(s):** notification, Notification Preferences Screen (21.10) — NEW feature folder
- **Story points:** 5 — Small screen over HRD-01's per-Consultant preference field.
- **Dependencies:** HRD-01
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, notification, phase1

**Acceptance Criteria**
- Given a Consultant opens Notification Preferences, when the screen loads, then the regional default secondary channel is pre-selected, and the Consultant can override it.
- Given a Consultant overrides their default, when they save, then HRD-01's routing logic uses the override on all subsequent notifications.

**Sub-tasks**
- [EXTEND] Backend: `updateNotificationPreference` business logic / state-transition method
- [EXTEND] Backend: domain event publication (`@Transactional`, same method scope)
- [EXTEND] Backend: REST endpoint `PUT /api/v1/consultants/{id}/notification-preference`
- [NEW] Backend: unit test
- [NEW] Backend: module/integration test
- [NEW] Frontend: `useNotificationPreferences` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `NotificationPreferences.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

#### HRD-05: Implement the full cancellation workflow across policy check, approval, and refund

**As a** Consultant, **I want** have a cancellation move through policy check → refund/penalty calculation → my approval (if needed) → refund processed as one coherent workflow, **so that** PRD §12.5's workflow is realized end-to-end, connecting FIN-16's calculation to a real notification and status trail.

- **PRD reference(s):** §12.5 Cancellation & Dispute Handling
- **Module(s)/Screen(s):** booking, payments, notification
- **Story points:** 8 — End-to-end orchestration story tying together FIN-16 (calculation), booking's cancellation entry point, and HRD-01–03's notification dispatch.
- **Dependencies:** FIN-16, HRD-03
- **Testing tier(s):** unit, module (@ApplicationModuleTest), integration (Testcontainers)
- **Labels:** backend, booking, payments, notification, phase1

**Acceptance Criteria**
- Given a cancellation with no penalty is submitted, when the workflow runs, then it completes without requiring explicit approval, and both email and configured secondary-channel notifications fire on refund per T11-adjacent §22.7 rule.

**Sub-tasks**
- [EXTEND] Backend: cancellation endpoint orchestrates FIN-16's policy-check/approval/refund state machine end-to-end
- [NEW] Backend: notification dispatch on refund completion (both channels per §22.7)
- [NEW] Backend: unit test
- [NEW] Backend: integrationTest — full cancellation-to-refund-to-notification path

#### HRD-06: Track disputes as tickets, not email handoffs

**As a** Consultant/Super Admin, **I want** have a flagged dispute create a trackable ticket with status, not just an email, **so that** PRD §12.5's dispute-tracking requirement is met.

- **PRD reference(s):** §12.5 Cancellation & Dispute Handling (dispute flagging)
- **Module(s)/Screen(s):** notification, booking
- **Story points:** 5 — UI layer over FIN-16's `DisputeTicket` entity, with status tracking visible to both roles.
- **Dependencies:** FIN-16
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, notification, booking, phase1

**Acceptance Criteria**
- Given a dispute is flagged on a booking, when the flag is submitted, then a `DisputeTicket` (FIN-16) is visible with a status the Consultant and Super Admin can both track to resolution, not just an emailed notice.

**Sub-tasks**
- [NEW] Backend: `GET /api/v1/disputes?consultantId=` paginated endpoint
- [NEW] Frontend: `useDisputeTickets` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `DisputeTicketTracker.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

#### HRD-07: Implement PNR/Booking Search across all product types

**As a** User, **I want** search by PNR/internal booking reference and get a result regardless of underlying product type, **so that** PRD §16 and §22.8's T12 requirement are met.

- **PRD reference(s):** §16 PNR Search; §22.8 T12
- **Module(s)/Screen(s):** booking
- **Story points:** 5 — Single search endpoint over BOK-19's `pnrSearchableRef`, paginated per FND-23's convention.
- **Dependencies:** BOK-19, FND-23
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, booking, phase1

**Acceptance Criteria**
- Given a booking reference is entered in PNR search, when the search runs, then it returns results regardless of whether the underlying product is a flight, hotel, transfer, cruise, or activity (T12).

**Sub-tasks**
- [NEW] Backend: `GET /api/v1/bookings/search?ref=` endpoint (paginated) across all product-type line items
- [NEW] Backend: unit test — one case per product type
- [NEW] Backend: module test

#### HRD-08: Build the PNR/Booking Search screen

**As a** User, **I want** enter a single reference and see a booking summary across all product types, click through to full detail, **so that** PRD §21.9's layout is implemented.

- **PRD reference(s):** §21.9 PNR / Booking Search
- **Module(s)/Screen(s):** booking, PNR / Booking Search (21.9) — NEW feature folder
- **Story points:** 5 — Frontend consumer of HRD-07's endpoint.
- **Dependencies:** HRD-07
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test, e2e
- **Labels:** frontend, booking, phase1

**Acceptance Criteria**
- Given a User submits a PNR/booking reference, when results load, then a summary is shown regardless of product type, with a click-through to the full booking detail view.

**Sub-tasks**
- [NEW] Frontend: `usePnrBookingSearch` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `PnrBookingSearch.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Frontend: Playwright e2e spec (extends `search-flow.spec.ts` pattern, PRD §9.1 flow)

#### HRD-09: Build the Consultant Dashboard

**As a** Consultant, **I want** see bookings this month, top packages, wallet balance, pending quotations, and active campaigns in one place, **so that** PRD §9.5 and §21.5's dashboard spec are implemented.

- **PRD reference(s):** §9.5 Reporting & Dashboard Spec; §21.5 Consultant Dashboard
- **Module(s)/Screen(s):** booking, payments, ads, Consultant Dashboard (21.5) — NEW feature folder
- **Story points:** 8 — Aggregates read models across three modules (booking, payments, ads) into one dashboard — the broadest single-screen data-fetching surface in the catalogue.
- **Dependencies:** BOK-12, FIN-06, ADS-09, FND-23
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test, e2e
- **Labels:** backend, frontend, phase1

**Acceptance Criteria**
- Given a Consultant with existing activity opens their dashboard, when the page loads, then summary cards (bookings this month, GMV, wallet balance) and tabs (Top Packages, Pending Quotations, Active Campaigns) are all populated from real data.

**Sub-tasks**
- [NEW] Backend: `GET /api/v1/dashboard/consultant` composite read endpoint (paginated sub-collections)
- [NEW] Frontend: `useConsultantDashboard` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `ConsultantDashboard.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)
- [NEW] Frontend: Playwright e2e spec (extends `search-flow.spec.ts` pattern, PRD §9.1 flow)

#### HRD-10: Show an onboarding checklist instead of empty charts for new Consultants

**As a** new Consultant, **I want** see an onboarding checklist instead of empty dashboard charts before I have any bookings, **so that** PRD §21.5's empty-state requirement is met.

- **PRD reference(s):** §21.5 Consultant Dashboard (empty states)
- **Module(s)/Screen(s):** Consultant Dashboard (21.5)
- **Story points:** 3 — Conditional presentational branch on top of HRD-09's dashboard.
- **Dependencies:** HRD-09
- **Testing tier(s):** component test
- **Labels:** frontend, phase1

**Acceptance Criteria**
- Given a new Consultant with zero bookings opens their dashboard, when the page loads, then an onboarding checklist is shown in place of empty charts, not a blank/zeroed-out dashboard.

**Sub-tasks**
- [EXTEND] Frontend: empty-state branch on `ConsultantDashboard` when zero bookings exist
- [NEW] Frontend: component test — empty-state renders the checklist, not zeroed charts

#### HRD-11: Build the Super Admin Dashboard

**As a** Super Admin, **I want** see all-Consultant GMV, supplier performance, AI governance summary, and ad spend across Consultants, **so that** PRD §9.5 and §21.6's global reporting spec are implemented.

- **PRD reference(s):** §9.5 Reporting & Dashboard Spec; §21.6 Super Admin Console (Global Reporting)
- **Module(s)/Screen(s):** booking, supplier, ai, ads, Super Admin Console (21.6)
- **Story points:** 8 — Cross-module aggregation at platform scope (not per-tenant) — the Super Admin equivalent of HRD-09, broader in module reach.
- **Dependencies:** HRD-09, AI-11, FND-02
- **Testing tier(s):** unit, module (@ApplicationModuleTest), component test
- **Labels:** backend, frontend, phase1

**Acceptance Criteria**
- Given Super Admin opens Global Reporting, when the page loads, then all-Consultant GMV, per-supplier performance, an AI governance summary, and ad spend across Consultants are all shown, scoped to the SUPER_ADMIN 'view all' path.

**Sub-tasks**
- [NEW] Backend: `GET /api/v1/dashboard/super-admin` composite read endpoint, SUPER_ADMIN-only
- [NEW] Frontend: `useSuperAdminDashboard` hook (React Query for server data per RULES.md §7.1)
- [NEW] Frontend: `SuperAdminDashboard.tsx` component — all 5 PRD Part 21 states (default/loading/success/empty/error)
- [NEW] Frontend: component test (Testing Library, co-located, asserts on role/label per RULES.md §7.3)

#### HRD-12: Tune inventory sync batch cadence per supplier

**As a** platform reliability owner, **I want** have each supplier's static-content sync run on its documented cadence (nightly for Hotelbeds, weekly for Widgety, etc.), **so that** PRD §10.5 and §10.2.x's per-supplier sync-frequency notes are implemented as scheduled jobs, not one generic cadence.

- **PRD reference(s):** §10.5 Inventory Sync; §10.2.1/10.2.6/10.2.7 (per-supplier sync frequency)
- **Module(s)/Screen(s):** supplier
- **Story points:** 5 — Scheduled-job configuration per supplier, differentiated cadence — mechanically repetitive but must not be collapsed into one job.
- **Dependencies:** FND-11
- **Testing tier(s):** unit, integration (Testcontainers)
- **Labels:** backend, supplier, phase1

**Acceptance Criteria**
- Given Hotelbeds' nightly Content API batch job runs, when it completes, then static content (images, descriptions, amenities) refreshes without affecting real-time search/pricing.
- Given Widgety's weekly ship-image/deck-plan sync job runs, when it completes, then it does not run more frequently than weekly, matching the lower change-frequency rationale in §10.2.6.

**Sub-tasks**
- [NEW] Backend: per-supplier scheduled sync job (Hotelbeds nightly, Widgety/HBActivities per their documented cadence)
- [NEW] Backend: unit test — cadence configuration per supplier
- [NEW] Backend: integrationTest — job execution against LocalStack-backed storage

#### HRD-13: Alert Super Admin on inventory sync staleness beyond threshold

**As a** Super Admin, **I want** be alerted if a supplier's synced content becomes stale beyond a defined threshold, **so that** PRD §10.5's sync-failure alerting requirement is implemented for the live-API suppliers (complementing DMC-11's manual-DMC equivalent).

- **PRD reference(s):** §10.5 Inventory Sync
- **Module(s)/Screen(s):** supplier
- **Story points:** 3 — Threshold check + alert dispatch over HRD-12's scheduled jobs.
- **Dependencies:** HRD-12
- **Testing tier(s):** unit, module (@ApplicationModuleTest)
- **Labels:** backend, supplier, phase1

**Acceptance Criteria**
- Given a supplier's static content sync job fails or its last-successful-run exceeds the staleness threshold, when the check runs, then Super Admin receives an alert naming the affected supplier.

**Sub-tasks**
- [NEW] Backend: staleness-threshold check per supplier sync job
- [NEW] Backend: alert dispatch to Super Admin on breach
- [NEW] Backend: unit test — threshold boundary

---

## Frontend Shell

*10 stories, 55 story points.*

#### FES-01: Register all PRD Part 21 screens as code-split routes

**As a** frontend engineer, **I want** have every screen registered in `App.tsx` as its own route, code-split with `React.lazy`, **so that** the app doesn't ship one monolithic bundle as the ten distinct Part 21 screens land, several of which (Super Admin Console) serve a completely different persona from the Consultant/User screens, per frontend-best-practices §2.

- **PRD reference(s):** §21 Screen-by-Screen UI Specification (all subsections)
- **Module(s)/Screen(s):** Frontend shell (all screens)
- **Story points:** 5 — Routing skeleton is mechanical; the discipline is code-splitting every route from the start rather than retrofitting later.
- **Dependencies:** None
- **Testing tier(s):** component test
- **Labels:** frontend, foundation, phase1

**Acceptance Criteria**
- Given a route for a screen not yet visited is navigated to, when the bundle loads, then only that screen's chunk is fetched, wrapped in `Suspense` with a loading fallback.

**Sub-tasks**
- [NEW] Frontend: route registration for all 10 PRD Part 21 screens in `App.tsx`
- [NEW] Frontend: `React.lazy` + `Suspense` per route
- [NEW] Frontend: component test — lazy route resolves and renders

#### FES-02: Establish the app-wide provider stack slot for theme/branding context

**As a** frontend engineer, **I want** have a defined slot between `QueryClientProvider` and `BrowserRouter` for app-wide providers, **so that** theme/branding (FES-06) and auth (FES-07) context land in the same established position rather than each PR guessing where to put its provider.

- **PRD reference(s):** §13.2 Branding Configuration
- **Module(s)/Screen(s):** Frontend shell (provider stack)
- **Story points:** 3 — Small, mechanical, but establishes a convention every subsequent provider-adding story depends on.
- **Dependencies:** None
- **Testing tier(s):** unit
- **Labels:** frontend, foundation, phase1

**Acceptance Criteria**
- Given a new app-wide provider is added, when it is wired into `main.tsx`, then it sits between `QueryClientProvider` and `BrowserRouter` per the documented convention, unless it specifically needs router context.

**Sub-tasks**
- [EXTEND] Frontend: `main.tsx` provider stack — documented slot + placeholder composition helper
- [NEW] Frontend: unit test — provider order asserted

#### FES-03: Introduce a Zustand store for the in-progress itinerary-builder draft

**As a** Consultant/User, **I want** have my itinerary-builder draft persist across the multi-step wizard without being lost or duplicated by React Query, **so that** RULES.md §7.1's state-management boundary is established deliberately — cross-cutting client state that outlives one component tree uses Zustand, never a copy of React Query data.

- **PRD reference(s):** §7.1 State management boundaries (RULES.md, reconciliation item)
- **Module(s)/Screen(s):** Itinerary Builder (21.2)
- **Story points:** 5 — First real Zustand usage in the codebase (currently a zero-usage dependency) — establishes the pattern the itinerary-builder and later screens will follow.
- **Dependencies:** None
- **Testing tier(s):** unit, component test
- **Labels:** frontend, foundation, phase1

**Acceptance Criteria**
- Given a Consultant swaps a line item in one step of the builder, when they navigate to another step, then the draft state persists via the Zustand store, not local `useState` scoped to the now-unmounted step component.
- Given server data (search results) is fetched, when it is inspected, then it is never copied into the Zustand store — only genuinely cross-cutting draft state lives there.

**Sub-tasks**
- [NEW] Frontend: `itineraryDraftStore` (Zustand)
- [NEW] Frontend: unit test — draft persists across simulated step navigation
- [NEW] Frontend: component test — store never receives a React Query result copy

#### FES-04: Build shared UI primitives — Button, Card, TextField, Select

**As a** frontend engineer, **I want** have generic, accessible primitives in `shared/components` instead of each feature reinventing form controls, **so that** every consumer gets correct `label`/`htmlFor`/`aria-invalid`/`aria-describedby` wiring for free, per frontend-best-practices §5.

- **PRD reference(s):** §21 (cross-screen UI consistency)
- **Module(s)/Screen(s):** shared (frontend)
- **Story points:** 8 — First entries in an empty `shared/components` — four primitives, each needing correct accessibility wiring per RULES.md §7.3, is genuinely the size of a small design-system slice.
- **Dependencies:** FND-19
- **Testing tier(s):** component test
- **Labels:** frontend, foundation, phase1

**Acceptance Criteria**
- Given a `TextField` primitive is rendered with a validation error, when it is inspected, then `aria-invalid` and `aria-describedby` are wired to the error message automatically, with no per-consumer a11y code required.

**Sub-tasks**
- [NEW] Frontend: `Button` component
- [NEW] Frontend: `Card` component
- [NEW] Frontend: `TextField` component (label/aria wiring built-in)
- [NEW] Frontend: `Select` component
- [NEW] Frontend: component test per primitive

#### FES-05: Build shared MapPanel/ResultsPanel layout primitives

**As a** frontend engineer, **I want** have a reusable split-panel layout pair for the map+results shape shared by Search Dashboard and Itinerary Builder, **so that** PRD §21.1/§21.2's shared split-panel structure isn't duplicated per screen, per frontend-best-practices §5.

- **PRD reference(s):** §21.1 Search Dashboard; §21.2 Itinerary Builder
- **Module(s)/Screen(s):** shared (frontend)
- **Story points:** 5 — Two components extracted from FND-13's real implementation, generalized for reuse by FND-16.
- **Dependencies:** FND-13
- **Testing tier(s):** component test
- **Labels:** frontend, foundation, phase1

**Acceptance Criteria**
- Given `MapPanel`/`ResultsPanel` are composed on the Search Dashboard, when they render, then pins/results follow the documented split-panel layout (map left/top on desktop/mobile per §21.1).
- Given the same primitives are composed on the Itinerary Builder, when they render, then the layout matches without re-implementing the split-panel CSS/structure.

**Sub-tasks**
- [NEW] Frontend: `MapPanel` shared component (extracted/generalized from FND-13)
- [NEW] Frontend: `ResultsPanel` shared component
- [NEW] Frontend: component test per primitive

#### FES-06: Implement runtime-configurable white-label theme tokens

**As a** Consultant, **I want** see my branding (logo, colors) applied at runtime without a build-time deploy, **so that** PRD §13.2's per-tenant, runtime-applied branding is supported via CSS custom properties, per frontend-best-practices §5.

- **PRD reference(s):** §13.2 Branding Configuration
- **Module(s)/Screen(s):** shared (frontend)
- **Story points:** 5 — Design-system-level runtime theming mechanism; must be chosen before styled components proliferate, per frontend-best-practices §5's explicit warning.
- **Dependencies:** FND-06, FES-02
- **Testing tier(s):** unit, component test
- **Labels:** frontend, foundation, whitelabel, phase1

**Acceptance Criteria**
- Given a Consultant's branding profile (FND-06) specifies primary/secondary colors, when their storefront loads, then CSS custom properties (`--adren-primary`, etc.) are set at runtime from that profile — no build-time-baked Tailwind config values.

**Sub-tasks**
- [NEW] Frontend: CSS custom-property theme-token contract
- [NEW] Frontend: runtime theme provider reading FND-06's branding profile
- [NEW] Frontend: component test — token values reflect the loaded profile

#### FES-07: Add an auth/session context with per-role route guards

**As a** Consultant/User/Super Admin, **I want** only reach routes appropriate to my role, **so that** FND-01/FND-02's backend authorization is mirrored on the frontend so a User never even sees a Super-Admin-only route rendered before a 403.

- **PRD reference(s):** §6 Roles & Permissions Matrix
- **Module(s)/Screen(s):** Frontend shell (all screens)
- **Story points:** 8 — Session context + guard-per-route wiring across all 10 screens; correctness depends on staying in sync with FND-01/02's backend role matrix.
- **Dependencies:** FND-01, FND-02, FES-01, FES-02
- **Testing tier(s):** unit, component test, e2e
- **Labels:** frontend, foundation, security, phase1

**Acceptance Criteria**
- Given a USER-role session attempts to navigate to the Super Admin Console route, when the route guard evaluates, then navigation is redirected before the Super Admin Console component ever mounts.

**Sub-tasks**
- [NEW] Frontend: auth/session context (principal: userId/role/consultantId, mirroring FND-01)
- [NEW] Frontend: per-route guard component wrapping each protected `<Route>`
- [NEW] Frontend: component test — guard redirect per role
- [NEW] Frontend: e2e — unauthorized navigation attempt redirected

#### FES-08: Adopt react-hook-form + zod as the form/validation standard

**As a** frontend engineer, **I want** have one consistent form/validation approach before the Traveler Detail form, Package Builder form, and onboarding wizard are built, **so that** frontend-best-practices §4's recommendation is adopted deliberately, avoiding each form reinventing its own `useState`-per-field handling.

- **PRD reference(s):** §21.3 Package Builder; §21.4 Booking & Payment Flow; §21.6 Super Admin Console (onboarding wizard)
- **Module(s)/Screen(s):** Frontend shell (tooling)
- **Story points:** 5 — Library adoption + one reference form migration to prove the pattern, ahead of BOK-11/BOK-13/ADS-03/FES-09 all needing it.
- **Dependencies:** FES-04
- **Testing tier(s):** component test
- **Labels:** frontend, foundation, phase1

**Acceptance Criteria**
- Given a new multi-field form is built after this story lands, when it is inspected, then it uses `react-hook-form` + a `zod` schema via `@hookform/resolvers`, not a bespoke per-field `useState` pattern.

**Sub-tasks**
- [NEW] Frontend: `react-hook-form` + `zod` + `@hookform/resolvers` dependency adoption
- [NEW] Frontend: one reference form migrated to prove the pattern (e.g. the Search Dashboard's date/pax fields)
- [NEW] Frontend: component test — validation error surfaces via FES-04's `TextField` aria wiring

#### FES-09: Build a schema-driven, market-dependent onboarding wizard field engine

**As a** frontend engineer, **I want** have the Consultant/Super Admin onboarding wizard resolve its required fields from data, not a hardcoded per-market conditional tree, **so that** PRD §24.7's data-driven KYC principle is mirrored on the frontend, matching FND-04's backend rule table so a market-rule change never requires a frontend deploy that can drift from the backend.

- **PRD reference(s):** §24.7 NFR Regional Compliance; §13.1 Consultant Onboarding; §21.6 Super Admin Console
- **Module(s)/Screen(s):** Frontend shell (tooling)
- **Story points:** 8 — Schema-driven form-field resolution engine consumed by both FND-04's Consultant wizard and any future market-dependent form — genuine architectural piece, not a simple form.
- **Dependencies:** FND-04, FES-08
- **Testing tier(s):** unit, component test
- **Labels:** frontend, foundation, whitelabel, phase1

**Acceptance Criteria**
- Given the market→required-fields rule table (FND-04) changes on the backend, when the onboarding wizard is rendered, then its required-field set updates without a frontend code change, fetched from the backend's rule table rather than a duplicated hardcoded map.

**Sub-tasks**
- [NEW] Frontend: schema-driven field-resolution engine consuming FND-04's market rule table
- [NEW] Frontend: unit test — field set changes when the backend rule table changes
- [NEW] Frontend: component test — wizard renders correctly for two different markets

#### FES-10: Add a global toast/notification queue for async operation feedback

**As a** Consultant/User, **I want** see a toast confirmation or error for async operations (save, publish, payment) across any screen, **so that** async feedback is consistent platform-wide rather than each feature inventing its own transient-message pattern.

- **PRD reference(s):** §21 (cross-screen UX consistency)
- **Module(s)/Screen(s):** shared (frontend)
- **Story points:** 3 — Small, well-scoped Zustand store (cross-cutting client state per RULES.md §7.1) plus a toast-rendering component.
- **Dependencies:** FES-03
- **Testing tier(s):** component test
- **Labels:** frontend, foundation, phase1

**Acceptance Criteria**
- Given an async mutation succeeds or fails on any screen, when the result resolves, then a toast is queued via the shared Zustand-backed toast store and displayed with an appropriate ARIA live region per RULES.md §7.3.

**Sub-tasks**
- [NEW] Frontend: `toastQueueStore` (Zustand)
- [NEW] Frontend: `ToastContainer` component with ARIA live region
- [NEW] Frontend: component test — queued toast renders and auto-dismisses

---

## DevOps/Infra

*9 stories, 30 story points.*

#### OPS-01: Extend docker-compose with LocalStack S3/SQS/SNS/Secrets Manager/KMS services

**As a** backend engineer, **I want** have every AWS-shaped service the MVP needs available locally via LocalStack, not just the current Postgres+base-LocalStack baseline, **so that** FND-11/FND-12/BOK-15's Secrets Manager, KMS, and S3 dependencies all have a local dev target before any of those stories can be verified.

- **PRD reference(s):** §5 System Architecture Overview
- **Module(s)/Screen(s):** Infra (docker-compose)
- **Story points:** 5 — Extends the existing reference `docker-compose.yml` — mechanical service addition, but a blocking prerequisite for several FND/BOK/FIN stories.
- **Dependencies:** None
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, foundation, phase1

**Acceptance Criteria**
- Given `docker compose up -d` is run, when the stack starts, then Postgres, LocalStack S3, SQS, SNS, Secrets Manager, and KMS are all available and reachable from `bootRun`.

**Sub-tasks**
- [NEW] Infra: `docker-compose.yml` LocalStack service list extended to S3/SQS/SNS/Secrets Manager/KMS
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### OPS-02: Provision Adren-owned supplier credentials in LocalStack Secrets Manager

**As a** backend engineer, **I want** have a provisioning script seed LocalStack Secrets Manager with placeholder Hotelbeds/STUBA/TBO/etc. credential entries, **so that** FND-11's Secrets-Manager-by-ARN pattern has real local entries to reference during development and integrationTest runs.

- **PRD reference(s):** §5.3 Secrets handling (RULES.md)
- **Module(s)/Screen(s):** Infra (LocalStack)
- **Story points:** 5 — Scripted seed data — mechanical, but exercises the exact mechanism FND-11 depends on.
- **Dependencies:** OPS-01, FND-11
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, foundation, security, phase1

**Acceptance Criteria**
- Given the provisioning script is run against a fresh LocalStack instance, when it completes, then one Secrets Manager entry per supplier exists, matching the ARNs FND-11's credential entities expect.

**Sub-tasks**
- [NEW] Infra: LocalStack Secrets Manager seed script (one entry per Adren-owned supplier)
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### OPS-03: Provision LocalStack S3 buckets for vouchers and the document vault

**As a** backend engineer, **I want** have S3 buckets available locally for voucher PDFs and Traveler Profile document-vault files, **so that** BOK-15's voucher generation and BOK-14's document vault have a storage target before those stories can be verified end-to-end.

- **PRD reference(s):** §20.11 Voucher (pdf_reference); §20.10 Traveler Profile (document_vault[])
- **Module(s)/Screen(s):** Infra (LocalStack)
- **Story points:** 3 — Small, scripted bucket provisioning.
- **Dependencies:** OPS-01
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, foundation, phase1

**Acceptance Criteria**
- Given the provisioning script is run, when it completes, then a `vouchers` bucket and a `traveler-documents` bucket exist with the encryption-at-rest configuration BOK-15/BOK-14 expect.

**Sub-tasks**
- [NEW] Infra: LocalStack S3 bucket provisioning script (`vouchers`, `traveler-documents`)
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### OPS-04: Establish Flyway migration discipline across all modules

**As a** backend engineer, **I want** have every module's first real entity land with a correctly-numbered, additive-only Flyway migration, **so that** RULES.md §4.2's migration discipline (never edit a merged migration, one file per change, module-owned tables) is enforced as modules move from stub to real.

- **PRD reference(s):** §4.2 Migration discipline (RULES.md)
- **Module(s)/Screen(s):** Infra (Flyway)
- **Story points:** 3 — Convention documentation plus a CI check that migration numbering is strictly incrementing and never edited post-merge.
- **Dependencies:** OPS-01
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, foundation, phase1

**Acceptance Criteria**
- Given a new module's first entity is added (e.g. `ai`'s `AiSuggestionAuditLog`), when its migration is written, then it is `V<n>__ai_init.sql`, strictly incrementing from the current head, owning only `ai`-prefixed tables.

**Sub-tasks**
- [NEW] Infra: CI check: Flyway migration numbering strictly incrementing, no edits to merged migrations
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### OPS-05: Wire ./gradlew check and npm test/coverage/lint into CI on every PR

**As a** engineering team, **I want** have every PR automatically run the full backend (`./gradlew check`) and frontend (`npm run test:coverage`, `npm run lint`) gates, **so that** module-boundary violations (`ModularityTests`), coverage regressions, and lint failures are caught before merge, not discovered later.

- **PRD reference(s):** §8 PR / Code Review Checklist (RULES.md)
- **Module(s)/Screen(s):** Infra (CI)
- **Story points:** 5 — Standard CI pipeline wiring; the specific gate list (check vs. test, coverage thresholds, lint) matters more than the CI platform mechanics.
- **Dependencies:** FND-19, OPS-04
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, foundation, phase1

**Acceptance Criteria**
- Given a PR is opened, when CI runs, then `./gradlew check`, `npm run test:coverage`, and `npm run lint` all execute and the PR is blocked from merge if any fails.

**Sub-tasks**
- [NEW] Infra: CI pipeline configuration running `./gradlew check` + `npm run test:coverage` + `npm run lint` on every PR
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### OPS-06: Bump the Gradle build to the Java 25 toolchain once available

**As a** backend engineer, **I want** build against the Java 25 toolchain the project targets, once a Java 25 JDK is available in the build environment, **so that** the scaffold, currently authored against a Java 21 JDK per backend/README's own note, stops carrying a known version gap.

- **PRD reference(s):** backend/README.md toolchain note
- **Module(s)/Screen(s):** Infra (Gradle)
- **Story points:** 2 — Toolchain version bump — small, but explicitly flagged as a known gap in the existing reference implementation.
- **Dependencies:** None
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, foundation, phase1

**Acceptance Criteria**
- Given a Java 25 JDK is available in the build environment, when `./gradlew build` is run, then it builds against the Java 25 toolchain with no compatibility warnings.

**Sub-tasks**
- [NEW] Infra: `build.gradle.kts` toolchain version bump to Java 25 (once available)
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### OPS-07: Configure the Groq API key as a proper secret boundary

**As a** backend engineer, **I want** have the Groq API key sourced from LocalStack Secrets Manager in test/dev and real Secrets Manager in any non-local profile, **so that** RULES.md §5.3's rule — no real integration credential as a plaintext config value or environment variable outside local Docker Compose — applies to `adren.ai.groq` exactly as it does to supplier credentials.

- **PRD reference(s):** §5.3 Secrets handling (RULES.md)
- **Module(s)/Screen(s):** Infra (secrets)
- **Story points:** 2 — Same pattern as FND-11/OPS-02, applied to one additional secret.
- **Dependencies:** OPS-02, AI-01
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, foundation, security, phase1

**Acceptance Criteria**
- Given the `ai` module resolves its Groq API key in a non-local profile, when resolution runs, then it reads from Secrets Manager by ARN, never a plaintext env var default.

**Sub-tasks**
- [NEW] Infra: Groq API key sourced from Secrets Manager by ARN outside local profile
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### OPS-08: Wire module-documentation generation into the release checklist

**As a** engineering team, **I want** have `ModularityTests.writeModuleDocumentation()`'s PlantUML output copied into `doc/architecture/` as a standard release step, **so that** the module map in documentation can't silently drift from the actual code structure, per the backend-spring-modulith skill.

- **PRD reference(s):** backend-spring-modulith skill (module doc generation)
- **Module(s)/Screen(s):** Infra (release process)
- **Story points:** 2 — Process/checklist wiring, not new code — codifies an already-documented but not-yet-enforced step.
- **Dependencies:** None
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, foundation, phase1

**Acceptance Criteria**
- Given a release checklist is run, when the module-documentation step executes, then `build/spring-modulith-docs/*.puml` is regenerated and copied into `doc/architecture/`, and the diff is reviewed as part of the release PR.

**Sub-tasks**
- [NEW] Infra: Release checklist step: regenerate and copy PlantUML module docs into `doc/architecture/`
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

#### OPS-09: Define per-environment application.yml profiles

**As a** backend engineer, **I want** have distinct `local`/`test`/`staging` Spring profiles with the correct secret-sourcing and service-endpoint boundaries per profile, **so that** the local-only plaintext credential exception in RULES.md §5.3 stays scoped to `local` and never leaks into a shared profile.

- **PRD reference(s):** §5.3 Secrets handling (RULES.md)
- **Module(s)/Screen(s):** Infra (config)
- **Story points:** 3 — Config-file structuring; the guarantee is the absence of a plaintext fallback outside `local`, which needs its own check.
- **Dependencies:** OPS-02, OPS-07
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** devops, foundation, phase1

**Acceptance Criteria**
- Given the application starts under the `local` profile, when config resolves, then plaintext local Docker Compose credentials are used.
- Given the application starts under any non-local profile, when config resolves, then every credential resolves via Secrets Manager by ARN — no plaintext fallback exists in that profile's config.

**Sub-tasks**
- [NEW] Infra: `application-local.yml` / `application-test.yml` / `application-staging.yml` profile split
- [NEW] Infra: README / RULES.md documentation update
- [NEW] Infra: verification step (local run and/or CI check)

---

## Test Infrastructure

*9 stories, 38 story points.*

#### TST-01: Extend the Testcontainers base infrastructure for new modules

**As a** backend engineer, **I want** have `TestInfrastructure` provide Postgres + LocalStack containers usable by every module's `integrationTest`, not just `booking`'s, **so that** as `ai`/`payments`/`whitelabel`/`ads`/`compliance` move from stub to real modules, each gets the same Testcontainers foundation `BookingEndToEndIT` already established.

- **PRD reference(s):** testing-strategy skill (End-to-end tier)
- **Module(s)/Screen(s):** Infra (test)
- **Story points:** 5 — Extends the existing reference `TestInfrastructure.java` to cover the LocalStack services OPS-01 adds.
- **Dependencies:** OPS-01
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, testing, foundation, phase1

**Acceptance Criteria**
- Given a new module's `integrationTest` extends the shared base, when it runs, then it gets a real Postgres + LocalStack (S3/Secrets Manager/KMS as needed) without redefining container setup.

**Sub-tasks**
- [NEW] Test infra: `TestInfrastructure` extended with S3/Secrets Manager/KMS LocalStack containers
- [NEW] Test infra: sample test exercising the new harness
- [NEW] Test infra: CI wiring / gradle-or-npm script update

#### TST-02: Extend ModularityTests coverage as stub modules become real

**As a** backend engineer, **I want** have `ModularityTests.moduleBoundariesAreRespected()` continue passing as `ai`/`payments`/`whitelabel`/`ads`/`compliance` gain real code, **so that** the module-boundary enforcement mechanism doesn't silently stop covering a module just because it moved past package-info-only stub status.

- **PRD reference(s):** backend-spring-modulith skill (Verifying boundaries)
- **Module(s)/Screen(s):** Infra (test)
- **Story points:** 3 — `ApplicationModules.of(...)` already scans the full application context — this story is verifying/documenting that no manual step is needed, plus a regression test.
- **Dependencies:** TST-01
- **Testing tier(s):** unit
- **Labels:** backend, testing, foundation, phase1

**Acceptance Criteria**
- Given any module gains its first real `internal/` class, when `./gradlew check` runs, then `ApplicationModules.verify()` includes that module in its boundary check with no manual test-list update required.

**Sub-tasks**
- [EXTEND] Backend: regression test confirming `ApplicationModules.verify()` auto-includes newly-real modules
- [NEW] Backend: CI assertion — a deliberately-introduced cross-module `.internal` import fails the build

#### TST-03: Scaffold Playwright e2e specs for Flow B and Flow C journeys

**As a** QA/frontend engineer, **I want** have e2e coverage for Package creation (Flow B) and Direct Booking (Flow C), not just the existing Flow A search spec, **so that** PRD §9.1's three flows are all covered per testing-strategy's 'reserve e2e for journeys' guidance — currently only `search-flow.spec.ts` exists.

- **PRD reference(s):** §9.1 Flow B; §9.1 Flow C; testing-strategy skill (Frontend e2e tier)
- **Module(s)/Screen(s):** Infra (test)
- **Story points:** 5 — Two new Playwright specs following `search-flow.spec.ts`'s established pattern.
- **Dependencies:** BOK-11, BOK-13
- **Testing tier(s):** e2e
- **Labels:** frontend, testing, foundation, phase1

**Acceptance Criteria**
- Given `npm run test:e2e` is run, when the suite executes, then it includes a Flow B spec (itinerary→quotation→package→publish) and a Flow C spec (search/select package→traveler details→payment→confirmation) alongside the existing Flow A spec.

**Sub-tasks**
- [NEW] Test infra: `e2e/package-creation-flow.spec.ts` and `e2e/direct-booking-flow.spec.ts`
- [NEW] Test infra: sample test exercising the new harness
- [NEW] Test infra: CI wiring / gradle-or-npm script update

#### TST-04: Wire MSW into the frontend test setup for realistic API mocking

**As a** frontend engineer, **I want** have `src/test/setup.ts` intercept `apiClient` calls via MSW instead of mocking hook functions directly, **so that** tests exercise the real request/response shape once features start making real `apiClient` calls, per testing-strategy's explicit note that `msw` is installed but unwired.

- **PRD reference(s):** testing-strategy skill (Frontend tiers, MSW note); §7.1 (RULES.md, React Query reconciliation)
- **Module(s)/Screen(s):** Infra (test)
- **Story points:** 5 — Wires an already-installed dependency; the risk is in migrating existing mocked-hook tests to the new pattern without regressing coverage.
- **Dependencies:** FES-08
- **Testing tier(s):** component test
- **Labels:** frontend, testing, foundation, phase1

**Acceptance Criteria**
- Given a component test needs a server response, when it runs, then MSW intercepts the `apiClient` HTTP call and returns the configured response — no `apiClient` function itself is mocked.

**Sub-tasks**
- [NEW] Test infra: MSW server registered in `src/test/setup.ts`, existing search-dashboard tests migrated to MSW handlers
- [NEW] Test infra: sample test exercising the new harness
- [NEW] Test infra: CI wiring / gradle-or-npm script update

#### TST-05: Raise Vitest coverage thresholds as real feature coverage grows

**As a** frontend engineer, **I want** have the coverage gate ratchet upward from the current scaffold-stage floor (70% lines/functions/statements, 60% branches), **so that** the coverage gate stays a meaningful floor rather than a permanently-loose scaffold artifact, per testing-strategy's explicit 'raise as real feature coverage grows' guidance.

- **PRD reference(s):** testing-strategy skill (Frontend tiers, coverage thresholds)
- **Module(s)/Screen(s):** Infra (test)
- **Story points:** 2 — Small, periodic config change — the story is establishing the review checkpoint/process, not a one-time number bump.
- **Dependencies:** TST-04
- **Testing tier(s):** component test
- **Labels:** frontend, testing, foundation, phase1

**Acceptance Criteria**
- Given real feature coverage in `src/features/` exceeds the current threshold by a defined margin, when the threshold-review checkpoint is reached, then `vite.config.ts`'s coverage thresholds are raised to match, not left at the scaffold-stage floor indefinitely.

**Sub-tasks**
- [EXTEND] Frontend: `vite.config.ts` coverage thresholds raised
- [NEW] Frontend: coverage-threshold review noted in the release checklist

#### TST-06: Separate supplier sandbox-vs-production fixtures in CI

**As a** QA/backend engineer, **I want** have supplier integration tests run against both sandbox and production-shaped fixtures, flagged separately, **so that** PRD §23.2 Edge Case #4 and T19/T20 are protected against regressions — sandbox and production supplier environments are documented to behave differently (Hotelbeds/TBO specifically).

- **PRD reference(s):** §23.2 Edge Case #4; §25 T19/T20; testing-strategy skill (Supplier integration tests)
- **Module(s)/Screen(s):** supplier (test infra)
- **Story points:** 5 — Test-fixture separation + CI job structure; the actual production fixtures are stubbed/synthetic in MVP (real production access is Phase 2's SUP epic).
- **Dependencies:** TST-01
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, testing, foundation, supplier, phase1

**Acceptance Criteria**
- Given a supplier client's `integrationTest` suite runs in CI, when results are reported, then sandbox-fixture and production-fixture-shaped test runs are reported as two distinct, separately-flagged CI jobs, not assumed equivalent.

**Sub-tasks**
- [NEW] Backend: sandbox-shaped and production-shaped fixture sets per supplier client
- [NEW] Backend: CI job split — sandbox-fixture run vs. production-fixture-shaped run, separately flagged

#### TST-07: Build a representative seed/fixture dataset across all six markets

**As a** QA/backend engineer, **I want** have a representative set of Consultants, Itineraries, and Bookings across all six markets (India, Australia, UK, USA, Dubai/UAE, Denmark) available for module/integration tests, **so that** market-dependent logic (KYC, GST/TCS, ATOL, currency) can be tested against realistic data shapes instead of every test author inventing their own fixtures.

- **PRD reference(s):** §13.1 Consultant Onboarding (per-market); §17 Regional Compliance
- **Module(s)/Screen(s):** Infra (test)
- **Story points:** 5 — Fixture-data authoring across 6 markets × multiple entity types — breadth work, not complexity.
- **Dependencies:** TST-01, FND-04
- **Testing tier(s):** integration (Testcontainers)
- **Labels:** backend, testing, foundation, phase1

**Acceptance Criteria**
- Given a module/integrationTest needs a UK Consultant with a dynamic flight+hotel package, when it requests the seed fixture, then one is available pre-built, matching the ATOL-relevant shape BOK-11 tests need.

**Sub-tasks**
- [NEW] Backend: seed-data builder per market (Consultant + representative Itinerary/Booking)
- [NEW] Backend: fixture usable from both `test` and `integrationTest` source sets

#### TST-08: Build a contract-test harness for the normalized SupplierSearchResult shape

**As a** backend engineer, **I want** have shared assertion helpers that verify every supplier client's output conforms to the normalized result shape, **so that** PRD §20.2–20.6's normalized-field discipline (backend-best-practices §6) is enforced consistently as more supplier clients are added in Phase 2's SUP epic.

- **PRD reference(s):** §20.2-20.6 Line Item data dictionary; backend-best-practices skill §6 (normalization discipline)
- **Module(s)/Screen(s):** supplier (test infra)
- **Story points:** 5 — Shared test-helper library; low runtime complexity, high leverage as supplier count grows.
- **Dependencies:** TST-01
- **Testing tier(s):** unit
- **Labels:** backend, testing, foundation, supplier, phase1

**Acceptance Criteria**
- Given a new supplier client's mapping is unit-tested, when the shared contract assertion is applied, then it fails the build if a supplier-specific field name (e.g. TBO's `TraceId`) leaks into the public normalized result shape.

**Sub-tasks**
- [NEW] Backend: `SupplierSearchResultContractAssertions` shared test helper
- [NEW] Backend: applied to `HotelbedsClient`'s existing test as the reference usage

#### TST-09: Build an AI governance audit-log test harness asserting the 100%-logged invariant

**As a** backend engineer, **I want** have a reusable test harness that asserts every AI call in a test run produced exactly one audit-log entry, **so that** PRD §11.2/§24.3's 100%-logged, no-sampling requirement is enforced by the AI module's own test suite, not just documented.

- **PRD reference(s):** §11.2 principle 5; §24.3 NFR AI Governance
- **Module(s)/Screen(s):** ai (test infra)
- **Story points:** 3 — Focused assertion helper on top of AI-07's audit table — small but load-bearing for every future AI-module test.
- **Dependencies:** AI-07, TST-01
- **Testing tier(s):** module (@ApplicationModuleTest)
- **Labels:** backend, testing, foundation, ai, phase1

**Acceptance Criteria**
- Given N AI calls are made within a module/integrationTest run, when the harness's assertion is applied, then exactly N audit-log entries exist — any mismatch fails the test, not just a manual review.

**Sub-tasks**
- [NEW] Backend: `AiAuditCompletenessAssertions` shared test helper
- [NEW] Backend: applied to AI-02/AI-03's existing tests as the reference usage

---
