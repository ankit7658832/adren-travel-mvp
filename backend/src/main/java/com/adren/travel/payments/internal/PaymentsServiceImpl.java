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
import com.adren.travel.payments.PaymentsApi;
import com.adren.travel.payments.SellRateCalculation;
import com.adren.travel.payments.SnapshotFxRateCommand;
import com.adren.travel.payments.WalletView;
import com.adren.travel.payments.event.CommissionCalculatedEvent;
import com.adren.travel.payments.event.CurrencyBufferAppliedEvent;
import com.adren.travel.payments.event.FxRateSnapshotTakenEvent;
import com.adren.travel.payments.event.MarkupRuleConfiguredEvent;
import com.adren.travel.payments.event.StripePaymentSucceededEvent;
import com.adren.travel.payments.event.WalletProvisionedEvent;
import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
class PaymentsServiceImpl implements PaymentsApi {

    private final MarkupRuleRepository markupRuleRepository;
    private final WalletRepository walletRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final ApplicationEventPublisher events;
    private final PricingPipeline pricingPipeline;
    private final StripeClient stripeClient;

    PaymentsServiceImpl(MarkupRuleRepository markupRuleRepository, WalletRepository walletRepository,
                         PaymentIntentRepository paymentIntentRepository, ApplicationEventPublisher events,
                         PricingPipeline pricingPipeline, StripeClient stripeClient) {
        this.markupRuleRepository = markupRuleRepository;
        this.walletRepository = walletRepository;
        this.paymentIntentRepository = paymentIntentRepository;
        this.events = events;
        this.pricingPipeline = pricingPipeline;
        this.stripeClient = stripeClient;
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

    @Override
    @Transactional
    public Money calculateCommission(CalculateCommissionCommand command) {
        Money commissionAmount = command.netRate().percentOf(command.commissionPercent());
        events.publishEvent(new CommissionCalculatedEvent(command.bookingId(), command.consultantId(),
            command.netRate(), commissionAmount));
        return commissionAmount;
    }

    @Override
    @Transactional
    public Money applyCurrencyBuffer(ApplyCurrencyBufferCommand command) {
        Money bufferedAmount = command.fxConvertedBase().applyMarkupPercent(command.bufferPercent());
        events.publishEvent(new CurrencyBufferAppliedEvent(command.bookingId(), command.consultantId(),
            command.fxConvertedBase(), bufferedAmount));
        return bufferedAmount;
    }

    @Override
    @Transactional
    public FxRateSnapshot snapshotFxRate(SnapshotFxRateCommand command) {
        FxRateSnapshot snapshot = new FxRateSnapshot(command.supplierCurrency(), command.sellCurrency(),
            command.rate(), Instant.now());
        events.publishEvent(new FxRateSnapshotTakenEvent(command.bookingId(), command.consultantId(), snapshot));
        return snapshot;
    }

    @Override
    @Transactional
    public SellRateCalculation calculateSellRate(CalculateSellRateCommand command) {
        return pricingPipeline.calculate(command);
    }

    @Override
    @Transactional
    public PaymentIntentView createPaymentIntent(CreatePaymentIntentCommand command) {
        UUID scopedConsultantId = CurrentPrincipal.resolveTenantScope(command.consultantId());
        StripePaymentIntent intent = stripeClient.createPaymentIntent(command.amount(), command.bookingReferenceId());

        paymentIntentRepository.save(new PaymentIntentRecord(intent.paymentIntentId(), command.bookingReferenceId(),
            scopedConsultantId, intent.amount().amount(), intent.amount().currency(), intent.status()));

        return new PaymentIntentView(intent.paymentIntentId(), intent.clientSecret(), intent.amount(), intent.status());
    }

    @Override
    @Transactional
    public void handleStripeWebhook(HandleStripeWebhookCommand command) {
        if (!"payment_intent.succeeded".equals(command.type()) && !"payment_intent.payment_failed".equals(command.type())) {
            return;
        }

        PaymentIntentRecord record = paymentIntentRepository.findById(command.paymentIntentId())
            .orElseThrow(() -> new IllegalArgumentException("No PaymentIntent: " + command.paymentIntentId()));

        if ("payment_intent.succeeded".equals(command.type())) {
            record.markAs(PaymentIntentStatus.SUCCEEDED);
            paymentIntentRepository.save(record);
            events.publishEvent(new StripePaymentSucceededEvent(record.getBookingReferenceId(),
                record.getConsultantId(), new Money(record.getAmount(), record.getCurrency())));
        } else {
            record.markAs(PaymentIntentStatus.FAILED);
            paymentIntentRepository.save(record);
        }
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
