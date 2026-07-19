package com.adren.travel.payments;

import com.adren.travel.payments.event.CommissionCalculatedEvent;
import com.adren.travel.payments.event.CreditThresholdBreachedEvent;
import com.adren.travel.payments.event.CurrencyBufferAppliedEvent;
import com.adren.travel.payments.event.FxRateSnapshotTakenEvent;
import com.adren.travel.payments.event.MarkupRuleConfiguredEvent;
import com.adren.travel.payments.event.RefundCalculatedEvent;
import com.adren.travel.payments.event.StripePaymentSucceededEvent;
import com.adren.travel.payments.event.WalletHoldDebitedEvent;
import com.adren.travel.payments.event.WalletHoldPlacedEvent;
import com.adren.travel.payments.event.WalletHoldReleasedEvent;
import com.adren.travel.payments.event.WalletProvisionedEvent;
import com.adren.travel.payments.event.WalletTopUpReconciledEvent;
import com.adren.travel.security.AdrenPrincipal;
import com.adren.travel.security.Role;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import com.adren.travel.shared.ProductCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // FIN-08: placeHold now enforces availableBalance + creditLimit >= amount
    // — every test below that expects a hold to succeed needs a funded
    // wallet first (a fresh auto-provisioned wallet starts at zero/zero).
    private void seedSufficientCreditLimit(UUID consultantId) {
        jdbcTemplate.update(
            "INSERT INTO wallet (consultant_id, available_balance, credit_limit, pending_holds, currency, updated_at) " +
                "VALUES (?, 0, 100000, 0, 'INR', now()) " +
                "ON CONFLICT (consultant_id) DO UPDATE SET credit_limit = EXCLUDED.credit_limit",
            consultantId);
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

    /**
     * FIN-15's actual acceptance criterion, PRD §23.4 Edge Case #10: a
     * top-up succeeds at the gateway (the PaymentIntent is created) but the
     * confirming webhook is delayed — until it arrives, the Consultant must
     * not be able to book against those funds. Proven end to end: wallet
     * balance stays at zero through the "delay" (no webhook call yet), and
     * only moves once the webhook is simulated arriving.
     */
    @Test
    void aDelayedTopUpWebhookLeavesTheWalletUnreconciledUntilItArrivesFIN15(Scenario scenario) {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        Money amount = new Money(BigDecimal.valueOf(5_000), CurrencyCode.INR);

        PaymentIntentView intent = paymentsApi.initiateWalletTopUp(new InitiateWalletTopUpCommand(consultantId, amount));

        // The "delay" — no webhook has arrived yet. A booking attempted now
        // must see these funds as unavailable (FIN-08's credit-limit check
        // reads availableBalance, which this hasn't touched).
        WalletView duringDelay = paymentsApi.getWallet(consultantId);
        assertThat(duringDelay.availableBalance()).isEqualByComparingTo("0");

        var webhookCommand = new HandleStripeWebhookCommand("payment_intent.succeeded", intent.paymentIntentId());
        scenario.stimulate(() -> paymentsApi.handleStripeWebhook(webhookCommand))
            .andWaitForEventOfType(WalletTopUpReconciledEvent.class)
            .matchingMappedValue(WalletTopUpReconciledEvent::consultantId, consultantId);
    }

    @Test
    void aUserCannotCreateAPaymentIntentForAnotherConsultantFIN11() {
        authenticateAs(Role.USER, UUID.randomUUID());
        var command = new CreatePaymentIntentCommand(UUID.randomUUID(), UUID.randomUUID(),
            new Money(BigDecimal.valueOf(1_000), CurrencyCode.INR));

        assertThatThrownBy(() -> paymentsApi.createPaymentIntent(command)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void placingAHoldPublishesWalletHoldPlacedEventFIN07(Scenario scenario) {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        seedSufficientCreditLimit(consultantId);
        Money amount = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);

        scenario.stimulate(() -> paymentsApi.placeHold(new WalletHoldCommand(bookingId, consultantId, amount)))
            .andWaitForEventOfType(WalletHoldPlacedEvent.class)
            .matchingMappedValue(WalletHoldPlacedEvent::bookingId, bookingId);
    }

    @Test
    void resolvingAHoldAsDebitPublishesWalletHoldDebitedEventFIN07(Scenario scenario) {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        seedSufficientCreditLimit(consultantId);
        Money amount = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);
        paymentsApi.placeHold(new WalletHoldCommand(bookingId, consultantId, amount));

        scenario.stimulate(() -> paymentsApi.resolveHoldAsDebit(new WalletHoldCommand(bookingId, consultantId, amount)))
            .andWaitForEventOfType(WalletHoldDebitedEvent.class)
            .matchingMappedValue(WalletHoldDebitedEvent::bookingId, bookingId);
    }

    @Test
    void resolvingAHoldAsAReleasePublishesWalletHoldReleasedEventFIN07(Scenario scenario) {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        seedSufficientCreditLimit(consultantId);
        Money amount = new Money(BigDecimal.valueOf(500), CurrencyCode.INR);
        paymentsApi.placeHold(new WalletHoldCommand(bookingId, consultantId, amount));

        scenario.stimulate(() -> paymentsApi.resolveHoldAsRelease(new WalletHoldCommand(bookingId, consultantId, amount)))
            .andWaitForEventOfType(WalletHoldReleasedEvent.class)
            .matchingMappedValue(WalletHoldReleasedEvent::bookingId, bookingId);
    }

    // Deliberately no Scenario/stimulate here (unlike the two tests above):
    // an immediate read-back after scenario.stimulate() isn't guaranteed to
    // see the underlying entity write in this same test method (confirmed
    // earlier in this project via direct Postgres inspection) — direct
    // calls avoid that flakiness entirely.
    @Test
    void theFullHoldThenDebitLifecycleUpdatesTheWalletCorrectlyFIN07() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        seedSufficientCreditLimit(consultantId);
        Money amount = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);

        paymentsApi.placeHold(new WalletHoldCommand(bookingId, consultantId, amount));
        WalletView afterHold = paymentsApi.getWallet(consultantId);
        assertThat(afterHold.pendingHolds()).isEqualByComparingTo("11500");
        assertThat(afterHold.availableBalance()).isEqualByComparingTo("0");

        paymentsApi.resolveHoldAsDebit(new WalletHoldCommand(bookingId, consultantId, amount));

        WalletView afterDebit = paymentsApi.getWallet(consultantId);
        assertThat(afterDebit.pendingHolds()).isEqualByComparingTo("0");
        assertThat(afterDebit.availableBalance()).isEqualByComparingTo("-11500");
    }

    @Test
    void findWalletLedgerReturnsEveryEntryForTheConsultantWhenUnfilteredFIN09() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        seedSufficientCreditLimit(consultantId);
        Money amount = new Money(BigDecimal.valueOf(500), CurrencyCode.INR);
        paymentsApi.placeHold(new WalletHoldCommand(bookingId, consultantId, amount));
        paymentsApi.resolveHoldAsDebit(new WalletHoldCommand(bookingId, consultantId, amount));

        var page = paymentsApi.findWalletLedger(consultantId, null, org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(WalletLedgerEntryView::type).containsExactlyInAnyOrder("HOLD", "DEBIT");
    }

    @Test
    void findWalletLedgerFiltersByTypeFIN09() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        seedSufficientCreditLimit(consultantId);
        Money amount = new Money(BigDecimal.valueOf(500), CurrencyCode.INR);
        paymentsApi.placeHold(new WalletHoldCommand(bookingId, consultantId, amount));
        paymentsApi.resolveHoldAsDebit(new WalletHoldCommand(bookingId, consultantId, amount));

        var page = paymentsApi.findWalletLedger(consultantId, "DEBIT", org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(WalletLedgerEntryView::type).containsExactly("DEBIT");
    }

    @Test
    void resolvingAHoldAsAReleaseLeavesAvailableBalanceUntouchedFIN07() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        seedSufficientCreditLimit(consultantId);
        Money amount = new Money(BigDecimal.valueOf(500), CurrencyCode.INR);
        paymentsApi.placeHold(new WalletHoldCommand(bookingId, consultantId, amount));

        paymentsApi.resolveHoldAsRelease(new WalletHoldCommand(bookingId, consultantId, amount));

        WalletView afterRelease = paymentsApi.getWallet(consultantId);
        assertThat(afterRelease.pendingHolds()).isEqualByComparingTo("0");
        assertThat(afterRelease.availableBalance()).isEqualByComparingTo("0");
    }

    /**
     * HRD-02: proves the {@code CreditThresholdBreachedEvent} really does
     * fire against a real Postgres transaction — the interesting part
     * mocks can't prove, since the whole point of the {@code
     * REQUIRES_NEW}-transactional publisher is that Modulith's own
     * event-publication registry write survives a rollback it would
     * otherwise be part of.
     */
    @Test
    void aRejectedHoldStillPublishesCreditThresholdBreachedEventFIN08HRD02(Scenario scenario) {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        // No seedSufficientCreditLimit call — a freshly auto-provisioned
        // wallet starts at zero balance/zero credit limit, guaranteeing
        // this hold breaches it.
        Money amount = new Money(BigDecimal.valueOf(500), CurrencyCode.INR);

        scenario.stimulate(() -> {
                try {
                    paymentsApi.placeHold(new WalletHoldCommand(bookingId, consultantId, amount));
                } catch (CreditLimitExceededException expected) {
                    // The block itself is proven elsewhere (FIN-08's own
                    // tests) — this scenario is about the event surviving it.
                }
            })
            .andWaitForEventOfType(CreditThresholdBreachedEvent.class)
            .matchingMappedValue(CreditThresholdBreachedEvent::consultantId, consultantId);
    }

    @Test
    void retryingAPlaceHoldForTheSameBookingIsANoOpFIN10() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        seedSufficientCreditLimit(consultantId);
        Money amount = new Money(BigDecimal.valueOf(500), CurrencyCode.INR);

        paymentsApi.placeHold(new WalletHoldCommand(bookingId, consultantId, amount));
        paymentsApi.placeHold(new WalletHoldCommand(bookingId, consultantId, amount)); // simulated retry

        WalletView wallet = paymentsApi.getWallet(consultantId);
        assertThat(wallet.pendingHolds()).isEqualByComparingTo("500");
    }

    @Test
    void calculatingARefundAfterTheDeadlinePublishesRefundCalculatedEventRequiringApprovalFIN13(Scenario scenario) {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Money sellPrice = new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR);
        FxRateSnapshot originalFxRateSnapshot = new FxRateSnapshot(CurrencyCode.USD, CurrencyCode.INR,
            BigDecimal.valueOf(80), java.time.Instant.now().minusSeconds(7200));
        var command = new CalculateRefundCommand(bookingId, consultantId, sellPrice,
            java.time.Instant.now().minusSeconds(3600), java.time.Instant.now(), BigDecimal.valueOf(25),
            originalFxRateSnapshot);

        scenario.stimulate(() -> paymentsApi.calculateRefund(command))
            .andWaitForEventOfType(RefundCalculatedEvent.class)
            .matchingMappedValue(RefundCalculatedEvent::requiresConsultantApproval, true);
    }

    @Test
    void calculatingARefundNeverWritesAWalletLedgerEntryFIN13() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Money sellPrice = new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR);
        FxRateSnapshot originalFxRateSnapshot = new FxRateSnapshot(CurrencyCode.USD, CurrencyCode.INR,
            BigDecimal.valueOf(80), java.time.Instant.now().minusSeconds(7200));

        paymentsApi.calculateRefund(new CalculateRefundCommand(bookingId, consultantId, sellPrice,
            java.time.Instant.now().minusSeconds(3600), java.time.Instant.now(), BigDecimal.valueOf(25),
            originalFxRateSnapshot));

        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM wallet_ledger_entry WHERE related_booking_id = ?", Long.class, bookingId);
        assertThat(count).isZero();
    }

    /**
     * FIN-17's real acceptance criterion: against the actual application.yml
     * wiring (not a hand-constructed test double), the India GST/TCS layer
     * is off by default — PRD §19's tax-counsel sign-off is still pending,
     * so a real Spring context must never silently apply the illustrative
     * rates. Flipping it on is covered at the unit tier
     * (PaymentsServiceImplTest), which also proves the actual GST/TCS math.
     */
    @Test
    void indiaGstTcsIsDisabledByDefaultInTheRealApplicationConfigurationFIN17() {
        Money margin = new Money(BigDecimal.valueOf(50_000), CurrencyCode.INR);
        Money packageValue = new Money(BigDecimal.valueOf(1_000_000), CurrencyCode.INR);

        IndiaGstTcsCalculation calculation = paymentsApi.calculateIndiaGstTcs(
            new CalculateIndiaGstTcsCommand(UUID.randomUUID(), UUID.randomUUID(), margin, packageValue));

        assertThat(calculation.applied()).isFalse();
        assertThat(calculation.gstAmount().amount()).isEqualByComparingTo("0");
        assertThat(calculation.tcsAmount().amount()).isEqualByComparingTo("0");
    }

    @Test
    void ukTomsVatIsDisabledByDefaultInTheRealApplicationConfigurationFIN18() {
        Money margin = new Money(BigDecimal.valueOf(1_000), CurrencyCode.GBP);

        UkTomsVatCalculation calculation = paymentsApi.calculateUkTomsVat(
            new CalculateUkTomsVatCommand(UUID.randomUUID(), UUID.randomUUID(), margin));

        assertThat(calculation.applied()).isFalse();
        assertThat(calculation.vatAmount().amount()).isEqualByComparingTo("0");
    }

    /**
     * ADS-14's real acceptance criterion: against the actual application.yml
     * wiring, the ad-spend managed-service fee is off by default — PRD §19's
     * business confirmation is still pending, so a real Spring context must
     * never silently apply the placeholder percentage. Flipping it on is
     * covered at the unit tier (PaymentsServiceImplTest), which also proves
     * the actual fee math and the real event's publication.
     */
    @Test
    void adSpendBillingIsDisabledByDefaultInTheRealApplicationConfigurationADS14() {
        Money spendAmount = new Money(BigDecimal.valueOf(1_000), CurrencyCode.INR);

        AdSpendBillingCalculation calculation = paymentsApi.calculateAdSpendBilling(
            new CalculateAdSpendBillingCommand(UUID.randomUUID(), UUID.randomUUID(), spendAmount));

        assertThat(calculation.applied()).isFalse();
        assertThat(calculation.feeAmount().amount()).isEqualByComparingTo("0");
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
