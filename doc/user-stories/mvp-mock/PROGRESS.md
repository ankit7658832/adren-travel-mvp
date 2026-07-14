# MVP-Mock Story Progress

Tracks all 142 mock-phase stories across 10 epics. Checked = story fully implemented, tested, acceptance criteria verified, and committed.

See `doc/phases.md` for the dependency-derived build order and `doc/architecture/RULES.md` for the per-story quality bar.


## Foundation (24)

- [x] **FND-01** — stand up stateless spring security with role and tenant aware principal
- [ ] **FND-02** — enforce prd 6 role matrix via method level preauthorize on module api interfaces
- [ ] **FND-03** — close tenant isolation gap on itinerary booking lookups
- [ ] **FND-04** — super admin onboards a consultant via a market driven kyc wizard
- [ ] **FND-05** — super admin manages consultant lifecycle view suspend reinstate
- [ ] **FND-06** — configure consultant white label branding logo colors domain
- [ ] **FND-07** — propagate branding domain changes to the live storefront without a redeploy
- [ ] **FND-08** — enforce dynamic per consultant cors allow list for white label domains
- [ ] **FND-09** — consultant manages users staff sub agents under their own account
- [ ] **FND-10** — super admin manages adren owned supplier api credentials
- [ ] **FND-11** — store adren owned supplier credentials in secrets manager not plaintext config
- [ ] **FND-12** — store byos credentials as row level per consultant encrypted secrets
- [ ] **FND-13** — extend search dashboard with map based multi location multi select search
- [ ] **FND-14** — implement the default selection algorithm for per location product pre selection
- [ ] **FND-15** — surface auto selected best available match label on defaulted line items
- [ ] **FND-16** — build the itinerary builder screen with per location cards and alternate selection panel
- [ ] **FND-17** — add locale market selection alongside existing multi currency support
- [ ] **FND-18** — add a root and per route errorboundary
- [x] **FND-19** — add eslint flat config with jsx a11y
- [x] **FND-20** — resolve the path alias half configuration (already satisfied by prior work — vite.config.ts's resolve.alias already matches tsconfig, multiple cross-feature `@/` imports already in place)
- [ ] **FND-21** — propagate a correlation id traceid across the async event listener boundary
- [ ] **FND-22** — adopt rfc 7807 problem details error responses with per module controlleradvice
- [x] **FND-23** — convert all collection endpoints to paginated responses
- [ ] **FND-24** — adopt structured json logging with mandatory mdc fields

## Booking Core (20)

- [ ] **BOK-01** — add transactional boundaries to booking state change methods
- [ ] **BOK-02** — fix bookingconfirmedevent to carry money instead of decomposed amount currency
- [ ] **BOK-03** — add hotel line items to an itinerary
- [ ] **BOK-04** — add flight line items to an itinerary mystifly
- [ ] **BOK-05** — add transfer line items to an itinerary transferz
- [ ] **BOK-06** — add cruise line items to an itinerary widgety
- [ ] **BOK-07** — add activity line items to an itinerary hbactivities
- [ ] **BOK-08** — save an itinerary as a quotation
- [ ] **BOK-09** — create quotation entity with fx rate validity window
- [ ] **BOK-10** — convert a quotation to a package
- [ ] **BOK-11** — build the package builder screen with uk atol disclosure gate
- [ ] **BOK-12** — publish a package making it visible to users
- [ ] **BOK-13** — build the direct booking payment flow user facing
- [ ] **BOK-14** — capture traveler profile details including passport document vault
- [ ] **BOK-15** — generate a voucher on booking confirmation including atol certificate for uk dynamic packages
- [ ] **BOK-16** — prevent double booking of the last available inventory unit under concurrent requests
- [ ] **BOK-17** — consolidate mixed currency line items to the consultant s sell currency at checkout
- [ ] **BOK-18** — recalculate price when traveler count changes after quotation but before booking
- [ ] **BOK-19** — generate a pnr searchable reference on every booking
- [ ] **BOK-20** — deduplicate the same physical hotel property offered by two suppliers

## Financial Layer (18)

- [ ] **FIN-01** — configure per consultant per category markup rules
- [ ] **FIN-02** — track adren commission separately from consultant markup
- [ ] **FIN-03** — apply a configurable currency buffer on top of markup
- [ ] **FIN-04** — snapshot and lock the fx rate at quotation time
- [ ] **FIN-05** — calculate sell rate through the full net buffer markup commission pipeline
- [ ] **FIN-06** — model the wallet with balance credit limit and pending holds
- [ ] **FIN-07** — place a hold on booking confirmation release or debit on final confirmation
- [ ] **FIN-08** — block booking confirmation on credit limit breach with an actionable message
- [ ] **FIN-09** — build the wallet billing screen with pre payment breach warning
- [ ] **FIN-10** — guarantee atomic idempotent wallet ledger writes
- [ ] **FIN-11** — integrate stripe for payment collection across six settlement currencies
- [ ] **FIN-12** — support on account billing as a payment method
- [ ] **FIN-13** — process refunds and credit notes tied to supplier cancellation policy
- [ ] **FIN-14** — reuse the original fx snapshot when calculating a refund
- [ ] **FIN-15** — reconcile wallet top up when the payment gateway webhook is delayed or fails
- [ ] **FIN-16** — build the cancellation dispute handling workflow
- [ ] **FIN-17** — implement india gst tcs calculation layer for outbound packages
- [ ] **FIN-18** — implement uk toms vat calculation layer

## AI Layer (13)

- [ ] **AI-01** — integrate a groq client wrapper for the ai module
- [ ] **AI-02** — generate an itinerary from natural language or structured input
- [ ] **AI-03** — complete a partially built itinerary with ai
- [ ] **AI-04** — include supplier source and live availability status on every ai suggestion
- [ ] **AI-05** — model ai failure no viable suggestion as an explicit response state
- [ ] **AI-06** — enforce mandatory human in the loop approval before an ai itinerary reaches the traveler
- [ ] **AI-07** — write a 100 logged insert only ai suggestion audit trail
- [ ] **AI-08** — capture both the original ai suggestion and the consultant s edited final version in the audit trail
- [ ] **AI-09** — re validate ai suggested pricing at booking time if it has gone stale
- [ ] **AI-10** — build the complete with ai entry point with source availability badges
- [ ] **AI-11** — build the ai governance audit log viewer in the super admin console
- [ ] **AI-12** — generate ai ad creative variants grounded in package content and live pricing
- [ ] **AI-13** — bound ai response latency to protect the 10 minute itinerary target

## Local DMC + BYOS (11)

- [ ] **DMC-01** — submit a new local dmc for onboarding
- [ ] **DMC-02** — run the local dmc pending active vetting workflow
- [ ] **DMC-03** — bulk upload local dmc inventory via a validated csv template tool
- [ ] **DMC-04** — track local dmc quality signal cancellation rate and complaint count
- [ ] **DMC-05** — flag a local dmc to both the onboarding consultant and super admin on threshold breach
- [ ] **DMC-06** — let a consultant enter their own supplier api credentials byos
- [ ] **DMC-07** — make the supplier integration layer credential source agnostic
- [ ] **DMC-08** — merge byos inventory into search results using standard normalization and default selection
- [ ] **DMC-09** — scope byos inventory and credentials strictly to the owning consultant
- [ ] **DMC-10** — manage local dmc inventory items after onboarding crud
- [ ] **DMC-11** — alert on stale local dmc inventory beyond a defined threshold

## Ads/Campaign Management (15)

- [ ] **ADS-01** — provision a meta ad account business manager for a consultant
- [ ] **ADS-02** — model the ad campaign entity and its status state machine
- [ ] **ADS-03** — build the campaign builder screen package selector audience budget duration inputs
- [ ] **ADS-04** — generate and display ai creative variant gallery in the campaign builder
- [ ] **ADS-05** — require consultant approval per creative variant before submission
- [ ] **ADS-06** — route approved campaigns through super admin brand safety policy review
- [ ] **ADS-07** — launch an approved campaign under the adren managed meta account
- [ ] **ADS-08** — display a campaign status stepper matching the status enum
- [ ] **ADS-09** — flow campaign performance data back to the consultant dashboard
- [ ] **ADS-10** — enforce near real time spend cap on active campaigns
- [ ] **ADS-11** — implement campaign approval workflow guardrails and billing transparency
- [ ] **ADS-12** — auto pause a campaign when its linked package price changes
- [ ] **ADS-13** — surface a clear suspended action required status on meta account suspension
- [ ] **ADS-14** — define ad spend billing model per settlement currency
- [ ] **ADS-15** — apply brand safety policy template guardrails to campaign submissions

## Hardening (13)

- [ ] **HRD-01** — implement notification dispatch for email plus region configurable secondary channel
- [ ] **HRD-02** — wire all prd 15 notification trigger events
- [ ] **HRD-03** — make every notification listener idempotent
- [ ] **HRD-04** — build the notification preferences screen
- [ ] **HRD-05** — implement the full cancellation workflow across policy check approval and refund
- [ ] **HRD-06** — track disputes as tickets not email handoffs
- [ ] **HRD-07** — implement pnr booking search across all product types
- [ ] **HRD-08** — build the pnr booking search screen
- [ ] **HRD-09** — build the consultant dashboard
- [ ] **HRD-10** — show an onboarding checklist instead of empty charts for new consultants
- [ ] **HRD-11** — build the super admin dashboard
- [ ] **HRD-12** — tune inventory sync batch cadence per supplier
- [ ] **HRD-13** — alert super admin on inventory sync staleness beyond threshold

## Frontend Shell (10)

- [x] **FES-01** — register all prd part 21 screens as code split routes
- [ ] **FES-02** — establish the app wide provider stack slot for theme branding context
- [ ] **FES-03** — introduce a zustand store for the in progress itinerary builder draft
- [ ] **FES-04** — build shared ui primitives button card textfield select
- [ ] **FES-05** — build shared mappanel resultspanel layout primitives
- [ ] **FES-06** — implement runtime configurable white label theme tokens
- [ ] **FES-07** — add an auth session context with per role route guards
- [ ] **FES-08** — adopt react hook form zod as the form validation standard
- [ ] **FES-09** — build a schema driven market dependent onboarding wizard field engine
- [ ] **FES-10** — add a global toast notification queue for async operation feedback

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
Total: 142 stories.
