-- AUTH-01: real, credential-checked login (PRD §6/§13.1) — one row per
-- loginable identity across all three roles. Owned entirely by the
-- security module (RULES.md §4.2) so authentication never requires a
-- cross-module read of another module's own tables; `consultant_id` is a
-- plain UUID reference (not a FK) since SUPER_ADMIN rows never have one.

CREATE TABLE principal_credential (
    credential_id  UUID PRIMARY KEY,
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(20) NOT NULL,
    consultant_id  UUID,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX idx_principal_credential_email ON principal_credential (lower(email));
