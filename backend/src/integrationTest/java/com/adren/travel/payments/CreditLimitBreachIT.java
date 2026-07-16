package com.adren.travel.payments;

import com.adren.travel.infra.TestInfrastructure;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
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
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FIN-08's real acceptance criterion, proven against a real (containerized)
 * Postgres: a booking that would push a Consultant's wallet past {@code
 * availableBalance + creditLimit} is rejected outright — the {@code
 * chk_wallet_within_credit_limit} CHECK constraint (V29) and the
 * application-level guard in {@code Wallet.placeHold} together, not just
 * the mocked-repository unit-test tier in {@code PaymentsServiceImplTest}.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(initializers = TestInfrastructure.class)
class CreditLimitBreachIT {

    @Autowired
    PaymentsApi paymentsApi;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void placingAHoldThatWouldExceedAvailableCreditIsRejectedWithAnActionableMessage() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        seedWallet(consultantId, BigDecimal.valueOf(200), BigDecimal.valueOf(300)); // 500 total available
        Money amount = new Money(BigDecimal.valueOf(501), CurrencyCode.INR);

        assertThatThrownBy(() -> paymentsApi.placeHold(new WalletHoldCommand(UUID.randomUUID(), consultantId, amount)))
            .isInstanceOf(CreditLimitExceededException.class)
            .hasMessageContaining("top up");

        BigDecimal pendingHolds = jdbcTemplate.queryForObject(
            "SELECT pending_holds FROM wallet WHERE consultant_id = ?", BigDecimal.class, consultantId);
        assertThat(pendingHolds).isEqualByComparingTo("0"); // rejected, never partially applied
    }

    @Test
    void placingAHoldWithinAvailableCreditSucceeds() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        seedWallet(consultantId, BigDecimal.valueOf(200), BigDecimal.valueOf(300));
        Money amount = new Money(BigDecimal.valueOf(500), CurrencyCode.INR);

        paymentsApi.placeHold(new WalletHoldCommand(UUID.randomUUID(), consultantId, amount));

        BigDecimal pendingHolds = jdbcTemplate.queryForObject(
            "SELECT pending_holds FROM wallet WHERE consultant_id = ?", BigDecimal.class, consultantId);
        assertThat(pendingHolds).isEqualByComparingTo("500");
    }

    private void seedWallet(UUID consultantId, BigDecimal availableBalance, BigDecimal creditLimit) {
        jdbcTemplate.update(
            "INSERT INTO wallet (consultant_id, available_balance, credit_limit, pending_holds, currency, updated_at) " +
                "VALUES (?, ?, ?, 0, 'INR', now())",
            consultantId, availableBalance, creditLimit);
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
