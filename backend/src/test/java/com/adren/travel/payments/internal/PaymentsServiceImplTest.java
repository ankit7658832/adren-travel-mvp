package com.adren.travel.payments.internal;

import com.adren.travel.payments.CalculateCommissionCommand;
import com.adren.travel.payments.ConfigureMarkupCommand;
import com.adren.travel.payments.MarkupRuleView;
import com.adren.travel.payments.MarkupType;
import com.adren.travel.payments.WalletView;
import com.adren.travel.payments.event.CommissionCalculatedEvent;
import com.adren.travel.payments.event.MarkupRuleConfiguredEvent;
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
    ApplicationEventPublisher events;

    PaymentsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentsServiceImpl(markupRuleRepository, walletRepository, events);
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

    private static void authenticateAs(Role role, UUID consultantId) {
        AdrenPrincipal principal = new AdrenPrincipal(UUID.randomUUID(), role, consultantId);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
