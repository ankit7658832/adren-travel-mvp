-- Payments, Yield/Markup & Wallet module (PRD Section 12.1 — FIN-01).
-- One file per module's first entity, per RULES.md Section 4.2 —
-- module-owned tables only. consultant_id is a foreign key VALUE only,
-- not a constraint across module-owned schemas.

CREATE TABLE markup_rule (
    id                 UUID PRIMARY KEY,
    consultant_id      UUID NOT NULL,
    category           VARCHAR(20) NOT NULL,
    markup_type        VARCHAR(20) NOT NULL,
    percentage_value   NUMERIC(19,4),
    flat_fee_amount    NUMERIC(19,4),
    flat_fee_currency  VARCHAR(10),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (consultant_id, category)
);
