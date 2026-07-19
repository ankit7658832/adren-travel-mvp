package com.adren.travel.payments.internal;

import com.adren.travel.payments.ApplyCurrencyBufferCommand;
import com.adren.travel.payments.CalculateCommissionCommand;
import com.adren.travel.payments.CalculateIndiaGstTcsCommand;
import com.adren.travel.payments.CalculateRefundCommand;
import com.adren.travel.payments.CalculateSellRateCommand;
import com.adren.travel.payments.CalculateUkTomsVatCommand;
import com.adren.travel.payments.ConfigureMarkupCommand;
import com.adren.travel.payments.CreatePaymentIntentCommand;
import com.adren.travel.payments.CreditLimitExceededException;
import com.adren.travel.payments.FxRateSnapshot;
import com.adren.travel.payments.HandleStripeWebhookCommand;
import com.adren.travel.payments.IndiaGstTcsCalculation;
import com.adren.travel.payments.InitiateWalletTopUpCommand;
import com.adren.travel.payments.MarkupRuleView;
import com.adren.travel.payments.MarkupType;
import com.adren.travel.payments.PaymentIntentStatus;
import com.adren.travel.payments.PaymentIntentView;
import com.adren.travel.payments.RefundCalculation;
import com.adren.travel.payments.SellRateCalculation;
import com.adren.travel.payments.SnapshotFxRateCommand;
import com.adren.travel.payments.UkTomsVatCalculation;
import com.adren.travel.payments.WalletHoldCommand;
import com.adren.travel.payments.WalletView;
import com.adren.travel.payments.event.CommissionCalculatedEvent;
import com.adren.travel.payments.event.CurrencyBufferAppliedEvent;
import com.adren.travel.payments.event.FxRateSnapshotTakenEvent;
import com.adren.travel.payments.event.IndiaGstTcsCalculatedEvent;
import com.adren.travel.payments.event.MarkupRuleConfiguredEvent;
import com.adren.travel.payments.event.RefundCalculatedEvent;
import com.adren.travel.payments.event.StripePaymentSucceededEvent;
import com.adren.travel.payments.event.UkTomsVatCalculatedEvent;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    WalletLedgerEntryRepository walletLedgerEntryRepository;

    @Mock
    ApplicationEventPublisher events;

    @Mock
    StripeClient stripeClient;

    @Mock
    CreditThresholdBreachEventPublisher creditThresholdBreachEventPublisher;

    PaymentsServiceImpl service;

    @BeforeEach
    void setUp() {
        // Disabled by default (illustrative rates, pending tax-counsel
        // sign-off — see IndiaTaxProperties' Javadoc); FIN-17's own tests
        // construct an enabled instance explicitly where they need one.
        IndiaTaxProperties disabledIndiaTax = new IndiaTaxProperties(false, BigDecimal.valueOf(5),
            BigDecimal.valueOf(5), BigDecimal.valueOf(700_000));
        UkTomsVatProperties disabledUkTomsVat = new UkTomsVatProperties(false, BigDecimal.valueOf(20));
        service = new PaymentsServiceImpl(markupRuleRepository, walletRepository, paymentIntentRepository,
            walletLedgerEntryRepository, new WalletLedgerEntryRecorder(walletLedgerEntryRepository), events,
            new PricingPipeline(markupRuleRepository, events), stripeClient, disabledIndiaTax, disabledUkTomsVat,
            creditThresholdBreachEventPublisher);
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
    void findWalletLedgerReturnsEveryEntryWhenNoTypeFilterIsSuppliedFIN09() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        WalletLedgerEntry entry = new WalletLedgerEntry(UUID.randomUUID(), consultantId, LedgerEntryType.TOP_UP,
            BigDecimal.valueOf(1_000), CurrencyCode.INR, null, BigDecimal.valueOf(1_000));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        when(walletLedgerEntryRepository.findByConsultantId(consultantId, pageable))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(entry)));

        org.springframework.data.domain.Page<com.adren.travel.payments.WalletLedgerEntryView> page =
            service.findWalletLedger(consultantId, null, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).type()).isEqualTo("TOP_UP");
        verify(walletLedgerEntryRepository, org.mockito.Mockito.never())
            .findByConsultantIdAndType(any(), any(), any());
    }

    @Test
    void findWalletLedgerFiltersByTypeWhenSuppliedFIN09() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        WalletLedgerEntry entry = new WalletLedgerEntry(UUID.randomUUID(), consultantId, LedgerEntryType.REFUND,
            BigDecimal.valueOf(500), CurrencyCode.INR, UUID.randomUUID(), BigDecimal.valueOf(500));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        when(walletLedgerEntryRepository.findByConsultantIdAndType(consultantId, LedgerEntryType.REFUND, pageable))
            .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(entry)));

        org.springframework.data.domain.Page<com.adren.travel.payments.WalletLedgerEntryView> page =
            service.findWalletLedger(consultantId, "REFUND", pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).type()).isEqualTo("REFUND");
    }

    @Test
    void findWalletLedgerRejectsAnUnknownTypeFIN09() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);

        assertThatThrownBy(() -> service.findWalletLedger(consultantId, "NOT_A_REAL_TYPE", pageable))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aConsultantCannotQueryAnotherConsultantsLedgerFIN09() {
        UUID ownConsultantId = UUID.randomUUID();
        UUID otherConsultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, ownConsultantId);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);

        assertThatThrownBy(() -> service.findWalletLedger(otherConsultantId, null, pageable))
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
            BigDecimal.valueOf(1_000), CurrencyCode.INR, PaymentIntentStatus.REQUIRES_PAYMENT_METHOD,
            PaymentIntentPurpose.BOOKING);
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
            BigDecimal.valueOf(1_000), CurrencyCode.INR, PaymentIntentStatus.REQUIRES_PAYMENT_METHOD,
            PaymentIntentPurpose.BOOKING);
        when(paymentIntentRepository.findById("pi_123")).thenReturn(Optional.of(record));

        service.handleStripeWebhook(new HandleStripeWebhookCommand("payment_intent.payment_failed", "pi_123"));

        assertThat(record.getStatus()).isEqualTo(PaymentIntentStatus.FAILED);
        verifyNoInteractions(events);
    }

    @Test
    void initiatingAWalletTopUpCreatesAPaymentIntentWithoutCreditingTheWalletFIN15() {
        UUID consultantId = UUID.randomUUID();
        authenticateAs(Role.CONSULTANT, consultantId);
        Money amount = new Money(BigDecimal.valueOf(5_000), CurrencyCode.INR);
        when(stripeClient.createPaymentIntent(eq(amount), any())).thenReturn(
            new StripePaymentIntent("pi_topup_1", "pi_topup_1_secret", amount, PaymentIntentStatus.REQUIRES_PAYMENT_METHOD));

        PaymentIntentView intent = service.initiateWalletTopUp(new InitiateWalletTopUpCommand(consultantId, amount));

        assertThat(intent.paymentIntentId()).isNotBlank();
        // FIN-15: availableBalance is deliberately NOT touched at initiation
        // — only handleStripeWebhook's reconciliation does that.
        verifyNoInteractions(walletRepository);
        ArgumentCaptor<PaymentIntentRecord> captor = ArgumentCaptor.forClass(PaymentIntentRecord.class);
        verify(paymentIntentRepository).save(captor.capture());
        assertThat(captor.getValue().getPurpose()).isEqualTo(PaymentIntentPurpose.WALLET_TOP_UP);
    }

    @Test
    void aSucceededTopUpWebhookCreditsTheWalletAndPublishesWalletTopUpReconciledEventFIN15() {
        UUID consultantId = UUID.randomUUID();
        Wallet wallet = new Wallet(consultantId, CurrencyCode.INR);
        when(walletRepository.findById(consultantId)).thenReturn(Optional.of(wallet));
        PaymentIntentRecord record = new PaymentIntentRecord("pi_topup", UUID.randomUUID(), consultantId,
            BigDecimal.valueOf(5_000), CurrencyCode.INR, PaymentIntentStatus.REQUIRES_PAYMENT_METHOD,
            PaymentIntentPurpose.WALLET_TOP_UP);
        when(paymentIntentRepository.findById("pi_topup")).thenReturn(Optional.of(record));

        service.handleStripeWebhook(new HandleStripeWebhookCommand("payment_intent.succeeded", "pi_topup"));

        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("5000");
        verify(walletRepository).save(wallet);
        ArgumentCaptor<WalletLedgerEntry> entryCaptor = ArgumentCaptor.forClass(WalletLedgerEntry.class);
        verify(walletLedgerEntryRepository).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getType()).isEqualTo(LedgerEntryType.TOP_UP);

        ArgumentCaptor<WalletTopUpReconciledEvent> eventCaptor = ArgumentCaptor.forClass(WalletTopUpReconciledEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().consultantId()).isEqualTo(consultantId);
        assertThat(eventCaptor.getValue().amount().amount()).isEqualByComparingTo("5000");
    }

    @Test
    void aRetriedSucceededWebhookDoesNotCreditTheWalletTwiceFIN15() {
        UUID consultantId = UUID.randomUUID();
        PaymentIntentRecord record = new PaymentIntentRecord("pi_topup", UUID.randomUUID(), consultantId,
            BigDecimal.valueOf(5_000), CurrencyCode.INR, PaymentIntentStatus.SUCCEEDED, // already reconciled
            PaymentIntentPurpose.WALLET_TOP_UP);
        when(paymentIntentRepository.findById("pi_topup")).thenReturn(Optional.of(record));

        service.handleStripeWebhook(new HandleStripeWebhookCommand("payment_intent.succeeded", "pi_topup"));

        // FIN-15: the whole point of the guard is that reconcileWalletTopUp
        // never runs on a retry — no wallet lookup, no ledger write, no event.
        verifyNoInteractions(walletRepository, events);
        verify(walletLedgerEntryRepository, org.mockito.Mockito.never()).save(any());
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

    @Test
    void placeHoldIncreasesPendingHoldsAndPublishesTheEventFIN07() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Wallet wallet = new Wallet(consultantId, CurrencyCode.INR);
        wallet.grantCreditLimit(BigDecimal.valueOf(20_000)); // FIN-08: within credit limit, not a breach case
        when(walletRepository.findById(consultantId)).thenReturn(Optional.of(wallet));
        Money amount = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);

        service.placeHold(new WalletHoldCommand(bookingId, consultantId, amount));

        assertThat(wallet.getPendingHolds()).isEqualByComparingTo("11500");
        verify(walletRepository).save(wallet);

        ArgumentCaptor<WalletLedgerEntry> entryCaptor = ArgumentCaptor.forClass(WalletLedgerEntry.class);
        verify(walletLedgerEntryRepository).saveAndFlush(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getType()).isEqualTo(LedgerEntryType.HOLD);
        assertThat(entryCaptor.getValue().getRelatedBookingId()).isEqualTo(bookingId);

        ArgumentCaptor<WalletHoldPlacedEvent> eventCaptor = ArgumentCaptor.forClass(WalletHoldPlacedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().bookingId()).isEqualTo(bookingId);
        assertThat(eventCaptor.getValue().amount()).isEqualTo(amount);
    }

    @Test
    void placeHoldAutoProvisionsAWalletWhenNoneExistsFIN07() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        when(walletRepository.findById(consultantId)).thenReturn(Optional.empty());
        // FIN-08: a freshly auto-provisioned wallet starts at zero balance
        // AND zero credit limit — a non-zero amount would now correctly
        // trip the credit-limit check (see placeHoldIsBlockedWhenItWouldExceedAvailableCreditFIN08),
        // so this test uses a zero amount to isolate what it actually
        // verifies: that provisioning happens, not the credit check.
        Money amount = Money.zero(CurrencyCode.INR);

        service.placeHold(new WalletHoldCommand(bookingId, consultantId, amount));

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, org.mockito.Mockito.atLeastOnce()).save(walletCaptor.capture());
        assertThat(walletCaptor.getValue().getPendingHolds()).isEqualByComparingTo("0");
    }

    @Test
    void placeHoldIsBlockedWhenItWouldExceedAvailableCreditFIN08() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Wallet wallet = new Wallet(consultantId, CurrencyCode.INR); // zero balance, zero credit limit
        when(walletRepository.findById(consultantId)).thenReturn(Optional.of(wallet));
        Money amount = new Money(BigDecimal.valueOf(1_000), CurrencyCode.INR);

        assertThatThrownBy(() -> service.placeHold(new WalletHoldCommand(bookingId, consultantId, amount)))
            .isInstanceOf(CreditLimitExceededException.class);

        assertThat(wallet.getPendingHolds()).isEqualByComparingTo("0"); // rejected, not partially applied
        verify(walletRepository, org.mockito.Mockito.never()).save(any());
        verify(walletLedgerEntryRepository, org.mockito.Mockito.never()).saveAndFlush(any());
        verifyNoInteractions(events);
        // HRD-02: the notification-trigger event still fires, via its own
        // REQUIRES_NEW-transactional publisher — never through `events`
        // directly, since that would roll back with this (blocked) transaction.
        verify(creditThresholdBreachEventPublisher).publish(bookingId, consultantId, amount);
    }

    @Test
    void placeHoldIsBlockedWhenItWouldExceedTheCombinedBalancePlusCreditFIN08() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Wallet wallet = new Wallet(consultantId, CurrencyCode.INR);
        wallet.credit(BigDecimal.valueOf(300));
        wallet.grantCreditLimit(BigDecimal.valueOf(200)); // 500 total available
        when(walletRepository.findById(consultantId)).thenReturn(Optional.of(wallet));
        Money amount = new Money(BigDecimal.valueOf(501), CurrencyCode.INR); // 1 over the limit

        assertThatThrownBy(() -> service.placeHold(new WalletHoldCommand(bookingId, consultantId, amount)))
            .isInstanceOf(CreditLimitExceededException.class);
    }

    @Test
    void placeHoldSucceedsWhenExactlyAtTheCombinedBalancePlusCreditBoundaryFIN08() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Wallet wallet = new Wallet(consultantId, CurrencyCode.INR);
        wallet.credit(BigDecimal.valueOf(300));
        wallet.grantCreditLimit(BigDecimal.valueOf(200));
        when(walletRepository.findById(consultantId)).thenReturn(Optional.of(wallet));
        Money amount = new Money(BigDecimal.valueOf(500), CurrencyCode.INR); // exactly at the boundary

        service.placeHold(new WalletHoldCommand(bookingId, consultantId, amount));

        assertThat(wallet.getPendingHolds()).isEqualByComparingTo("500");
        verifyNoInteractions(creditThresholdBreachEventPublisher);
    }

    @Test
    void placeHoldIsANoOpWhenAHoldAlreadyExistsForTheBookingFIN10() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        when(walletLedgerEntryRepository.existsByRelatedBookingIdAndType(bookingId, LedgerEntryType.HOLD))
            .thenReturn(true);
        Money amount = new Money(BigDecimal.valueOf(1_000), CurrencyCode.INR);

        service.placeHold(new WalletHoldCommand(bookingId, consultantId, amount));

        verifyNoInteractions(walletRepository);
        verify(walletLedgerEntryRepository, org.mockito.Mockito.never()).saveAndFlush(any());
        verifyNoInteractions(events);
    }

    @Test
    void placeHoldLosingAConcurrentRaceDoesNotSaveTheWalletOrPublishFIN10() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Wallet wallet = new Wallet(consultantId, CurrencyCode.INR);
        wallet.grantCreditLimit(BigDecimal.valueOf(20_000)); // FIN-08: within credit limit, not a breach case
        when(walletRepository.findById(consultantId)).thenReturn(Optional.of(wallet));
        when(walletLedgerEntryRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));
        Money amount = new Money(BigDecimal.valueOf(1_000), CurrencyCode.INR);

        service.placeHold(new WalletHoldCommand(bookingId, consultantId, amount));

        // The in-memory wallet mutation happened (placeHold ran before the
        // ledger-insert attempt), but since we lost the race, it must
        // never be persisted — the concurrent winner already applied it.
        verify(walletRepository, org.mockito.Mockito.never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void resolveHoldAsDebitDecreasesPendingHoldsAndAvailableBalanceFIN07() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Wallet wallet = new Wallet(consultantId, CurrencyCode.INR);
        wallet.grantCreditLimit(BigDecimal.valueOf(20_000)); // FIN-08: within credit limit, not a breach case
        wallet.placeHold(BigDecimal.valueOf(11_500));
        when(walletRepository.findById(consultantId)).thenReturn(Optional.of(wallet));
        Money amount = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);

        service.resolveHoldAsDebit(new WalletHoldCommand(bookingId, consultantId, amount));

        assertThat(wallet.getPendingHolds()).isEqualByComparingTo("0");
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("-11500");

        ArgumentCaptor<WalletHoldDebitedEvent> eventCaptor = ArgumentCaptor.forClass(WalletHoldDebitedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().bookingId()).isEqualTo(bookingId);
    }

    @Test
    void resolveHoldAsDebitFailsWhenNoWalletExistsFIN07() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        when(walletRepository.findById(consultantId)).thenReturn(Optional.empty());
        Money amount = new Money(BigDecimal.valueOf(1_000), CurrencyCode.INR);

        assertThatThrownBy(() -> service.resolveHoldAsDebit(new WalletHoldCommand(bookingId, consultantId, amount)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void resolveHoldAsReleaseDecreasesOnlyPendingHoldsFIN07() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Wallet wallet = new Wallet(consultantId, CurrencyCode.INR);
        wallet.grantCreditLimit(BigDecimal.valueOf(20_000)); // FIN-08: within credit limit, not a breach case
        wallet.placeHold(BigDecimal.valueOf(500));
        when(walletRepository.findById(consultantId)).thenReturn(Optional.of(wallet));
        Money amount = new Money(BigDecimal.valueOf(500), CurrencyCode.INR);

        service.resolveHoldAsRelease(new WalletHoldCommand(bookingId, consultantId, amount));

        assertThat(wallet.getPendingHolds()).isEqualByComparingTo("0");
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("0");

        ArgumentCaptor<WalletHoldReleasedEvent> eventCaptor = ArgumentCaptor.forClass(WalletHoldReleasedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().bookingId()).isEqualTo(bookingId);
    }

    @Test
    void payOnAccountRecordsALedgerEntryWithoutTouchingWalletBalanceOrPublishesTheEventFIN12() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Wallet wallet = new Wallet(consultantId, CurrencyCode.INR); // zero balance, zero credit limit
        when(walletRepository.findById(consultantId)).thenReturn(Optional.of(wallet));
        Money amount = new Money(BigDecimal.valueOf(11_500), CurrencyCode.INR);

        service.payOnAccount(new WalletHoldCommand(bookingId, consultantId, amount));

        // FIN-12: never gated by FIN-08's credit-limit check, and never
        // touches balance/pendingHolds — a zero-zero wallet still succeeds.
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("0");
        assertThat(wallet.getPendingHolds()).isEqualByComparingTo("0");
        verify(walletRepository).save(wallet);

        ArgumentCaptor<WalletLedgerEntry> entryCaptor = ArgumentCaptor.forClass(WalletLedgerEntry.class);
        verify(walletLedgerEntryRepository).saveAndFlush(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getType()).isEqualTo(LedgerEntryType.ON_ACCOUNT);
        assertThat(entryCaptor.getValue().getRelatedBookingId()).isEqualTo(bookingId);

        ArgumentCaptor<com.adren.travel.payments.event.BookingPaidOnAccountEvent> eventCaptor =
            ArgumentCaptor.forClass(com.adren.travel.payments.event.BookingPaidOnAccountEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().bookingId()).isEqualTo(bookingId);
        assertThat(eventCaptor.getValue().amount()).isEqualTo(amount);
    }

    @Test
    void payOnAccountAutoProvisionsAWalletWhenNoneExistsFIN12() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        when(walletRepository.findById(consultantId)).thenReturn(Optional.empty());
        Money amount = new Money(BigDecimal.valueOf(1_000), CurrencyCode.INR);

        service.payOnAccount(new WalletHoldCommand(bookingId, consultantId, amount));

        verify(walletRepository, org.mockito.Mockito.atLeastOnce()).save(any());
    }

    @Test
    void payOnAccountIsANoOpWhenAnEntryAlreadyExistsForTheBookingFIN10() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        when(walletLedgerEntryRepository.existsByRelatedBookingIdAndType(bookingId, LedgerEntryType.ON_ACCOUNT))
            .thenReturn(true);
        Money amount = new Money(BigDecimal.valueOf(1_000), CurrencyCode.INR);

        service.payOnAccount(new WalletHoldCommand(bookingId, consultantId, amount));

        verifyNoInteractions(walletRepository);
        verify(walletLedgerEntryRepository, org.mockito.Mockito.never()).saveAndFlush(any());
        verifyNoInteractions(events);
    }

    @Test
    void processRefundCreditsTheWalletAndPublishesTheEventFIN16() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Wallet wallet = new Wallet(consultantId, CurrencyCode.INR); // zero balance, zero credit limit
        when(walletRepository.findById(consultantId)).thenReturn(Optional.of(wallet));
        Money amount = new Money(BigDecimal.valueOf(7_000), CurrencyCode.INR);

        service.processRefund(new WalletHoldCommand(bookingId, consultantId, amount));

        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("7000");
        verify(walletRepository).save(wallet);

        ArgumentCaptor<WalletLedgerEntry> entryCaptor = ArgumentCaptor.forClass(WalletLedgerEntry.class);
        verify(walletLedgerEntryRepository).saveAndFlush(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getType()).isEqualTo(LedgerEntryType.REFUND);
        assertThat(entryCaptor.getValue().getRelatedBookingId()).isEqualTo(bookingId);

        ArgumentCaptor<com.adren.travel.payments.event.RefundProcessedEvent> eventCaptor =
            ArgumentCaptor.forClass(com.adren.travel.payments.event.RefundProcessedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().bookingId()).isEqualTo(bookingId);
        assertThat(eventCaptor.getValue().amount()).isEqualTo(amount);
    }

    @Test
    void processRefundAutoProvisionsAWalletWhenNoneExistsFIN16() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        when(walletRepository.findById(consultantId)).thenReturn(Optional.empty());
        Money amount = new Money(BigDecimal.valueOf(500), CurrencyCode.INR);

        service.processRefund(new WalletHoldCommand(bookingId, consultantId, amount));

        verify(walletRepository, org.mockito.Mockito.atLeastOnce()).save(any());
    }

    @Test
    void processRefundIsANoOpWhenAnEntryAlreadyExistsForTheBookingFIN10() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        when(walletLedgerEntryRepository.existsByRelatedBookingIdAndType(bookingId, LedgerEntryType.REFUND))
            .thenReturn(true);
        Money amount = new Money(BigDecimal.valueOf(500), CurrencyCode.INR);

        service.processRefund(new WalletHoldCommand(bookingId, consultantId, amount));

        verifyNoInteractions(walletRepository);
        verify(walletLedgerEntryRepository, org.mockito.Mockito.never()).saveAndFlush(any());
        verifyNoInteractions(events);
    }

    @Test
    void calculateRefundReturnsAFullRefundWhenCancelledBeforeTheDeadlineFIN13() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Money sellPrice = new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR);
        Instant deadline = Instant.now().plusSeconds(3600);
        Instant cancelledAt = Instant.now();
        FxRateSnapshot originalFxRateSnapshot = new FxRateSnapshot(CurrencyCode.USD, CurrencyCode.INR,
            BigDecimal.valueOf(80), Instant.now().minusSeconds(7200));

        RefundCalculation calculation = service.calculateRefund(new CalculateRefundCommand(
            bookingId, consultantId, sellPrice, deadline, cancelledAt, BigDecimal.valueOf(50), originalFxRateSnapshot));

        assertThat(calculation.refundAmount()).isEqualTo(sellPrice);
        assertThat(calculation.penaltyAmount().amount()).isEqualByComparingTo("0");
        assertThat(calculation.requiresConsultantApproval()).isFalse();

        ArgumentCaptor<RefundCalculatedEvent> captor = ArgumentCaptor.forClass(RefundCalculatedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().requiresConsultantApproval()).isFalse();
    }

    @Test
    void calculateRefundAppliesThePenaltyAndRequiresApprovalWhenCancelledAfterTheDeadlineFIN13() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Money sellPrice = new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR);
        Instant deadline = Instant.now().minusSeconds(3600); // already past
        Instant cancelledAt = Instant.now();
        FxRateSnapshot originalFxRateSnapshot = new FxRateSnapshot(CurrencyCode.USD, CurrencyCode.INR,
            BigDecimal.valueOf(80), Instant.now().minusSeconds(7200));

        RefundCalculation calculation = service.calculateRefund(new CalculateRefundCommand(
            bookingId, consultantId, sellPrice, deadline, cancelledAt, BigDecimal.valueOf(30), originalFxRateSnapshot));

        assertThat(calculation.penaltyAmount().amount()).isEqualByComparingTo("3000.00");
        assertThat(calculation.refundAmount().amount()).isEqualByComparingTo("7000.00");
        assertThat(calculation.requiresConsultantApproval()).isTrue();

        ArgumentCaptor<RefundCalculatedEvent> captor = ArgumentCaptor.forClass(RefundCalculatedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().requiresConsultantApproval()).isTrue();
    }

    @Test
    void calculateRefundNeverMovesMoneyFIN13() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Money sellPrice = new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR);
        FxRateSnapshot originalFxRateSnapshot = new FxRateSnapshot(CurrencyCode.USD, CurrencyCode.INR,
            BigDecimal.valueOf(80), Instant.now().minusSeconds(7200));

        service.calculateRefund(new CalculateRefundCommand(
            bookingId, consultantId, sellPrice, Instant.now().minusSeconds(1), Instant.now(), BigDecimal.valueOf(20),
            originalFxRateSnapshot));

        verifyNoInteractions(walletRepository, walletLedgerEntryRepository);
    }

    @Test
    void calculateRefundRejectsAnOutOfRangePenaltyPercentFIN13() {
        Money sellPrice = new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR);
        FxRateSnapshot originalFxRateSnapshot = new FxRateSnapshot(CurrencyCode.USD, CurrencyCode.INR,
            BigDecimal.valueOf(80), Instant.now().minusSeconds(7200));

        assertThatThrownBy(() -> new CalculateRefundCommand(
            UUID.randomUUID(), UUID.randomUUID(), sellPrice, Instant.now(), Instant.now(), BigDecimal.valueOf(150),
            originalFxRateSnapshot))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculateRefundConvertsUsingTheOriginalFxSnapshotRateNotAFreshOneFIN14() {
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Money sellPrice = new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR);
        // Original rate locked at booking time: 1 USD = 80 INR.
        FxRateSnapshot originalFxRateSnapshot = new FxRateSnapshot(CurrencyCode.USD, CurrencyCode.INR,
            BigDecimal.valueOf(80), Instant.now().minusSeconds(7200));

        RefundCalculation calculation = service.calculateRefund(new CalculateRefundCommand(
            bookingId, consultantId, sellPrice, Instant.now().plusSeconds(3600), Instant.now(),
            BigDecimal.valueOf(50), originalFxRateSnapshot));

        // Full refund (10,000 INR) converted back at the ORIGINAL 80 rate = 125.00 USD,
        // regardless of whatever the market rate might be today — there is no
        // "current rate" input anywhere on this command for it to have used instead.
        assertThat(calculation.refundAmountInSupplierCurrency().currency()).isEqualTo(CurrencyCode.USD);
        assertThat(calculation.refundAmountInSupplierCurrency().amount()).isEqualByComparingTo("125.00");
    }

    @Test
    void calculateRefundRejectsASnapshotWhoseSellCurrencyDoesNotMatchTheRefundFIN14() {
        Money sellPrice = new Money(BigDecimal.valueOf(10_000), CurrencyCode.INR);
        FxRateSnapshot mismatchedSnapshot = new FxRateSnapshot(CurrencyCode.USD, CurrencyCode.GBP,
            BigDecimal.valueOf(80), Instant.now().minusSeconds(7200));

        assertThatThrownBy(() -> new CalculateRefundCommand(
            UUID.randomUUID(), UUID.randomUUID(), sellPrice, Instant.now(), Instant.now(), BigDecimal.valueOf(20),
            mismatchedSnapshot))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculateIndiaGstTcsAppliesNothingWhenTheFeatureFlagIsOffFIN17() {
        // Default `service` from setUp() is built with the flag disabled.
        Money margin = new Money(BigDecimal.valueOf(50_000), CurrencyCode.INR);
        Money packageValue = new Money(BigDecimal.valueOf(1_000_000), CurrencyCode.INR);

        IndiaGstTcsCalculation calculation = service.calculateIndiaGstTcs(
            new CalculateIndiaGstTcsCommand(UUID.randomUUID(), UUID.randomUUID(), margin, packageValue));

        assertThat(calculation.applied()).isFalse();
        assertThat(calculation.gstAmount().amount()).isEqualByComparingTo("0");
        assertThat(calculation.tcsAmount().amount()).isEqualByComparingTo("0");
    }

    @Test
    void calculateIndiaGstTcsAppliesGstOnMarginAndTcsOnPackageValueAboveThresholdWhenEnabledFIN17() {
        IndiaTaxProperties enabledIndiaTax = new IndiaTaxProperties(true, BigDecimal.valueOf(5),
            BigDecimal.valueOf(5), BigDecimal.valueOf(700_000));
        UkTomsVatProperties disabledUkTomsVat = new UkTomsVatProperties(false, BigDecimal.valueOf(20));
        PaymentsServiceImpl enabledService = new PaymentsServiceImpl(markupRuleRepository, walletRepository,
            paymentIntentRepository, walletLedgerEntryRepository,
            new WalletLedgerEntryRecorder(walletLedgerEntryRepository), events,
            new PricingPipeline(markupRuleRepository, events), stripeClient, enabledIndiaTax, disabledUkTomsVat,
            creditThresholdBreachEventPublisher);
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Money margin = new Money(BigDecimal.valueOf(50_000), CurrencyCode.INR);
        Money packageValue = new Money(BigDecimal.valueOf(1_000_000), CurrencyCode.INR); // over the 700,000 threshold

        IndiaGstTcsCalculation calculation = enabledService.calculateIndiaGstTcs(
            new CalculateIndiaGstTcsCommand(bookingId, consultantId, margin, packageValue));

        assertThat(calculation.applied()).isTrue();
        assertThat(calculation.gstAmount().amount()).isEqualByComparingTo("2500.00"); // 5% of margin
        assertThat(calculation.tcsAmount().amount()).isEqualByComparingTo("50000.00"); // 5% of package value

        ArgumentCaptor<IndiaGstTcsCalculatedEvent> captor = ArgumentCaptor.forClass(IndiaGstTcsCalculatedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().applied()).isTrue();
    }

    @Test
    void calculateIndiaGstTcsAppliesNoTcsBelowTheThresholdEvenWhenEnabledFIN17() {
        IndiaTaxProperties enabledIndiaTax = new IndiaTaxProperties(true, BigDecimal.valueOf(5),
            BigDecimal.valueOf(5), BigDecimal.valueOf(700_000));
        UkTomsVatProperties disabledUkTomsVat = new UkTomsVatProperties(false, BigDecimal.valueOf(20));
        PaymentsServiceImpl enabledService = new PaymentsServiceImpl(markupRuleRepository, walletRepository,
            paymentIntentRepository, walletLedgerEntryRepository,
            new WalletLedgerEntryRecorder(walletLedgerEntryRepository), events,
            new PricingPipeline(markupRuleRepository, events), stripeClient, enabledIndiaTax, disabledUkTomsVat,
            creditThresholdBreachEventPublisher);
        Money margin = new Money(BigDecimal.valueOf(20_000), CurrencyCode.INR);
        Money packageValue = new Money(BigDecimal.valueOf(500_000), CurrencyCode.INR); // under the threshold

        IndiaGstTcsCalculation calculation = enabledService.calculateIndiaGstTcs(
            new CalculateIndiaGstTcsCommand(UUID.randomUUID(), UUID.randomUUID(), margin, packageValue));

        assertThat(calculation.gstAmount().amount()).isEqualByComparingTo("1000.00"); // GST still applies to margin
        assertThat(calculation.tcsAmount().amount()).isEqualByComparingTo("0"); // TCS does not, below the threshold
    }

    @Test
    void calculateUkTomsVatAppliesNothingWhenTheFeatureFlagIsOffFIN18() {
        // Default `service` from setUp() is built with the flag disabled.
        Money margin = new Money(BigDecimal.valueOf(1_000), CurrencyCode.GBP);

        UkTomsVatCalculation calculation = service.calculateUkTomsVat(
            new CalculateUkTomsVatCommand(UUID.randomUUID(), UUID.randomUUID(), margin));

        assertThat(calculation.applied()).isFalse();
        assertThat(calculation.vatAmount().amount()).isEqualByComparingTo("0");
    }

    @Test
    void calculateUkTomsVatAppliesVatOnMarginOnlyWhenEnabledFIN18() {
        UkTomsVatProperties enabledUkTomsVat = new UkTomsVatProperties(true, BigDecimal.valueOf(20));
        IndiaTaxProperties disabledIndiaTax = new IndiaTaxProperties(false, BigDecimal.valueOf(5),
            BigDecimal.valueOf(5), BigDecimal.valueOf(700_000));
        PaymentsServiceImpl enabledService = new PaymentsServiceImpl(markupRuleRepository, walletRepository,
            paymentIntentRepository, walletLedgerEntryRepository,
            new WalletLedgerEntryRecorder(walletLedgerEntryRepository), events,
            new PricingPipeline(markupRuleRepository, events), stripeClient, disabledIndiaTax, enabledUkTomsVat,
            creditThresholdBreachEventPublisher);
        UUID bookingId = UUID.randomUUID();
        UUID consultantId = UUID.randomUUID();
        Money margin = new Money(BigDecimal.valueOf(1_000), CurrencyCode.GBP);

        UkTomsVatCalculation calculation = enabledService.calculateUkTomsVat(
            new CalculateUkTomsVatCommand(bookingId, consultantId, margin));

        assertThat(calculation.applied()).isTrue();
        assertThat(calculation.vatAmount().amount()).isEqualByComparingTo("200.00"); // 20% of margin only

        ArgumentCaptor<UkTomsVatCalculatedEvent> captor = ArgumentCaptor.forClass(UkTomsVatCalculatedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().applied()).isTrue();
        assertThat(captor.getValue().vatAmount().amount()).isEqualByComparingTo("200.00");
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
