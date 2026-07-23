package com.adren.travel.security.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Mock-phase-only {@link PasswordResetEmailSender} — logs rather than calling a real provider. */
@Component
class StubPasswordResetEmailSender implements PasswordResetEmailSender {

    private static final Logger log = LoggerFactory.getLogger(StubPasswordResetEmailSender.class);

    @Override
    public void sendResetLink(String email, String resetUrl) {
        log.info("Password-reset email stub dispatch to={}, resetUrl={}", email, resetUrl);
    }
}
