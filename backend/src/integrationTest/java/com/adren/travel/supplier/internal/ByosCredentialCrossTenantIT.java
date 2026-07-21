package com.adren.travel.supplier.internal;

import com.adren.travel.infra.TestInfrastructure;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.supplier.ByosCredentialSummary;
import com.adren.travel.supplier.SupplierId;
import com.adren.travel.supplier.event.ByosCredentialSavedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
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

    @TestConfiguration
    static class CapturedEventsConfig {
        @Bean
        CapturedEvents capturedEvents() {
            return new CapturedEvents();
        }
    }

    static class CapturedEvents {
        final List<ByosCredentialSavedEvent> received = new ArrayList<>();

        @EventListener
        void on(ByosCredentialSavedEvent event) {
            received.add(event);
        }
    }

    @Autowired
    ByosCredentialService byosCredentialService;

    @Autowired
    SupplierCredentialResolver credentialResolver;

    @Autowired
    CapturedEvents capturedEvents;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        capturedEvents.received.clear();
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

    @Test
    void savingAByosCredentialPublishesTheSavedEventDMC06() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);

        byosCredentialService.save(SupplierId.HOTELBEDS, "consultant-own-hotelbeds-api-key");

        assertThat(capturedEvents.received).containsExactly(new ByosCredentialSavedEvent(consultantId, SupplierId.HOTELBEDS));
    }

    @Test
    void findByosCredentialsForCurrentConsultantListsOnlyTheCallersOwnConfiguredSuppliersDMC06() {
        UUID consultantA = UUID.randomUUID();
        UUID consultantB = UUID.randomUUID();

        authenticateAs(Role.CONSULTANT, consultantA);
        byosCredentialService.save(SupplierId.HOTELBEDS, "consultant-a-hotelbeds-key");
        byosCredentialService.save(SupplierId.STUBA, "consultant-a-stuba-key");
        SecurityContextHolder.clearContext();

        authenticateAs(Role.CONSULTANT, consultantB);
        byosCredentialService.save(SupplierId.TBO, "consultant-b-tbo-key");

        authenticateAs(Role.CONSULTANT, consultantA);
        List<ByosCredentialSummary> summaries = byosCredentialService.findByosCredentialsForCurrentConsultant();

        assertThat(summaries).extracting(ByosCredentialSummary::supplierId)
            .containsExactlyInAnyOrder(SupplierId.HOTELBEDS, SupplierId.STUBA);
    }

    /**
     * DMC-09's core proof for the credential-resolution path a search
     * request runs through: {@link ByosCredentialService#readForCurrentConsultant}
     * takes no consultantId parameter at all, so Consultant B's own search
     * can never resolve Consultant A's BYOS credential — there is no input
     * through which A's tenant could even be requested.
     */
    @Test
    void readForCurrentConsultantNeverResolvesAnotherConsultantsCredentialDMC09() {
        UUID consultantA = UUID.randomUUID();
        UUID consultantB = UUID.randomUUID();

        authenticateAs(Role.CONSULTANT, consultantA);
        byosCredentialService.save(SupplierId.HOTELBEDS, "consultant-a-hotelbeds-key");
        SecurityContextHolder.clearContext();

        authenticateAs(Role.CONSULTANT, consultantB);
        assertThat(byosCredentialService.readForCurrentConsultant(SupplierId.HOTELBEDS)).isEmpty();
    }

    /**
     * The exact call {@code SupplierAggregationService.searchHotels} makes
     * at the top of every search request — full real chain (Postgres + KMS),
     * not just {@code ByosCredentialService} in isolation. Consultant B's
     * search must never fall through to Consultant A's BYOS row — whatever
     * it resolves to (empty, or Adren's own Hotelbeds credential if some
     * other test in this shared-Postgres suite happened to provision one
     * first via {@code SupplierSecretsManagerIT}) is fine; only Consultant
     * A's specific BYOS value would indicate a cross-tenant leak. TST-01 —
     * this used to assert {@code isEmpty()} outright, which is only true
     * when no Adren-owned Hotelbeds credential exists yet in this test
     * run's shared TestInfrastructure Postgres — an assumption that broke
     * whenever SupplierSecretsManagerIT (which provisions exactly that)
     * happened to run first, since *IT classes share one container by
     * design (TST-01's own scope). Fixed to assert the actual invariant
     * this test is named for, not an incidental one.
     */
    @Test
    void credentialResolverNeverResolvesAnotherConsultantsByosCredentialDuringSearchDMC09() {
        UUID consultantA = UUID.randomUUID();
        UUID consultantB = UUID.randomUUID();

        authenticateAs(Role.CONSULTANT, consultantA);
        byosCredentialService.save(SupplierId.HOTELBEDS, "consultant-a-hotelbeds-key");
        SecurityContextHolder.clearContext();

        authenticateAs(Role.CONSULTANT, consultantB);
        assertThat(credentialResolver.resolve(SupplierId.HOTELBEDS))
            .isNotEqualTo(java.util.Optional.of("consultant-a-hotelbeds-key"));
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
