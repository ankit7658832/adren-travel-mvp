package com.adren.travel.payments.internal;

import com.adren.travel.payments.ApplyCurrencyBufferCommand;
import com.adren.travel.payments.CalculateCommissionCommand;
import com.adren.travel.payments.CalculateIndiaGstTcsCommand;
import com.adren.travel.payments.CalculateRefundCommand;
import com.adren.travel.payments.CalculateSellRateCommand;
import com.adren.travel.payments.ConfigureMarkupCommand;
import com.adren.travel.payments.CreatePaymentIntentCommand;
import com.adren.travel.payments.FxRateSnapshot;
import com.adren.travel.payments.HandleStripeWebhookCommand;
import com.adren.travel.payments.IndiaGstTcsCalculation;
import com.adren.travel.payments.InitiateWalletTopUpCommand;
import com.adren.travel.payments.MarkupRuleView;
import com.adren.travel.payments.MarkupType;
import com.adren.travel.payments.PaymentIntentStatus;
import com.adren.travel.payments.PaymentIntentView;
import com.adren.travel.payments.PaymentsApi;
import com.adren.travel.payments.RefundCalculation;
import com.adren.travel.payments.SellRateCalculation;
import com.adren.travel.payments.SnapshotFxRateCommand;
import com.adren.travel.payments.WalletHoldCommand;
import com.adren.travel.payments.WalletView;
import com.adren.travel.payments.event.BookingPaidOnAccountEvent;
import com.adren.travel.payments.event.CommissionCalculatedEvent;
import com.adren.travel.payments.event.CurrencyBufferAppliedEvent;
import com.adren.travel.payments.event.FxRateSnapshotTakenEvent;
import com.adren.travel.payments.event.IndiaGstTcsCalculatedEvent;
import com.adren.travel.payments.event.MarkupRuleConfiguredEvent;
import com.adren.travel.payments.event.RefundCalculatedEvent;
import com.adren.travel.payments.event.StripePaymentSucceededEvent;
import com.adren.travel.payments.event.WalletTopUpReconciledEvent;
import com.adren.travel.payments.event.WalletHoldDebitedEvent;
import com.adren.travel.payments.event.WalletHoldPlacedEvent;
import com.adren.travel.payments.event.WalletHoldReleasedEvent;
import com.adren.travel.payments.event.WalletProvisionedEvent;
import com.adren.travel.security.CurrentPrincipal;
import com.adren.travel.shared.CurrencyCode;
import com.adren.travel.shared.Money;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
class PaymentsServiceImpl implements PaymentsApi {

    private final MarkupRuleRepository markupRuleRepository;
    private final WalletRepository walletRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final WalletLedgerEntryRepository walletLedgerEntryRepository;
    private final WalletLedgerEntryRecorder walletLedgerEntryRecorder;
    private final ApplicationEventPublisher events;
    private final PricingPipeline pricingPipeline;
    private final StripeClient stripeClient;
    private final IndiaTaxProperties indiaTaxProperties;

