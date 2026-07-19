package com.adren.travel.payments.event;

import com.adren.travel.shared.Money;

import java.util.UUID;

/** Published whenever an ad-spend increment's managed-service fee is calculated (PRD §1, §19, ADS-14) — the reconciliation point against FIN-06's wallet/ledger. */
public record AdSpendBillingCalculatedEvent(UUID campaignId, UUID consultantId, Money spendAmount, Money feeAmount, boolean applied) {
}
