package com.adren.travel.whitelabel;

import java.util.Map;

/**
 * Cross-module-safe input to {@link WhitelabelApi#onboardConsultant} — a
 * plain value, never a JPA entity (RULES.md §1.4). {@code kycFields} is
 * keyed by {@link KycFieldDefinition#fieldKey()}.
 */
public record OnboardConsultantCommand(String businessName, Market homeMarket, Map<String, String> kycFields) {
}
