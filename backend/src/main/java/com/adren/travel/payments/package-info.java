/**
 * Payments, Yield/Markup & Wallet module (PRD Section 12). Owns markup
 * calculation, the FX buffer (Section 12.2), the Consultant wallet/credit
 * ledger (Section 12.3, data dictionary 20.12), and Stripe integration
 * (Section 12.4). Listens to BookingConfirmedEvent from the Booking module
 * to finalize wallet holds/debits.
 * <p>
 * Scaffold status: package-info only.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Payments, Yield/Markup & Wallet")
package com.adren.travel.payments;
