package com.adren.travel.payments.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Binds {@code adren.payments.tax.uk-toms.*} from application.yml (PRD
 * §12.1 Worked Example D, §17.2, §19, FIN-18) — same off-by-default,
 * config-flag-gated shape as {@code IndiaTaxProperties}, for the same
 * reason: PRD §19 flags the exact TOMS VAT rate as pending UK tax-counsel
 * sign-off. {@code vatPercent} is the PRD's own illustrative figure (20%),
 * not a confirmed real value.
 */
@ConfigurationProperties(prefix = "adren.payments.tax.uk-toms")
record UkTomsVatProperties(boolean enabled, BigDecimal vatPercent) {
}
