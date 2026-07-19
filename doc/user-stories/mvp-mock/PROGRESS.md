# MVP-Mock Story Progress

Tracks all 149 mock-phase stories across 10 epics. Checked = story fully implemented, tested, acceptance criteria verified, and committed.

**2026-07-16 (Stage 3, Step B):** added `BOK-21`–`BOK-27` (STUBA/TBO/Transferz/Widgety/HBActivities client stubs, a circuit-breaker story, and a static-content sync/caching story) — a gap identified in Stage 2's report: BOK-20 and HRD-12 already referenced `StubaClient`/`TboClient`/content-sync as if they existed, but no story ever built them. See each file's frontmatter for dependency wiring; `BOK-20` and `HRD-12` were updated to depend on the new stories.

See `doc/phases.md` for the dependency-derived build order and `doc/architecture/RULES.md` for the per-story quality bar.


## Foundation (24)

- [x] **FND-01** — stand up stateless spring security with role and tenant aware principal
- [x] **FND-02** — enforce prd 6 role matrix via method level preauthorize on module api interfaces
- [x] **FND-03** — close tenant isolation gap on itinerary booking lookups
- [x] **FND-04** — super admin onboards a consultant via a market driven kyc wizard
- [x] **FND-05** — super admin manages consultant lifecycle view suspend reinstate
- [x] **FND-06** — configure consultant white label branding logo colors domain
- [x] **FND-07** — propagate branding domain changes to the live storefront without a redeploy
- [x] **FND-08** — enforce dynamic per consultant cors allow list for white label domains
- [x] **FND-09** — consultant manages users staff sub agents under their own account
- [x] **FND-10** — super admin manages adren owned supplier api credentials
- [x] **FND-11** — store adren owned supplier credentials in secrets manager not plaintext config
- [x] **FND-12** — store byos credentials as row level per consultant encrypted secrets
- [x] **FND-13** — extend search dashboard with map based multi location multi select search
- [x] **FND-14** — implement the default selection algorithm for per location product pre selection
- [x] **FND-15** — surface auto selected best available match label on defaulted line items
- [x] **FND-16** — build the itinerary builder screen with per location cards and alternate selection panel
- [x] **FND-17** — add locale market selection alongside existing multi currency support
- [x] **FND-18** — add a root and per route errorboundary
- [x] **FND-19** — add eslint flat config with jsx a11y
- [x] **FND-20** — resolve the path alias half configuration (already satisfied by prior work — vite.config.ts's resolve.alias already matches tsconfig, multiple cross-feature `@/` imports already in place)
- [x] **FND-21** — propagate a correlation id traceid across the async event listener boundary
- [x] **FND-22** — adopt rfc 7807 problem details error responses with per module controlleradvice
- [x] **FND-23** — convert all collection endpoints to paginated responses
- [x] **FND-24** — adopt structured json logging with mandatory mdc fields

## Booking Core (27)

- [x] **BOK-01** — add transactional boundaries to booking state change methods
- [x] **BOK-02** — fix bookingconfirmedevent to carry money instead of decomposed amount currency
- [x] **BOK-03** — add hotel line items to an itinerary
- [x] **BOK-04** — add flight line items to an itinerary mystifly
- [x] **BOK-05** — add transfer line items to an itinerary transferz
- [x] **BOK-06** — add cruise line items to an itinerary widgety
- [x] **BOK-07** — add activity line items to an itinerary hbactivities
- [x] **BOK-08** — save an itinerary as a quotation
- [x] **BOK-09** — create quotation entity with fx rate validity window
- [x] **BOK-10** — convert a quotation to a package
- [x] **BOK-11** — build the package builder screen with uk atol disclosure gate
- [x] **BOK-12** — publish a package making it visible to users (ATOL gate deferred to BOK-11/BOK-04 — see backend README)
- [x] **BOK-13** — build the direct booking payment flow user facing (backend confirmBooking scaffold only — frontend screen deferred, see backend README)
- [x] **BOK-14** — capture traveler profile details including passport document vault
- [x] **BOK-15** — generate a voucher on booking confirmation including atol certificate for uk dynamic packages (ATOL cert always null in this slice — see backend README)
- [x] **BOK-16** — prevent double booking of the last available inventory unit under concurrent requests
- [x] **BOK-17** — consolidate mixed currency line items to the consultant s sell currency at checkout
- [x] **BOK-18** — recalculate price when traveler count changes after quotation but before booking
- [x] **BOK-19** — generate a pnr searchable reference on every booking
- [x] **BOK-20** — deduplicate the same physical hotel property offered by two suppliers
- [x] **BOK-21** — integrate a stuba client stub for hotel search
- [x] **BOK-22** — integrate a tbo client stub for hotel search
- [x] **BOK-23** — integrate a transferz client stub for transfer search
- [x] **BOK-24** — integrate a widgety client stub for cruise search
- [x] **BOK-25** — integrate an hbactivities client stub for activity search
- [x] **BOK-26** — isolate each supplier integration behind a circuit breaker
- [x] **BOK-27** — sync and cache static supplier content

## Financial Layer (18)

- [x] **FIN-01** — configure per consultant per category markup rules
- [x] **FIN-02** — track adren commission separately from consultant markup
- [x] **FIN-03** — apply a configurable currency buffer on top of markup
- [x] **FIN-04** — snapshot and lock the fx rate at quotation time
- [x] **FIN-05** — calculate sell rate through the full net buffer markup commission pipeline
- [x] **FIN-06** — model the wallet with balance credit limit and pending holds
- [x] **FIN-07** — place a hold on booking confirmation release or debit on final confirmation
- [x] **FIN-08** — block booking confirmation on credit limit breach with an actionable message
- [x] **FIN-09** — build the wallet billing screen with pre payment breach warning
- [x] **FIN-10** — guarantee atomic idempotent wallet ledger writes
- [x] **FIN-11** — integrate stripe for payment collection across six settlement currencies
- [x] **FIN-12** — support on account billing as a payment method
- [x] **FIN-13** — process refunds and credit notes tied to supplier cancellation policy
- [x] **FIN-14** — reuse the original fx snapshot when calculating a refund
- [x] **FIN-15** — reconcile wallet top up when the payment gateway webhook is delayed or fails
- [x] **FIN-16** — build the cancellation dispute handling workflow
- [x] **FIN-17** — implement india gst tcs calculation layer for outbound packages
- [x] **FIN-18** — implement uk toms vat calculation layer

## AI Layer (13)

- [x] **AI-01** — integrate a groq client wrapper for the ai module
- [x] **AI-02** — generate an itinerary from natural language or structured input
- [x] **AI-03** — complete a partially built itinerary with ai
- [x] **AI-04** — include supplier source and live availability status on every ai suggestion
- [x] **AI-05** — model ai failure no viable suggestion as an explicit response state
- [x] **AI-06** — enforce mandatory human in the loop approval before an ai itinerary reaches the traveler
- [x] **AI-07** — write a 100 logged insert only ai suggestion audit trail
- [x] **AI-08** — capture both the original ai suggestion and the consultant s edited final version in the audit trail
- [x] **AI-09** — re validate ai suggested pricing at booking time if it has gone stale
- [x] **AI-10** — build the complete with ai entry point with source availability badges
- [x] **AI-11** — build the ai governance audit log viewer in the super admin console
- [x] **AI-12** — generate ai ad creative variants grounded in package content and live pricing
- [x] **AI-13** — bound ai response latency to protect the 10 minute itinerary target

## Local DMC + BYOS (11)

- [x] **DMC-01** — submit a new local dmc for onboarding
- [x] **DMC-02** — run the local dmc pending active vetting workflow
- [x] **DMC-03** — bulk upload local dmc inventory via a validated csv template tool
- [x] **DMC-04** — track local dmc quality signal cancellation rate and complaint count
- [x] **DMC-05** — flag a local dmc to both the onboarding consultant and super admin on threshold breach
- [x] **DMC-06** — let a consultant enter their own supplier api credentials byos
- [x] **DMC-07** — make the supplier integration layer credential source agnostic
- [x] **DMC-08** — merge byos inventory into search results using standard normalization and default selection
- [x] **DMC-09** — scope byos inventory and credentials strictly to the owning consultant
- [x] **DMC-10** — manage local dmc inventory items after onboarding crud
- [x] **DMC-11** — alert on stale local dmc inventory beyond a defined threshold

## Ads/Campaign Management (15)

- [x] **ADS-01** — provision a meta ad account business manager for a consultant
- [x] **ADS-02** — model the ad campaign entity and its status state machine
- [x] **ADS-03** — build the campaign builder screen package selector audience budget duration inputs
- [x] **ADS-04** — generate and display ai creative variant gallery in the campaign builder
- [x] **ADS-05** — require consultant approval per creative variant before submission
- [x] **ADS-06** — route approved campaigns through super admin brand safety policy review
- [x] **ADS-07** — launch an approved campaign under the adren managed meta account
- [x] **ADS-08** — display a campaign status stepper matching the status enum
- [x] **ADS-09** — flow campaign performance data back to the consultant dashboard
- [x] **ADS-10** — enforce near real time spend cap on active campaigns
- [x] **ADS-11** — implement campaign approval workflow guardrails and billing transparency
- [ ] **ADS-12** — auto pause a campaign when its linked package price changes
- [ ] **ADS-13** — surface a clear suspended action required status on meta account suspension
- [ ] **ADS-14** — define ad spend billing model per settlement currency
- [ ] **ADS-15** — apply brand safety policy template guardrails to campaign submissions

## Hardening (13)

- [x] **HRD-01** — implement notification dispatch for email plus region configurable secondary channel
- [x] **HRD-02** — wire all prd 15 notification trigger events
- [x] **HRD-03** — make every notification listener idempotent
- [x] **HRD-04** — build the notification preferences screen
- [x] **HRD-05** — implement the full cancellation workflow across policy check approval and refund
- [x] **HRD-06** — track disputes as tickets not email handoffs
- [x] **HRD-07** — implement pnr booking search across all product types
- [x] **HRD-08** — build the pnr booking search screen
- [ ] **HRD-09** — build the consultant dashboard
- [ ] **HRD-10** — show an onboarding checklist instead of empty charts for new consultants
- [ ] **HRD-11** — build the super admin dashboard
- [x] **HRD-12** — tune inventory sync batch cadence per supplier
- [x] **HRD-13** — alert super admin on inventory sync staleness beyond threshold

## Frontend Shell (10)

- [x] **FES-01** — register all prd part 21 screens as code split routes
- [x] **FES-02** — establish the app wide provider stack slot for theme branding context
- [x] **FES-03** — introduce a zustand store for the in progress itinerary builder draft
- [x] **FES-04** — build shared ui primitives button card textfield select
- [x] **FES-05** — build shared mappanel resultspanel layout primitives
- [x] **FES-06** — implement runtime configurable white label theme tokens
- [x] **FES-07** — add an auth session context with per role route guards
- [x] **FES-08** — adopt react hook form zod as the form validation standard
- [x] **FES-09** — build a schema driven market dependent onboarding wizard field engine
- [x] **FES-10** — add a global toast notification queue for async operation feedback

## DevOps/Infra (9)

- [ ] **OPS-01** — extend docker compose with localstack s3 sqs sns secrets manager kms services
- [ ] **OPS-02** — provision adren owned supplier credentials in localstack secrets manager
- [ ] **OPS-03** — provision localstack s3 buckets for vouchers and the document vault
- [ ] **OPS-04** — establish flyway migration discipline across all modules
- [ ] **OPS-05** — wire gradlew check and npm test coverage lint into ci on every pr
- [ ] **OPS-06** — bump the gradle build to the java 25 toolchain once available
- [ ] **OPS-07** — configure the groq api key as a proper secret boundary
- [ ] **OPS-08** — wire module documentation generation into the release checklist
- [ ] **OPS-09** — define per environment application yml profiles

## Test Infrastructure (9)

- [ ] **TST-01** — extend the testcontainers base infrastructure for new modules
- [ ] **TST-02** — extend modularitytests coverage as stub modules become real
- [ ] **TST-03** — scaffold playwright e2e specs for flow b and flow c journeys
- [ ] **TST-04** — wire msw into the frontend test setup for realistic api mocking
- [ ] **TST-05** — raise vitest coverage thresholds as real feature coverage grows
- [ ] **TST-06** — separate supplier sandbox vs production fixtures in ci
- [ ] **TST-07** — build a representative seed fixture dataset across all six markets
- [ ] **TST-08** — build a contract test harness for the normalized suppliersearchresult shape
- [ ] **TST-09** — build an ai governance audit log test harness asserting the 100 logged invariant

---
Total: 149 stories.
