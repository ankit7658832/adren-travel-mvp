# ADREN TRAVEL
## Product Requirements Document (PRD) — Detailed Edition
**Version:** 2.0 (MVP / Phase 1 — Detailed/Engineering Edition)
**Home Market:** India (INR, compulsory) | **Expansion Markets:** Australia, UK, USA, Dubai (UAE), Denmark
**Founder:** Ankit Prasad | **CTO:** Aayush Bhankale
**Document Owner:** Product Team
**Status:** Draft for Review

---

## How to Use This Document

This is the detailed/engineering edition of the ADREN TRAVEL PRD, expanding the Master PRD with field-level specifications intended for direct use by engineering, QA, and design. It is organized so that:
- **Business stakeholders** can read Parts 0–8 for strategy, scope, and commercial logic.
- **Developers** can go directly to Parts 9–17 (module specs) and Part 20 (data dictionary) for implementation detail.
- **QA** can use Part 22 (acceptance criteria) and Part 25 (test scenarios) directly as a test-case source.
- **Designers/Frontend engineers** can use Part 21 (screen-by-screen UI specification) as a build reference.

Heading numbers are stable across revisions — use them when referencing this document in Jira tickets, code comments, or review threads (e.g., "per PRD Section 10.6.3, Hotelbeds error handling").

---

# 0. Brand Meaning

**ADREN** = **Adventure**, **Drift**, **Roam**, and **Enjoy your Nest**.

