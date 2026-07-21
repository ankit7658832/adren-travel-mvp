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
-- row at all — Super Admin is a role, not a tenant).

INSERT INTO consultant (consultant_id, business_name, home_market, status, created_at)
VALUES ('00000000-0000-0000-0000-0000000000c1', 'Dev Consultant Co.', 'INDIA', 'ACTIVE', now());

INSERT INTO consultant_kyc_field (consultant_id, field_key, field_value)
VALUES
    ('00000000-0000-0000-0000-0000000000c1', 'gstRegistration', 'DEV-GST-0001'),
    ('00000000-0000-0000-0000-0000000000c1', 'businessPan', 'DEV-PAN-0001'),
    ('00000000-0000-0000-0000-0000000000c1', 'bankDetails', 'DEV-IFSC0001/00000001');
