package com.adren.travel.security.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code adren.security.password-reset.*} from application.yml.
 * {@code frontendBaseUrl} is where the reset link embedded in the email
 * points — configurable since it differs per environment (RULES.md §5.3),
 * never hardcoded to localhost outside the local-dev default.
 */
@ConfigurationProperties(prefix = "adren.security.password-reset")
record PasswordResetProperties(String frontendBaseUrl, long tokenExpirationMinutes) {
}
