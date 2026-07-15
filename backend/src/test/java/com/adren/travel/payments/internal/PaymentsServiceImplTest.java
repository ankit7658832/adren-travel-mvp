package com.adren.travel.payments.internal;

import com.adren.travel.payments.ApplyCurrencyBufferCommand;
import com.adren.travel.payments.CalculateCommissionCommand;
import com.adren.travel.payments.CalculateSellRateCommand;
import com.adren.travel.payments.ConfigureMarkupCommand;
import com.adren.travel.payments.CreatePaymentIntentCommand;
import com.adren.travel.payments.FxRateSnapshot;
import com.adren.travel.payments.HandleStripeWebhookCommand;
import com.adren.travel.payments.MarkupRuleView;
import com.adren.travel.payments.MarkupType;
import com.adren.travel.payments.PaymentIntentStatus;
import com.adren.travel.payments.PaymentIntentView;
import com.adren.travel.payments.SellRateCalculation;
import com.adren.travel.payments.SnapshotFxRateCommand;
import com.adren.travel.payments.WalletView;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** FIN-01's core acceptance criteria: per-Consultant, per-category markup configuration. */
@ExtendWith(MockitoExtension.class)
class PaymentsServiceImplTest {

    @Mock
    MarkupRuleRepository markupRuleRepository;

    @Mock
    WalletRepository walletRepository;

    @Mock
    PaymentIntentRepository paymentIntentRepository;

    @Mock
    ApplicationEventPublisher events;

    @Mock
    StripeClient stripeClient;

    PaymentsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentsServiceImpl(markupRuleRepository, walletRepository, paymentIntentRepository, events,
            new PricingPipeline(markupRuleRepository, events), stripeClient);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void configuresAPercentageMarkupRuleAndPublishesEvent() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(markupRuleRepository.findByConsultantIdAndCategory(consultantId, ProductCategory.HOTEL))
            .thenReturn(Optional.empty());
        var command = new ConfigureMarkupCommand(ProductCategory.HOTEL, MarkupType.PERCENTAGE,
            BigDecimal.valueOf(15), null, null);

        service.configureMarkup(consultantId, command);

        ArgumentCaptor<MarkupRule> captor = ArgumentCaptor.forClass(MarkupRule.class);
        verify(markupRuleRepository).save(captor.capture());
        assertThat(captor.getValue().getConsultantId()).isEqualTo(consultantId);
        assertThat(captor.getValue().getCategory()).isEqualTo(ProductCategory.HOTEL);
        assertThat(captor.getValue().getPercentageValue()).isEqualByComparingTo("15");

