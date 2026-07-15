package com.adren.travel.payments;

import com.adren.travel.infra.TestInfrastructure;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.shared.ProductCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full end-to-end integration test for FIN-05's pricing pipeline: real
 * Spring Boot context, real Postgres (via Testcontainers) — proves the
 * pipeline runs against a genuinely PERSISTED-and-read-back markup rule
 * (FIN-01's {@code MarkupRule}, saved via {@code configureMarkup} then
 * read back inside {@code calculateSellRate}'s markup step), not a mocked
 * repository, and reproduces PRD §12.1 Worked Example B to the cent.
 * <p>
 * Requires Docker to be available on the host/CI runner — see
 * {@code BookingEndToEndIT} for the same tier/shape.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = TestInfrastructure.class)
class PricingPipelineEndToEndIT {

    @Autowired
    PaymentsApi paymentsApi;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void calculatesTheFullSellRatePipelineAgainstAReallyPersistedMarkupRule() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        paymentsApi.configureMarkup(consultantId, new ConfigureMarkupCommand(
            ProductCategory.HOTEL, MarkupType.PERCENTAGE, BigDecimal.valueOf(15), null, null));

        // PRD §12.1 Worked Example B: EUR 100 at an illustrative rate of
        // 1 EUR = INR 96, 3% currency buffer, 15% markup on the adjusted base.
        var command = new CalculateSellRateCommand(UUID.randomUUID(), consultantId, ProductCategory.HOTEL,
            new Money(BigDecimal.valueOf(100), CurrencyCode.INR), CurrencyCode.INR,
            BigDecimal.valueOf(96), BigDecimal.valueOf(3), BigDecimal.ZERO);

        SellRateCalculation result = paymentsApi.calculateSellRate(command);

        assertThat(result.fxConvertedBase().amount()).isEqualByComparingTo("9600.00");
        assertThat(result.bufferedAmount().amount()).isEqualByComparingTo("9888.00");
        assertThat(result.markupAmount().amount()).isEqualByComparingTo("1483.20");
        assertThat(result.sellRate().amount()).isEqualByComparingTo("11371.20");

        List<MarkupRuleView> rules = paymentsApi.findMarkupRules(consultantId);
        assertThat(rules).extracting(MarkupRuleView::category).contains(ProductCategory.HOTEL);
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
