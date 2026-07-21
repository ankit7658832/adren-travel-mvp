package com.adren.travel.booking;

import com.adren.travel.infra.MarketSeedFixtures;
import com.adren.travel.infra.TestInfrastructure;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.whitelabel.Market;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TST-07's own sample-test sub-task: proves {@link MarketSeedFixtures}'
 * rows aren't just present in Postgres but genuinely readable by real
 * business logic — {@code publishPackage}'s real {@code
 * whitelabelApi.findConsultantMarket} lookup resolves the UK market from
 * the seeded Consultant row, correctly triggering BOK-11's ATOL gate.
 * <p>
 * Lives in {@code booking} (not {@code infra}, where {@link
 * MarketSeedFixtures} itself lives) — {@code infra} must stay a
 * dependency-free leaf package every module's tests can depend on
 * ({@code TestInfrastructure}'s own design); a test that ALSO needs {@link
 * BookingApi} living in {@code infra} instead would create {@code infra ->
 * booking}, which Spring Modulith's whole-classpath verification (run by
 * every {@code @ApplicationModuleTest} in the suite) correctly rejected as
 * a cycle the first time this file was written there (confirmed directly:
 * {@code ai -> supplier -> infra -> booking -> ai}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(initializers = TestInfrastructure.class)
class MarketSeedFixturesIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BookingApi bookingApi;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void seedsOneConsultantPerMarketWithTheRealRequiredKycFields() {
        List<MarketSeedFixtures.ConsultantFixture> consultants = MarketSeedFixtures.seedConsultantPerMarket(jdbcTemplate);

        assertThat(consultants).extracting(MarketSeedFixtures.ConsultantFixture::market)
            .containsExactlyInAnyOrder(Market.values());

        MarketSeedFixtures.ConsultantFixture india = consultants.stream()
            .filter(c -> c.market() == Market.INDIA).findFirst().orElseThrow();
        Long kycFieldCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM consultant_kyc_field WHERE consultant_id = ?", Long.class, india.consultantId());
        assertThat(kycFieldCount).isEqualTo(3); // gstRegistration, businessPan, bankDetails
    }

    @Test
    void ukDynamicComboPackageFixtureIsBlockedFromPublishingUntilAtolDisclosureIsCompletedBOK11() {
        MarketSeedFixtures.PackageFixture fixture = MarketSeedFixtures.seedUkDynamicFlightHotelComboPackage(jdbcTemplate);
        authenticateAs(Role.CONSULTANT, fixture.consultantId());

        assertThatThrownBy(() -> bookingApi.publishPackage(fixture.packageId(), false))
            .isInstanceOf(AtolDisclosureRequiredException.class);

        bookingApi.completeAtolDisclosure(fixture.packageId());
        assertThat(bookingApi.publishPackage(fixture.packageId(), false)).isEqualTo(fixture.packageId());
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
