package com.adren.travel.security.internal;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Signs and parses the stateless JWTs {@link JwtAuthenticationFilter} reads
 * on every request (RULES.md §5.1). One HMAC key per running instance,
 * sourced from {@link JwtProperties} — never hardcoded, never logged.
 * <p>
 * There is no login/token-issuance endpoint in the Foundation epic's scope
 * (no story requests one yet); {@link #generateToken(AdrenPrincipal)} exists
 * so tests can mint a valid principal-carrying token, and so a future
 * authentication endpoint has a ready-made place to call into.
 */
@Component
class JwtTokenService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_CONSULTANT_ID = "consultantId";

    private final SecretKey signingKey;
    private final Duration expiration;

    JwtTokenService(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(properties.expirationMinutes());
    }

    String generateToken(AdrenPrincipal principal) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
            .subject(principal.userId().toString())
            .claim(CLAIM_ROLE, principal.role().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expiration)));
        if (principal.consultantId() != null) {
            builder.claim(CLAIM_CONSULTANT_ID, principal.consultantId().toString());
        }
        return builder.signWith(signingKey).compact();
    }

    /**
     * @throws JwtException if the token is malformed, expired, or fails
     *                       signature verification — callers must treat this
     *                       as "not authenticated", never propagate the raw
     *                       parser exception to a client (RULES.md §6.2 —
     *                       never leak internal detail in a client-facing message).
     */
    AdrenPrincipal parseToken(String token) {
        var claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        UUID userId = UUID.fromString(claims.getSubject());
        Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
        String consultantIdClaim = claims.get(CLAIM_CONSULTANT_ID, String.class);
        UUID consultantId = consultantIdClaim == null ? null : UUID.fromString(consultantIdClaim);
        return new AdrenPrincipal(userId, role, consultantId);
    }
}
