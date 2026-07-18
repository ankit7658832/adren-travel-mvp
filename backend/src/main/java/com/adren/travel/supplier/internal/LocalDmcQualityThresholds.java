package com.adren.travel.supplier.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Binds {@code adren.supplier.local-dmc.*} from application.yml (DMC-05) —
 * the thresholds {@link LocalDmcService} flags a Local DMC's record against.
 */
@ConfigurationProperties(prefix = "adren.supplier.local-dmc")
record LocalDmcQualityThresholds(BigDecimal cancellationRateThreshold, int complaintCountThreshold,
                                  int inventoryStalenessThresholdDays) {
}
