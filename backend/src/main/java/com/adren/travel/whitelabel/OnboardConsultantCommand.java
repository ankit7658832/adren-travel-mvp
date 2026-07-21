package com.adren.travel.whitelabel;

import java.util.Map;

/**
 * Cross-module-safe input to {@link WhitelabelApi#onboardConsultant} — a
 * plain value, never a JPA entity (RULES.md §1.4). {@code kycFields} is
 * keyed by {@link KycFieldDefinition#fieldKey()}.
 *
 * @param email          the new Consultant's own login email (AUTH-01) —
 *                        registered as a real, password-checked credential
 * @param initialPassword the Consultant's initial login password, set by
 *                        the onboarding Super Admin
 */
public record OnboardConsultantCommand(
    String businessName, Market homeMarket, Map<String, String> kycFields, String email, String initialPassword) {
}
