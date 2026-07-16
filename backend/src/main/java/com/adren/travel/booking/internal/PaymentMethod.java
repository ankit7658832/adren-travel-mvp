package com.adren.travel.booking.internal;

/** Per PRD Section 20.8 — booking.payment_method field. ON_ACCOUNT (FIN-12) is not wired to any confirmation path yet. */
enum PaymentMethod {
    WALLET,
    STRIPE,
    ON_ACCOUNT
}
