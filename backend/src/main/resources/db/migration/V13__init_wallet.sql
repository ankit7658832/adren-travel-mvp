-- Payments, Yield/Markup & Wallet module (PRD Section 12.3, data
-- dictionary 20.12 — FIN-06). One row per Consultant, keyed directly by
-- consultant_id since exactly one wallet exists per tenant.

CREATE TABLE wallet (
    consultant_id     UUID PRIMARY KEY,
    available_balance NUMERIC(19,4) NOT NULL,
    credit_limit      NUMERIC(19,4) NOT NULL,
    pending_holds     NUMERIC(19,4) NOT NULL,
    currency          VARCHAR(10) NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL
);
