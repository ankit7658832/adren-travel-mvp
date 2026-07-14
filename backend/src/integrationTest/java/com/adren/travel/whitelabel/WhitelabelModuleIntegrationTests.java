package com.adren.travel.whitelabel;

import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.whitelabel.event.BrandingUpdatedEvent;
import com.adren.travel.whitelabel.event.ConsultantOnboardedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.domain.PageRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code @ApplicationModuleTest} for the whitelabel module — real Spring
 * wiring + real (local) Postgres, verifying the event-publication contract
 * (FND-04) and that {@code @PreAuthorize} actually gates onboarding to
 * SUPER_ADMIN (PRD §6). {@code DIRECT_DEPENDENCIES} is required since
 * FND-09: {@code WhitelabelServiceImpl} now has a real constructor
 * dependency on {@code security.CapabilityGrantService}.
 * {@code @EnableMethodSecurity} is enabled locally since a module test's
 * slice doesn't include {@code security.internal.SecurityConfig} (mirrors
 * {@code BookingApiMethodSecurityTest}'s approach).
 */
@ApplicationModuleTest(ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
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

    @Test
    void aConsultantCanAddAndListTheirOwnUsers() {
        // consultant_user.consultant_id has a real FK to consultant (same
        // module's own tables) — a genuine Consultant row must exist first,
        // unlike the pure-authorization tests above.
        authenticateAs(Role.SUPER_ADMIN, null);
        UUID consultantId = whitelabelApi.onboardConsultant(
            new OnboardConsultantCommand("Test Co", Market.DENMARK, Map.of("cvrRegistrationNumber", "CVR1", "bankDetails", "x")));

        authenticateAs(Role.CONSULTANT, consultantId);
        UUID userId = whitelabelApi.addUser(new AddUserCommand("staff@example.com", "Staff"));

        var page = whitelabelApi.findUsersByConsultant(PageRequest.of(0, 20));
        assertThat(page.getContent()).extracting(ConsultantUserView::userId).contains(userId);
    }

    @Test
    void updatingBrandingPublishesBrandingUpdatedEvent(Scenario scenario) {
        authenticateAs(Role.SUPER_ADMIN, null);
        UUID consultantId = whitelabelApi.onboardConsultant(
            new OnboardConsultantCommand("Test Co", Market.DENMARK, Map.of("cvrRegistrationNumber", "CVR1", "bankDetails", "x")));
        var command = new UpdateBrandingCommand(consultantId, "https://cdn/logo.png", null,
            "#FFFFFF", "#000000", "#111111", "consultant.example.com");

        scenario.stimulate(() -> whitelabelApi.updateBranding(command))
            .andWaitForEventOfType(BrandingUpdatedEvent.class)
            .matchingMappedValue(BrandingUpdatedEvent::domain, "consultant.example.com");

        BrandingProfileView saved = whitelabelApi.findBranding(consultantId);
        assertThat(saved.domain()).isEqualTo("consultant.example.com");
        assertThat(saved.backgroundColor()).isEqualTo("#FFFFFF");
    }

    @Test
    void aConsultantPrincipalCannotUpdateBranding() {
        authenticateAs(Role.CONSULTANT);
        var command = new UpdateBrandingCommand(UUID.randomUUID(), null, null, "#FFFFFF", "#000000", "#111111", null);

        assertThatThrownBy(() -> whitelabelApi.updateBranding(command))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aConsultantCanChangeTheirOwnPreferredLocaleToOneOfferedForTheirMarket() {
        authenticateAs(Role.SUPER_ADMIN, null);
        UUID consultantId = whitelabelApi.onboardConsultant(
            new OnboardConsultantCommand("Test Co", Market.DENMARK, Map.of("cvrRegistrationNumber", "CVR1", "bankDetails", "x")));

        authenticateAs(Role.CONSULTANT, consultantId);
        assertThat(whitelabelApi.availableLocalesFor(Market.DENMARK)).contains(com.adren.travel.shared.LocaleCode.DA);
        whitelabelApi.changePreferredLocale(com.adren.travel.shared.LocaleCode.DA);
    }

    private static void authenticateAs(Role role) {
        authenticateAs(role, role == Role.SUPER_ADMIN ? null : UUID.randomUUID());
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
