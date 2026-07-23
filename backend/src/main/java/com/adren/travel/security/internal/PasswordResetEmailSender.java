package com.adren.travel.security.internal;

/**
 * Seam over an email provider for the password-reset link — same "own
 * stub, real provider is production-tier work" shape as {@code
 * notification.internal.EmailClient}/{@code StubEmailClient} (HRD-01).
 * Kept inside {@code security} rather than reused from {@code
 * notification}: that module's {@code EmailClient} is keyed by {@code
 * consultantId} (SUPER_ADMIN and a Consultant's own login have none to
 * key by), and password reset needs the raw email address {@link
 * PrincipalCredential} already carries.
 */
interface PasswordResetEmailSender {

    void sendResetLink(String email, String resetUrl);
}