    PaymentsServiceImpl(MarkupRuleRepository markupRuleRepository, WalletRepository walletRepository,
                         PaymentIntentRepository paymentIntentRepository,
                         WalletLedgerEntryRepository walletLedgerEntryRepository,
                         WalletLedgerEntryRecorder walletLedgerEntryRecorder, ApplicationEventPublisher events,
                         PricingPipeline pricingPipeline, StripeClient stripeClient,
                         IndiaTaxProperties indiaTaxProperties) {
        this.markupRuleRepository = markupRuleRepository;
        this.walletRepository = walletRepository;
        this.paymentIntentRepository = paymentIntentRepository;
        this.walletLedgerEntryRepository = walletLedgerEntryRepository;
        this.walletLedgerEntryRecorder = walletLedgerEntryRecorder;
        this.events = events;
        this.pricingPipeline = pricingPipeline;
        this.stripeClient = stripeClient;
        this.indiaTaxProperties = indiaTaxProperties;
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
            scopedConsultantId, intent.amount().amount(), intent.amount().currency(), intent.status(),
            PaymentIntentPurpose.BOOKING));

        return new PaymentIntentView(intent.paymentIntentId(), intent.clientSecret(), intent.amount(), intent.status());
    }

    @Override
    @Transactional
    public PaymentIntentView initiateWalletTopUp(InitiateWalletTopUpCommand command) {
        UUID scopedConsultantId = CurrentPrincipal.resolveTenantScope(command.consultantId());
        // No real booking to reference — stripeClient's signature still
        // wants a UUID (it's an opaque pass-through for the stub, see its
        // own Javadoc), so a synthetic one is fine here.
        StripePaymentIntent intent = stripeClient.createPaymentIntent(command.amount(), UUID.randomUUID());

        paymentIntentRepository.save(new PaymentIntentRecord(intent.paymentIntentId(), UUID.randomUUID(),
            scopedConsultantId, intent.amount().amount(), intent.amount().currency(), intent.status(),
            PaymentIntentPurpose.WALLET_TOP_UP));

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
            // FIN-15: Stripe retries webhooks on delay/failure (the exact
            // scenario PRD §23.4 Edge Case #10 names) — without this guard,
            // a retried "succeeded" webhook for an already-reconciled
            // top-up would credit the wallet a second time.
            boolean alreadyReconciled = record.getStatus() == PaymentIntentStatus.SUCCEEDED;
            record.markAs(PaymentIntentStatus.SUCCEEDED);
            paymentIntentRepository.save(record);
            if (alreadyReconciled) {
                return;
            }
            if (record.getPurpose() == PaymentIntentPurpose.WALLET_TOP_UP) {
                reconcileWalletTopUp(record);
            } else {
                events.publishEvent(new StripePaymentSucceededEvent(record.getBookingReferenceId(),
                    record.getConsultantId(), new Money(record.getAmount(), record.getCurrency())));
            }
        } else {
            record.markAs(PaymentIntentStatus.FAILED);
            paymentIntentRepository.save(record);
        }
    }

    // FIN-15: the ONLY place availableBalance is credited for a top-up — a
    // booking attempted against funds from a not-yet-webhooked top-up sees
    // an availableBalance that simply doesn't include them yet, so FIN-08's
    // credit-limit check blocks it structurally, not via a special case.
    private void reconcileWalletTopUp(PaymentIntentRecord record) {
        Wallet wallet = walletRepository.findById(record.getConsultantId())
            .orElseGet(() -> provisionWallet(record.getConsultantId()));
        wallet.credit(record.getAmount());
        walletRepository.save(wallet);

        WalletLedgerEntry entry = new WalletLedgerEntry(UUID.randomUUID(), record.getConsultantId(),
            LedgerEntryType.TOP_UP, record.getAmount(), record.getCurrency(), null, wallet.getAvailableBalance());
        walletLedgerEntryRepository.save(entry);

        events.publishEvent(new WalletTopUpReconciledEvent(record.getConsultantId(),
            new Money(record.getAmount(), record.getCurrency())));
    }

    @Override
    @Transactional
    public void placeHold(WalletHoldCommand command) {
        if (walletLedgerEntryRepository.existsByRelatedBookingIdAndType(command.bookingId(), LedgerEntryType.HOLD)) {
            return; // FIN-10: already recorded — idempotent no-op on a sequential retry.
        }

        Wallet wallet = walletRepository.findById(command.consultantId())
            .orElseGet(() -> provisionWallet(command.consultantId()));
        wallet.placeHold(command.amount().amount());

        if (!tryRecordAndApply(command, LedgerEntryType.HOLD, wallet)) {
            return; // FIN-10: lost a concurrent race — the other writer already recorded this hold.
        }

        events.publishEvent(new WalletHoldPlacedEvent(command.bookingId(), command.consultantId(), command.amount()));
    }

    @Override
    @Transactional
    public void resolveHoldAsDebit(WalletHoldCommand command) {
        if (walletLedgerEntryRepository.existsByRelatedBookingIdAndType(command.bookingId(), LedgerEntryType.DEBIT)) {
            return;
        }

        Wallet wallet = walletRepository.findById(command.consultantId())
            .orElseThrow(() -> new IllegalStateException("No wallet for consultant: " + command.consultantId()));
        wallet.resolveHoldAsDebit(command.amount().amount());

        if (!tryRecordAndApply(command, LedgerEntryType.DEBIT, wallet)) {
            return;
        }

        events.publishEvent(new WalletHoldDebitedEvent(command.bookingId(), command.consultantId(), command.amount()));
    }

    @Override
    @Transactional
    public void resolveHoldAsRelease(WalletHoldCommand command) {
        if (walletLedgerEntryRepository.existsByRelatedBookingIdAndType(command.bookingId(), LedgerEntryType.RELEASE)) {
            return;
        }

        Wallet wallet = walletRepository.findById(command.consultantId())
            .orElseThrow(() -> new IllegalStateException("No wallet for consultant: " + command.consultantId()));
        wallet.resolveHoldAsRelease(command.amount().amount());

        if (!tryRecordAndApply(command, LedgerEntryType.RELEASE, wallet)) {
            return;
        }

        events.publishEvent(new WalletHoldReleasedEvent(command.bookingId(), command.consultantId(), command.amount()));
    }

    @Override
    @Transactional
    public void payOnAccount(WalletHoldCommand command) {
        if (walletLedgerEntryRepository.existsByRelatedBookingIdAndType(command.bookingId(), LedgerEntryType.ON_ACCOUNT)) {
            return; // FIN-10: already recorded — idempotent no-op on a sequential retry.
        }

        Wallet wallet = walletRepository.findById(command.consultantId())
            .orElseGet(() -> provisionWallet(command.consultantId()));
        // FIN-12: deliberately no wallet.placeHold/resolveHoldAsDebit call
        // here — On-Account billing is settled later, not against wallet
        // balance/credit/pendingHolds, so tryRecordAndApply's save() below
        // just re-persists the wallet unchanged.

        if (!tryRecordAndApply(command, LedgerEntryType.ON_ACCOUNT, wallet)) {
            return; // FIN-10: lost a concurrent race — the other writer already recorded this.
        }

        events.publishEvent(new BookingPaidOnAccountEvent(command.bookingId(), command.consultantId(), command.amount()));
    }

    @Override
    @Transactional
    public RefundCalculation calculateRefund(CalculateRefundCommand command) {
        boolean cancelledBeforeDeadline = command.cancelledAt().isBefore(command.cancellationDeadline());

        Money refundAmount;
        Money penaltyAmount;
        if (cancelledBeforeDeadline) {
            refundAmount = command.sellPrice();
            penaltyAmount = Money.zero(command.sellPrice().currency());
        } else {
            penaltyAmount = command.sellPrice().percentOf(command.postDeadlinePenaltyPercent());
            refundAmount = command.sellPrice().percentOf(BigDecimal.valueOf(100).subtract(command.postDeadlinePenaltyPercent()));
        }
        boolean requiresConsultantApproval = penaltyAmount.amount().signum() > 0;

        events.publishEvent(new RefundCalculatedEvent(command.bookingId(), command.consultantId(), refundAmount,
            penaltyAmount, requiresConsultantApproval));

        return new RefundCalculation(refundAmount, penaltyAmount, requiresConsultantApproval);
    }

    @Override
    @Transactional
    public IndiaGstTcsCalculation calculateIndiaGstTcs(CalculateIndiaGstTcsCommand command) {
        Money gstAmount;
        Money tcsAmount;
        boolean applied = indiaTaxProperties.enabled();
        if (applied) {
            gstAmount = command.marginAmount().percentOf(indiaTaxProperties.gstPercent());
            boolean overThreshold = command.packageValue().amount().compareTo(indiaTaxProperties.tcsThreshold()) > 0;
            tcsAmount = overThreshold
                ? command.packageValue().percentOf(indiaTaxProperties.tcsPercent())
                : Money.zero(command.packageValue().currency());
        } else {
            // PRD §19: exact rates pending tax-counsel sign-off — never
            // silently charge the PRD's illustrative figures as if final.
            gstAmount = Money.zero(command.marginAmount().currency());
            tcsAmount = Money.zero(command.packageValue().currency());
        }

        events.publishEvent(new IndiaGstTcsCalculatedEvent(command.bookingId(), command.consultantId(), gstAmount,
            tcsAmount, applied));

        return new IndiaGstTcsCalculation(gstAmount, tcsAmount, applied);
    }

    // FIN-10: the ledger insert is attempted (and, on a unique-constraint
    // conflict, safely rejected) BEFORE the wallet mutation is persisted —
    // if we lose a concurrent race, the wallet save below never happens,
    // so a losing writer never double-applies its in-memory mutation.
    private boolean tryRecordAndApply(WalletHoldCommand command, LedgerEntryType type, Wallet wallet) {
        WalletLedgerEntry entry = new WalletLedgerEntry(UUID.randomUUID(), command.consultantId(), type,
            command.amount().amount(), command.amount().currency(), command.bookingId(), wallet.getAvailableBalance());
        if (!walletLedgerEntryRecorder.tryRecord(entry)) {
            return false;
        }
        walletRepository.save(wallet);
        return true;
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
