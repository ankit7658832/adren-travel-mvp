package com.adren.travel.payments.internal;

import com.adren.travel.payments.CalculateSellRateCommand;
import com.adren.travel.payments.FxRateSnapshot;
import com.adren.travel.payments.MarkupType;
import com.adren.travel.payments.SellRateCalculation;
import com.adren.travel.payments.event.CommissionCalculatedEvent;
import com.adren.travel.payments.event.CurrencyBufferAppliedEvent;
import com.adren.travel.payments.event.FxRateSnapshotTakenEvent;
import com.adren.travel.shared.Money;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Composes the FX-snapshot (FIN-04), currency buffer (FIN-03), markup
 * (FIN-01) and commission (FIN-02) steps into one line-item sell-rate
 * calculation (PRD §12.1 Worked Examples A &amp; B, FIN-05). Implements
 * the math directly against {@code MarkupRuleRepository} and the shared
 * {@code Money} primitives, rather than calling back through
 * {@code PaymentsServiceImpl}'s own API methods, so there is one
 * {@code @Transactional} boundary (the caller's) and no self-invocation
 * proxy pitfalls.
 * <p>
 * Order: FX-convert the supplier net rate into the sell currency using the
 * locked rate, apply the currency buffer to get the adjusted base, apply
 * the Consultant's configured markup on the adjusted base to get
 * {@code sellRate}, and separately calculate Adren's commission on the
 * FX-converted (pre-buffer) net — per Worked Example A, commission is
 * deducted from the Consultant's payout, not folded into {@code sellRate}.
 */
@Component
class PricingPipeline {

    private final MarkupRuleRepository markupRuleRepository;
    private final ApplicationEventPublisher events;

    PricingPipeline(MarkupRuleRepository markupRuleRepository, ApplicationEventPublisher events) {
        this.markupRuleRepository = markupRuleRepository;
        this.events = events;
    }

    SellRateCalculation calculate(CalculateSellRateCommand command) {
        FxRateSnapshot snapshot = new FxRateSnapshot(command.netRate().currency(), command.sellCurrency(),
            command.fxRate(), Instant.now());
        events.publishEvent(new FxRateSnapshotTakenEvent(command.bookingId(), command.consultantId(), snapshot));

        Money fxConvertedBase = command.netRate().convertTo(command.sellCurrency(), command.fxRate());

        Money bufferedAmount = fxConvertedBase.applyMarkupPercent(command.bufferPercent());
        events.publishEvent(new CurrencyBufferAppliedEvent(command.bookingId(), command.consultantId(),
            fxConvertedBase, bufferedAmount));

        MarkupRule rule = markupRuleRepository
            .findByConsultantIdAndCategory(command.consultantId(), command.category())
            .orElseThrow(() -> new IllegalStateException(
                "No markup rule configured for consultant %s, category %s".formatted(
                    command.consultantId(), command.category())));
        Money markupAmount = rule.getMarkupType() == MarkupType.PERCENTAGE
            ? bufferedAmount.percentOf(rule.getPercentageValue())
            : new Money(rule.getFlatFeeAmount(), rule.getFlatFeeCurrency());
        Money sellRate = bufferedAmount.plus(markupAmount);

        Money commissionAmount = fxConvertedBase.percentOf(command.commissionPercent());
        events.publishEvent(new CommissionCalculatedEvent(command.bookingId(), command.consultantId(),
            fxConvertedBase, commissionAmount));

        return new SellRateCalculation(command.netRate(), snapshot, fxConvertedBase, bufferedAmount, markupAmount,
            sellRate, commissionAmount);
    }
}
