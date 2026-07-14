-- FND-10: Adren-owned supplier API credential management (PRD Section
-- 21.6/10.2). secret_value is a placeholder column FND-11 replaces with
-- an ARN reference once Secrets Manager wiring lands (RULES.md 5.3).

CREATE TABLE supplier_credential (
    supplier_id             VARCHAR(30) PRIMARY KEY,
    secret_value            VARCHAR(500) NOT NULL,
    last_modified_by_user_id UUID NOT NULL,
    last_modified_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Insert-only audit trail — who changed which supplier credential and when,
-- never the secret value itself.
CREATE TABLE supplier_credential_audit_log (
    id               UUID PRIMARY KEY,
    supplier_id      VARCHAR(30) NOT NULL,
    changed_by_user_id UUID NOT NULL,
    changed_at       TIMESTAMP WITH TIME ZONE NOT NULL
);
