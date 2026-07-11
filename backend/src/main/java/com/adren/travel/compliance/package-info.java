/**
 * Regional Compliance & Localization module (PRD Section 17). Owns the
 * data-driven per-market rules engine (ATOL flag for UK dynamic packages,
 * India GST/TCS calculation, US state-level Seller of Travel checks) — per
 * Section 24.7 NFR, this must be configuration-driven, not hardcoded
 * per-market conditionals scattered across other modules.
 * <p>
 * Scaffold status: package-info only.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Regional Compliance & Localization")
package com.adren.travel.compliance;
