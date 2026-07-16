package com.adren.travel.booking.internal;

/** Per PRD Section 20.8 — booking.payment_method field. ON_ACCOUNT is confirmBookingOnAccount's payment method (FIN-12). */
enum PaymentMethod {
    WALLET,
    STRIPE,
    ON_ACCOUNT
}
