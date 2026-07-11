# ADREN TRAVEL
## Product Requirements Document (PRD)
**Version:** 1.1 (MVP / Phase 1 — Global Markets: Australia, UK, USA, Dubai/UAE, Denmark)
**Document Owner:** Product Team
**Status:** Draft for Review

---

# 0. Brand Meaning

**ADREN** = **Adv**enture, **D**rift, **R**oam, and **E**njoy your **N**est.

The name encodes the product's promise across two audiences:
- **For the traveler (end consumer, served indirectly through Consultants):** Adventure, Drift, Roam — discovery, movement, spontaneity.
- **For the travel agent/Consultant (Adren's direct B2B customer):** Enjoy your Nest — build your own branded travel business on Adren's infrastructure without owning the technology, supplier contracts, or backend complexity.

This dual meaning should carry into brand voice, marketing copy, and the white-label domain experience Consultants build for their own clients.

---

# 1. Executive Summary

ADREN TRAVEL is a B2B SaaS travel booking platform that enables travel Consultants (agents/agencies) to build, price, and sell multi-product itineraries — hotels, transport, transfers, cruises, and activities — under their own white-labeled brand, without needing direct supplier contracts, technology infrastructure, or performance-marketing capability.

**Problem it solves:**
- Independent travel agents in Adren's home market (**India**) and across its target expansion markets — Australia, UK, USA, Dubai (UAE), and Denmark — lack access to enterprise-grade multi-supplier inventory, AI-assisted itinerary tools, and digital marketing capability in one platform, all of which are normally out of reach without significant capital, technical teams, or multi-jurisdiction compliance expertise.
- Existing B2B travel portals (TBO, Travelomatix, Travelopro-class platforms) solve inventory aggregation but stop at booking. None currently bundle AI itinerary generation with governance, nor performance-marketing execution, into the same platform — and few handle multi-market regulatory complexity (ATOL/UK, GDPR/EU, ATAS/Australia, state-level Seller of Travel rules/USA, DTCM/Dubai) out of the box.

**What ADREN TRAVEL does:**
- Aggregates inventory from Hotelbeds, STUBA, TBO (hotels), Mystifly (transport), Transferz (transfers), Widgety (cruise), HBActivities (activities), plus Consultant-sourced Local DMC inventory and BYOS (Bring Your Own Supplier) connections — all of which have genuinely global coverage suitable for Adren's target markets.
- Lets Consultants build a complete, multi-location itinerary in under 10 minutes using a map-based, AI-assisted itinerary builder.
- Provides full white-label branding (logo, domain, colors, background imagery) managed by Super Admin per Consultant.
- Includes built-in yield/markup management, true multi-currency (**INR as the compulsory home-market currency**, plus AUD, GBP, USD, AED, DKK for target markets) and multi-language support, and Stripe-based payments.
- **Manages performance marketing on behalf of Consultants** — Meta ad account and Business Manager setup, campaign launch for packages, and AI-generated ad creative variants — fully in MVP scope per business direction, with per-market currency and targeting.

**Target customer:** Travel agents and small-to-mid agencies operating on a B2B model, based in **India (Adren's home market)** and expanding to **Australia, UK, USA, Dubai (UAE), and Denmark**, who want enterprise-grade booking + marketing infrastructure without building it themselves or navigating multi-country travel regulation alone.

**Business model:** SaaS/white-label licensing to Consultants, combined with markup/commission share on bookings and a managed-service fee on ad spend executed through the platform.

---

# 2. Goals & Success Metrics

| Goal | Metric | MVP Target (illustrative — to be finalized with business) |
|---|---|---|
| Fast itinerary creation | Median time from search to complete itinerary | ≤ 10 minutes |
| Consultant adoption | Number of active Consultants onboarded | Defined per go-to-market plan |
| Booking conversion | Itinerary → confirmed booking rate | Baseline to be established post-launch |
| Platform trust in AI | % of AI-suggested itineraries approved without manual correction | Tracked from launch, target improves over time |
| Marketing effectiveness | Campaigns launched per Consultant per month; cost-per-booking from Meta campaigns | Tracked from launch |
| Revenue | GMV (Gross Merchandise Value) processed through platform | Defined per business plan |
| Platform reliability | Uptime of core booking engine | 99.5%+ |

---

# 3. Personas

### 3.1 Super Admin (Adren Internal)
Owns the platform. Onboards Consultants, configures white-label domains, manages supplier credentials centrally, oversees Meta ad account provisioning, and has full visibility into all Consultant activity, financials, and AI governance logs.

### 3.2 Consultant (Primary B2B Customer)
A travel agent/agency owner who licenses the Adren platform, based in one of Adren's target markets (Australia, UK, USA, Dubai/UAE, or Denmark). Builds itineraries and packages, manages their own Users/sub-agents, onboards Local DMCs, sets markup, and launches marketing campaigns for their packages. This is the core paying customer. Onboarding requirements (KYC, licensing) differ by the Consultant's home market — see Section 13.1 and the new Section 19 (Regional Compliance & Localization).

### 3.3 User (Consultant's Staff / Sub-agent)
Operates under a Consultant's account. Can search, build itineraries, and book products, but cannot change markup rules, onboard suppliers, or manage branding — scope defined by permissions the Consultant grants (see Roles & Permissions Matrix).

### 3.4 End Traveler (Indirect, Non-logged-in Actor)
The Consultant's or User's client. Does not log into Adren directly in MVP — receives itineraries/quotations/vouchers via the Consultant's white-labeled domain or shared document. Traveler data (documents, preferences) is stored for reuse across bookings.

### 3.5 Local DMC / Supplier (Onboarded by Consultant)
A regional supplier (hotel, transfer, or activity provider) that a Consultant onboards directly ("Localism") outside the standard API-connected suppliers. Requires a vetting workflow before their inventory becomes bookable.

---

# 4. Scope — MVP (Phase 1)

Per business direction, **all functional areas are in MVP**, including Meta Ads/Campaign management. Nothing is deferred to a later phase in this version of the PRD. The following are all in scope for Phase 1:

1. Multi-role platform: Super Admin, Consultant, User
2. White-label domain setup: logo, background image, brand color, per-Consultant branding
3. Supplier credential management (Super Admin controlled)
4. Multi-location, map-based search and itinerary builder (10-minute target)
5. AI-assisted itinerary generation with governance/anti-hallucination controls
6. Package creation, quotation saving, voucher generation
7. Booking across Hotels, Transport, Transfers, Cruise, Activities
8. Local DMC onboarding (Localism) by Consultants
9. BYOS — Consultant-brought supplier connections (e.g., their own Hotelbeds/Stuba credentials)
10. Yield/Markup management (per-Consultant, per-product)
11. Wallet / credit-limit / commission ledger for Consultants
12. Multi-language, multi-currency support
13. Consultant dashboard and reporting
14. Stripe payment integration
15. Notifications (email, WhatsApp/SMS) and cancellation management
16. Inventory sync across all connected suppliers
17. PNR search
18. **Meta Ads Campaign module** — Adren-managed ad accounts/Business Managers, campaign launch for Consultant packages, AI-generated creative variants, spend caps and approval workflow

### Explicitly out of scope for MVP (flagged for future consideration, not built now)
- Sub-agent hierarchy beyond one level (Consultant → User); multi-tier franchise networks
- BNPL/instant credit-line fintech product
- Cross-Consultant marketplace sharing of Local DMC inventory
- Native mobile apps (MVP is responsive web)
- Visa and travel insurance as bookable products (flagged as a near-term addition, not MVP-blocking)

---

# 5. System Architecture Overview (High-Level)

```
                         ┌─────────────────────────┐
                         │      Super Admin Console │
                         │  (branding, suppliers,   │
                         │   ad account provisioning)│
                         └────────────┬─────────────┘
                                      │
        ┌─────────────────────────────┴─────────────────────────────┐
        │                     ADREN CORE PLATFORM                    │
        │                                                             │
        │  ┌───────────────┐   ┌───────────────┐   ┌───────────────┐ │
        │  │ Booking Engine │   │   AI Layer     │   │  Payments &   │ │
        │  │ (search,       │◄──┤ (itinerary gen,│   │  Wallet Layer │ │
        │  │  itinerary,    │   │  governance,   │   │  (Stripe,     │ │
        │  │  package,      │   │  ad creative)  │   │  markup,      │ │
        │  │  quotation)    │   └───────────────┘   │  commission)  │ │
        │  └───────┬────────┘                       └───────────────┘ │
        │          │                                                   │
        │  ┌───────▼────────┐   ┌───────────────┐   ┌───────────────┐ │
        │  │ Supplier API    │   │ Notifications  │   │  Ads/Campaign  │ │
        │  │ Integration     │   │ (email/WhatsApp│   │  Module (Meta) │ │
        │  │ Layer           │   │  /SMS)         │   └───────────────┘ │
        │  └───────┬────────┘   └───────────────┘                      │
        └──────────┼─────────────────────────────────────────────────┘
                    │
   ┌────────────────┴──────────────────────────────────────────┐
   │  Hotelbeds │ STUBA │ TBO │ Mystifly │ Transferz │ Widgety   │
   │  HBActivities │ Local DMCs (per Consultant) │ BYOS sources  │
   └───────────────────────────────────────────────────────────┘

                    Consultant White-Label Layer
        (custom domain, logo, colors, background per Consultant)
                                │
                        End Traveler (indirect)
```

Engineering will translate this into a detailed technical architecture document separately; this PRD defines functional boundaries only.

---

# 6. Roles & Permissions Matrix

| Capability | Super Admin | Consultant | User |
|---|:---:|:---:|:---:|
| Onboard Consultants | ✅ | ❌ | ❌ |
| Configure white-label domain/branding | ✅ | View only | ❌ |
| Add/manage supplier API credentials | ✅ | ❌ | ❌ |
| Provision Meta ad account/Business Manager | ✅ | ❌ | ❌ |
| Search & build itinerary | ✅ (test mode) | ✅ | ✅ |
| Use AI itinerary builder | ✅ | ✅ | ✅ |
| Approve AI-generated itinerary before sending to traveler | N/A | ✅ | ✅ (if permitted by Consultant) |
| Create package | ✅ | ✅ | ❌ (unless granted) |
| Set markup/yield rules | ✅ (platform-wide defaults) | ✅ (own account) | ❌ |
| Onboard Local DMC (Localism) | ❌ | ✅ | ❌ |
| Add BYOS supplier connection | ❌ | ✅ | ❌ |
| Add/manage Users under own account | ❌ | ✅ | ❌ |
| View own wallet/credit ledger | ✅ (all) | ✅ (own) | ❌ |
| Make booking | ✅ (test) | ✅ | ✅ |
| Generate voucher/quotation | ✅ | ✅ | ✅ |
| Launch Meta campaign for a package | ❌ (executes on Consultant's behalf) | ✅ (request/approve) | ❌ |
| View AI governance/audit logs | ✅ (all) | ✅ (own) | ❌ |
| Cancel/refund booking | ✅ | ✅ (own bookings) | Request only, Consultant approves |

---

# 7. Assumptions, Dependencies, Risks

**Assumptions**
- Consultants have valid business registration and market-specific licensing sufficient for KYC — the specific requirement varies by home market (see Section 19): ABN (Australia), Companies House number + ATOL where applicable (UK), EIN/state Seller of Travel registration where applicable (USA), DTCM trade license (Dubai/UAE), CVR number (Denmark).
- Supplier APIs (Hotelbeds, STUBA, TBO, Mystifly, Transferz, Widgety, HBActivities) provide sandbox/test access during build, and their inventory depth is confirmed sufficient across India (home market) and all five target expansion markets — general market research indicates all are globally operating suppliers, but destination-strength verification per market should happen during technical due diligence.
- Meta Business API access and ad account provisioning-at-scale is technically and commercially feasible under Adren's own umbrella account structure, across India and all five target markets' regional ad policies.
- Stripe (or equivalent) can process settlement in **INR (compulsory home-market currency)**, AUD, GBP, USD, AED, and DKK, and supports the region-specific payment preferences noted in Section 19.

**Dependencies**
- Third-party supplier API uptime and rate limits directly affect booking engine reliability.
- Meta policy compliance (ad content, billing) is an external dependency outside Adren's control — changes to Meta's API/policy can affect the Ads module without notice, and policy interpretation can vary by target-market region.
- Stripe account setup and regional payment compliance across India and all five target markets, including local payment method support (e.g., UPI in India, MobilePay in Denmark) where required.
- Legal/compliance review per market — India's own travel trade/consumer protection and GST/TCS requirements, plus UK ATOL & Package Travel Regulations 2018, EU/UK GDPR, Danish Package Travel Act, Australian ATAS & Privacy Act, US state-level Seller of Travel laws, UAE data protection law — see Section 19.

**Risks**
- **Ad account liability**: Since Adren manages Meta ad accounts and billing on behalf of Consultants, disputes over spend, policy violations by Consultant-submitted content, or account suspension are a direct operational and financial risk to Adren, now multiplied across India and five expansion markets' ad policy nuances. Mitigation: mandatory spend caps, creative approval workflow, and clear Consultant-facing terms of service before campaign launch.
- **AI hallucination in itinerary/pricing**: Mitigated via governance framework in Section 11, but residual risk remains if supplier data is stale or incomplete.
- **Duplicate inventory across suppliers** (e.g., same hotel available via both Hotelbeds and TBO) causing pricing confusion — requires normalization logic (Section 10).
- **Local DMC quality inconsistency** — unvetted Localism suppliers could damage traveler trust; mitigated via onboarding vetting workflow.
- **Regulatory non-compliance risk (global expansion)**: Operating across six jurisdictions (India plus five expansion markets) with different package-travel, data-protection, tax, and travel-agent licensing regimes creates real legal exposure if the platform doesn't enforce market-specific rules (e.g., failing to generate an ATOL certificate for a UK dynamic package, mishandling EU traveler data under GDPR, or misapplying India GST/TCS on outbound packages). Mitigation: Section 19 compliance logic must be enforced at the platform level, not left to Consultant discretion.
- **Currency/FX exposure**: Settling in six currencies (INR plus AUD, GBP, USD, AED, DKK) against suppliers who themselves invoice in EUR/USD/GBP creates FX risk between rate confirmation and settlement. Mitigation: currency buffer logic in Section 12.2.
- **Support coverage gap**: Six markets span nearly every timezone band (IST, AEST, GMT/BST, EST/PST, GST, CET); without planned coverage, Consultants in some regions may have no live support during their business hours.

---

# 8. Release Plan (Within MVP)

Even though everything is scoped into Phase 1/MVP per business direction, build sequencing still matters for engineering. Recommended internal build order (not a scope cut, purely a sequencing recommendation):

1. **Foundation**: Roles, white-label setup, supplier credential management, core search/itinerary builder
2. **Booking core**: Package creation, quotation, voucher, booking flow across all 5 product types
3. **Financial layer**: Yield/markup, wallet/credit ledger, Stripe integration
4. **AI layer**: AI itinerary generation + governance framework
5. **Local DMC + BYOS**: Onboarding and vetting workflows
6. **Ads/Campaign module**: Meta account provisioning, campaign launch, AI creative generation
7. **Hardening**: Notifications, PNR search, reporting dashboards, inventory sync tuning

This sequencing lets financial and booking-critical paths stabilize before the higher-risk Ads module is wired in — reducing the chance that ad-account issues block core booking revenue at launch.

---

# 9. Module: Core Booking Engine

## 9.1 User Flows

**Flow A — Itinerary Creation (Target: ≤10 minutes)**
1. Consultant/User logs in → lands on Search Dashboard.
2. Enters multi-location search (e.g., "Goa, Udaipur, Jaipur") in a single search box supporting multi-select.
3. Enters dates, pax count, and traveler preferences (optional).
4. Clicks Search → system geocodes each location and displays a map with pins for each destination.
5. For each location, system pre-selects one default product per category (Hotel, Activity, Transfer, Transport, Cruise where applicable) based on the Default Selection Algorithm (Section 9.2).
6. Consultant/User can accept defaults or swap any product via a side panel showing alternate options per location.
7. AI layer (Section 11) can be invoked at any point to auto-complete the remaining itinerary based on budget/preferences.
8. Once finalized, itinerary is saved as a **Quotation**.
9. Quotation can be converted to a **Package** (reusable, sellable to multiple travelers) or directly to a **Booking**.

**Flow B — Package Creation**
1. Consultant selects a saved itinerary/quotation → "Convert to Package."
2. Sets package name, validity dates, pricing (base + markup), and traveler capacity.
3. Publishes package — becomes visible to their Users for direct booking, and becomes eligible for Meta campaign promotion (Section 14).

**Flow C — Direct Booking (User)**
1. User searches available Packages or builds a custom itinerary (if permitted).
2. Selects traveler(s), enters traveler details.
3. Reviews price breakdown (net + markup, shown per Consultant's configured visibility rules).
4. Proceeds to payment (Stripe) or on-account/wallet billing.
5. Booking confirmed → voucher auto-generated → notification sent (email/WhatsApp/SMS).

## 9.2 Default Selection Algorithm (for auto-selected products)

When multiple locations are searched and the system must pre-select one default product per category, the following priority order applies (configurable by Super Admin as a platform default, overridable by Consultant):

1. **Availability** — must be confirmable in real time; unavailable options are excluded first.
2. **Consultant's configured priority** — if a Consultant has set a preferred supplier or margin target, that takes precedence.
3. **Best margin** — among available, confirmable options, the one yielding the highest margin under the Consultant's markup rules is pre-selected by default.
4. **Rating/quality score** — used as a tiebreaker when margins are equal.

This logic must be explicitly surfaced to the Consultant/User in the UI (e.g., "Auto-selected: Best available match") so it doesn't appear as a hidden or opaque decision.

## 9.3 Data Model (Key Entities — Field-Level Summary)

**Itinerary**
| Field | Type | Notes |
|---|---|---|
| itinerary_id | UUID | Primary key |
| consultant_id | UUID | Owner |
| locations[] | Array | Ordered list of destinations |
| date_range | Date/Date | Start/end |
| pax_count | Integer | Adults/children breakdown |
| line_items[] | Array of references | Hotel/Transport/Transfer/Cruise/Activity references |
| currency | Enum | Base currency for pricing |
| status | Enum | Draft / Quotation / Booked / Cancelled |
| ai_generated | Boolean | Flag if AI-assisted |
| created_at / updated_at | Timestamp | |

**Line Item (Hotel example)**
| Field | Type | Notes |
|---|---|---|
| supplier_id | Enum | Hotelbeds / STUBA / TBO / Local DMC / BYOS |
| supplier_rate_id | String | Supplier's internal rate reference |
| property_name | String | |
| room_type | String | |
| meal_plan | Enum | RO/BB/HB/FB/AI |
| cancellation_deadline | Timestamp | Timezone-aware |
| net_rate | Decimal | Supplier net cost |
| markup_applied | Decimal | Per Consultant rule |
| sell_rate | Decimal | net + markup |
| currency | Enum | |
| fx_rate_snapshot | Decimal | Rate at time of quote, locked |

**Package**
| Field | Type | Notes |
|---|---|---|
| package_id | UUID | |
| source_itinerary_id | UUID | |
| name / description | String | |
| validity_start / end | Date | |
| base_price | Decimal | |
| markup_price | Decimal | |
| max_pax | Integer | |
| promoted_via_ads | Boolean | Links to Ads module |

**Booking, Quotation, Traveler Profile, Voucher** follow similar structured field-level tables (to be expanded in the Engineering Data Dictionary appendix during build).

## 9.4 Business Rules & Edge Cases

| Scenario | Expected Behavior |
|---|---|
| No supplier has availability for a searched location | System shows "No inventory available" for that location/category, does not silently drop it; AI layer must not fabricate an option. |
| Two suppliers offer the same physical hotel | Deduplicated using property-matching logic (name + geocode + star rating); the one selected follows the Default Selection Algorithm. |
| Currency mismatch between Consultant's base currency and supplier's native currency | FX conversion applied at time of quote, rate snapshot locked to the quotation to avoid drift before booking confirmation. |
| Partial cancellation (e.g., one traveler out of a group) | Recalculates pro-rated refund per supplier's cancellation policy; does not cancel the entire booking. |
| Supplier overbooking discovered post-confirmation | Triggers an alert workflow to Consultant + Super Admin with suggested alternate inventory; not a silent failure. |
| AI itinerary produces zero valid results for a location | AI must explicitly state it could not complete that segment rather than substituting an unrelated product. |

## 9.5 Reporting & Dashboard Spec

**Consultant Dashboard**
- Bookings this month (count, GMV)
- Top-performing packages
- Wallet/credit balance
- Pending quotations awaiting traveler confirmation
- Active Meta campaigns and performance snapshot (bookings sourced from ads)

**Super Admin Dashboard**
- All-Consultant GMV and growth
- Supplier performance (uptime, booking success rate per supplier)
- AI governance summary (approval rate, flagged incidents)
- Ad spend across all Consultants, account health status

## 9.6 Non-Functional Requirements
- Itinerary generation (search → pre-filled map view): target ≤ 10 minutes end-to-end for a Consultant/User, inclusive of manual adjustments.
- Search response time: supplier aggregation results returned within an agreed SLA (to be set with engineering, typically low single-digit seconds for cached/normalized inventory).
- System uptime: 99.5%+ for core booking engine.
- All monetary calculations use decimal-safe arithmetic (no floating-point rounding errors) given multi-currency markup stacking.

---

# 10. Module: Supplier & Inventory Integration

## 10.1 Supplier Overview

| Supplier | Product | Integration Type | Notes |
|---|---|---|---|
| Hotelbeds | Hotels | API (bedbank) | Primary hotel wholesaler |
| STUBA | Hotels | API | Secondary hotel source, used for normalization/rate comparison |
| TBO | Hotels | API | Tertiary hotel source, common in India market |
| Mystifly | Transport (Flights) | API | LCC + GDS-style fare aggregation |
| Transferz | Transfers | API | Airport/point-to-point transfers |
| Widgety | Cruise | API | Cruise inventory |
| HBActivities | Activities | API | Tours/experiences |
| Local DMC (Localism) | Hotels/Transfers/Activities | Manual onboarding + Consultant-managed | Requires vetting workflow (10.3) |
| BYOS | Any product | Consultant-provided API credentials | Consultant brings own supplier account (e.g., their own Hotelbeds contract) |

## 10.2 Per-Supplier Integration Requirements (Summary — full field mapping to be an Engineering Appendix)

For each API-connected supplier, the integration must define:
- **Authentication method** (API key, OAuth, IP whitelisting per supplier's requirement)
- **Search request/response mapping** into Adren's normalized line-item schema (Section 9.3)
- **Error handling table** — timeout, no availability, rate expired, invalid request — each with a defined user-facing message and retry/fallback behavior
- **Rate limit handling** — queuing/backoff strategy so one supplier's limits don't degrade overall search performance
- **Booking confirmation & cancellation API mapping**
- **Sync frequency** — real-time for search, scheduled sync for static content (images, amenities, descriptions)

## 10.3 Local DMC (Localism) Onboarding & Vetting Workflow

1. Consultant submits Local DMC details (business info, product categories offered, sample rates, references).
2. Adren (or delegated Consultant-level review, per final business decision) reviews for basic legitimacy — business registration, contactability.
3. Local DMC is marked **Pending → Active** only after at least one verification step is complete.
4. Local DMC inventory is manually entered or bulk-uploaded by the Consultant (template-based) since no live API exists for most local suppliers.
5. Ongoing quality signal: booking cancellation rate and traveler complaint flagging tied to the Local DMC record, visible to the onboarding Consultant and Super Admin.

## 10.4 BYOS (Bring Your Own Supplier)

- Consultant enters their own supplier API credentials (e.g., their own Hotelbeds contract).
- Credentials stored encrypted, scoped only to that Consultant's account — never visible to other Consultants or Users beyond permission scope.
- BYOS inventory is merged into search results alongside Adren's own supplier connections, following the same normalization and Default Selection Algorithm.

## 10.5 Inventory Sync
- Real-time availability/pricing calls at time of search (no stale pricing shown to traveler).
- Scheduled sync (e.g., nightly) for static content: property images, descriptions, amenities — to reduce API load.
- Sync failure alerting to Super Admin if any supplier integration goes stale beyond a defined threshold.

---

# 11. Module: AI Itinerary & Governance

## 11.1 AI Capabilities in Scope
- AI-assisted itinerary generation from natural-language or structured input (e.g., "5-day Goa + Udaipur trip, mid-budget, 2 adults").
- AI-assisted completion of partially built itineraries (fill remaining locations/categories).
- AI-generated ad creative variants for Meta campaigns (Section 14).

## 11.2 Governance Framework ("AI should not hallucinate")

This is a trust-critical requirement and is treated as a first-class module, not a feature flag.

**Core principles:**
1. **Grounded generation only** — AI may only select from live, supplier-confirmed inventory returned by the Booking Engine's search layer. It must never generate a hotel name, price, or availability claim that isn't backed by an actual supplier response.
2. **Confidence and availability indicators** — every AI-suggested line item is shown with its source supplier and a real-time availability confirmation status, not presented as a static suggestion.
3. **Mandatory human-in-the-loop approval** — no AI-generated itinerary is sent to a traveler without explicit Consultant/User review and approval. This is enforced at the workflow level, not just a UI suggestion.
4. **Explicit failure states** — if AI cannot confidently complete a segment (e.g., no inventory matches the budget), it must say so rather than substituting an unrelated or out-of-budget option.
5. **Full audit logging** — every AI suggestion is logged with: input prompt, supplier data used, output shown, and whether it was accepted/edited/rejected by the Consultant/User. This log is queryable by Super Admin and by the owning Consultant.

## 11.3 Acceptance Criteria (Sample)
- Given a search with zero available suppliers for a location, when AI is invoked, then AI must explicitly state "no inventory available" for that segment and must not propose a placeholder.
- Given an AI-generated itinerary, when a Consultant reviews it, then every line item must display its supplier source and live availability status before it can be approved.
- Given an approved AI itinerary is later found to reference stale pricing (supplier price changed between generation and approval), then the system must re-validate price at booking time and flag any discrepancy before payment.

---

# 12. Module: Payments, Yield/Markup & Wallet

## 12.1 Markup & Commission Engine
- Markup configurable by Consultant, per product category (Hotel/Transport/Transfer/Cruise/Activity), with support for percentage-based or flat-fee markup.
- Commission structure (what Adren pays/charges Consultant) is tracked separately from markup — markup is Consultant's own margin on top of net rate; commission is Adren's platform take, defined per commercial agreement tier.
- **Worked example:**
  - Supplier net rate: $10,000 (supplier's invoicing currency, e.g. EUR from Hotelbeds)
  - Consultant markup (15%): $1,500 → Sell rate to traveler: $11,500 (in Consultant's configured sell currency, e.g. AUD/GBP/USD/AED/DKK)
  - Adren platform commission (on net rate, e.g. 5%): $500, deducted from Consultant's payout
  - Consultant's net earning: $1,500 (markup) − applicable platform commission structure per agreement

## 12.2 Multi-Currency & FX Buffer (India + Global Markets)
- Adren operates across **six settlement currencies: INR (compulsory — Adren's home-market currency), AUD, GBP, USD, AED, DKK**. Suppliers themselves invoice in their own currencies (Hotelbeds typically EUR, TBO typically USD/EUR/INR, Stuba often GBP-capable) — meaning every booking carries at least one currency conversion between supplier net rate and Consultant sell rate.
- INR is treated as the default/base currency for the platform's home-market operations (India-based Consultants, and Adren's own internal reporting currency), with AUD/GBP/USD/AED/DKK as the five additional settlement currencies for expansion-market Consultants.
- A **currency buffer of 2–5% above the commercial markup** is applied at quote time to protect against exchange-rate movement between rate confirmation and final settlement. This buffer is configurable per Consultant/market by Super Admin, not a fixed global constant.
- FX rate is snapshotted and locked at the point of quotation (per the itinerary data model, Section 9.3) so the traveler-facing price does not drift before booking confirmation.
- Local payment method support beyond card/Stripe default should be evaluated per market — **UPI is the dominant preference in India** and should be treated as a core payment method, not optional; **MobilePay for Denmark** is similarly important; card and bank transfer remain standard for UK/US/Australia, and Dubai retains a meaningful card/cash mix.

## 12.3 Wallet & Credit Limit
- Each Consultant has a wallet with: available balance, credit limit (Super Admin configured), pending holds (for bookings in process), denominated in the Consultant's home-market currency.
- Booking confirmation places a hold on the wallet/credit line; hold is released or converted to a debit on final confirmation.
- Credit limit breach: booking cannot be confirmed until Consultant tops up or clears outstanding balance — system must show a clear, actionable message, not a silent failure.

## 12.4 Stripe Integration
- Payment collection from traveler (where Consultant chooses to route payment through Adren) or on-account billing to Consultant's wallet — across INR, AUD, GBP, USD, AED, DKK settlement.
- Refund and credit-note workflow tied to supplier cancellation policy — partial and full refund paths both required.
- Reconciliation report: matches Stripe transactions against bookings/wallet ledger entries for finance audit, per currency and per market.

## 12.5 Cancellation & Dispute Handling
- Cancellation initiated by Consultant/User → system checks supplier cancellation policy → calculates refund/penalty → routes for Consultant approval if penalty applies → processes refund via Stripe/wallet credit.
- Dispute flagging: if a Consultant disputes a supplier's cancellation penalty, a ticket is created and tracked to resolution, not just an email handoff.

---

# 13. Module: White-Label & Admin Console

## 13.1 Consultant Onboarding (Super Admin) — Per-Market KYC

Onboarding is no longer a single generic form — the Super Admin console must present the correct document/license checklist based on the Consultant's declared home market, per the table below (full detail in Section 19).

1. Super Admin creates Consultant account, selects the Consultant's home market (**India** / Australia / UK / USA / Dubai-UAE / Denmark), and the console presents the matching KYC checklist:

| Market | Core KYC Requirement |
|---|---|
| **India (compulsory home market)** | **GST registration, business PAN, IATA/TAAI number where applicable, bank details for payout — mandatory for every India-based Consultant, not optional** |
| Australia | Business registration (ABN), ATAS accreditation (if applicable), bank details for payout |
| UK | Companies House registration number, ATOL license (mandatory if Consultant will sell dynamic flight+hotel packages), bank details |
| USA | EIN / business registration, state-level Seller of Travel registration (required in CA, FL, WA, HI, IA — checked against Consultant's declared state), bank details |
| Dubai / UAE | DTCM (Dubai Department of Tourism) trade license, bank details |
| Denmark | CVR registration number, bank details |

2. Assigns Consultant to a white-label domain (custom domain or Adren-provided subdomain).
3. Configures supplier credentials the Consultant will have access to (or enables Consultant to add their own via BYOS).
4. Sets initial credit limit and commission tier, denominated in the Consultant's home-market currency.
5. If the Consultant's market carries an additional compliance flag (e.g., UK ATOL, EU/UK GDPR data handling), the system marks the account accordingly so downstream modules (package creation, Ads) enforce the right behavior automatically — see Section 19.

## 13.2 Branding Configuration
- Logo upload
- Background image upload for Consultant's domain
- Primary/secondary text color selection
- Domain mapping (custom domain CNAME setup, guided by Super Admin)

## 13.3 Multi-Language & Multi-Currency
- Language pack selection per Consultant domain (traveler-facing content). India (Hindi + regional language consideration alongside English) is the home-market baseline; all five expansion markets are English-primary for B2B trade, with Danish as the one clear secondary-language candidate for Denmark. Arabic is not required for Dubai B2B (English is the standard trade language), but can be considered for future B2C-facing expansion.
- Base currency per Consultant — **INR (compulsory default for India-based Consultants)**, or AUD, GBP, USD, AED, DKK for expansion-market Consultants — with per-booking currency conversion and FX buffer as covered in Section 12.
- Currency/language do not affect backend supplier communication — normalization happens at the presentation layer.

---

# 14. Module: Ads/Campaign Management (Meta) — MVP Scope

## 14.1 Overview
Adren manages Meta ad accounts and Business Managers centrally on behalf of Consultants. Consultants do not need to create or manage their own ad account or Business Manager — they provide only basic business information, and Adren handles provisioning, billing infrastructure, and campaign execution tooling.

**Global markets note:** Campaign targeting, ad spend billing currency, and Meta ad policy interpretation all vary by the Consultant's home market (Australia, UK, USA, Dubai/UAE, Denmark). Ad accounts should be structured so each Consultant's campaigns bill in their home-market currency (Section 12.2) and target audiences within their configured destination markets, with Super Admin policy review accounting for regional ad-content rules (e.g., certain claims restricted differently in EU/UK vs. USA vs. UAE).

## 14.2 Flow
1. Consultant creates a Package (Section 9) and opts to "Promote this Package."
2. Consultant provides basic campaign inputs: target audience description, budget, campaign duration, destination market.
3. AI layer generates multiple ad creative variants (images/copy) based on the package's itinerary content and imagery.
4. Consultant reviews and selects/approves creative variant(s) — **mandatory approval step**, no auto-publish without Consultant sign-off.
5. Campaign is submitted for **Super Admin brand-safety and policy review** before going live (ensures compliance with Meta's advertising policies and Adren's own brand standards).
6. Once approved, campaign launches under Adren-managed ad account, tagged to the Consultant and Package for attribution.
7. Performance data (impressions, clicks, cost-per-booking) flows back into the Consultant Dashboard (Section 9.5).

## 14.3 Controls & Guardrails (Mandatory — Not Optional)
- **Spend caps** — hard budget ceiling per campaign, configurable by Super Admin as a platform-wide safety limit and by Consultant within that ceiling.
- **Approval workflow** — no campaign goes live without both Consultant creative approval and Super Admin policy review.
- **Brand-safety review of AI-generated creatives** — checks for pricing accuracy (must match live package price), prohibited claims, and Meta ad policy compliance (e.g., no misleading discount claims) before submission.
- **Billing transparency** — Consultant sees exact spend, platform management fee (if applicable), and performance metrics; no hidden charges.
- **Account suspension handling** — defined escalation process if Meta suspends an ad account or flags a campaign, including Consultant notification and remediation steps.

## 14.4 AI Creative Generation
- Generates multiple variants (image + copy combinations) per package, pulling from itinerary imagery and package details already in the system (not freely generated stock content unrelated to the actual package).
- All generated pricing/claims in ad copy must be validated against the live Package price at time of generation — no stale or inflated pricing claims.

## 14.5 Acceptance Criteria (Sample)
- Given a Consultant submits a campaign request, when AI generates creatives, then every price mentioned in ad copy must match the current live Package price exactly.
- Given a campaign exceeds its configured spend cap, when the cap is reached, then the campaign must pause automatically and notify the Consultant — it must not continue spending past the cap.
- Given a campaign is rejected in Super Admin policy review, then the Consultant must receive a clear reason and be able to resubmit with corrections.

## 14.6 Non-Functional / Compliance Notes
- All ad account provisioning must comply with Meta's Business API terms for managed/agency-style account structures.
- Financial liability for ad spend sits with Adren's managed account structure — billing terms with Consultants (prepay, wallet deduction, or invoiced) must be explicitly defined in the commercial agreement, referenced but not detailed in this PRD.

---

# 15. Notifications & Cancellation Management

- **Channels:** Email (all transactional, all markets), with secondary channel **configurable per Consultant region** rather than one global default — WhatsApp is the natural secondary channel for Dubai/UAE, while SMS is more standard for UK/US/Australia/Denmark. Super Admin sets the regional default; Consultant can override per their own client base.
- **Trigger events:** Booking confirmed, payment received, cancellation processed, refund issued, AI itinerary ready for approval, campaign approved/live/paused, credit limit threshold reached.
- **Cancellation workflow:** as detailed in Section 12.5 — policy-aware, partial-cancellation capable, dispute-trackable.

---

# 16. PNR Search

- Consultants/Users can search existing bookings by PNR (or Adren's internal booking reference) to retrieve full itinerary, traveler, and payment status without needing to know which product category it falls under.
- Search must work across all product types (flights via Mystifly, hotels, transfers, cruise, activities) from a single search field.

---

# 17. Module: Regional Compliance & Localization

This section consolidates the market-specific rules referenced throughout the document (Sections 1, 7, 12, 13, 14, 15) into one place engineering and legal can review together. This is now a first-class module, not a footnote, given the platform operates across six distinct regulatory regimes — **India as the compulsory home market**, plus five expansion markets.

## 17.1 Market-by-Market Requirements

| Market | Package Travel / Licensing | Data Protection | Consultant KYC | Currency | Notes |
|---|---|---|---|---|---|
| **India (compulsory home market)** | Travel trade generally self-regulated (IATA/TAAI accreditation common but not universally mandatory); GST applicable on services, TCS (Tax Collected at Source) applies on outbound tour packages above notified thresholds | India's Digital Personal Data Protection Act (DPDP) | GST registration, business PAN, IATA/TAAI where applicable | **INR** | Base/default currency and market for the platform; every Consultant onboarding flow must support this market fully regardless of expansion-market scope |
| **UK** | Package Travel Regulations 2018 — **ATOL license (CAA)** required if Consultant combines flight + accommodation into a dynamic package. Platform must auto-generate ATOL certificates and display PTR 2018 pre-contractual info. | UK GDPR | Companies House number, ATOL number | GBP | VAT under TOMS (Tour Operators Margin Scheme) affects markup-based VAT calculation |
| **Denmark** | Danish Package Travel Act (EU-aligned) | EU GDPR — traveler PII may need EU data residency | CVR number | DKK | MobilePay is a dominant local payment method — evaluate support alongside Stripe |
| **Australia** | No single national licensing law, but **ATAS accreditation** is the recognized industry trust mark | Australian Privacy Act | ABN, ATAS (where applicable) | AUD | Australian Consumer Law applies to consumer-facing claims in packages/ads |
| **USA** | No federal package-travel law; **state-level Seller of Travel registration** required in California, Florida, Washington, Hawaii, Iowa — checked against Consultant's declared state | State + federal privacy rules (varies) | EIN/business registration, state Seller of Travel number where applicable | USD | Multi-state compliance logic needed if Consultants operate across state lines |
| **Dubai / UAE** | DTCM (Dubai Department of Tourism) trade license required for Consultants | UAE data protection law | DTCM license number | AED | English is standard B2B trade language; card/cash payment mix still common |

## 17.2 Platform Enforcement Requirements
- **ATOL/PTR 2018 (UK)**: Package creation flow (Section 9) must detect when a UK Consultant is combining flight + accommodation and trigger mandatory ATOL certificate generation and PTR 2018 disclosure display before the package can be published — this cannot be an optional checkbox.
- **Data residency (EU/UK)**: Traveler PII for Denmark/UK-based bookings should be evaluated for EU/UK data residency hosting requirements under GDPR — an infrastructure decision for engineering, not just a policy statement.
- **US state-level licensing**: Consultant onboarding (Section 13.1) must capture the Consultant's operating state(s) and flag Seller of Travel registration requirements where applicable, rather than assuming a single national rule.
- **Currency/VAT calculation**: UK TOMS VAT logic sits alongside the standard markup engine (Section 12.1) as a market-specific calculation layer — flagged for finance/legal sign-off before build.

## 17.3 Support & Operations Coverage
- Six markets span nearly every timezone band: **IST (India, UTC+5:30)**, AEST (Australia), GMT/BST (UK), EST/PST (USA), GST (Dubai, UTC+4), CET (Denmark, UTC+1).
- Recommend defining support-desk coverage windows per region now (e.g., follow-the-sun coverage or defined regional business hours) rather than assuming a single global support shift — to be confirmed with Operations before build.

## 17.4 Open Compliance Questions (Flagged for Legal Review)
- Whether Adren itself (vs. each individual Consultant) needs any of the above licenses (e.g., does Adren need its own ATOL or DTCM registration given its role in managing bookings/ads) — this depends on the exact legal structure of the Consultant relationship and should be reviewed with counsel before launch.
- Whether Ads module targeting/claims review (Section 14) needs per-market legal sign-off on ad copy templates, given Australian Consumer Law and UK/EU advertising standards differ from US norms.

---

# 18. Glossary

| Term | Definition |
|---|---|
| Consultant | Adren's direct B2B customer — a travel agent/agency licensing the platform |
| User | Staff/sub-agent operating under a Consultant's account |
| Localism | Local DMC inventory manually onboarded by a Consultant |
| BYOS | Bring Your Own Supplier — Consultant's own supplier API credentials |
| Yield/Markup | Margin a Consultant applies on top of supplier net rate |
| PNR | Passenger Name Record / internal booking reference used for retrieval |
| Net Rate | Supplier's base cost before markup |
| Sell Rate | Final price shown to traveler (net + markup) |
| ATOL | Air Travel Organiser's Licence — UK CAA license required for dynamic flight+accommodation packages |
| ATAS | Australian industry accreditation scheme for travel agents |
| DTCM | Dubai Department of Tourism and Commerce Marketing — issues Dubai travel trade licenses |
| TOMS | Tour Operators Margin Scheme — UK VAT calculation method for margin-based travel sales |

---

# 19. Open Items for Business Confirmation

- Exact commission tiers and how they interact with Consultant markup (Section 12.1) — needs finance sign-off.
- Whether Local DMC vetting (Section 10.3) is Adren-reviewed or Consultant self-certified.
- Ad spend billing model — prepay vs. wallet deduction vs. invoiced (Section 14.6), now per settlement currency.
- Sub-agent hierarchy depth beyond Consultant → User (flagged as out of MVP scope in Section 4, confirm before build lock).
- Whether Adren itself requires market-specific licensing (Section 17.4) — legal review required before launch.
- EU/UK data residency approach for traveler PII (Section 17.2) — infrastructure decision needed from engineering.
- Support coverage model across six timezones spanning India and all expansion markets (Section 17.3) — Operations input needed.

---

*End of Document — v1.0*