        ArgumentCaptor<MarkupRuleConfiguredEvent> eventCaptor = ArgumentCaptor.forClass(MarkupRuleConfiguredEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().consultantId()).isEqualTo(consultantId);
        assertThat(eventCaptor.getValue().category()).isEqualTo(ProductCategory.HOTEL);
    }

    @Test
    void configuresAFlatFeeMarkupRuleForActivities() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(markupRuleRepository.findByConsultantIdAndCategory(consultantId, ProductCategory.ACTIVITY))
            .thenReturn(Optional.empty());
        var command = new ConfigureMarkupCommand(ProductCategory.ACTIVITY, MarkupType.FLAT_FEE,
            null, BigDecimal.valueOf(500), CurrencyCode.INR);

        service.configureMarkup(consultantId, command);

        ArgumentCaptor<MarkupRule> captor = ArgumentCaptor.forClass(MarkupRule.class);
        verify(markupRuleRepository).save(captor.capture());
        assertThat(captor.getValue().getMarkupType()).isEqualTo(MarkupType.FLAT_FEE);
        assertThat(captor.getValue().getFlatFeeAmount()).isEqualByComparingTo("500");
        assertThat(captor.getValue().getFlatFeeCurrency()).isEqualTo(CurrencyCode.INR);
    }

    @Test
    void rejectsAPercentageRuleMissingItsValue() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        var command = new ConfigureMarkupCommand(ProductCategory.HOTEL, MarkupType.PERCENTAGE, null, null, null);

        assertThatThrownBy(() -> service.configureMarkup(consultantId, command))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAFlatFeeRuleMissingItsAmountOrCurrency() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        var command = new ConfigureMarkupCommand(ProductCategory.ACTIVITY, MarkupType.FLAT_FEE, null, null, null);

        assertThatThrownBy(() -> service.configureMarkup(consultantId, command))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aConsultantCannotConfigureAnotherConsultantsMarkupFND03Style() {
        UUID ownConsultantId = UUID.randomUUID();
        UUID otherConsultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, ownConsultantId);
        var command = new ConfigureMarkupCommand(ProductCategory.HOTEL, MarkupType.PERCENTAGE,
            BigDecimal.valueOf(15), null, null);

        assertThatThrownBy(() -> service.configureMarkup(otherConsultantId, command))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void reconfiguringACategoryReplacesTheExistingRuleRatherThanAddingASecondOne() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        MarkupRule existing = new MarkupRule(UUID.randomUUID(), consultantId, ProductCategory.HOTEL,
            MarkupType.PERCENTAGE, BigDecimal.valueOf(10), null, null);
        when(markupRuleRepository.findByConsultantIdAndCategory(consultantId, ProductCategory.HOTEL))
            .thenReturn(Optional.of(existing));
        var command = new ConfigureMarkupCommand(ProductCategory.HOTEL, MarkupType.PERCENTAGE,
            BigDecimal.valueOf(20), null, null);

        service.configureMarkup(consultantId, command);

        verify(markupRuleRepository).save(existing);
        assertThat(existing.getPercentageValue()).isEqualByComparingTo("20");
    }

    @Test
    void findMarkupRulesReturnsOnlyTheCallersOwnRules() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        MarkupRule rule = new MarkupRule(UUID.randomUUID(), consultantId, ProductCategory.HOTEL,
            MarkupType.PERCENTAGE, BigDecimal.valueOf(15), null, null);
        when(markupRuleRepository.findByConsultantId(consultantId)).thenReturn(List.of(rule));

        List<MarkupRuleView> views = service.findMarkupRules(consultantId);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).category()).isEqualTo(ProductCategory.HOTEL);
        assertThat(views.get(0).percentageValue()).isEqualByComparingTo("15");
    }

    @Test
    void aFirstTimeWalletQueryAutoProvisionsAZeroBalanceWalletAndPublishesTheEventFIN06() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(walletRepository.findById(consultantId)).thenReturn(Optional.empty());

        WalletView view = service.getWallet(consultantId);

        assertThat(view.consultantId()).isEqualTo(consultantId);
        assertThat(view.availableBalance()).isEqualByComparingTo("0");
        assertThat(view.creditLimit()).isEqualByComparingTo("0");
        assertThat(view.pendingHolds()).isEqualByComparingTo("0");
        assertThat(view.currency()).isEqualTo(CurrencyCode.INR);
        verify(walletRepository).save(any(Wallet.class));

        ArgumentCaptor<WalletProvisionedEvent> eventCaptor = ArgumentCaptor.forClass(WalletProvisionedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().consultantId()).isEqualTo(consultantId);
    }

    @Test
    void aSubsequentWalletQueryReturnsTheExistingWalletWithoutReProvisioningFIN06() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        Wallet existing = new Wallet(consultantId, CurrencyCode.GBP);
        when(walletRepository.findById(consultantId)).thenReturn(Optional.of(existing));

        WalletView view = service.getWallet(consultantId);

        assertThat(view.currency()).isEqualTo(CurrencyCode.GBP);
        verify(walletRepository, org.mockito.Mockito.never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void aConsultantCannotQueryAnotherConsultantsWalletFIN06() {
        UUID ownConsultantId = UUID.randomUUID();
        UUID otherConsultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, ownConsultantId);

        assertThatThrownBy(() -> service.getWallet(otherConsultantId))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void calculatesCommissionAsAPercentageOfNetRateAndPublishesTheEventFIN02() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Money netRate = new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR);
        var command = new CalculateCommissionCommand(bookingId, consultantId, netRate, BigDecimal.valueOf(5));

        Money commission = service.calculateCommission(command);

        assertThat(commission.amount()).isEqualByComparingTo("500.00");
        assertThat(commission.currency()).isEqualTo(CurrencyCode.INR);

        ArgumentCaptor<CommissionCalculatedEvent> eventCaptor = ArgumentCaptor.forClass(CommissionCalculatedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().bookingId()).isEqualTo(bookingId);
        assertThat(eventCaptor.getValue().consultantId()).isEqualTo(consultantId);
        assertThat(eventCaptor.getValue().netRate()).isEqualTo(netRate);
        assertThat(eventCaptor.getValue().commissionAmount().amount()).isEqualByComparingTo("500.00");
    }

    @Test
    void commissionAndMarkupAreDistinctAmountsNotNettedTogetherFIN02() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        when(markupRuleRepository.findByConsultantIdAndCategory(consultantId, ProductCategory.HOTEL))
            .thenReturn(Optional.empty());
        Money netRate = new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR);

        // 15% Consultant markup, per Worked Example A.
        service.configureMarkup(consultantId, new ConfigureMarkupCommand(
            ProductCategory.HOTEL, MarkupType.PERCENTAGE, BigDecimal.valueOf(15), null, null));
        // 5% Adren commission on the same net rate.
        Money commission = service.calculateCommission(
            new CalculateCommissionCommand(UUID.randomUUID(), consultantId, netRate, BigDecimal.valueOf(5)));

        ArgumentCaptor<MarkupRule> markupCaptor = ArgumentCaptor.forClass(MarkupRule.class);
        verify(markupRuleRepository).save(markupCaptor.capture());
        Money markupAmount = netRate.percentOf(markupCaptor.getValue().getPercentageValue());

        assertThat(markupAmount.amount()).isEqualByComparingTo("1500.00");
        assertThat(commission.amount()).isEqualByComparingTo("500.00");
        assertThat(markupAmount.amount()).isNotEqualByComparingTo(commission.amount());
    }

    @Test
    void rejectsANegativeCommissionPercentFIN02() {
        Money netRate = new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR);

        assertThatThrownBy(() -> new CalculateCommissionCommand(
            UUID.randomUUID(), UUID.randomUUID(), netRate, BigDecimal.valueOf(-1)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void appliesTheCurrencyBufferToTheFxConvertedBaseAndPublishesTheEventFIN03() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        // PRD §12.1 Worked Example B: EUR 100 converted to INR 9,600.
        Money fxConvertedBase = new Money(BigDecimal.valueOf(9_600), CurrencyCode.INR);
        var command = new ApplyCurrencyBufferCommand(bookingId, consultantId, fxConvertedBase, BigDecimal.valueOf(3));

        Money buffered = service.applyCurrencyBuffer(command);

        assertThat(buffered.amount()).isEqualByComparingTo("9888.00");

        ArgumentCaptor<CurrencyBufferAppliedEvent> eventCaptor = ArgumentCaptor.forClass(CurrencyBufferAppliedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().bookingId()).isEqualTo(bookingId);
        assertThat(eventCaptor.getValue().fxConvertedBase()).isEqualTo(fxConvertedBase);
        assertThat(eventCaptor.getValue().bufferedAmount().amount()).isEqualByComparingTo("9888.00");
    }

    @Test
    void theBufferedAmountFeedsIntoMarkupAsTheAdjustedBaseMatchingWorkedExampleBFIN03() {
        Money fxConvertedBase = new Money(BigDecimal.valueOf(9_600), CurrencyCode.INR);
        Money buffered = service.applyCurrencyBuffer(new ApplyCurrencyBufferCommand(
            UUID.randomUUID(), UUID.randomUUID(), fxConvertedBase, BigDecimal.valueOf(3)));

        Money sellRate = buffered.applyMarkupPercent(BigDecimal.valueOf(15));

        assertThat(buffered.amount()).isEqualByComparingTo("9888.00");
        assertThat(sellRate.amount()).isEqualByComparingTo("11371.20");
    }

    @Test
    void rejectsANegativeBufferPercentFIN03() {
        Money fxConvertedBase = new Money(BigDecimal.valueOf(9_600), CurrencyCode.INR);

        assertThatThrownBy(() -> new ApplyCurrencyBufferCommand(
            UUID.randomUUID(), UUID.randomUUID(), fxConvertedBase, BigDecimal.valueOf(-1)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void snapshotsAndLocksTheFxRateAndPublishesTheEventFIN04() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        var command = new SnapshotFxRateCommand(bookingId, consultantId, CurrencyCode.AED, CurrencyCode.INR,
            BigDecimal.valueOf(23.5));

        FxRateSnapshot snapshot = service.snapshotFxRate(command);

        assertThat(snapshot.supplierCurrency()).isEqualTo(CurrencyCode.AED);
        assertThat(snapshot.sellCurrency()).isEqualTo(CurrencyCode.INR);
        assertThat(snapshot.rate()).isEqualByComparingTo("23.5");
        assertThat(snapshot.snapshotAt()).isNotNull();

        ArgumentCaptor<FxRateSnapshotTakenEvent> eventCaptor = ArgumentCaptor.forClass(FxRateSnapshotTakenEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().bookingId()).isEqualTo(bookingId);
        assertThat(eventCaptor.getValue().snapshot()).isEqualTo(snapshot);
    }

    @Test
    void aLaterMarketMovementDoesNotChangeAnAlreadyTakenSnapshotFIN04() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        FxRateSnapshot originalSnapshot = service.snapshotFxRate(new SnapshotFxRateCommand(
            bookingId, consultantId, CurrencyCode.AED, CurrencyCode.INR, BigDecimal.valueOf(23.5)));

        // Simulates the market rate moving before booking confirmation
        // (T7) — a fresh snapshot for a DIFFERENT booking must not mutate
        // or replace the one already taken above.
        service.snapshotFxRate(new SnapshotFxRateCommand(
            UUID.randomUUID(), consultantId, CurrencyCode.AED, CurrencyCode.INR, BigDecimal.valueOf(24.1)));

        assertThat(originalSnapshot.rate()).isEqualByComparingTo("23.5");
    }

    @Test
    void rejectsANonPositiveRateFIN04() {
        assertThatThrownBy(() -> new SnapshotFxRateCommand(
            UUID.randomUUID(), UUID.randomUUID(), CurrencyCode.AED, CurrencyCode.INR, BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reproducesWorkedExampleAExactlySingleCurrencyNoBufferFIN05() {
        UUID consultantId = UUID.randomUUID();
        MarkupRule rule = new MarkupRule(UUID.randomUUID(), consultantId, ProductCategory.HOTEL,
            MarkupType.PERCENTAGE, BigDecimal.valueOf(15), null, null);
        when(markupRuleRepository.findByConsultantIdAndCategory(consultantId, ProductCategory.HOTEL))
            .thenReturn(Optional.of(rule));
        var command = new CalculateSellRateCommand(UUID.randomUUID(), consultantId, ProductCategory.HOTEL,
            new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR), CurrencyCode.INR,
            BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.valueOf(5));

        SellRateCalculation result = service.calculateSellRate(command);

        assertThat(result.fxConvertedBase().amount()).isEqualByComparingTo("10000.00");
        assertThat(result.bufferedAmount().amount()).isEqualByComparingTo("10000.00");
        assertThat(result.markupAmount().amount()).isEqualByComparingTo("1500.00");
        assertThat(result.sellRate().amount()).isEqualByComparingTo("11500.00");
        assertThat(result.commissionAmount().amount()).isEqualByComparingTo("500.00");
    }

    @Test
    void reproducesWorkedExampleBExactlyFxConversionPlusBufferFIN05() {
        UUID consultantId = UUID.randomUUID();
        MarkupRule rule = new MarkupRule(UUID.randomUUID(), consultantId, ProductCategory.HOTEL,
            MarkupType.PERCENTAGE, BigDecimal.valueOf(15), null, null);
        when(markupRuleRepository.findByConsultantIdAndCategory(consultantId, ProductCategory.HOTEL))
            .thenReturn(Optional.of(rule));
        // EUR 100 at an illustrative rate of 1 EUR = INR 96.
        var command = new CalculateSellRateCommand(UUID.randomUUID(), consultantId, ProductCategory.HOTEL,
            new Money(BigDecimal.valueOf(100), CurrencyCode.INR), CurrencyCode.INR,
            BigDecimal.valueOf(96), BigDecimal.valueOf(3), BigDecimal.ZERO);

        SellRateCalculation result = service.calculateSellRate(command);

        assertThat(result.fxConvertedBase().amount()).isEqualByComparingTo("9600.00");
        assertThat(result.bufferedAmount().amount()).isEqualByComparingTo("9888.00");
        assertThat(result.markupAmount().amount()).isEqualByComparingTo("1483.20");
        assertThat(result.sellRate().amount()).isEqualByComparingTo("11371.20");
        assertThat(result.fxRateSnapshot().rate()).isEqualByComparingTo("96");
    }

    @Test
    void rejectsCalculatingASellRateWithNoMarkupRuleConfiguredFIN05() {
        UUID consultantId = UUID.randomUUID();
        when(markupRuleRepository.findByConsultantIdAndCategory(consultantId, ProductCategory.HOTEL))
            .thenReturn(Optional.empty());
        var command = new CalculateSellRateCommand(UUID.randomUUID(), consultantId, ProductCategory.HOTEL,
            new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR), CurrencyCode.INR,
            BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.valueOf(5));

        assertThatThrownBy(() -> service.calculateSellRate(command)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @EnumSource(CurrencyCode.class)
    void createsAPaymentIntentForEachOfTheSixSettlementCurrenciesFIN11(CurrencyCode currency) {
        UUID bookingReferenceId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        Money amount = new Money(BigDecimal.valueOf(1_000), currency);
        when(stripeClient.createPaymentIntent(amount, bookingReferenceId)).thenReturn(
            new StripePaymentIntent("pi_123", "pi_123_secret_abc", amount, PaymentIntentStatus.REQUIRES_PAYMENT_METHOD));

        PaymentIntentView view = service.createPaymentIntent(
            new CreatePaymentIntentCommand(bookingReferenceId, consultantId, amount));

        assertThat(view.paymentIntentId()).isEqualTo("pi_123");
        assertThat(view.clientSecret()).isEqualTo("pi_123_secret_abc");
        assertThat(view.amount()).isEqualTo(amount);
        assertThat(view.status()).isEqualTo(PaymentIntentStatus.REQUIRES_PAYMENT_METHOD);

        ArgumentCaptor<PaymentIntentRecord> captor = ArgumentCaptor.forClass(PaymentIntentRecord.class);
        verify(paymentIntentRepository).save(captor.capture());
        assertThat(captor.getValue().getBookingReferenceId()).isEqualTo(bookingReferenceId);
        assertThat(captor.getValue().getCurrency()).isEqualTo(currency);
    }

    @Test
    void aConsultantCannotCreateAPaymentIntentForAnotherConsultantFIN11() {
        authenticateAs(Role.CONSULTANT, UUID.randomUUID());
        var command = new CreatePaymentIntentCommand(UUID.randomUUID(), UUID.randomUUID(),
            new Money(BigDecimal.valueOf(1_000), CurrencyCode.INR));

        assertThatThrownBy(() -> service.createPaymentIntent(command)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(stripeClient);
    }

    @Test
    void aSucceededWebhookMarksTheRecordAndPublishesStripePaymentSucceededEventFIN11() {
        UUID bookingReferenceId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        PaymentIntentRecord record = new PaymentIntentRecord("pi_123", bookingReferenceId, consultantId,
            BigDecimal.valueOf(1_000), CurrencyCode.INR, PaymentIntentStatus.REQUIRES_PAYMENT_METHOD);
        when(paymentIntentRepository.findById("pi_123")).thenReturn(Optional.of(record));

        service.handleStripeWebhook(new HandleStripeWebhookCommand("payment_intent.succeeded", "pi_123"));

        assertThat(record.getStatus()).isEqualTo(PaymentIntentStatus.SUCCEEDED);
        ArgumentCaptor<StripePaymentSucceededEvent> eventCaptor = ArgumentCaptor.forClass(StripePaymentSucceededEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().bookingReferenceId()).isEqualTo(bookingReferenceId);
        assertThat(eventCaptor.getValue().consultantId()).isEqualTo(consultantId);
    }

    @Test
    void aFailedWebhookMarksTheRecordFailedWithoutPublishingAnEventFIN11() {
        PaymentIntentRecord record = new PaymentIntentRecord("pi_123", UUID.randomUUID(), UUID.randomUUID(),
            BigDecimal.valueOf(1_000), CurrencyCode.INR, PaymentIntentStatus.REQUIRES_PAYMENT_METHOD);
        when(paymentIntentRepository.findById("pi_123")).thenReturn(Optional.of(record));

        service.handleStripeWebhook(new HandleStripeWebhookCommand("payment_intent.payment_failed", "pi_123"));

        assertThat(record.getStatus()).isEqualTo(PaymentIntentStatus.FAILED);
        verifyNoInteractions(events);
    }

    @Test
    void rejectsAWebhookForAnUnknownPaymentIntentFIN11() {
        when(paymentIntentRepository.findById("pi_unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handleStripeWebhook(
            new HandleStripeWebhookCommand("payment_intent.succeeded", "pi_unknown")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ignoresAWebhookEventTypeItDoesNotActOnFIN11() {
        service.handleStripeWebhook(new HandleStripeWebhookCommand("charge.refunded", "pi_123"));

        verifyNoInteractions(paymentIntentRepository, events);
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
