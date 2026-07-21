-- Local-dev-only seed data. Only ever applied when the `dev` Spring
-- profile is active (see application-dev.yml's extra spring.flyway.locations
-- entry) — the default spring.flyway.locations (application.yml) never
-- includes db/dev-seed, so this migration is invisible to every other
-- profile (test, prod, or no profile at all). V900 is deliberately far
-- outside the main db/migration sequence (currently V1..V46) so it can
-- never collide with a real migration's version number.
--
-- Seeds one fixed-UUID, ACTIVE, India-market Consultant so a developer
-- has a real tenant to authenticate as locally without running the full
-- onboarding flow first. See DevAuthController for how to mint a JWT for
-- this Consultant (or for a SUPER_ADMIN session, which needs no seeded
-- row at all — Super Admin is a role, not a tenant). AUTH-01 adds a real,
-- password-checked login for both: POST /api/v1/auth/login with either
--   dev-consultant@adren.travel / DevPassword1!
--   dev-super-admin@adren.travel / SuperAdminPassword1!
-- (hashes below are BCryptPasswordEncoder.encode(...) of those two
-- passwords — SecurityConfig's own encoder, not a hand-rolled one).

INSERT INTO consultant (consultant_id, business_name, home_market, status, created_at)
VALUES ('00000000-0000-0000-0000-0000000000c1', 'Dev Consultant Co.', 'INDIA', 'ACTIVE', now());

INSERT INTO consultant_kyc_field (consultant_id, field_key, field_value)
VALUES
    ('00000000-0000-0000-0000-0000000000c1', 'gstRegistration', 'DEV-GST-0001'),
    ('00000000-0000-0000-0000-0000000000c1', 'businessPan', 'DEV-PAN-0001'),
    ('00000000-0000-0000-0000-0000000000c1', 'bankDetails', 'DEV-IFSC0001/00000001');

INSERT INTO principal_credential (credential_id, email, password_hash, role, consultant_id, created_at)
VALUES
    ('00000000-0000-0000-0000-0000000000c1', 'dev-consultant@adren.travel',
     '$2a$10$c6I1227Oyin1qJb0CozpG.Ps1XgFqja3.C7e4dms4ud2s39/HFbiS', 'CONSULTANT',
     '00000000-0000-0000-0000-0000000000c1', now()),
    ('00000000-0000-0000-0000-0000000000a1', 'dev-super-admin@adren.travel',
     '$2a$10$7odWddC7UozYMbtb0zsMdegkm4uyq0dOK8JxTzu2F6PwWYrkBln2a', 'SUPER_ADMIN',
     NULL, now());
