package com.adren.travel.payments.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Binds {@code adren.payments.ad-spend.*} from application.yml (PRD §1
 * Executive Summary's managed-service-fee business model, §19, ADS-14) —
 * same off-by-default, config-flag-gated shape as {@code
 * UkTomsVatProperties}/{@code IndiaTaxProperties}, for the same reason:
 * the exact ad-spend billing model is an open item pending business
 * confirmation. {@code managedServiceFeePercent} is a placeholder figure.
 */
@ConfigurationProperties(prefix = "adren.payments.ad-spend")
record AdSpendBillingProperties(boolean enabled, BigDecimal managedServiceFeePercent) {
}
