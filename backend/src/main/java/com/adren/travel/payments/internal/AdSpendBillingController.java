package com.adren.travel.payments.internal;

import com.adren.travel.payments.AdSpendBillingCalculation;
import com.adren.travel.payments.CalculateAdSpendBillingCommand;
import com.adren.travel.payments.PaymentsApi;
import com.adren.travel.shared.Money;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PRD §1, §19, ADS-14 — an internal billing-pipeline step: calculates the
 * managed-service fee on an ad-spend increment. Not called via {@code
 * @PreAuthorize} inside {@link PaymentsApi#calculateAdSpendBilling} (same
 * internal-pricing-pipeline-step shape as {@code calculateCommission}),
 * unlike every consumer-facing endpoint elsewhere in this codebase.
 */
@RestController
@RequestMapping("/api/v1/ad-spend-billing")
class AdSpendBillingController {

    private final PaymentsApi paymentsApi;

    AdSpendBillingController(PaymentsApi paymentsApi) {
        this.paymentsApi = paymentsApi;
    }

    @PostMapping
    AdSpendBillingCalculation calculate(@Valid @RequestBody CalculateAdSpendBillingRequest request) {
        return paymentsApi.calculateAdSpendBilling(new CalculateAdSpendBillingCommand(
            request.campaignId(), request.consultantId(), new Money(request.spendAmount(), request.currency())));
    }
}
