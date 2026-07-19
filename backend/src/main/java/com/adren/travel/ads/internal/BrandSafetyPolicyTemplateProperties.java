package com.adren.travel.ads.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds {@code adren.ads.brand-safety.banned-phrases} from application.yml
 * (PRD §14.3, ADS-15) — a data-driven rule set (RULES.md §24.7's same
 * "configurable, not hardcoded per-case conditionals" reasoning already
 * established for KYC rules), so the phrase list can change independent
 * of a full release cycle rather than living in a Java {@code switch}.
 */
@ConfigurationProperties(prefix = "adren.ads.brand-safety")
record BrandSafetyPolicyTemplateProperties(List<String> bannedPhrases) {
}
