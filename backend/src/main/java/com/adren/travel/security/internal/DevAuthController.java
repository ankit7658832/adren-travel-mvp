package com.adren.travel.security.internal;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Local-dev-only convenience: mints a real, validly-signed JWT for a
 * fixed dev identity, since no login/token-issuance endpoint exists
 * anywhere in the mvp-mock story catalogue yet ({@link JwtTokenService}'s
 * own Javadoc: "so a future authentication endpoint has a ready-made
 * place to call into" — this is that place, scoped as narrowly as
 * possible rather than a real auth flow). {@code @Profile("dev")}: this
 * bean does not exist at all unless {@code SPRING_PROFILES_ACTIVE=dev} is
 * set, so its path in {@code SecurityConfig.PUBLIC_ENDPOINTS} is inert
 * (an ordinary unmapped 404) in every other environment.
 * <p>
 * {@code CONSULTANT} mints against the fixed seed row {@code
 * V900__dev_seed_consultant.sql} inserts — {@code db/dev-seed}, only on
 * this same {@code dev} profile's Flyway location list.
 */
@RestController
@RequestMapping("/dev-auth")
@Profile("dev")
class DevAuthController {

    private static final UUID DEV_CONSULTANT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    private final JwtTokenService jwtTokenService;

    DevAuthController(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * {@code GET /dev-auth/token?role=SUPER_ADMIN} or {@code
     * ?role=CONSULTANT} — returns a fresh Bearer token for that role,
     * long-lived per {@code application-dev.yml}'s expiration override.
     */
    @GetMapping("/token")
    DevTokenResponse token(@RequestParam Role role) {
        UUID consultantId = role == Role.SUPER_ADMIN ? null : DEV_CONSULTANT_ID;
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), role, consultantId);
        return new DevTokenResponse(jwtTokenService.generateToken(principal), role, consultantId);
    }

    record DevTokenResponse(String token, Role role, UUID consultantId) {
    }
}
