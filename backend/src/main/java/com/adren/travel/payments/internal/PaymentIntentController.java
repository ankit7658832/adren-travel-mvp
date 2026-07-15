package com.adren.travel.payments.internal;

import com.adren.travel.payments.CreatePaymentIntentCommand;
import com.adren.travel.payments.PaymentIntentView;
import com.adren.travel.payments.PaymentsApi;
import com.adren.travel.shared.Money;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** PRD §12.4 — creates a Stripe PaymentIntent for a booking (FIN-11). */
@RestController
@RequestMapping("/api/v1/payments/payment-intents")
class PaymentIntentController {

    private final PaymentsApi paymentsApi;

    PaymentIntentController(PaymentsApi paymentsApi) {
        this.paymentsApi = paymentsApi;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PaymentIntentView create(@Valid @RequestBody CreatePaymentIntentRequest request) {
        return paymentsApi.createPaymentIntent(new CreatePaymentIntentCommand(request.bookingReferenceId(),
            request.consultantId(), new Money(request.amount(), request.currency())));
    }
}
