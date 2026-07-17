-- FIN-15: distinguishes a booking-payment PaymentIntent from a wallet
-- top-up PaymentIntent, so the Stripe webhook handler can branch on
-- reconciliation behavior (credit the wallet vs. confirm a booking).
ALTER TABLE payment_intent ADD COLUMN purpose VARCHAR(20) NOT NULL DEFAULT 'BOOKING';
