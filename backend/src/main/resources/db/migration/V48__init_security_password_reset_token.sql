-- SCR-00b (doc/ADREN_UIUX_SPEC.md S5.2) — a one-time, expiring token
-- issued when a Consultant/User/Super Admin requests a password reset.
-- credential_id is a plain UUID reference (not a FK), same reasoning as
-- principal_credential.consultant_id (V47): security owns this table
-- entirely, no cross-module FK.

CREATE TABLE password_reset_token (
    token_id      UUID PRIMARY KEY,
    credential_id UUID NOT NULL,
    expires_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at       TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_password_reset_token_credential_id ON password_reset_token (credential_id);
