-- Booking module: Voucher (PRD Section 20.11, BOK-15). booking_id is a
-- plain UUID, not an FK: confirmBooking is still a simplified stub that
-- doesn't persist a real Booking entity, so there's no booking table yet
-- to reference (RULES.md Section 4.2's value-not-constraint guidance
-- applies here too, though for a different reason than usual).

CREATE TABLE voucher (
    voucher_id                 UUID PRIMARY KEY,
    booking_id                 UUID NOT NULL,
    pdf_reference              VARCHAR(500) NOT NULL,
    generated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    atol_certificate_reference VARCHAR(500)
);
