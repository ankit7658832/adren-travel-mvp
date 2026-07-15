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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

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

    @Autowired
    CorsConfigurationSource corsConfigurationSource;

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
        // branding_profile.domain is UNIQUE and this local Postgres persists
        // across test runs (unlike Testcontainers) — a fixed literal here
        // would collide with a previous run's row.
        String domain = uniqueDomain();
        var command = new UpdateBrandingCommand(consultantId, "https://cdn/logo.png", null,
            "#FFFFFF", "#000000", "#111111", domain);

        // Scenario.stimulate(...) runs the stimulus in its own test-managed
        // transaction scope for the purpose of observing the published
        // event — it does not guarantee the entity write is durably
        // visible to a later, separate read in this same test method (the
        // event completes even though a subsequent findBranding() here
        // reliably 404s). The save-then-read-back behavior is covered by
        // aSecondBrandingSaveIsVisibleOnTheNextReadWithoutWaitingForTheCacheTtlFND07,
        // which calls updateBranding/findBranding directly with no Scenario
        // involved — this test stays scoped to its own acceptance
        // criterion, the event-publication contract itself (FND-06's
        // "domain event publication" sub-task).
        scenario.stimulate(() -> whitelabelApi.updateBranding(command))
            .andWaitForEventOfType(BrandingUpdatedEvent.class)
            .matchingMappedValue(BrandingUpdatedEvent::domain, domain);
    }

    @Test
    void aSecondBrandingSaveIsVisibleOnTheNextReadWithoutWaitingForTheCacheTtlFND07() {
        authenticateAs(Role.SUPER_ADMIN, null);
        UUID consultantId = whitelabelApi.onboardConsultant(
            new OnboardConsultantCommand("Test Co", Market.DENMARK, Map.of("cvrRegistrationNumber", "CVR1", "bankDetails", "x")));
        String firstDomain = uniqueDomain();
        String secondDomain = uniqueDomain();
        whitelabelApi.updateBranding(new UpdateBrandingCommand(consultantId, "https://cdn/logo.png", null,
            "#FFFFFF", "#000000", "#111111", firstDomain));
        // Populate the cache with the first version, the same way a live
        // storefront read would (FND-07's BrandingCache).
        assertThat(whitelabelApi.findBranding(consultantId).domain()).isEqualTo(firstDomain);

        // PRD §24.5 — this second save must be visible on the very next
        // read, not after BrandingCache.TTL (30s) expires; if
        // BrandingCacheInvalidationListener weren't evicting on commit,
        // this assertion would still see the first domain for up to 30s.
        whitelabelApi.updateBranding(new UpdateBrandingCommand(consultantId, "https://cdn/logo.png", null,
            "#EEEEEE", "#222222", "#333333", secondDomain));

        assertThat(whitelabelApi.findBranding(consultantId).domain()).isEqualTo(secondDomain);
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

    @Test
    void theRealCorsConfigurationSourceAllowsAMappedDomainAndRejectsAnUnmappedOneFND08() {
        authenticateAs(Role.SUPER_ADMIN, null);
        UUID consultantId = whitelabelApi.onboardConsultant(
            new OnboardConsultantCommand("Test Co", Market.DENMARK, Map.of("cvrRegistrationNumber", "CVR1", "bankDetails", "x")));
        String domain = uniqueDomain();
        whitelabelApi.updateBranding(new UpdateBrandingCommand(consultantId, "https://cdn/logo.png", null,
            "#FFFFFF", "#000000", "#111111", domain));

        MockHttpServletRequest mappedRequest = new MockHttpServletRequest();
        mappedRequest.addHeader("Origin", "https://" + domain);
        CorsConfiguration mapped = corsConfigurationSource.getCorsConfiguration(mappedRequest);
        assertThat(mapped).isNotNull();
        assertThat(mapped.getAllowedOrigins()).containsExactly("https://" + domain);

        MockHttpServletRequest unmappedRequest = new MockHttpServletRequest();
        unmappedRequest.addHeader("Origin", "https://not-a-real-consultant-domain.example.com");
        // RULES.md §5.4 — no wildcard fallback exists anywhere in the real,
        // fully-wired active configuration: an unmapped origin gets null,
        // not an allow-all CorsConfiguration.
        assertThat(corsConfigurationSource.getCorsConfiguration(unmappedRequest)).isNull();
    }

    /** branding_profile.domain is UNIQUE and this local Postgres persists across test runs. */
    private static String uniqueDomain() {
        return "consultant-" + UUID.randomUUID() + ".example.com";
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