The name encodes the product's promise across two audiences:
- **For the traveler** (end consumer, served indirectly through Consultants): Adventure, Drift, Roam — discovery, movement, spontaneity.
- **For the travel agent/Consultant** (Adren's direct B2B customer): Enjoy your Nest — build your own branded travel business on Adren's infrastructure without owning the technology, supplier contracts, or backend complexity.

This dual meaning should carry into brand voice, marketing copy, and the white-label domain experience Consultants build for their own clients.

---

# 1. Executive Summary

ADREN TRAVEL is a B2B SaaS travel booking platform that enables travel Consultants (agents/agencies) to build, price, and sell multi-product itineraries — hotels, transport, transfers, cruises, and activities — under their own white-labeled brand, without needing direct supplier contracts, technology infrastructure, or performance-marketing capability.

**Problem it solves:**
- Independent travel agents in Adren's home market (**India**) and across its target expansion markets — Australia, UK, USA, Dubai (UAE), and Denmark — lack access to enterprise-grade multi-supplier inventory, AI-assisted itinerary tools, and digital marketing capability in one platform.
- Existing B2B travel portals (TBO, Travelomatix, Travelopro-class platforms) solve inventory aggregation but stop at booking. None currently bundle AI itinerary generation with governance, nor performance-marketing execution, into the same platform — and few handle multi-market regulatory complexity out of the box.

**What ADREN TRAVEL does:**
- Aggregates inventory from Hotelbeds, STUBA, TBO (hotels), Mystifly (transport), Transferz (transfers), Widgety (cruise), HBActivities (activities), plus Consultant-sourced Local DMC inventory and BYOS connections.
- Lets Consultants build a complete, multi-location itinerary in under 10 minutes using a map-based, AI-assisted itinerary builder.
- Provides full white-label branding managed by Super Admin per Consultant.
- Includes yield/markup management, true multi-currency (**INR compulsory**, plus AUD, GBP, USD, AED, DKK), multi-language support, and Stripe-based payments.
- Manages performance marketing on behalf of Consultants — Meta ad account/Business Manager setup, campaign launch, AI-generated creative — fully in MVP scope.

**Target customer:** Travel agents and small-to-mid agencies, based in **India (home market)** and expanding to Australia, UK, USA, Dubai (UAE), and Denmark.

**Business model:** SaaS/white-label licensing plus markup/commission share on bookings and a managed-service fee on ad spend.

**Leadership:** Founder — Ankit Prasad. CTO — Aayush Bhankale.

---

# 2. Goals & Success Metrics

| Goal | Metric | MVP Target |
|---|---|---|
| Fast itinerary creation | Median time from search to complete itinerary | ≤ 10 minutes |
| Consultant adoption | Number of active Consultants onboarded | Per go-to-market plan |
| Booking conversion | Itinerary → confirmed booking rate | Baseline post-launch |
| Platform trust in AI | % AI itineraries approved without correction | Tracked from launch |
| Marketing effectiveness | Campaigns/Consultant/month; cost-per-booking | Tracked from launch |
| Revenue | GMV processed through platform | Per business plan |
| Platform reliability | Uptime of core booking engine | 99.5%+ |

---

# 3. Personas

### 3.1 Super Admin (Adren Internal)
Owns the platform. Onboards Consultants, configures white-label domains, manages supplier credentials centrally, oversees Meta ad account provisioning, full visibility into Consultant activity, financials, and AI governance logs.

### 3.2 Consultant (Primary B2B Customer)
Travel agent/agency owner licensing the Adren platform, based in India or an expansion market. Builds itineraries/packages, manages Users, onboards Local DMCs, sets markup, launches campaigns. Onboarding requirements differ by home market (Section 13.1, Section 17).

### 3.3 User (Consultant's Staff / Sub-agent)
Operates under a Consultant's account. Searches, builds itineraries, books products; cannot change markup, onboard suppliers, or manage branding unless granted.

### 3.4 End Traveler (Indirect, Non-logged-in Actor)
The Consultant's/User's client. Does not log in during MVP; receives itineraries/vouchers via Consultant's domain.

### 3.5 Local DMC / Supplier (Onboarded by Consultant)
Regional supplier onboarded directly ("Localism"), requiring vetting before inventory is bookable.

---

# 4. Scope — MVP (Phase 1)

All functional areas are in MVP, including Meta Ads/Campaign management:

1. Multi-role platform: Super Admin, Consultant, User
2. White-label domain setup
3. Supplier credential management
4. Multi-location, map-based search and itinerary builder (10-minute target)
5. AI-assisted itinerary generation with governance
6. Package creation, quotation saving, voucher generation
7. Booking across Hotels, Transport, Transfers, Cruise, Activities
8. Local DMC onboarding (Localism)
9. BYOS
10. Yield/Markup management
11. Wallet / credit-limit / commission ledger
12. Multi-language, multi-currency support (INR + AUD/GBP/USD/AED/DKK)
13. Consultant dashboard and reporting
14. Stripe payment integration
15. Notifications and cancellation management
16. Inventory sync
17. PNR search
18. Meta Ads Campaign module

### Explicitly out of scope for MVP
- Sub-agent hierarchy beyond one level
- BNPL/instant credit-line fintech product
- Cross-Consultant marketplace sharing of Local DMC inventory
- Native mobile apps (MVP is responsive web)
- Visa and travel insurance as bookable products

---

# 5. System Architecture Overview (High-Level)

```
                         +--------------------------+
                         |     Super Admin Console  |
                         | (branding, suppliers,    |
                         |  ad account provisioning) |
                         +-------------+-------------+
                                       |
        +------------------------------+------------------------------+
        |                       ADREN CORE PLATFORM                   |
        |                                                             |
        |  +---------------+   +---------------+   +---------------+  |
        |  | Booking Engine |   |    AI Layer    |   |  Payments &   |  |
        |  | (search,       |<--+ (itinerary gen,|   |  Wallet Layer |  |
        |  |  itinerary,    |   |  governance,   |   |  (Stripe,     |  |
        |  |  package,      |   |  ad creative)  |   |  markup,      |  |
        |  |  quotation)    |   +---------------+   |  commission)  |  |
        |  +-------+--------+                       +---------------+  |
        |          |                                                   |
        |  +-------v--------+   +---------------+   +---------------+  |
        |  | Supplier API    |   | Notifications  |   |  Ads/Campaign  |  |
        |  | Integration     |   | (email/WhatsApp|   |  Module (Meta) |  |
        |  | Layer           |   |  /SMS)         |   +---------------+  |
        |  +-------+--------+   +---------------+                      |
        +----------+---------------------------------------------------+
                   |
   +---------------+-----------------------------------------------+
   |  Hotelbeds | STUBA | TBO | Mystifly | Transferz | Widgety      |
   |  HBActivities | Local DMCs (per Consultant) | BYOS sources     |
   +------------------------------------------------------------------+

                    Consultant White-Label Layer
        (custom domain, logo, colors, background per Consultant)
                                |
                        End Traveler (indirect)
```

Engineering will translate this into a detailed technical architecture document separately; this PRD defines functional boundaries only.

---

# 6. Roles & Permissions Matrix

| Capability | Super Admin | Consultant | User |
|---|---|---|---|
| Onboard Consultants | Yes | No | No |
| Configure white-label domain/branding | Yes | View only | No |
| Add/manage supplier API credentials | Yes | No | No |
| Provision Meta ad account/Business Manager | Yes | No | No |
| Search & build itinerary | Yes (test mode) | Yes | Yes |
| Use AI itinerary builder | Yes | Yes | Yes |
| Approve AI itinerary before sending to traveler | N/A | Yes | Yes (if permitted) |
| Create package | Yes | Yes | No (unless granted) |
| Set markup/yield rules | Yes (defaults) | Yes (own) | No |
| Onboard Local DMC | No | Yes | No |
| Add BYOS supplier connection | No | Yes | No |
| Add/manage Users under own account | No | Yes | No |
| View own wallet/credit ledger | Yes (all) | Yes (own) | No |
| Make booking | Yes (test) | Yes | Yes |
| Generate voucher/quotation | Yes | Yes | Yes |
| Launch Meta campaign for a package | No (executes) | Yes (request/approve) | No |
| View AI governance/audit logs | Yes (all) | Yes (own) | No |
| Cancel/refund booking | Yes | Yes (own) | Request only |

---

# 7. Assumptions, Dependencies, Risks

**Assumptions**
- Consultants have valid business registration and market-specific licensing (GST/PAN India-compulsory, ABN Australia, Companies House+ATOL UK, EIN+state Seller of Travel USA, DTCM Dubai/UAE, CVR Denmark).
- Supplier APIs provide sandbox/test access during build across India and all five expansion markets.
- Meta Business API access and ad account provisioning-at-scale is feasible under Adren's umbrella structure.
- Stripe (or equivalent) can process INR, AUD, GBP, USD, AED, DKK settlement.

**Dependencies**
- Third-party supplier API uptime and rate limits.
- Meta policy compliance is an external dependency.
- Stripe setup and local payment method support (UPI India, MobilePay Denmark).
- Legal/compliance review per market — see Section 17.

**Risks**
- Ad account liability (Adren manages Meta accounts/billing on Consultants' behalf).
- AI hallucination in itinerary/pricing (mitigated Section 11).
- Duplicate inventory across suppliers (mitigated Section 10).
- Local DMC quality inconsistency (mitigated Section 10.3).
- Regulatory non-compliance across six jurisdictions (mitigated Section 17).
- Currency/FX exposure across six settlement currencies (mitigated Section 12.2).
- Support coverage gap across six timezones.

---

# 8. Release Plan (Within MVP)

1. **Foundation**: Roles, white-label setup, supplier credential management, core search/itinerary builder
2. **Booking core**: Package creation, quotation, voucher, booking flow
3. **Financial layer**: Yield/markup, wallet/credit ledger, Stripe integration
4. **AI layer**: AI itinerary generation + governance framework
5. **Local DMC + BYOS**: Onboarding and vetting workflows
6. **Ads/Campaign module**: Meta account provisioning, campaign launch, AI creative generation
7. **Hardening**: Notifications, PNR search, reporting dashboards, inventory sync tuning

---

# 9. Module: Core Booking Engine

## 9.1 User Flows

**Flow A — Itinerary Creation (Target: ≤10 minutes)**
1. Consultant/User logs in → lands on Search Dashboard.
2. Enters multi-location search in a single search box supporting multi-select.
3. Enters dates, pax count, traveler preferences (optional).
4. Clicks Search → system geocodes each location and displays a map with pins.
5. For each location, system pre-selects one default product per category based on the Default Selection Algorithm (9.2).
6. Consultant/User accepts defaults or swaps any product via a side panel.
7. AI layer (Section 11) can auto-complete the remaining itinerary.
8. Finalized itinerary saved as a **Quotation**.
9. Quotation converts to a **Package** or directly to a **Booking**.

**Flow B — Package Creation**
1. Consultant selects a saved itinerary/quotation → "Convert to Package."
2. Sets name, validity dates, pricing, traveler capacity.
3. Publishes package — visible to Users, eligible for Meta campaign promotion.

**Flow C — Direct Booking (User)**
1. User searches available Packages or builds custom itinerary.
2. Selects traveler(s), enters details.
3. Reviews price breakdown.
4. Proceeds to payment (Stripe) or wallet billing.
5. Booking confirmed → voucher auto-generated → notification sent.

## 9.2 Default Selection Algorithm
1. **Availability** — must be confirmable in real time.
2. **Consultant's configured priority** — preferred supplier/margin target.
3. **Best margin** — highest margin among available options.
4. **Rating/quality score** — tiebreaker.

Must be explicitly surfaced in UI ("Auto-selected: Best available match").

## 9.3 Data Model (see full field-level dictionary in Part 20)

Summary entities: Itinerary, Line Item, Package, Booking, Quotation, Traveler Profile, Voucher. Full field tables are in **Part 20 — Complete Data Dictionary**.

## 9.4 Business Rules & Edge Cases (summary — full catalogue in Part 23)

| Scenario | Expected Behavior |
|---|---|
| No supplier availability for a location | Shows "No inventory available"; AI must not fabricate an option |
| Two suppliers offer the same physical hotel | Deduplicated via property-matching; Default Selection Algorithm chooses |
| Currency mismatch | FX conversion at quote time, rate snapshot locked |
| Partial cancellation | Pro-rated refund; does not cancel entire booking |
| Supplier overbooking post-confirmation | Alert workflow with suggested alternates |
| AI itinerary produces zero valid results | AI states failure explicitly |

## 9.5 Reporting & Dashboard Spec

**Consultant Dashboard:** Bookings this month, top packages, wallet balance, pending quotations, active campaigns.
**Super Admin Dashboard:** All-Consultant GMV, supplier performance, AI governance summary, ad spend across Consultants.

## 9.6 Non-Functional Requirements (summary — full per-module NFRs in Part 24)
- Itinerary generation ≤10 minutes end-to-end.
- Search SLA: low single-digit seconds for cached/normalized inventory.
- Uptime: 99.5%+.
- Decimal-safe arithmetic for all monetary calculations.

---

# 10. Module: Supplier & Inventory Integration

## 10.1 Supplier Overview

| Supplier | Product | Integration Type | Notes |
|---|---|---|---|
| Hotelbeds | Hotels | API (bedbank) | Primary hotel wholesaler |
| STUBA | Hotels | API | Secondary hotel source |
| TBO | Hotels | API | Tertiary hotel source, strong in India |
| Mystifly | Transport (Flights) | API | LCC + GDS-style fare aggregation |
| Transferz | Transfers | API | 150+ country coverage |
| Widgety | Cruise | API | Cruise inventory |
| HBActivities | Activities | API | Tours/experiences |
| Local DMC (Localism) | Hotels/Transfers/Activities | Manual + Consultant-managed | Vetting workflow (10.3) |
| BYOS | Any product | Consultant-provided credentials | Own supplier account |

## 10.2 Per-Supplier Integration Requirements — Detailed Engineering Appendix

This section provides the field-level integration specification for each connected supplier. Each subsection defines authentication, request/response mapping, error handling, rate-limit behavior, booking/cancellation mapping, and sync frequency — sufficient for an engineer to scope the integration ticket without further discovery.

### 10.2.1 Hotelbeds (Hotels)

**Authentication:** API key + shared secret, request signed via SHA-256 hash of (API key + secret + UTC timestamp), passed as `X-Signature` header. IP whitelisting required per Hotelbeds account setup.

**Search request/response mapping:**

| Hotelbeds Field | Adren Normalized Field | Notes |
|---|---|---|
| `hotelCode` | `supplier_rate_id` (composite) | Combined with rate key |
| `rateKey` | `supplier_rate_id` | Opaque token, must be passed back unmodified at booking |
| `hotelName` | `property_name` | — |
| `categoryName` | `star_rating` | Mapped to normalized 1–5 scale |
| `boardName` | `meal_plan` | Mapped to RO/BB/HB/FB/AI enum via lookup table |
| `net` | `net_rate` | In Hotelbeds' invoicing currency (typically EUR) |
| `cancellationPolicies[]` | `cancellation_deadline` | Take earliest `from` date as the deadline |
| `rooms[].name` | `room_type` | — |

**Error handling:**

| Hotelbeds Error | User-Facing Message | Retry/Fallback |
|---|---|---|
| `SOAP-ENV:Server` timeout | "Hotelbeds is temporarily unavailable — showing results from other suppliers" | Exclude from this search cycle, retry next search |
| `RATE_STALE` / rate key expired at booking | "This rate has expired — please re-search" | Force new search, do not silently re-price |
| No availability | (silent — simply omit from result set) | No retry needed |
| Auth failure (`INVALID_SIGNATURE`) | N/A — logged to Super Admin, not user-facing | Alert Super Admin, disable this supplier's results until resolved |

**Rate limits:** Hotelbeds enforces per-second call caps by contract tier. Implement a token-bucket limiter in the integration layer; queue overflow requests with backoff rather than dropping them.

**Booking/cancellation mapping:** Booking confirmation call requires the exact unmodified `rateKey` from search; cancellation uses Hotelbeds' booking reference returned at confirmation, mapped to Adren's internal `booking_id`.

**Sync frequency:** Real-time for search/pricing. Static content (images, descriptions, amenities) synced via Hotelbeds' Content API on a nightly batch job.

### 10.2.2 STUBA (Hotels)

**Authentication:** Username/password-based session token (XML API), token refreshed per session with a defined expiry.

**Search request/response mapping:** Similar structure to Hotelbeds but XML-based; key fields `HotelId`, `OfferId` (maps to `supplier_rate_id`), `RoomTypeName` (`room_type`), `MealType` (`meal_plan`), `SellingPrice` (mapped to `net_rate` — note STUBA may return sell price directly in some contract configurations, requiring a reverse-markup calculation to derive true net rate; confirm contract terms during integration).

**Error handling:** Session-expiry errors must trigger automatic re-authentication and a single retry before surfacing a user-facing "temporarily unavailable" message. Timeout and no-availability handling follow the same pattern as 10.2.1.

**Rate limits:** Lower default concurrency than Hotelbeds; integration layer must throttle STUBA calls independently, not share a global rate-limit bucket across suppliers.

**Booking/cancellation mapping:** STUBA booking confirmation requires the `OfferId` plus a re-validation call immediately before confirmation (STUBA does not guarantee rate validity for the full search-to-book window).

**Sync frequency:** Real-time search; static content sync frequency to be confirmed with STUBA's content API availability during technical due diligence (flagged as an open item).

### 10.2.3 TBO (Hotels)

**Authentication:** API key-based, IP whitelisting, separate credentials for test/UAT vs. production environments.

**Search request/response mapping:** `HotelCode` + `TraceId` (TBO requires all calls within a search session to reuse the same `TraceId` — this must be stored against the in-progress Itinerary draft, not just the final line item). `ResultIndex` maps to `supplier_rate_id`. `DayRates` array aggregated into `net_rate`.

**Error handling:** TBO's `TraceId` expires after a defined window (session-based); expired `TraceId` must trigger a full re-search, not a partial retry. Error table follows the same shape as 10.2.1/10.2.2 (timeout, stale rate, no availability, auth failure).

**Rate limits:** TBO rate limits are typically account-tier based; confirm limits during contract negotiation and configure the token-bucket limiter accordingly.

**Booking/cancellation mapping:** Booking requires `ResultIndex` + `TraceId` + guest details in a single call; TBO returns a `BookingId` used for all subsequent cancellation/amendment calls.

**Sync frequency:** Real-time search only; TBO's static content is generally lower-priority for nightly sync given overlapping coverage with Hotelbeds/STUBA — deduplication logic (Section 9.4) determines which supplier's content "wins" for a shared property.

### 10.2.4 Mystifly (Transport / Flights)

**Authentication:** API key + username/password combination per Mystifly's SOAP/REST hybrid API, session-based.

**Search request/response mapping:** `FlightSearchResult` → normalized flight line item with fields: `AirlineCode`, `FlightNumber`, `FareBasisCode` (→ `supplier_rate_id`), `TotalFare` (→ `net_rate`), `BaggageAllowance`, `CabinClass`. PNR generated at booking maps to Adren's PNR search (Section 16).

**Error handling:** Fare-expiry is common in flight APIs — Mystifly fares typically expire faster than hotel rates (minutes, not hours). The itinerary builder must re-validate flight pricing immediately before payment capture, not rely on the original search-time price alone.

**Rate limits:** Mystifly enforces per-minute search caps; given flights are typically the most frequently re-searched product (due to fast fare expiry), this integration needs its own dedicated rate-limit bucket, separate from hotel suppliers.

**Booking/cancellation mapping:** Booking call returns an airline PNR; cancellation/refund follows airline fare-rule logic (non-refundable, partially refundable, fully refundable), which must be surfaced to the Consultant before cancellation is confirmed (ties into Section 12.5 dispute handling).

**Sync frequency:** Real-time only — flight inventory has no meaningful "static content" sync analogous to hotel images/amenities.

### 10.2.5 Transferz (Transfers)

**Authentication:** API key, REST-based.

**Search request/response mapping:** `TransferOptionId` (→ `supplier_rate_id`), `VehicleType`, `PickupPoint`/`DropoffPoint` (geocoded, linked to the itinerary's location entries), `Price` (→ `net_rate`).

**Error handling:** No-coverage-at-location is a distinct case from no-availability (Transferz may not service a given pickup/dropoff pair at all) — this must produce a different user-facing message ("Transfers not available for this route" vs. "No transfer options available right now") so Consultants understand whether to try a different product type entirely.

**Rate limits:** Standard REST rate limiting; moderate priority given transfers are typically a secondary line item per itinerary.

**Booking/cancellation mapping:** Straightforward confirm/cancel calls against `TransferOptionId`; cancellation deadlines are typically shorter than hotel bookings (often within 24–48 hours of pickup) — this must be reflected distinctly in the cancellation-deadline field per line item.

**Sync frequency:** Real-time only.

### 10.2.6 Widgety (Cruise)

**Authentication:** API key-based, partner-tier access levels affecting available inventory depth.

**Search request/response mapping:** `SailingId` (→ `supplier_rate_id`), `CabinCategory` (→ `room_type` equivalent), `CruiseLine`, `Itinerary` (Widgety's own multi-port itinerary structure needs to be flattened into Adren's single-line-item model, with port-by-port detail stored as itinerary metadata rather than separate line items).

**Error handling:** Cruise inventory has materially longer booking-to-sail windows than other products; "rate expired" is less common but "cabin category sold out" is common — must be a distinct, clearly labeled failure state.

**Rate limits:** Lower call volume expected given cruise is a lower-frequency product category; standard REST throttling sufficient.

**Booking/cancellation mapping:** Cruise bookings often require additional passenger documentation (passport details) at time of booking, not just at check-in — this must be captured in the Traveler Profile data model (Part 20) before a cruise booking can be confirmed.

**Sync frequency:** Real-time search; static content (ship images, deck plans) synced weekly given lower change frequency than hotel content.

### 10.2.7 HBActivities (Activities)

**Authentication:** API key-based, REST.

**Search request/response mapping:** `ActivityId` (→ `supplier_rate_id`), `ActivityName` (→ `property_name` equivalent), `DurationMinutes`, `Price` (→ `net_rate`), `AvailableSlots[]` (time-slot based availability, distinct from date-range availability used by hotels).

**Error handling:** Time-slot-specific sellouts are common (e.g., a specific tour departure time is full while others on the same day are open) — the error/empty state must communicate "this time slot is full, try another time" rather than a blanket unavailability message.

**Rate limits:** Standard REST throttling.

**Booking/cancellation mapping:** Activity bookings often require exact headcount at time of booking (not amendable post-confirmation in many supplier contracts) — this constraint must be surfaced to the Consultant/User before they proceed to payment.

**Sync frequency:** Real-time for slot availability; static content (descriptions, images) synced nightly.

### 10.2.8 Local DMC (Localism) — Manual Integration

No live API. Integration is a data-entry/bulk-upload interface rather than a request/response API contract. See Section 10.3 for the full onboarding and vetting workflow. Engineering scope here is a CRUD interface plus a CSV/template bulk-upload tool with validation (required fields: product name, category, net rate, currency, cancellation policy text, availability calendar).

### 10.2.9 BYOS (Bring Your Own Supplier)

Technically identical integration pattern to whichever supplier the Consultant connects (most commonly Hotelbeds or STUBA, per Section 10.4), but credentials are Consultant-scoped rather than Adren-scoped. Engineering must ensure the integration layer is credential-source-agnostic — the same Hotelbeds integration code path should work whether the credentials are Adren's own or a Consultant's BYOS credentials, differentiated only by which credential set is loaded at request time.

## 10.3 Local DMC (Localism) Onboarding & Vetting Workflow

1. Consultant submits Local DMC details (business info, product categories, sample rates, references).
2. Adren (or delegated Consultant-level review) reviews for basic legitimacy.
3. Local DMC marked **Pending → Active** only after at least one verification step.
4. Inventory manually entered or bulk-uploaded (template-based).
5. Ongoing quality signal: cancellation rate and complaint flagging tied to the record.

## 10.4 BYOS (Bring Your Own Supplier)
- Consultant enters own supplier API credentials.
- Credentials stored encrypted, scoped only to that Consultant's account.
- BYOS inventory merged into search results following the same normalization and Default Selection Algorithm.

## 10.5 Inventory Sync
- Real-time availability/pricing at search time.
- Scheduled sync for static content.
- Sync failure alerting to Super Admin beyond a defined staleness threshold.

---

# 11. Module: AI Itinerary & Governance

## 11.1 AI Capabilities in Scope
- AI-assisted itinerary generation from natural-language or structured input.
- AI-assisted completion of partially built itineraries.
- AI-generated ad creative variants for Meta campaigns.

## 11.2 Governance Framework ("AI should not hallucinate")

**Core principles:**
1. **Grounded generation only** — AI may only select from live, supplier-confirmed inventory.
2. **Confidence and availability indicators** — every suggestion shows source supplier and live status.
3. **Mandatory human-in-the-loop approval** — enforced at workflow level.
4. **Explicit failure states** — AI states inability rather than substituting.
5. **Full audit logging** — every suggestion logged with input, source data, output, and disposition.

## 11.3 Acceptance Criteria (expanded set in Part 22)
- Given zero available suppliers for a location, AI states "no inventory available."
- Given an AI-generated itinerary, every line item shows supplier source and live availability before approval.
- Given stale pricing discovered post-approval, system re-validates at booking time.

---

# 12. Module: Payments, Yield/Markup & Wallet

## 12.1 Markup & Commission Engine
- Markup configurable per Consultant, per product category, percentage-based or flat-fee.
- Commission tracked separately from markup.

### Worked Example A — Standard Markup + Commission
- Supplier net rate: 10,000 units
- Consultant markup (15%): 1,500 → Sell rate: 11,500
- Adren commission (5% of net): 500, deducted from Consultant payout
- Consultant net earning: 1,500 minus applicable commission structure

### Worked Example B — Currency Buffer Applied (India, INR)
- Supplier net rate: EUR 100 (Hotelbeds)
- FX conversion to INR at snapshot rate (illustrative 1 EUR = INR 96): INR 9,600
- Currency buffer (3%): INR 288 added → adjusted base: INR 9,888
- Consultant markup (15% on adjusted base): INR 1,483.20 → Sell rate: INR 11,371.20

### Worked Example C — India GST/TCS on Outbound Package
- Package sell price: INR 100,000
- GST on service component (illustrative 5% on markup/service fee, not full package value — exact treatment to be confirmed with tax counsel): applied to the Consultant's margin component only
- TCS (illustrative 5%, applicable above notified threshold per current India rules) collected on the outbound package value at time of sale, remitted per Income Tax Act requirements
- **Note:** Exact GST/TCS treatment requires finance/tax counsel sign-off before implementation — this example is illustrative of the calculation layer's shape, not a final rate specification.

### Worked Example D — UK TOMS VAT (illustrative)
- Package cost (Consultant's margin only, per TOMS mechanism): GBP 200
- TOMS VAT (illustrative 20% on margin, not full package price): GBP 40
- **Note:** Requires UK tax counsel sign-off; TOMS calculation is materially different from standard VAT and must not be approximated as a flat percentage of total sale price.

## 12.2 Multi-Currency & FX Buffer (India + Global Markets)
- Six settlement currencies: **INR (compulsory)**, AUD, GBP, USD, AED, DKK.
- INR is the default/base currency for home-market operations and Adren's internal reporting.
- Currency buffer of 2–5% above markup, configurable per Consultant/market.
- FX rate snapshotted and locked at quotation.
- UPI (India) and MobilePay (Denmark) flagged as core local payment methods.

## 12.3 Wallet & Credit Limit
- Wallet: available balance, credit limit, pending holds, denominated in home-market currency.
- Booking confirmation places a hold; released or converted to debit on final confirmation.
- Credit limit breach blocks confirmation with a clear, actionable message.

## 12.4 Stripe Integration
- Payment collection or on-account billing across all six currencies.
- Refund/credit-note workflow tied to supplier cancellation policy.
- Reconciliation report per currency and market.

## 12.5 Cancellation & Dispute Handling
- Cancellation → policy check → refund/penalty calculation → Consultant approval if penalty applies → refund processed.
- Dispute flagging creates a tracked ticket, not just an email handoff.

---

# 13. Module: White-Label & Admin Console

## 13.1 Consultant Onboarding — Per-Market KYC

| Market | Core KYC Requirement |
|---|---|
| India (compulsory) | GST registration, business PAN, IATA/TAAI where applicable, bank details — mandatory |
| Australia | ABN, ATAS accreditation (if applicable), bank details |
| UK | Companies House number, ATOL license (if dynamic packaging flight+hotel), bank details |
| USA | EIN/business registration, state-level Seller of Travel registration where applicable, bank details |
| Dubai/UAE | DTCM trade license, bank details |
| Denmark | CVR registration number, bank details |

## 13.2 Branding Configuration
- Logo upload, background image, primary/secondary text color, domain mapping (CNAME).

## 13.3 Multi-Language & Multi-Currency
- India: English + Hindi/regional consideration. Expansion markets: English-primary; Danish secondary for Denmark.
- Base currency: INR (compulsory default for India) or AUD/GBP/USD/AED/DKK.

---

# 14. Module: Ads/Campaign Management (Meta) — MVP Scope

## 14.1 Overview
Adren manages Meta ad accounts/Business Managers centrally. Campaign targeting, billing currency, and policy interpretation vary by Consultant's home market.

## 14.2 Flow
1. Consultant creates Package, opts to "Promote this Package."
2. Provides campaign inputs: audience, budget, duration, destination market.
3. AI generates creative variants.
4. Consultant approves — mandatory step.
5. Super Admin brand-safety/policy review.
6. Campaign launches under Adren-managed account.
7. Performance data flows back to Consultant Dashboard.

## 14.3 Controls & Guardrails
- Spend caps, approval workflow, brand-safety review, billing transparency, account-suspension escalation.

## 14.4 AI Creative Generation
- Multiple variants per package, grounded in actual package content and live pricing.

---

# 15. Notifications & Cancellation Management
- Email (all markets) + region-configurable secondary channel (WhatsApp for India/Dubai, SMS for UK/US/Australia/Denmark).
- Trigger events: booking confirmed, payment received, cancellation, refund, AI approval needed, campaign status, credit threshold.
- Cancellation workflow per Section 12.5.

---

# 16. PNR Search
- Search by PNR/internal booking reference across all product types from a single field.

---

# 17. Module: Regional Compliance & Localization

## 17.1 Market-by-Market Requirements

| Market | Package Travel / Licensing | Data Protection | KYC | Currency |
|---|---|---|---|---|
| India (compulsory) | Self-regulated trade (IATA/TAAI); GST/TCS on outbound packages | India DPDP Act | GST, PAN, IATA/TAAI | INR |
| UK | Package Travel Regs 2018 — ATOL required for dynamic flight+hotel | UK GDPR | Companies House, ATOL | GBP |
| Denmark | Danish Package Travel Act | EU GDPR | CVR | DKK |
| Australia | ATAS accreditation (industry standard) | Australian Privacy Act | ABN, ATAS | AUD |
| USA | State-level Seller of Travel (CA/FL/WA/HI/IA) | State/federal rules | EIN, state registration | USD |
| Dubai/UAE | DTCM trade license | UAE data protection law | DTCM license | AED |

## 17.2 Platform Enforcement Requirements
- UK ATOL/PTR 2018 auto-enforcement in package creation flow.
- EU/UK data residency evaluation for traveler PII.
- US state-level licensing capture in onboarding.
- India GST/TCS calculation layer, distinct from UK TOMS logic.

## 17.3 Support & Operations Coverage
- IST, AEST, GMT/BST, EST/PST, GST, CET — six timezone bands; coverage model TBD with Operations.

## 17.4 Open Compliance Questions
- Whether Adren itself needs market-specific licensing.
- Whether Ads module needs per-market legal sign-off on templates.

---

# 18. Glossary

| Term | Definition |
|---|---|
| Consultant | Adren's direct B2B customer |
| User | Staff/sub-agent under a Consultant |
| Localism | Local DMC inventory manually onboarded |
| BYOS | Bring Your Own Supplier |
| Yield/Markup | Consultant's margin over net rate |
| PNR | Passenger Name Record / internal booking reference |
| Net Rate | Supplier's base cost |
| Sell Rate | Final traveler-facing price |
| ATOL | UK CAA license for dynamic flight+accommodation packages |
| ATAS | Australian travel agent accreditation |
| DTCM | Dubai Department of Tourism trade license authority |
| TOMS | UK Tour Operators Margin Scheme (VAT) |
| GST/TCS | India's tax framework for outbound packages |
| TraceId | TBO's session identifier linking search to booking |
| Rate Key | Hotelbeds' opaque token identifying a specific rate offer |

---

# 19. Open Items for Business Confirmation
- Exact commission tiers and interaction with markup.
- Local DMC vetting ownership (Adren vs. Consultant self-certification).
- Ad spend billing model per settlement currency.
- Sub-agent hierarchy depth beyond Consultant → User.
- Whether Adren itself requires market-specific licensing.
- EU/UK data residency approach.
- Support coverage model across six timezones.
- Exact GST/TCS and UK TOMS VAT rates and calculation mechanics (Section 12.1, Examples C & D) — tax counsel sign-off required before build.

---

# 20. Complete Data Dictionary

This part provides field-level specifications for every core entity, extending the summary given in Section 9.3.

## 20.1 Itinerary

| Field | Type | Notes |
|---|---|---|
| itinerary_id | UUID | Primary key |
| consultant_id | UUID | Owner (foreign key to Consultant) |
| created_by_user_id | UUID (nullable) | If created by a User rather than the Consultant directly |
| locations[] | Array<Location> | Ordered list of destinations, each geocoded |
| date_range | Date/Date | Start/end |
| pax_count | Object {adults, children, infants} | — |
| line_items[] | Array<reference> | References into Hotel/Transport/Transfer/Cruise/Activity line items |
| currency | Enum | INR/AUD/GBP/USD/AED/DKK |
| status | Enum | Draft / Quotation / Booked / Cancelled |
| ai_generated | Boolean | Flag if AI-assisted |
| ai_audit_log_id | UUID (nullable) | Link to AI governance log if applicable |
| created_at / updated_at | Timestamp | — |

## 20.2 Line Item — Hotel

(As per Section 9.3, repeated here for completeness.)

| Field | Type | Notes |
|---|---|---|
| line_item_id | UUID | Primary key |
| itinerary_id | UUID | Parent reference |
| supplier_id | Enum | Hotelbeds / STUBA / TBO / Local DMC / BYOS |
| supplier_rate_id | String | Supplier's opaque rate reference |
| property_name | String | — |
| room_type | String | — |
| meal_plan | Enum | RO/BB/HB/FB/AI |
| cancellation_deadline | Timestamp (timezone-aware) | — |
| net_rate | Decimal | Supplier net cost |
| markup_applied | Decimal | — |
| currency_buffer_applied | Decimal | Per Section 12.2 |
| sell_rate | Decimal | net + markup + buffer |
| currency | Enum | — |
| fx_rate_snapshot | Decimal | Locked at quote time |

## 20.3 Line Item — Flight (Mystifly)

| Field | Type | Notes |
|---|---|---|
| line_item_id | UUID | Primary key |
| airline_code | String | — |
| flight_number | String | — |
| fare_basis_code | String | Maps to supplier_rate_id |
| cabin_class | Enum | Economy/PremiumEconomy/Business/First |
| baggage_allowance | String | — |
| pnr | String (nullable until booked) | Airline PNR, distinct from Adren's internal booking_id |
| net_rate / sell_rate / currency / fx_rate_snapshot | — | Same pattern as Hotel |

## 20.4 Line Item — Transfer (Transferz)

| Field | Type | Notes |
|---|---|---|
| line_item_id | UUID | Primary key |
| vehicle_type | Enum | Sedan/SUV/Van/Luxury/etc. |
| pickup_point / dropoff_point | Geo-coordinate + label | Linked to itinerary locations |
| net_rate / sell_rate / currency / fx_rate_snapshot | — | Same pattern as Hotel |

## 20.5 Line Item — Cruise (Widgety)

| Field | Type | Notes |
|---|---|---|
| line_item_id | UUID | Primary key |
| sailing_id | String | Maps to supplier_rate_id |
| cruise_line | String | — |
| cabin_category | String | — |
| ports[] | Array<String> | Multi-port itinerary metadata |
| passenger_documents_required | Boolean | Triggers Traveler Profile passport capture requirement |
| net_rate / sell_rate / currency / fx_rate_snapshot | — | Same pattern as Hotel |

## 20.6 Line Item — Activity (HBActivities)

| Field | Type | Notes |
|---|---|---|
| line_item_id | UUID | Primary key |
| activity_id | String | Maps to supplier_rate_id |
| duration_minutes | Integer | — |
| time_slot | Timestamp | Specific departure/slot time, not a date range |
| headcount | Integer | Fixed at booking per supplier constraint (Section 10.2.7) |
| net_rate / sell_rate / currency / fx_rate_snapshot | — | Same pattern as Hotel |

## 20.7 Package

| Field | Type | Notes |
|---|---|---|
| package_id | UUID | Primary key |
| source_itinerary_id | UUID | — |
| consultant_id | UUID | Owner |
| name / description | String | — |
| validity_start / validity_end | Date | — |
| base_price | Decimal | — |
| markup_price | Decimal | — |
| max_pax | Integer | — |
| promoted_via_ads | Boolean | Links to Ads module |
| ad_campaign_id | UUID (nullable) | If promoted |
| is_dynamic_flight_hotel_combo | Boolean | Drives UK ATOL enforcement (Section 17.2) |

## 20.8 Booking

| Field | Type | Notes |
|---|---|---|
| booking_id | UUID | Primary key (Adren internal — distinct from supplier booking IDs and airline PNRs) |
| itinerary_id / package_id | UUID (one or the other) | — |
| consultant_id / user_id | UUID | Who made the booking |
| traveler_ids[] | Array<UUID> | References to Traveler Profile |
| status | Enum | Confirmed / PartiallyCancelled / Cancelled / Disputed |
| total_sell_price | Decimal | — |
| currency | Enum | — |
| payment_method | Enum | Stripe / Wallet / OnAccount |
| supplier_booking_refs[] | Array<{supplier_id, ref}> | One per line item's supplier confirmation |
| pnr_searchable_ref | String | Adren's own reference surfaced for PNR search (Section 16) |
| created_at | Timestamp | — |

## 20.9 Quotation

| Field | Type | Notes |
|---|---|---|
| quotation_id | UUID | Primary key |
| itinerary_id | UUID | — |
| valid_until | Timestamp | Rate/FX validity window |
| shared_with_traveler | Boolean | Whether sent externally |
| converted_to_booking_id | UUID (nullable) | — |

## 20.10 Traveler Profile

| Field | Type | Notes |
|---|---|---|
| traveler_id | UUID | Primary key |
| consultant_id | UUID | Owning Consultant (traveler data not shared across Consultants) |
| name / date_of_birth | String / Date | — |
| passport_number / passport_expiry / nationality | String / Date / String | Required for cruise/international bookings |
| document_vault[] | Array<file reference> | Encrypted storage references |
| preferences | Object | Meal, seating, accessibility, etc. |
| created_at / updated_at | Timestamp | — |

## 20.11 Voucher

| Field | Type | Notes |
|---|---|---|
| voucher_id | UUID | Primary key |
| booking_id | UUID | — |
| pdf_reference | String | Generated document storage path |
| generated_at | Timestamp | — |
| atol_certificate_reference | String (nullable) | Populated only for UK dynamic flight+hotel packages (Section 17.2) |

## 20.12 Wallet Ledger Entry

| Field | Type | Notes |
|---|---|---|
| ledger_entry_id | UUID | Primary key |
| consultant_id | UUID | — |
| type | Enum | TopUp / Hold / Debit / Refund / CommissionDeduction |
| amount | Decimal | — |
| currency | Enum | — |
| related_booking_id | UUID (nullable) | — |
| balance_after | Decimal | Snapshot for audit trail |
| created_at | Timestamp | — |

## 20.13 Ad Campaign

| Field | Type | Notes |
|---|---|---|
| campaign_id | UUID | Primary key |
| package_id | UUID | — |
| consultant_id | UUID | — |
| status | Enum | PendingApproval / PendingPolicyReview / Live / Paused / Rejected / SpendCapReached |
| budget_cap | Decimal | — |
| currency | Enum | Consultant's home-market currency |
| creative_variants[] | Array<{image_ref, copy_text, approved}> | AI-generated, Consultant-approved |
| meta_campaign_ref | String | External Meta campaign ID |
| spend_to_date | Decimal | — |
| performance_snapshot | Object {impressions, clicks, bookings_attributed} | — |

## 20.14 Local DMC Record

| Field | Type | Notes |
|---|---|---|
| dmc_id | UUID | Primary key |
| onboarding_consultant_id | UUID | — |
| business_name / contact_info | String | — |
| status | Enum | Pending / Active / Suspended |
| product_categories[] | Array<Enum> | Hotels/Transfers/Activities |
| cancellation_rate | Decimal | Rolling quality signal |
| complaint_count | Integer | Rolling quality signal |

---

# 21. Screen-by-Screen UI Specification

This part describes each core screen's layout and states at a functional level, sufficient for design and frontend engineering to scope work. It is not a visual mockup — it defines structure, states, and behavior.

## 21.1 Search Dashboard (Consultant/User)
- **Layout:** Top: multi-select location search box with autocomplete. Below: date range picker, pax count selector, optional budget/preference fields. Primary CTA: "Search."
- **Default state:** Empty search box, no results panel shown.
- **Loading state:** Skeleton loader on results panel after search submitted; search box remains editable.
- **Results state:** Map panel (left/top on mobile) with pins per location; itinerary panel (right/bottom) showing auto-selected default product per category per location.
- **Empty state:** If zero suppliers return availability for a location, that location's card shows "No inventory available" (per Section 9.4) rather than being silently omitted.
- **Error state:** If search fails entirely (all suppliers timeout), show a retry prompt, not a blank screen.

## 21.2 Itinerary Builder
- **Layout:** Per-location cards, each showing the auto-selected Hotel/Activity/Transfer/Transport/Cruise line item with a "Change" action opening a side panel of alternates.
- **Alternate-selection panel:** Filterable/sortable list (price, rating, supplier) of available options for that category/location.
- **AI-assist entry point:** Persistent "Complete with AI" button; when invoked, shows AI-suggested line items with source-supplier and availability badges (per Section 11.2) before they're added — never silently inserted.
- **Save states:** "Save as Quotation" (primary), auto-save draft indicator.

## 21.3 Package Builder
- **Layout:** Form over a selected Quotation — name, description, validity dates, pricing (base auto-filled, markup editable), max pax.
- **Validation states:** Cannot publish without all required fields; if `is_dynamic_flight_hotel_combo` is true and Consultant's market is UK, a mandatory ATOL disclosure step is inserted before publish is allowed (per Section 17.2) — this cannot be skipped.
- **Publish confirmation:** Shows a summary before final publish, including the "Promote this Package" opt-in leading into the Ads flow (Section 14).

## 21.4 Booking & Payment Flow (User-facing)
- **Layout:** Traveler detail form → price breakdown (collapsible net/markup detail per Consultant's visibility settings) → payment method selection (Stripe / Wallet / On-Account) → confirmation.
- **Validation:** Traveler documents (passport, etc.) required inline if any line item flags `passenger_documents_required` (cruise) or international travel.
- **Confirmation state:** Booking reference, voucher download link, and (for UK dynamic packages) ATOL certificate download link.

## 21.5 Consultant Dashboard
- **Layout:** Summary cards (bookings this month, GMV, wallet balance) at top; tabs below for Top Packages, Pending Quotations, Active Campaigns.
- **Empty states:** New Consultants with no bookings yet see an onboarding checklist instead of empty charts.

## 21.6 Super Admin Console
- **Layout:** Left navigation: Consultants, Suppliers, Ad Accounts, AI Governance Logs, Global Reporting.
- **Consultant onboarding wizard:** Multi-step form matching the per-market KYC table (Section 13.1) — the form's required fields change dynamically based on the selected home market.
- **Supplier credential management:** Masked credential fields, audit log of who last modified each credential set.

## 21.7 Wallet & Billing Screen
- **Layout:** Balance summary, transaction ledger (filterable by type per Section 20.12), top-up action, credit limit display.
- **Breach state:** If a pending booking would breach the credit limit, an inline warning appears before the user reaches the payment step (not after).

## 21.8 Campaign Builder (Ads Module)
- **Layout:** Package selector → audience/budget/duration inputs → AI-generated creative variant gallery (multiple image/copy combinations) → approval checkboxes per variant → submit for Super Admin review.
- **Status tracking:** Visual status stepper (Pending Approval → Pending Policy Review → Live / Rejected) matching the `status` enum in Section 20.13.

## 21.9 PNR / Booking Search
- **Layout:** Single search field, results list showing booking summary across all product types, click-through to full booking detail.

## 21.10 Notification Preferences Screen
- **Layout:** Per-Consultant toggle for secondary channel (WhatsApp/SMS) per Section 15, with regional default pre-selected but overridable.

---

# 22. Acceptance Criteria Catalogue

This part extends acceptance criteria (Given/When/Then format) across all MVP features, beyond the samples already given in Sections 11.3 and 14.5.

## 22.1 Multi-Location Search
- Given a Consultant enters 3+ locations in the search box, when they submit the search, then the system geocodes and displays a map pin for every location, even if one has no inventory.
- Given a location has zero results from all suppliers, when results render, then that location's card shows "No inventory available" rather than being omitted from the map.
- Given a search is in progress, when the Consultant edits the search box, then the in-progress search is cancelled rather than both results being merged.

## 22.2 Default Selection Algorithm
- Given multiple hotel options are available for a location, when the system auto-selects a default, then the selected option is the highest-margin confirmable option per Section 9.2.
- Given the Consultant has configured a preferred supplier, when auto-selection runs, then the preferred supplier's option is selected if available, overriding pure margin ranking.
- Given the UI displays an auto-selected item, when the Consultant views it, then a visible "Auto-selected: Best available match" label is present.

## 22.3 Itinerary → Quotation → Package → Booking Lifecycle
- Given an itinerary has at least one line item per required category, when "Save as Quotation" is clicked, then the itinerary status transitions from Draft to Quotation and becomes read-only except via explicit edit.
- Given a Quotation is converted to a Package, when the Package is published, then it becomes visible to the Consultant's Users.
- Given a Package includes both a flight and a hotel line item and the Consultant's market is UK, when the Consultant attempts to publish, then the system blocks publish until the ATOL disclosure step is completed.

## 22.4 Markup & Currency
- Given a Consultant has configured a 15% markup on hotels, when a hotel line item is added, then the sell_rate reflects net_rate × 1.15 plus the applicable currency buffer.
- Given a booking's supplier currency differs from the Consultant's sell currency, when a quote is generated, then the fx_rate_snapshot is locked and does not change even if market rates move before booking confirmation.
- Given a Consultant's wallet balance plus available credit is less than the booking total, when they attempt to confirm payment via wallet, then the system blocks confirmation with an actionable "top up required" message.

## 22.5 Local DMC Onboarding
- Given a Consultant submits a new Local DMC, when the submission is saved, then its status is Pending, not Active, until at least one verification step completes.
- Given a Local DMC's cancellation rate exceeds a defined threshold, when the quality signal updates, then both the onboarding Consultant and Super Admin see a flag on that DMC's record.

## 22.6 BYOS
- Given a Consultant adds their own Hotelbeds credentials via BYOS, when search runs, then BYOS inventory appears in results using the same normalization logic as Adren's own Hotelbeds connection, and is scoped only to that Consultant.

## 22.7 Notifications
- Given a booking is confirmed for a Dubai-based Consultant, when the confirmation notification fires, then WhatsApp is used as the default secondary channel unless the Consultant has overridden this preference.
- Given a cancellation is processed, when the refund is issued, then a notification fires on both the primary (email) and configured secondary channel.

## 22.8 PNR Search
- Given a booking reference is entered in PNR search, when the search runs, then it returns results regardless of whether the underlying product is a flight, hotel, transfer, cruise, or activity.

## 22.9 Regional Compliance
- Given a Consultant's declared home market is USA and their declared state is California, when they complete onboarding, then the system flags the California Seller of Travel registration requirement per Section 17.1.
- Given a UK Consultant publishes a dynamic flight+hotel package, when the booking is confirmed, then an ATOL certificate is generated and attached to the voucher (Section 20.11).

---

# 23. Expanded Edge-Case & Error Catalogue (Per Module)

## 23.1 Booking Engine
1. Two Users under the same Consultant attempt to book the last available inventory unit simultaneously → second attempt must fail gracefully with "no longer available," not a duplicate booking.
2. Itinerary contains a mix of INR and AED line items due to a BYOS supplier configured in a different currency than the Consultant's default → system must consolidate to the Consultant's sell currency at checkout, not present a mixed-currency total.
3. Traveler count changes after a Quotation is generated but before booking → price must be recalculated, not carried over from the stale Quotation.

## 23.2 Supplier Integration
4. A supplier's sandbox/test environment behaves differently from production (common with Hotelbeds/TBO) → integration tests must run against both, flagged separately in CI.
5. TBO's `TraceId` expires mid-itinerary-build (Section 10.2.3) → system must detect this and prompt a re-search rather than fail silently at booking.
6. Mystifly fare expires between search and payment capture (Section 10.2.4) → system must re-validate price immediately pre-payment and show a clear "price changed, please confirm" prompt rather than charging a stale amount.

## 23.3 AI Governance
7. AI is asked to complete an itinerary with a budget that no available inventory can meet → AI must state this explicitly, not silently pick the closest over-budget option.
8. AI-suggested itinerary is edited by the Consultant after generation, then re-approved → audit log must capture both the original AI suggestion and the edited final version, not overwrite the original.

## 23.4 Payments & Wallet
9. A refund is issued in a currency different from the original booking currency due to an FX rate change between booking and cancellation → refund amount must be calculated against the original locked fx_rate_snapshot, not the current rate.
10. Wallet top-up payment succeeds at the payment gateway but the webhook confirming it to Adren's system fails/delays → wallet balance must reconcile once the webhook is received/retried, with the booking flow blocked (not falsely allowed) in the interim.

## 23.5 Ads/Campaign
11. A campaign's linked Package is edited (price change) after the campaign is already live → system must detect the mismatch and pause the campaign until creative/pricing is re-approved (per Section 14.4's live-price validation requirement).
12. Meta suspends an ad account mid-campaign → all active campaigns under that Consultant must show a clear "suspended — action required" status, not silently stop spending with no explanation.

## 23.6 Regional Compliance
13. A Consultant's declared home market changes after onboarding (e.g., relocates from India to UK) → system must re-trigger the KYC checklist for the new market rather than leaving the account under the original market's rules.
14. A package combines a UK Consultant's flight+hotel with a Local DMC-sourced activity from India → ATOL applicability must be evaluated based on the flight+hotel combination specifically, independent of the additional local activity component.

---

# 24. Non-Functional Requirements Per Module

## 24.1 Booking Engine
- Itinerary generation ≤10 minutes end-to-end.
- Search aggregation SLA: low single-digit seconds for cached/normalized inventory, higher tolerance acceptable for live-only categories (activities, flights).
- Decimal-safe arithmetic throughout (no floating-point rounding errors given multi-currency markup stacking).

## 24.2 Supplier Integration
- Each supplier integration isolated behind a circuit breaker — one supplier's downtime must not degrade search latency for the others.
- Rate-limit compliance per supplier's contracted tier, independently throttled (Section 10.2).

## 24.3 AI Governance
- AI response latency target: itinerary suggestions returned within a few seconds per segment to stay within the overall 10-minute itinerary target.
- 100% of AI suggestions logged (no sampling) given the audit/trust requirement in Section 11.2.

## 24.4 Payments & Wallet
- PCI-DSS scope minimized by relying on Stripe's hosted payment elements rather than handling raw card data directly.
- All wallet ledger writes must be atomic and idempotent (no double-debit on retry).

## 24.5 White-Label & Admin
- Domain/branding changes propagate to the Consultant's live storefront within a defined short window (not requiring a full redeploy).

## 24.6 Ads/Campaign
- Spend-cap enforcement must be near-real-time — a campaign must not meaningfully overshoot its budget cap due to processing lag (Section 14.3).

## 24.7 Regional Compliance
- KYC checklist logic must be data-driven (configurable per market) rather than hardcoded per-market conditionals, since market rules (e.g., US state list, India tax thresholds) can change independent of a full release cycle.

---

# 25. Test Scenario Appendix (QA Reference)

This is a traceable subset of test scenarios derived from Part 22's acceptance criteria, structured for direct use in a QA test matrix. Full test-case IDs and execution tracking would live in the QA tooling (e.g., Jira/Xray), not this document — this appendix defines the scenario set to seed that matrix.

| # | Feature | Scenario | Expected Result |
|---|---|---|---|
| T1 | Multi-location search | Search 3 locations, 1 with zero inventory | Map shows all 3 pins; the zero-inventory location shows explicit empty state |
| T2 | Default selection | Two hotels available, different margins | Higher-margin option auto-selected, labeled as auto-selected |
| T3 | Default selection | Consultant has preferred supplier set | Preferred supplier's option selected even if not highest margin |
| T4 | Itinerary lifecycle | Save itinerary as Quotation | Status transitions Draft → Quotation, becomes read-only |
| T5 | UK ATOL enforcement | UK Consultant publishes flight+hotel package | Publish blocked until ATOL disclosure step completed |
| T6 | Markup calculation | 15% markup + 3% currency buffer on a hotel line item | Sell rate = net × 1.15, plus buffer applied per Section 12.2 formula |
| T7 | FX lock | Market FX rate changes after quote, before booking | Booking price uses the locked fx_rate_snapshot, not current rate |
| T8 | Wallet breach | Booking total exceeds available wallet + credit | Confirmation blocked with actionable top-up message |
| T9 | Local DMC onboarding | New DMC submitted | Status is Pending, not Active, until verified |
| T10 | BYOS scoping | Consultant A's BYOS credentials | Not visible or usable by Consultant B |
| T11 | Notification routing | Dubai Consultant, booking confirmed | WhatsApp used as default secondary channel |
| T12 | PNR search | Search by reference across product types | Returns result regardless of product category |
| T13 | AI grounding | AI asked to complete itinerary with unmeetable budget | AI explicitly states inability, no fabricated option |
| T14 | AI audit trail | Consultant edits AI suggestion before approval | Both original AI output and edited final version logged |
| T15 | Refund FX | Refund issued after FX market rate moved | Refund calculated against original locked snapshot |
| T16 | Campaign price sync | Package price changes while campaign is live | Campaign auto-pauses pending re-approval |
| T17 | Ad account suspension | Meta suspends account mid-campaign | Campaign status shows "suspended — action required" |
| T18 | Market change | Consultant relocates India → UK | KYC checklist re-triggers for UK requirements |
| T19 | TBO session expiry | TraceId expires mid-build | System prompts re-search, not silent failure |
| T20 | Mystifly fare expiry | Fare expires between search and payment | Price re-validated pre-payment, stale charge blocked |
| T21 | Concurrent booking | Two Users book last unit simultaneously | Second attempt fails gracefully, no duplicate booking |
| T22 | Cruise documentation | Cruise line item added to itinerary | Traveler Profile passport fields required before booking confirmation |
| T23 | Activity slot sellout | Specific time slot fully booked | Distinct "slot full, try another time" message shown |
| T24 | India GST/TCS | Outbound package sold to India-based Consultant's traveler | Tax calculation layer applies per Section 12.1 Example C (pending tax counsel sign-off) |

---

# 26. Document Change Log

| Version | Change |
|---|---|
| 1.0 | Initial MVP PRD, India-first framing |
| 1.1 | Global market expansion (Australia, UK, USA, Dubai, Denmark); INR reinstated as compulsory home-market currency |
| 2.0 | Detailed/Engineering Edition — added per-supplier integration appendix (Part 10.2), full data dictionary (Part 20), screen-by-screen UI spec (Part 21), acceptance criteria catalogue (Part 22), expanded edge-case catalogue (Part 23), per-module NFRs (Part 24), test scenario appendix (Part 25) |

---

*End of Document — v2.0 (Detailed Edition)*
