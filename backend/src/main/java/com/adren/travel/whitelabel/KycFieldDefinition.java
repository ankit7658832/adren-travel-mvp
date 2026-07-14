package com.adren.travel.whitelabel;

/**
 * One required (or optional) KYC field for a given {@link Market}, per PRD
 * §13.1's per-market table — e.g. India's {@code gstRegistration}. The
 * {@code fieldKey} is what {@link OnboardConsultantCommand#kycFields()} is
 * keyed by.
 */
public record KycFieldDefinition(String fieldKey, String label, boolean required) {
}
