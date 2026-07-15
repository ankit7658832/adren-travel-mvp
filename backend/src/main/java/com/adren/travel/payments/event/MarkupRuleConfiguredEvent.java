package com.adren.travel.payments.event;

import com.adren.travel.shared.ProductCategory;

import java.util.UUID;

/** Published when a Consultant's markup rule for a category is saved (PRD §12.1, FIN-01). */
public record MarkupRuleConfiguredEvent(UUID consultantId, ProductCategory category) {
}
