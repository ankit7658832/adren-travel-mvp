-- Booking module: Booking (PRD Section 20.8, BOK-19). confirmBooking/
-- confirmBookingFromPaymentWebhook were simplified stubs that never
-- persisted a real Booking row (see V18/V19's own comments) — this is
-- that entity, scoped to what BOK-19 needs (a PNR-searchable reference
-- distinct from any supplier/airline reference) rather than the full
-- Section 20.8 field set (traveler_ids[]/supplier_booking_refs[] are a
-- documented, flagged omission — no story currently populates them).
-- itinerary_id is nullable: the Stripe-webhook confirmation path
-- (confirmBookingFromPaymentWebhook) doesn't resolve it (BOK-16's own
-- scoping note explains why extending that path is a separate change).

CREATE TABLE booking (
    booking_id           UUID PRIMARY KEY,
    itinerary_id         UUID,
    consultant_id        UUID NOT NULL,
    status                VARCHAR(20) NOT NULL,
    total_sell_price      NUMERIC(19,4) NOT NULL,
    total_sell_currency   VARCHAR(10) NOT NULL,
    payment_method        VARCHAR(20) NOT NULL,
    pnr_searchable_ref    VARCHAR(20) NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_booking_pnr_searchable_ref UNIQUE (pnr_searchable_ref)
);

CREATE INDEX idx_booking_consultant_id ON booking (consultant_id);
CREATE INDEX idx_booking_pnr_searchable_ref ON booking (pnr_searchable_ref);
