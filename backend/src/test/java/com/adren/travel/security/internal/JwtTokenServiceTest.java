package com.adren.travel.security.internal;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private final JwtTokenService service =
        new JwtTokenService(new JwtProperties("unit-test-signing-secret-at-least-32-bytes-long", 60));

    @Test
    void roundTripsAConsultantPrincipal() {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), Role.CONSULTANT, UUID.randomUUID());

        String token = service.generateToken(principal);
        AdrenPrincipal parsed = service.parseToken(token);

        assertThat(parsed).isEqualTo(principal);
    }

    @Test
    void roundTripsASuperAdminPrincipalWithNullConsultantId() {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), Role.SUPER_ADMIN, null);

        String token = service.generateToken(principal);
        AdrenPrincipal parsed = service.parseToken(token);

        assertThat(parsed).isEqualTo(principal);
        assertThat(parsed.consultantId()).isNull();
    }

    @Test
    void rejectsATamperedToken() {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), Role.USER, UUID.randomUUID());
        String token = service.generateToken(principal);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> service.parseToken(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsATokenSignedWithADifferentSecret() {
        JwtTokenService otherService =
            new JwtTokenService(new JwtProperties("a-completely-different-signing-secret-32-bytes", 60));
        String token = otherService.generateToken(new AdrenPrincipal(UUID.randomUUID(), Role.USER, UUID.randomUUID()));

        assertThatThrownBy(() -> service.parseToken(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsAnExpiredToken() {
        JwtTokenService expiring =
            new JwtTokenService(new JwtProperties("unit-test-signing-secret-at-least-32-bytes-long", 0));
        String token = expiring.generateToken(new AdrenPrincipal(UUID.randomUUID(), Role.USER, UUID.randomUUID()));

        assertThatThrownBy(() -> service.parseToken(token)).isInstanceOf(JwtException.class);
    }
}
