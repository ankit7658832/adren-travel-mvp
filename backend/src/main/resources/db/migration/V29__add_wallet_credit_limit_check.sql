-- FIN-08, PRD §22.4 T8: DB-level backstop (not app-level-only, per
-- backend-best-practices §3) — a wallet row can never actually be
-- persisted in a state where pending_holds exceeds available_balance +
-- credit_limit, regardless of what path wrote it.
ALTER TABLE wallet ADD CONSTRAINT chk_wallet_within_credit_limit
    CHECK (available_balance + credit_limit - pending_holds >= 0);
