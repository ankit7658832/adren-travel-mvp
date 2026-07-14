package com.adren.travel.security.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code adren.security.jwt.*} from application.yml. The local-dev
 * default secret in application.yml is explicitly local-only per RULES.md
 * §5.3 — every non-local profile must override {@code ADREN_JWT_SECRET}.
 */
@ConfigurationProperties(prefix = "adren.security.jwt")
record JwtProperties(String secret, long expirationMinutes) {
}
