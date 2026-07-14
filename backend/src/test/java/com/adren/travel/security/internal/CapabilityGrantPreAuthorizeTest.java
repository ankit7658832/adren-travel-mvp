package com.adren.travel.security.internal;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.CapabilityGrantService;
import com.adren.travel.security.CapabilityGrantService.Capability;
import com.adren.travel.security.Role;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * FND-02's second acceptance criterion — a PRD §6 "No (unless granted)"
 * capability (e.g. a USER creating a Package) is rejected when the
 * per-Consultant grant flag is false and succeeds when true — proven here
 * against a small illustrative gated method rather than BOK-10's real
 * {@code createPackage} (Package doesn't exist as an entity/Api yet). This
 * exercises exactly the SpEL pattern (role check OR
 * {@code @capabilityGrantService.isGranted(...)}) that story is expected to
 * copy once it lands.
 */
class CapabilityGrantPreAuthorizeTest {

    @Component
    static class SampleGrantGatedService {
        @PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTANT') or "
            + "@capabilityGrantService.isGranted(authentication.principal.userId(), "
            + "T(com.adren.travel.security.CapabilityGrantService.Capability).CREATE_PACKAGE)")
        String createPackage() {
            return "created";
        }
    }

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        CapabilityGrantRepository capabilityGrantRepository() {
            return Mockito.mock(CapabilityGrantRepository.class);
        }

        @Bean("capabilityGrantService")
        CapabilityGrantService capabilityGrantService(CapabilityGrantRepository repository) {
            return new CapabilityGrantServiceImpl(repository);
        }

        @Bean
        SampleGrantGatedService sampleGrantGatedService() {
            return new SampleGrantGatedService();
        }
    }

    private static AnnotationConfigApplicationContext context;
    private static SampleGrantGatedService gatedService;
    private static CapabilityGrantRepository capabilityGrantRepository;

    @BeforeAll
    static void startContext() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        gatedService = context.getBean(SampleGrantGatedService.class);
        capabilityGrantRepository = context.getBean(CapabilityGrantRepository.class);
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aConsultantIsAllowedRegardlessOfAnyGrantFlag() {
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());

        assertThat(gatedService.createPackage()).isEqualTo("created");
    }

    @Test
    void aUserWithoutTheGrantIsRejected() {
        UUID userId = UUID.randomUUID();
        authenticateAs(Role.USER, userId);
        when(capabilityGrantRepository.findByUserIdAndCapability(userId, Capability.CREATE_PACKAGE))
            .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(gatedService::createPackage).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aUserWithTheGrantSucceeds() {
        UUID userId = UUID.randomUUID();
        authenticateAs(Role.USER, userId);
        CapabilityGrant grant = new CapabilityGrant(UUID.randomUUID(), userId, Capability.CREATE_PACKAGE, true);
        when(capabilityGrantRepository.findByUserIdAndCapability(userId, Capability.CREATE_PACKAGE))
            .thenReturn(java.util.Optional.of(grant));

        assertThat(gatedService.createPackage()).isEqualTo("created");
    }

    private static void authenticateAs(Role role, UUID userId) {
        AdrenPrincipal principal = new AdrenPrincipal(userId, role, role == Role.SUPER_ADMIN ? null : UUID.randomUUID());
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
