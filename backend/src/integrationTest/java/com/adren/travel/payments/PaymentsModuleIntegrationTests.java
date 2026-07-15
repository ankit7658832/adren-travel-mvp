package com.adren.travel.payments;

import com.adren.travel.payments.event.CommissionCalculatedEvent;
import com.adren.travel.payments.event.CurrencyBufferAppliedEvent;
import com.adren.travel.payments.event.FxRateSnapshotTakenEvent;
import com.adren.travel.payments.event.MarkupRuleConfiguredEvent;
import com.adren.travel.payments.event.StripePaymentSucceededEvent;
import com.adren.travel.payments.event.WalletProvisionedEvent;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.shared.ProductCategory;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code @ApplicationModuleTest} for the payments module — real Spring
 * wiring + real (local) Postgres, verifying the event-publication contract
 * (FIN-01) and that {@code @PreAuthorize} actually gates markup
 * configuration to CONSULTANT/SUPER_ADMIN (PRD §3.3's "cannot change
 * markup ... unless granted" for Users). {@code STANDALONE} (the default)
 * is enough here — unlike {@code whitelabel}, {@code PaymentsServiceImpl}
 * has no constructor dependency on another module's bean, only on
 * {@code security.CurrentPrincipal}'s static methods, which need no bean
 * of their own. {@code @EnableMethodSecurity} is enabled locally since a
 * module test's slice doesn't include {@code security.internal.SecurityConfig}.
 */
@ApplicationModuleTest
class PaymentsModuleIntegrationTests {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    PaymentsApi paymentsApi;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void configuringAMarkupRulePublishesMarkupRuleConfiguredEvent(Scenario scenario) {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        var command = new ConfigureMarkupCommand(ProductCategory.HOTEL, MarkupType.PERCENTAGE,
            BigDecimal.valueOf(15), null, null);

        scenario.stimulate(() -> paymentsApi.configureMarkup(consultantId, command))
            .andWaitForEventOfType(MarkupRuleConfiguredEvent.class)
            .matchingMappedValue(MarkupRuleConfiguredEvent::category, ProductCategory.HOTEL);
    }

    @Test
    void aUserCannotConfigureMarkupPerPrdSection33() {
        authenticateAs(Role.USER, UUID.randomUUID());
        var command = new ConfigureMarkupCommand(ProductCategory.HOTEL, MarkupType.PERCENTAGE,
            BigDecimal.valueOf(15), null, null);

        assertThatThrownBy(() -> paymentsApi.configureMarkup(UUID.randomUUID(), command))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aConsultantCanConfigureAndReadBackTheirOwnMarkupRules() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        paymentsApi.configureMarkup(consultantId, new ConfigureMarkupCommand(
            ProductCategory.HOTEL, MarkupType.PERCENTAGE, BigDecimal.valueOf(15), null, null));

        List<MarkupRuleView> rules = paymentsApi.findMarkupRules(consultantId);

        assertThat(rules).extracting(MarkupRuleView::category).contains(ProductCategory.HOTEL);
    }

    @Test
    void aConsultantCannotConfigureAnotherConsultantsMarkup() {
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());
        var command = new ConfigureMarkupCommand(ProductCategory.HOTEL, MarkupType.PERCENTAGE,
            BigDecimal.valueOf(15), null, null);

        assertThatThrownBy(() -> paymentsApi.configureMarkup(UUID.randomUUID(), command))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void queryingAWalletForTheFirstTimeAutoProvisionsItAndPublishesWalletProvisionedEventFIN06(Scenario scenario) {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);

        scenario.stimulate(() -> paymentsApi.getWallet(consultantId))
            .andWaitForEventOfType(WalletProvisionedEvent.class)
            .matchingMappedValue(WalletProvisionedEvent::consultantId, consultantId);
    }

    @Test
    void aWalletExposesBalanceCreditLimitAndPendingHoldsInTheHomeMarketCurrencyFIN06() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);

        WalletView wallet = paymentsApi.getWallet(consultantId);

        assertThat(wallet.consultantId()).isEqualTo(consultantId);
        assertThat(wallet.availableBalance()).isEqualByComparingTo("0");
        assertThat(wallet.creditLimit()).isEqualByComparingTo("0");
        assertThat(wallet.pendingHolds()).isEqualByComparingTo("0");
        assertThat(wallet.currency()).isEqualTo(CurrencyCode.INR);
    }

    @Test
    void aUserCanQueryTheirOwnWalletUnlikeMarkupConfigurationFIN06() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.USER, consultantId);

        WalletView wallet = paymentsApi.getWallet(consultantId);

        assertThat(wallet.consultantId()).isEqualTo(consultantId);
    }

    @Test
    void calculatingCommissionPublishesCommissionCalculatedEventFIN02(Scenario scenario) {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Money netRate = new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR);
        var command = new CalculateCommissionCommand(bookingId, consultantId, netRate, BigDecimal.valueOf(5));

        scenario.stimulate(() -> paymentsApi.calculateCommission(command))
            .andWaitForEventOfType(CommissionCalculatedEvent.class)
            .matchingMappedValue(CommissionCalculatedEvent::bookingId, bookingId);
    }

    @Test
    void commissionOnNetIsKeptSeparateFromConsultantMarkupFIN02() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        Money netRate = new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR);
        paymentsApi.configureMarkup(consultantId, new ConfigureMarkupCommand(
            ProductCategory.HOTEL, MarkupType.PERCENTAGE, BigDecimal.valueOf(15), null, null));

        Money commission = paymentsApi.calculateCommission(
            new CalculateCommissionCommand(UUID.randomUUID(), consultantId, netRate, BigDecimal.valueOf(5)));
        List<MarkupRuleView> rules = paymentsApi.findMarkupRules(consultantId);

        assertThat(commission.amount()).isEqualByComparingTo("500.00");
        assertThat(rules.get(0).percentageValue()).isEqualByComparingTo("15");
    }

    @Test
    void applyingTheCurrencyBufferPublishesCurrencyBufferAppliedEventFIN03(Scenario scenario) {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Money fxConvertedBase = new Money(BigDecimal.valueOf(9_600), CurrencyCode.INR);
        var command = new ApplyCurrencyBufferCommand(bookingId, consultantId, fxConvertedBase, BigDecimal.valueOf(3));

        scenario.stimulate(() -> paymentsApi.applyCurrencyBuffer(command))
            .andWaitForEventOfType(CurrencyBufferAppliedEvent.class)
            .matchingMappedValue(CurrencyBufferAppliedEvent::bookingId, bookingId);
    }

    @Test
    void theBufferIsAppliedToTheFxConvertedBaseBeforeMarkupMatchingWorkedExampleBFIN03() {
        Money fxConvertedBase = new Money(BigDecimal.valueOf(9_600), CurrencyCode.INR);

        Money buffered = paymentsApi.applyCurrencyBuffer(new ApplyCurrencyBufferCommand(
            UUID.randomUUID(), UUID.randomUUID(), fxConvertedBase, BigDecimal.valueOf(3)));

        assertThat(buffered.amount()).isEqualByComparingTo("9888.00");
    }

    @Test
    void snapshottingTheFxRatePublishesFxRateSnapshotTakenEventFIN04(Scenario scenario) {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        var command = new SnapshotFxRateCommand(bookingId, consultantId, CurrencyCode.AED, CurrencyCode.INR,
            BigDecimal.valueOf(23.5));

        scenario.stimulate(() -> paymentsApi.snapshotFxRate(command))
            .andWaitForEventOfType(FxRateSnapshotTakenEvent.class)
            .matchingMappedValue(FxRateSnapshotTakenEvent::bookingId, bookingId);
    }

    @Test
    void theSnapshotIsLockedRegardlessOfLaterCallsForOtherBookingsFIN04() {
        FxRateSnapshot snapshot = paymentsApi.snapshotFxRate(new SnapshotFxRateCommand(
            UUID.randomUUID(), UUID.randomUUID(), CurrencyCode.AED, CurrencyCode.INR, BigDecimal.valueOf(23.5)));

        paymentsApi.snapshotFxRate(new SnapshotFxRateCommand(
            UUID.randomUUID(), UUID.randomUUID(), CurrencyCode.AED, CurrencyCode.INR, BigDecimal.valueOf(24.1)));

        assertThat(snapshot.rate()).isEqualByComparingTo("23.5");
    }

    @Test
    void createsAPaymentIntentThenAWebhookMarksItSucceededAndPublishesTheEventFIN11(Scenario scenario) {
        UUID bookingReferenceId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        Money amount = new Money(BigDecimal.valueOf(11_500), CurrencyCode.GBP);

        PaymentIntentView intent = paymentsApi.createPaymentIntent(
            new CreatePaymentIntentCommand(bookingReferenceId, consultantId, amount));

        assertThat(intent.paymentIntentId()).isNotBlank();
        assertThat(intent.clientSecret()).isNotBlank();
        assertThat(intent.status()).isEqualTo(PaymentIntentStatus.REQUIRES_PAYMENT_METHOD);

        var webhookCommand = new HandleStripeWebhookCommand("payment_intent.succeeded", intent.paymentIntentId());
        scenario.stimulate(() -> paymentsApi.handleStripeWebhook(webhookCommand))
            .andWaitForEventOfType(StripePaymentSucceededEvent.class)
            .matchingMappedValue(StripePaymentSucceededEvent::bookingReferenceId, bookingReferenceId);
    }

    @Test
    void aUserCannotCreateAPaymentIntentForAnotherConsultantFIN11() {
        authenticateAs(Role.USER, UUID.randomUUID());
        var command = new CreatePaymentIntentCommand(UUID.randomUUID(), UUID.randomUUID(),
            new Money(BigDecimal.valueOf(1_000), CurrencyCode.INR));

        assertThatThrownBy(() -> paymentsApi.createPaymentIntent(command)).isInstanceOf(AccessDeniedException.class);
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
