---
id: HRD-14
epic: Hardening
phase: mock
status: not-started
story_points: 5
dependencies: ["FND-01", "FND-02", "FND-09"]
labels: ["backend", "frontend", "security", "phase1"]
prd_references: ["§6", "§13.1"]
modules_or_screens: ["security", "whitelabel", "Login Screen — NEW feature folder"]
testing_tiers: ["unit", "module (@ApplicationModuleTest)", "component test"]
---

# HRD-14: Implement a real, credential-checked login screen and endpoint

## Summary (business)
Consultants, their staff Users, and Super Admins can sign in with their own email and password and land straight in their dashboard, instead of the app having no login screen at all. This closes a gap found during the mock-complete Definition of Done walkthrough (`doc/phases.md` §5): every prior stage relied on a local-dev-only, unauthenticated token-minting shortcut (`DevAuthController`, `@Profile("dev")`) because no story in the 149-story mock catalogue ever specified a real login flow.

## User Story
**As a** Consultant, User, or Super Admin, **I want** to sign in with my own email and password, **so that** I reach my dashboard through a real, credential-checked login rather than a local-dev-only shortcut.

## Acceptance Criteria
- Given a Consultant, User, or Super Admin with a real, previously-registered credential, when they submit the correct email and password to `POST /api/v1/auth/login`, then they receive a valid, role-and-tenant-scoped bearer token identical in shape to `DevAuthController`'s (so every existing `@PreAuthorize` expression and `CurrentPrincipal` read keeps working unchanged).
- Given an incorrect password or an unknown email, when login is attempted, then the response is a 401 with a clear "Invalid email or password" detail — never revealing which of the two was wrong.
- Given a Super Admin onboards a new Consultant (`POST /api/v1/consultants`) or a Consultant adds a staff User (`POST /api/v1/users`), when the request includes the new identity's login email and password, then a real, hashed credential is registered in the same transaction — no Consultant or User can ever exist without a way to log in.
- Given the login screen, when a user submits invalid credentials, then a clear inline error is shown and the form remains usable for retry (no dead-end, no silent failure).

## Developer Notes
- **PRD reference(s):** §6 Roles & Permissions Matrix; §13.1 Consultant onboarding
- **Module(s)/Screen(s):** security (new `principal_credential` table/entity, `SecurityApi.registerCredential`, `POST /api/v1/auth/login`), whitelabel (`OnboardConsultantCommand`/`AddUserCommand` extended to carry credential fields), Login Screen — NEW feature folder
- **Story points:** 5 — A new cross-cutting credential store plus one new public endpoint, but no new domain concept beyond what `Role`/`AdrenPrincipal` already model.
- **Dependencies:** FND-01 (JWT infra this reuses), FND-02 (`@PreAuthorize` role matrix this must keep satisfying), FND-09 (Consultant Users — `AddUserCommand`'s extension point)
- **Design decision:** `PrincipalCredential.credentialId` is always the caller-supplied identity's own id (a `ConsultantUser.userId`, or the new `Consultant.consultantId` for the business-owner login) — never a freshly-minted id — so a real login's JWT `userId` claim lines up with `CapabilityGrantService.isGranted(userId, ...)` checks that already key off `ConsultantUser.userId` elsewhere (`AdsApi`, `BookingApi`'s `@PreAuthorize` expressions). SUPER_ADMIN has no pre-existing entity, so its credential row is seeded directly (`db/dev-seed/V900` locally); a real SUPER_ADMIN-provisioning flow is out of scope for this story.
- **Testing tier(s):** unit (`SecurityApiImplTest`), module (`@ApplicationModuleTest` covering login success/failure), component test (`LoginScreen.test.tsx`)

## Sub-tasks
- [NEW] Backend: `principal_credential` table (migration `V47`) + `PrincipalCredential` entity + repository
- [NEW] Backend: `SecurityApi.registerCredential` public method + `SecurityApiImpl` (BCrypt hashing via `PasswordEncoder`)
- [NEW] Backend: `POST /api/v1/auth/login` (`AuthController`), public per `SecurityConfig.PUBLIC_ENDPOINTS`
- [EXTEND] Backend: `OnboardConsultantCommand`/`AddUserCommand` carry `email`/password fields; `WhitelabelServiceImpl` registers a credential in the same transaction as onboarding/adding a user
- [NEW] Backend: unit test (`SecurityApiImplTest`, `WhitelabelServiceImplTest` credential-registration assertions)
- [NEW] Backend: module/integration test coverage for login success/failure paths
- [NEW] Frontend: `useLogin` hook (React Query mutation)
- [NEW] Frontend: `LoginScreen.tsx` — email/password form, inline error state, redirect to role's landing screen on success
- [NEW] Frontend: component test (Testing Library, co-located)
- [EXTEND] Frontend: `App.tsx` gains an unauthenticated `/login` route
