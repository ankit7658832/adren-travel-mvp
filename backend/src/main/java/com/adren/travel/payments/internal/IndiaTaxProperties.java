package com.adren.travel.payments.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Binds {@code adren.payments.tax.india.*} from application.yml (PRD §12.1
 * Worked Example C, §17.2, §19, FIN-17). {@code enabled} defaults false —
 * PRD §19 flags exact GST/TCS rates and mechanics as pending tax-counsel
 * sign-off, so this calculation layer must not silently start charging
 * illustrative rates on real transactions until explicitly turned on
 * (RULES.md's config-flag-gated pattern for every §19 open item).
 * {@code gstPercent}/{@code tcsPercent}/{@code tcsThreshold} are the PRD's
 * own illustrative figures (§12.1 Example C), not confirmed real values.
 */
@ConfigurationProperties(prefix = "adren.payments.tax.india")
record IndiaTaxProperties(boolean enabled, BigDecimal gstPercent, BigDecimal tcsPercent, BigDecimal tcsThreshold) {
}
