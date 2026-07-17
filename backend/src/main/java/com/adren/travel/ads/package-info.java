/**
 * Ads/Campaign Management module (PRD Section 14). Owns the Meta-managed ad
 * account/Business Manager provisioning, campaign lifecycle (data dictionary
 * 20.13), AI creative generation, and spend-cap enforcement (Section 24.6 —
 * near-real-time, so a campaign never meaningfully overshoots its cap).
 * <p>
 * Scaffold status: AI-12 (grounded ad-creative generation, {@link
 * com.adren.travel.ads.AdsApi#generateAdCreativeForPackage}) is the first
 * real content. Meta account provisioning, campaign lifecycle, and
 * spend-cap enforcement (the ADS-* stories) remain unbuilt.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Ads/Campaign Management")
package com.adren.travel.ads;
