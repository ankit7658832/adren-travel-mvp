-- FND-11 — Secrets Manager now owns the raw credential value; Postgres
-- persists only the ARN Secrets Manager returned (RULES.md Section 5.3).

ALTER TABLE supplier_credential RENAME COLUMN secret_value TO secret_arn;
ALTER TABLE supplier_credential ALTER COLUMN secret_arn TYPE VARCHAR(2048);
