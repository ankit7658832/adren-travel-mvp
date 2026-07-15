-- FND-12 — a Consultant's own BYOS supplier credentials (PRD Section
-- 10.4), row-level KMS-envelope-encrypted (RULES.md Section 5.3), distinct
-- from the shared Secrets-Manager-by-ARN pattern V9 introduced for
-- Adren's own supplier credentials. The plaintext credential is never
-- stored — only the AES-GCM ciphertext, its IV, and the KMS-wrapped data
-- key that decrypts it.
--
-- consultant_id is a foreign key VALUE only, not a constraint across
-- module-owned schemas (RULES.md Section 4.2) — supplier references
-- whitelabel's consultant_id the same way a future payments table would
-- reference itinerary_id, via application-level referential integrity
-- (CurrentPrincipal.resolveTenantScope), not a cross-module FK constraint.

CREATE TABLE byos_credential (
    id                      UUID PRIMARY KEY,
    consultant_id           UUID NOT NULL,
    supplier_id             VARCHAR(30) NOT NULL,
    ciphertext              BYTEA NOT NULL,
    iv                      BYTEA NOT NULL,
    wrapped_data_key        BYTEA NOT NULL,
    last_modified_by_user_id UUID NOT NULL,
    last_modified_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (consultant_id, supplier_id)
);
