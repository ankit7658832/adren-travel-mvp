-- Payments module: Stripe PaymentIntent tracking (PRD Section 12.4, FIN-11).
-- Keyed by Stripe's own PaymentIntent id since that's the only identifier
-- a webhook payload carries back to us.

CREATE TABLE payment_intent (
    payment_intent_id     VARCHAR(255) PRIMARY KEY,
    booking_reference_id  UUID NOT NULL,
    consultant_id         UUID NOT NULL,
    amount                NUMERIC(19,4) NOT NULL,
    currency              VARCHAR(10) NOT NULL,
    status                VARCHAR(30) NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL
);
