package com.adren.travel.supplier.internal;

import com.adren.travel.infra.TestInfrastructure;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.supplier.SupplierId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FND-12's core acceptance criteria, proven against a real (containerized)
 * KMS and Postgres: a BYOS credential is round-trippable through real
 * envelope encryption, and Consultant B's attempt to read Consultant A's
 * row is denied by the same tenant-isolation check FND-03 established —
 * see {@code ByosCredentialServiceTest} for the mocked-KMS unit-test tier
 * of the same behavior. Lives in {@code supplier.internal} (not
 * {@code supplier}) since {@link ByosCredentialService} is deliberately
 * package-private — this story adds no public API/REST surface at all.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(initializers = TestInfrastructure.class)
class ByosCredentialCrossTenantIT {

    @Autowired
    ByosCredentialService byosCredentialService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aSavedByosCredentialRoundTripsThroughRealKmsEnvelopeEncryption() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);

        byosCredentialService.save(SupplierId.HOTELBEDS, "consultant-own-hotelbeds-api-key");

        assertThat(byosCredentialService.read(consultantId, SupplierId.HOTELBEDS))
            .isEqualTo("consultant-own-hotelbeds-api-key");
    }

    @Test
    void consultantBCannotReadConsultantAsByosCredentialFND12() {
        UUID consultantA = UUID.randomUUID();
        UUID consultantB = UUID.randomUUID();

        authenticateAs(Role.CONSULTANT, consultantA);
        byosCredentialService.save(SupplierId.HOTELBEDS, "consultant-a-secret-value");

        authenticateAs(Role.CONSULTANT, consultantB);
        assertThatThrownBy(() -> byosCredentialService.read(consultantA, SupplierId.HOTELBEDS))
            .isInstanceOf(AccessDeniedException.class);
    }

    private static void authenticateAs(Role role, UUID consultantId) {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), role, consultantId);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
