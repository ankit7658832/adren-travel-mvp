package com.adren.travel.whitelabel;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.whitelabel.event.ConsultantOnboardedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code @ApplicationModuleTest} for the whitelabel module — real Spring
 * wiring + real (local) Postgres, verifying the event-publication contract
 * (FND-04) and that {@code @PreAuthorize} actually gates onboarding to
 * SUPER_ADMIN (PRD §6). Method security isn't active in this module's own
 * slice by default (the "security" module isn't a real compile-time
 * dependency of "whitelabel," just a library-level annotation import) —
 * {@code @EnableMethodSecurity} is enabled locally, mirroring
 * {@code BookingApiMethodSecurityTest}'s approach.
 */
@ApplicationModuleTest
class WhitelabelModuleIntegrationTests {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    WhitelabelApi whitelabelApi;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void onboardingAConsultantPublishesConsultantOnboardedEvent(Scenario scenario) {
        authenticateAs(Role.SUPER_ADMIN);
        var command = new OnboardConsultantCommand("Test Co", Market.DENMARK, Map.of("cvrRegistrationNumber", "CVR1", "bankDetails", "x"));

        scenario.stimulate(() -> whitelabelApi.onboardConsultant(command))
            .andWaitForEventOfType(ConsultantOnboardedEvent.class)
            .matchingMappedValue(ConsultantOnboardedEvent::homeMarket, Market.DENMARK);
    }

    @Test
    void aConsultantPrincipalCannotOnboardAnotherConsultant() {
        authenticateAs(Role.CONSULTANT);
        var command = new OnboardConsultantCommand("Test Co", Market.INDIA, Map.of());

        assertThatThrownBy(() -> whitelabelApi.onboardConsultant(command))
            .isInstanceOf(AccessDeniedException.class);
    }

    private static void authenticateAs(Role role) {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), role,
            role == Role.SUPER_ADMIN ? null : UUID.randomUUID());
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
