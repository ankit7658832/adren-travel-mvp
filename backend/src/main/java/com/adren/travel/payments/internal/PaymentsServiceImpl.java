package com.adren.travel.payments.internal;

import com.adren.travel.payments.ConfigureMarkupCommand;
import com.adren.travel.payments.MarkupRuleView;
import com.adren.travel.payments.MarkupType;
import com.adren.travel.payments.PaymentsApi;
import com.adren.travel.payments.WalletView;
import com.adren.travel.payments.event.MarkupRuleConfiguredEvent;
import com.adren.travel.payments.event.WalletProvisionedEvent;
import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.shared.CurrencyCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
class PaymentsServiceImpl implements PaymentsApi {

    private final MarkupRuleRepository markupRuleRepository;
    private final WalletRepository walletRepository;
    private final ApplicationEventPublisher events;

    PaymentsServiceImpl(MarkupRuleRepository markupRuleRepository, WalletRepository walletRepository,
                         ApplicationEventPublisher events) {
        this.markupRuleRepository = markupRuleRepository;
        this.walletRepository = walletRepository;
        this.events = events;
    }

    @Override
    @Transactional
    public void configureMarkup(UUID consultantId, ConfigureMarkupCommand command) {
        UUID scopedConsultantId = CurrentPrincipal.resolveTenantScope(consultantId);
        validate(command);

        MarkupRule rule = markupRuleRepository.findByConsultantIdAndCategory(scopedConsultantId, command.category())
            .map(existing -> {
                existing.update(command.markupType(), command.percentageValue(), command.flatFeeAmount(),
                    command.flatFeeCurrency());
                return existing;
            })
            .orElseGet(() -> new MarkupRule(UUID.randomUUID(), scopedConsultantId, command.category(),
                command.markupType(), command.percentageValue(), command.flatFeeAmount(), command.flatFeeCurrency()));
        markupRuleRepository.save(rule);

        events.publishEvent(new MarkupRuleConfiguredEvent(scopedConsultantId, command.category()));
    }

    @Override
    public List<MarkupRuleView> findMarkupRules(UUID consultantId) {
        UUID scopedConsultantId = CurrentPrincipal.resolveTenantScope(consultantId);
        return markupRuleRepository.findByConsultantId(scopedConsultantId).stream()
            .map(PaymentsServiceImpl::toView)
            .toList();
    }

    @Override
    @Transactional
    public WalletView getWallet(UUID consultantId) {
        UUID scopedConsultantId = CurrentPrincipal.resolveTenantScope(consultantId);
        Wallet wallet = walletRepository.findById(scopedConsultantId)
            .orElseGet(() -> provisionWallet(scopedConsultantId));
        return toView(wallet);
    }

    private Wallet provisionWallet(UUID consultantId) {
        Wallet wallet = new Wallet(consultantId, CurrencyCode.INR);
        walletRepository.save(wallet);
        events.publishEvent(new WalletProvisionedEvent(consultantId));
        return wallet;
    }

    // PRD §12.1 — a percentage-based rule carries only percentageValue; a
    // flat-fee rule carries only flatFeeAmount/flatFeeCurrency (RULES.md
    // §4.4: an amount is never auditable without its currency).
    private static void validate(ConfigureMarkupCommand command) {
        if (command.markupType() == MarkupType.PERCENTAGE) {
            if (command.percentageValue() == null) {
                throw new IllegalArgumentException("percentageValue is required for a PERCENTAGE markup rule");
            }
        } else if (command.markupType() == MarkupType.FLAT_FEE) {
            if (command.flatFeeAmount() == null || command.flatFeeCurrency() == null) {
                throw new IllegalArgumentException(
                    "flatFeeAmount and flatFeeCurrency are required for a FLAT_FEE markup rule");
            }
        }
    }

    private static MarkupRuleView toView(MarkupRule rule) {
        return new MarkupRuleView(rule.getConsultantId(), rule.getCategory(), rule.getMarkupType(),
            rule.getPercentageValue(), rule.getFlatFeeAmount(), rule.getFlatFeeCurrency(), rule.getUpdatedAt());
    }

    private static WalletView toView(Wallet wallet) {
        return new WalletView(wallet.getConsultantId(), wallet.getAvailableBalance(), wallet.getCreditLimit(),
            wallet.getPendingHolds(), wallet.getCurrency(), wallet.getUpdatedAt());
    }
}
