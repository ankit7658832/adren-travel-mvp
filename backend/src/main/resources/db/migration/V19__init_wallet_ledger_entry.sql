-- Payments module: Wallet Ledger Entry (PRD Section 20.12, FIN-07/FIN-10).
-- The UNIQUE constraint on (related_booking_id, type) is FIN-10's
-- idempotency guarantee: Postgres treats NULLs as distinct for
-- uniqueness, so top-up entries (no related booking) never collide with
-- each other under this constraint - only booking-scoped entries
-- (Hold/Debit/Release) are deduplicated by it.

CREATE TABLE wallet_ledger_entry (
    ledger_entry_id     UUID PRIMARY KEY,
    consultant_id       UUID NOT NULL,
    type                VARCHAR(30) NOT NULL,
    amount              NUMERIC(19,4) NOT NULL,
    currency            VARCHAR(10) NOT NULL,
    related_booking_id  UUID,
    balance_after       NUMERIC(19,4) NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_wallet_ledger_entry_booking_type UNIQUE (related_booking_id, type)
);

CREATE INDEX idx_wallet_ledger_entry_consultant_id ON wallet_ledger_entry (consultant_id);
